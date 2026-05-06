package de.pottgames.videocompressor.engine;

import de.pottgames.videocompressor.i18n.I18n;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates the preparation phase of video processing: probes source videos,
 * computes final editing presets via {@link Strategy}, and creates
 * {@link VideoJob} instances ready for encoding.
 *
 * <p>All work is performed asynchronously on virtual threads. Progress and
 * log events are dispatched to the provided {@link JobProgressListener}.
 * Callers (typically the UI layer) are responsible for marshalling callbacks
 * back to the JavaFX Application Thread if needed.</p>
 */
public class JobProcessor {

    private static final ExecutorService EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    // Export path is dynamically resolved via ExportPathResolver
    // (user video folder → user home → temp → current dir fallback)

    /**
     * Set to true when the user cancels processing.
     * Checked in both preparation and execution loops to allow
     * graceful early termination between jobs.
     */
    private volatile boolean isCancelled;

    /**
     * Asynchronously prepares a list of {@link VideoJob}s from the given
     * source files and user-selected preset.
     *
     * <p>For each source file this method:</p>
     * <ol>
     *   <li>Probes the video metadata via ffprobe</li>
     *   <li>Computes the final editing preset via
     *       {@link Strategy#calcEditingPreset(ProbeInfo, Preset)}</li>
     *   <li>Determines the output file path inside the export folder, using
     *       the (potentially adjusted) container extension from the
     *       editing preset</li>
     *   <li>Creates a {@link VideoJob} with a fresh {@link VideoJobStatus}</li>
     * </ol>
     *
     * @param sourceFiles  the imported video files to process
     * @param preset       the user-selected preset (may be adjusted per-file)
     * @param listener     receives progress callbacks during preparation
     * @return a future that completes with the list of prepared VideoJobs,
     *         or fails with a RuntimeException if preparation fails
     */
    public CompletableFuture<List<VideoJob>> prepareJobs(
        List<File> sourceFiles,
        Preset preset,
        JobProgressListener listener
    ) {
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException(I18n.get("job.no_source_files"))
            );
        }
        if (preset == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException(I18n.get("job.no_preset_selected"))
            );
        }
        if (listener == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException(I18n.get("job.listener_null"))
            );
        }

        return CompletableFuture.supplyAsync(
            () -> {
                List<VideoJob> jobs = new ArrayList<>();
                int total = sourceFiles.size();

                listener.onPreparationStarted(total);

                try {
                    for (int i = 0; i < total; i++) {
                        // Check cancellation between files
                        if (isCancelled) {
                            listener.onLog(I18n.get("job.cancelled_log"));
                            listener.onCancelled();
                            Ffprobe.cancel();
                            Ffmpeg.resetCancelled();
                            isCancelled = false;
                            throw new RuntimeException(
                                I18n.get("job.cancelled")
                            );
                        }

                        File file = sourceFiles.get(i);
                        int current = i + 1;
                        String fileName = file.getName();

                        // Step 1: Probe
                        listener.onPreparationProgress(
                            current,
                            total,
                            fileName,
                            I18n.get("job.analyzing_video")
                        );
                        ProbeInfo probeInfo = Ffprobe.probe(file);

                        // Check cancellation after probe (probe can be slow)
                        if (isCancelled) {
                            listener.onLog(I18n.get("job.cancelled_log"));
                            listener.onCancelled();
                            Ffprobe.cancel();
                            Ffmpeg.resetCancelled();
                            isCancelled = false;
                            throw new RuntimeException(
                                I18n.get("job.cancelled")
                            );
                        }

                        // Step 2: Compute editing preset via Strategy
                        listener.onPreparationProgress(
                            current,
                            total,
                            fileName,
                            I18n.get("job.computing_preset")
                        );
                        Preset editingPreset = Strategy.calcEditingPreset(
                            probeInfo,
                            preset
                        );

                        // Step 3: Determine output file path
                        File outputFile = buildOutputFile(file, editingPreset);

                        // Step 4: Create VideoJob
                        VideoJobStatus status = new VideoJobStatus();
                        // Set total duration from probe info for progress tracking
                        status.setTotalDurationMs(
                            (long) (probeInfo.duration() * 1000)
                        );
                        VideoJob job = new VideoJob(
                            outputFile,
                            probeInfo,
                            editingPreset,
                            status
                        );
                        jobs.add(job);
                    }

                    listener.onPreparationCompleted(jobs.size());
                    return jobs;
                } catch (Exception e) {
                    String errorMsg = I18n.get(
                        "job.preparation_error",
                        e.getMessage()
                    );
                    listener.onLog(I18n.get("job.error_log_prefix", errorMsg));
                    listener.onPreparationFailed(errorMsg);
                    throw new RuntimeException(errorMsg, e);
                }
            },
            EXECUTOR
        );
    }

    /**
     * Result of processing a single {@link VideoJob}.
     */
    private enum JobResult {
        SUCCESS,
        FAILURE,
    }

    /**
     * Asynchronously executes the given list of {@link VideoJob}s one by one.
     *
     * <p>For each job this method:</p>
     * <ol>
     *   <li>Builds the FFmpeg command line via {@link FfmpegCommandBuilder}</li>
     *   <li>Executes FFmpeg synchronously, parsing progress events</li>
     *   <li>Updates the {@link VideoJobStatus} and dispatches callbacks to the listener</li>
     * </ol>
     *
     * <p>Jobs are processed sequentially so that the UI can show clear per-video
     * progress. All work runs on virtual threads.</p>
     *
     * @param jobs     the prepared VideoJobs to execute
     * @param listener receives progress callbacks during execution
     * @return a future that completes when all jobs are done
     */
    public CompletableFuture<Void> executeJobs(
        List<VideoJob> jobs,
        JobProgressListener listener
    ) {
        if (jobs == null || jobs.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException(I18n.get("job.no_jobs"))
            );
        }
        if (listener == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException(I18n.get("job.listener_null"))
            );
        }

        return CompletableFuture.runAsync(
            () -> {
                int total = jobs.size();
                int completed = 0;
                int failed = 0;

                listener.onLog(I18n.get("job.processing_started", total));

                for (int i = 0; i < total; i++) {
                    // Check cancellation between jobs
                    if (isCancelled) {
                        listener.onLog(I18n.get("job.cancelled_log"));
                        listener.onCancelled();
                        Ffmpeg.cancel();
                        Ffmpeg.resetCancelled();
                        isCancelled = false;
                        break;
                    }

                    VideoJob job = jobs.get(i);
                    JobResult result = processJob(i, total, job, listener);

                    if (result == JobResult.SUCCESS) {
                        completed++;
                    } else {
                        failed++;
                    }
                }

                listener.onAllJobsCompleted(completed, failed);
            },
            EXECUTOR
        );
    }

    /**
     * Processes a single VideoJob by building the FFmpeg command, executing it,
     * and updating the job status based on progress events.
     *
     * <p>This method is intentionally synchronous with respect to FFmpeg execution
     * so that progress events are delivered in order and the UI can track
     * per-video progress cleanly.</p>
     *
     * @param index    0-based index of the job
     * @param total    total number of jobs
     * @param job      the VideoJob to process
     * @param listener receives progress callbacks
     * @return JobResult indicating success or failure
     */
    private JobResult processJob(
        int index,
        int total,
        VideoJob job,
        JobProgressListener listener
    ) {
        VideoJobStatus status = job.status();
        ProbeInfo probeInfo = job.sourceInfo();
        Preset preset = job.preset();
        File outputFile = job.outputFile();

        // Notify UI that this job started
        status.setStatus(VideoJobStatus.Status.RUNNING);
        listener.onJobStarted(index, total, probeInfo.file(), outputFile);

        // Delete existing output file to prevent FFmpeg from getting stuck
        if (outputFile.exists()) {
            listener.onLog(
                I18n.get("job.output_exists_deleting", outputFile.getName())
            );
            if (!outputFile.delete()) {
                listener.onLog(
                    I18n.get("job.output_delete_failed", outputFile.getName())
                );
            }
        }

        // Build FFmpeg command
        Path outputDir =
            outputFile.getParentFile() != null
                ? outputFile.getParentFile().toPath()
                : ExportPathResolver.getExportPath();
        List<String> cmd = FfmpegCommandBuilder.buildCommand(
            probeInfo,
            preset,
            outputDir
        );

        // Execute FFmpeg and handle events
        try {
            Ffmpeg.execute(cmd, event -> {
                switch (event) {
                    case FfmpegStartEvent(_, _) -> {
                        // Silently ignored – command line too verbose for UI log
                    }
                    case FfmpegProgressEvent progress -> {
                        // Update VideoJobStatus from progress event
                        if (progress.frameCount() != null) {
                            status.setFrameCount(progress.frameCount());
                        }
                        if (progress.fps() != null) {
                            status.setFps(progress.fps());
                        }
                        if (progress.sizeKb() != null) {
                            status.setOutputSizeBytes(
                                (long) (progress.sizeKb() * 1024)
                            );
                        }
                        if (progress.timeMs() != null) {
                            status.setCurrentTimeMs(progress.timeMs());
                            // Calculate progress percentage
                            if (status.getTotalDurationMs() > 0) {
                                double pct =
                                    ((double) progress.timeMs() /
                                        status.getTotalDurationMs()) *
                                    100.0;
                                status.setProgressPercent(Math.min(pct, 100.0));
                            }
                        }
                        if (progress.bitrateKbps() != null) {
                            status.setBitrateBps(
                                (long) (progress.bitrateKbps() * 1000)
                            );
                        }

                        // Notify UI of progress
                        listener.onJobProgress(index, status);
                    }
                    case FfmpegLogEvent log -> {
                        // Log warnings and errors immediately for visibility
                        if (
                            log.level() == FfmpegLogEvent.LogLevel.WARNING ||
                            log.level() == FfmpegLogEvent.LogLevel.ERROR
                        ) {
                            listener.onLog(
                                I18n.get(
                                    "job.ffmpeg_log_prefix",
                                    log.level(),
                                    log.message()
                                )
                            );
                        }
                    }
                    case FfmpegCompleteEvent complete -> {
                        long duration = complete.durationMs();
                        listener.onLog(
                            I18n.get(
                                "job.ffmpeg_finished",
                                complete.exitCode(),
                                duration
                            )
                        );

                        if (complete.isSuccess()) {
                            status.setStatus(VideoJobStatus.Status.COMPLETED);
                            status.setProgressPercent(100.0);
                        } else {
                            status.setStatus(VideoJobStatus.Status.FAILED);
                            status.setErrorCode(complete.exitCode());

                            // Log FFmpeg stderr output for debugging
                            List<String> stderr = complete.stderrLines();
                            if (stderr != null && !stderr.isEmpty()) {
                                listener.onLog(I18n.get("job.ffmpeg_stderr"));
                                // Show last 20 lines to avoid flooding the log
                                int start = Math.max(0, stderr.size() - 20);
                                for (int i = start; i < stderr.size(); i++) {
                                    listener.onLog("    " + stderr.get(i));
                                }
                                // Build error message from last meaningful stderr line
                                String lastError = stderr.get(
                                    stderr.size() - 1
                                );
                                status.setErrorMessage(
                                    I18n.get(
                                        "job.ffmpeg_exit_code",
                                        complete.exitCode(),
                                        lastError
                                    )
                                );
                            } else {
                                status.setErrorMessage(
                                    I18n.get(
                                        "job.ffmpeg_exit_code_short",
                                        complete.exitCode()
                                    )
                                );
                            }
                        }

                        // Notify UI of job completion
                        listener.onJobFinished(index, status);
                    }
                    case FfmpegErrorEvent error -> {
                        status.setStatus(VideoJobStatus.Status.FAILED);
                        status.setErrorMessage(error.message());
                        listener.onLog(
                            I18n.get("job.error_prefix", error.message())
                        );
                        listener.onJobFinished(index, status);
                    }
                }
            });

            // Determine result based on final status
            if (status.getStatus() == VideoJobStatus.Status.COMPLETED) {
                return JobResult.SUCCESS;
            } else {
                return JobResult.FAILURE;
            }
        } catch (Ffmpeg.FfmpegException e) {
            status.setStatus(VideoJobStatus.Status.FAILED);
            status.setErrorMessage(e.getMessage());
            listener.onLog(
                I18n.get(
                    "job.error_log_suffix",
                    index + 1,
                    total,
                    e.getMessage()
                )
            );
            listener.onJobFinished(index, status);
            return JobResult.FAILURE;
        } catch (Exception e) {
            status.setStatus(VideoJobStatus.Status.FAILED);
            status.setErrorMessage(
                I18n.get("job.unexpected_error", e.getMessage())
            );
            listener.onLog(
                I18n.get(
                    "job.unexpected_log_suffix",
                    index + 1,
                    total,
                    e.getMessage()
                )
            );
            listener.onJobFinished(index, status);
            return JobResult.FAILURE;
        }
    }

    /**
     * Cancels any currently running processing pipeline.
     * This destroys any running FFmpeg or ffprobe processes and signals
     * the processing loops to terminate early.
     *
     * <p>Call this from the UI thread or any other thread. The actual
     * process destruction happens immediately, but the processing loops
     * will check the flag between jobs.</p>
     *
     * @return true if cancellation was triggered, false if nothing was running
     */
    public boolean cancel() {
        isCancelled = true;
        boolean ffmpegKilled = Ffmpeg.cancel();
        boolean ffprobeKilled = Ffprobe.cancel();
        return ffmpegKilled || ffprobeKilled || isCancelled;
    }

    /**
     * Resets the internal cancellation flag so that future calls to
     * {@code prepareJobs} or {@code executeJobs} start with a clean slate.
     * Call this after cancelling and before starting a new processing run.
     */
    public void resetCancelled() {
        isCancelled = false;
    }

    /**
     * Builds the output file path inside the export folder.
     *
     * <p>The output filename is derived from the source filename with the
     * original extension replaced by the container extension from the
     * editing preset (which may differ if Strategy adjusted it for
     * codec-container compatibility).</p>
     *
     * @param sourceFile     the source video file
     * @param editingPreset  the final preset after Strategy adjustments
     * @return the target output file
     */
    private static File buildOutputFile(File sourceFile, Preset editingPreset) {
        String name = sourceFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName;
        if (dotIndex > 0) {
            baseName = name.substring(0, dotIndex);
        } else {
            baseName = name;
        }

        String ext = editingPreset.container().getExtension();
        String outputName = baseName + "." + ext;

        Path exportPath = ExportPathResolver.getExportPath();
        Path outputPath = exportPath.resolve(outputName);

        // Check if output path would overwrite the input file
        boolean wouldOverwrite = outputPath
            .toAbsolutePath()
            .equals(sourceFile.toPath().toAbsolutePath());

        if (wouldOverwrite) {
            outputName = "vc_" + outputName;
        }

        return exportPath.resolve(outputName).toFile();
    }
}
