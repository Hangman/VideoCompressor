package de.pottgames.videocompressor.engine;

import java.io.File;

/**
 * Callback interface for receiving progress updates from the {@link JobProcessor}
 * during video preparation and encoding.
 *
 * <p>This interface is intentionally free of JavaFX dependencies. Implementors
 * are responsible for marshalling callbacks back to the JavaFX Application Thread
 * if UI updates are needed (e.g., via {@code Platform.runLater}).</p>
 *
 * <p>All callback methods are invoked from non-UI threads (virtual threads
 * used by the engine layer).</p>
 */
public interface JobProgressListener {

    // ─────────────────────────────────────────────────────────────────────
    //  Preparation phase: probing source videos & creating VideoJobs
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called when the preparation phase starts (probing source videos and
     * creating {@link VideoJob}s via {@link Strategy#calcEditingPreset}).
     *
     * @param totalFiles the number of files that will be prepared
     */
    void onPreparationStarted(int totalFiles);

    /**
     * Called during preparation to report progress for a single file.
     *
     * @param current  1-based index of the currently prepared file
     * @param total    total number of files
     * @param fileName name of the file being prepared
     * @param message  human-readable status message
     *                 (e.g. "Probing...", "Berechne Preset...")
     */
    void onPreparationProgress(int current, int total, String fileName, String message);

    /**
     * Called when all jobs have been created successfully.
     *
     * @param jobCount the number of jobs that were created
     */
    void onPreparationCompleted(int jobCount);

    /**
     * Called when preparation fails before any jobs can be created.
     *
     * @param errorMessage description of the failure
     */
    void onPreparationFailed(String errorMessage);

    // ─────────────────────────────────────────────────────────────────────
    //  Processing phase: executing FFmpeg encoding jobs
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called when processing of a single video job starts.
     *
     * @param index      0-based index of the job in the processing queue
     * @param total      total number of jobs
     * @param sourceFile the source video file
     * @param outputFile the target output file
     */
    void onJobStarted(int index, int total, File sourceFile, File outputFile);

    /**
     * Called with progress updates during a single job's execution.
     *
     * @param index  0-based index of the currently processing job
     * @param status the current {@link VideoJobStatus} of the job
     */
    void onJobProgress(int index, VideoJobStatus status);

    /**
     * Called when a single job has finished (successfully or not).
     *
     * @param index  0-based index of the finished job
     * @param status the final {@link VideoJobStatus} of the job
     */
    void onJobFinished(int index, VideoJobStatus status);

    /**
     * Called when all jobs have been processed.
     *
     * @param totalCompleted number of jobs that completed successfully
     * @param totalFailed    number of jobs that failed
     */
    void onAllJobsCompleted(int totalCompleted, int totalFailed);

    // ─────────────────────────────────────────────────────────────────────
    //  General logging
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called to append a log line. Implementors can forward this to a
     * log/console UI element.
     *
     * @param message the log message
     */
    void onLog(String message);
}
