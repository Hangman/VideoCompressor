package de.pottgames.videocompressor.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Wrapper for the ffprobe executable.
 * Provides async and sync methods to probe video files and extract metadata.
 */
public class Ffprobe {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ExecutorService EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Tracks the currently running ffprobe process so that
     * {@link #cancel()} can terminate it.
     */
    private static final AtomicReference<Process> CURRENT_PROCESS =
        new AtomicReference<>();

    /**
     * Asynchronously probes a video file and returns a CompletableFuture
     * that will contain the ProbeInfo when the operation completes.
     *
     * @param file the video file to probe
     * @return a CompletableFuture containing the ProbeInfo
     */
    public static CompletableFuture<ProbeInfo> probeAsync(File file) {
        return CompletableFuture.supplyAsync(() -> probe(file), EXECUTOR);
    }

    /**
     * Synchronously probes a video file and returns the ProbeInfo.
     * This method blocks until the operation completes.
     *
     * @param file the video file to probe
     * @return the ProbeInfo containing video metadata
     * @throws RuntimeException if the probing fails
     */
    public static ProbeInfo probe(File file) {
        try {
            Process process = getProcess(file);
            CURRENT_PROCESS.set(process);

            byte[] outputBytes;
            try {
                // Read output
                outputBytes = process.getInputStream().readAllBytes();
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new IOException(
                        "ffprobe exited with code " + exitCode
                    );
                }
            } finally {
                CURRENT_PROCESS.set(null);
            }

            // Parse JSON
            JsonNode root = JSON_MAPPER.readTree(outputBytes);
            JsonNode formatNode = root.get("format");
            List<JsonNode> streams = new ArrayList<>();
            if (root.has("streams")) {
                for (JsonNode stream : root.get("streams")) {
                    streams.add(stream);
                }
            }

            // Find video stream
            JsonNode videoStream = null;
            for (JsonNode stream : streams) {
                if ("video".equals(stream.get("codec_type").asString())) {
                    videoStream = stream;
                    break;
                }
            }

            if (videoStream == null) {
                throw new IOException(
                    "No video stream found in " + file.getName()
                );
            }

            // Extract resolution
            int width = videoStream.has("width")
                ? videoStream.get("width").asInt()
                : 0;
            int height = videoStream.has("height")
                ? videoStream.get("height").asInt()
                : 0;

            // Extract FPS
            double fps = 0;
            if (videoStream.has("r_frame_rate")) {
                String frameRate = videoStream.get("r_frame_rate").asString();
                if (frameRate.contains("/")) {
                    String[] parts = frameRate.split("/");
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    fps = denominator != 0 ? numerator / denominator : 0;
                }
            }

            // Extract bitrate
            int bitrate = formatNode.has("bit_rate")
                ? formatNode.get("bit_rate").asInt()
                : 0;

            // Extract audio bitrate and channel count
            int audioBitrate = 0;
            int audioChannels = 0;
            for (JsonNode stream : streams) {
                String codecType = stream.has("codec_type")
                    ? stream.get("codec_type").asString()
                    : null;
                if ("audio".equals(codecType)) {
                    if (stream.has("bit_rate")) {
                        audioBitrate = stream.get("bit_rate").asInt();
                    }
                    if (stream.has("channels")) {
                        audioChannels = stream.get("channels").asInt();
                    }
                    break;
                }
            }

            // Extract duration
            double duration = formatNode.has("duration")
                ? Double.parseDouble(formatNode.get("duration").asString())
                : 0.0;

            // Extract codec
            String codec = videoStream.has("codec_name")
                ? videoStream.get("codec_name").asString()
                : "unknown";

            // File size
            long fileSize = file.length();

            return new ProbeInfo(
                file,
                width,
                height,
                fps,
                bitrate,
                audioBitrate,
                audioChannels,
                duration,
                codec,
                fileSize
            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(
                "Failed to probe file: " + file.getName(),
                e
            );
        }
    }

    private static Process getProcess(File file) throws IOException {
        Path ffprobePath = Engine.getFfprobePath();
        if (!ffprobePath.toFile().exists()) {
            throw new IOException(
                "ffprobe executable not found at: " + ffprobePath
            );
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            ffprobePath.toString(),
            "-v",
            "quiet",
            "-print_format",
            "json",
            "-show_format",
            "-show_streams",
            file.getAbsolutePath()
        );
        processBuilder.redirectErrorStream(true);

        return processBuilder.start();
    }

    /**
     * Cancels any currently running ffprobe process.
     * This destroys the underlying OS process immediately.
     * If no process is running, this is a no-op.
     *
     * @return true if a process was canceled, false if none was running
     */
    public static boolean cancel() {
        Process process = CURRENT_PROCESS.get();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            CURRENT_PROCESS.set(null);
            return true;
        }
        CURRENT_PROCESS.set(null);
        return false;
    }
}
