package de.pottgames.videocompressor.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic wrapper for executing FFmpeg commands with arbitrary parameters.
 *
 * Emits typed {@link FfmpegEvent}s during execution for progress tracking,
 * logging, and completion status. Callers consume events via a provided
 * {@link Consumer}.
 *
 * <h3>Usage example</h3>
 * <pre>
 * List&lt;String&gt; args = List.of(
 *     "-i", "input.mp4",
 *     "-c:v", "libx264", "-crf", "23",
 *     "output.mp4"
 * );
 *
 * Ffmpeg.execute(args, event -> {
 *     switch (event) {
 *         case FfmpegProgressEvent p -> System.out.println("Frame: " + p.frameCount());
 *         case FfmpegCompleteEvent c -> System.out.println("Done, exit code: " + c.exitCode());
 *         case FfmpegErrorEvent e -> System.err.println("Error: " + e.message());
 *         default -> {}
 *     }
 * });
 * </pre>
 */
public class Ffmpeg {

    private static final ExecutorService EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    // ── Regex patterns for parsing FFmpeg stderr ─────────────────────────

    /**
     * Matches FFmpeg progress lines like:
     * <pre>
     *   frame=1234 fps=25.0 q=28.0 size=12345kB time=00:01:23.45 bitrate=1234.5kbits/s speed=1.2x
     * </pre>
     * Delegated to {@link FfmpegProgress} for parsing.
     */

    /**
     * Matches FFmpeg log lines with a level prefix like:
     * <pre>
     *   [info] ...
     *   [warning] ...
     *   [error] ...
     * </pre>
     */
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^\\[(\\w+)\\]\\s*(.*)",
        Pattern.MULTILINE
    );

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Returns the path to the ffmpeg executable.
     *
     * @return the path to the ffmpeg executable
     */
    public static Path path() {
        return Engine.getFfmpegPath();
    }

    /**
     * Executes an FFmpeg command synchronously, emitting events to the
     * given consumer as the process runs.
     *
     * @param args     the FFmpeg CLI arguments (does not include the binary name)
     * @param consumer receives every {@link FfmpegEvent} during execution
     * @return the exit code of the process
     * @throws FfmpegException if the executable cannot be started
     */
    public static int execute(
        List<String> args,
        Consumer<FfmpegEvent> consumer
    ) {
        return executeAsync(args, consumer).join();
    }

    /**
     * Executes an FFmpeg command asynchronously, emitting events to the
     * given consumer as the process runs.
     *
     * @param args     the FFmpeg CLI arguments (does not include the binary name)
     * @param consumer receives every {@link FfmpegEvent} during execution
     * @return a CompletableFuture that completes with the exit code
     */
    public static CompletableFuture<Integer> executeAsync(
        List<String> args,
        Consumer<FfmpegEvent> consumer
    ) {
        return CompletableFuture.supplyAsync(
            () -> executeInternal(args, consumer),
            EXECUTOR
        );
    }

    // ── Internal execution logic ────────────────────────────────────────

    private static int executeInternal(
        List<String> args,
        Consumer<FfmpegEvent> consumer
    ) {
        Path ffmpegPath = path();

        if (!ffmpegPath.toFile().exists()) {
            consumer.accept(
                new FfmpegErrorEvent(
                    System.currentTimeMillis(),
                    "FFmpeg executable not found at: " + ffmpegPath,
                    null
                )
            );
            throw new FfmpegException(
                "FFmpeg executable not found at: " + ffmpegPath
            );
        }

        // Build full command
        List<String> fullCommand = new java.util.ArrayList<>();
        fullCommand.add(ffmpegPath.toString());
        fullCommand.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            long startTime = System.currentTimeMillis();

            // Emit start event
            consumer.accept(
                new FfmpegStartEvent(
                    System.currentTimeMillis(),
                    String.join(" ", fullCommand)
                )
            );

            // Collect all stderr lines for debugging on failure
            List<String> stderrLines = new java.util.ArrayList<>();

            // Read combined stdout+stderr line by line
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrLines.add(line);
                    handleLine(line, consumer);
                }
            }

            // Wait for process to finish
            int exitCode = process.waitFor();
            long endTime = System.currentTimeMillis();

            // Emit complete event with collected stderr lines
            consumer.accept(
                new FfmpegCompleteEvent(
                    endTime,
                    exitCode,
                    endTime - startTime,
                    stderrLines
                )
            );

            return exitCode;
        } catch (IOException e) {
            String msg = "Failed to start FFmpeg process: " + e.getMessage();
            consumer.accept(
                new FfmpegErrorEvent(System.currentTimeMillis(), msg, e)
            );
            throw new FfmpegException(msg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = "FFmpeg process was interrupted";
            consumer.accept(
                new FfmpegErrorEvent(System.currentTimeMillis(), msg, e)
            );
            throw new FfmpegException(msg, e);
        }
    }

    // ── Line parsing ────────────────────────────────────────────────────

    /**
     * Dispatches a single line of FFmpeg output to the appropriate event.
     */
    private static void handleLine(
        String line,
        Consumer<FfmpegEvent> consumer
    ) {
        long ts = System.currentTimeMillis();

        // Try progress pattern first
        if (FfmpegProgress.isProgress(line)) {
            consumer.accept(parseProgressEvent(ts, line));
            return;
        }

        // Try log pattern
        Matcher logMatcher = LOG_PATTERN.matcher(line);
        if (logMatcher.find()) {
            FfmpegLogEvent.LogLevel level = parseLogLevel(logMatcher.group(1));
            String message = logMatcher.group(2);
            consumer.accept(new FfmpegLogEvent(ts, level, message));
            return;
        }

        // Fallback: treat as unknown info log
        consumer.accept(
            new FfmpegLogEvent(ts, FfmpegLogEvent.LogLevel.UNKNOWN, line)
        );
    }

    /**
     * Parses a progress line into a {@link FfmpegProgressEvent}.
     */
    private static FfmpegProgressEvent parseProgressEvent(
        long timestamp,
        String rawProgress
    ) {
        FfmpegProgress progress = new FfmpegProgress(rawProgress);
        return new FfmpegProgressEvent(
            timestamp,
            (Long) progress.getFrame(), // frameCount
            (Double) progress.getFps(), // fps
            (Double) progress.getQ(), // quality
            (Double) (double) progress.getSize(), // sizeKb
            progress.getTime(), // timeStr
            (Long) progress.getTimeMs(), // timeMs
            (Double) progress.getBitrate(), // bitrateKbps
            (Double) progress.getSpeed() // speed
        );
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Maps a FFmpeg log level string to our enum.
     */
    private static FfmpegLogEvent.LogLevel parseLogLevel(String levelStr) {
        if (levelStr == null) {
            return FfmpegLogEvent.LogLevel.UNKNOWN;
        }

        return switch (levelStr.toLowerCase()) {
            case "info" -> FfmpegLogEvent.LogLevel.INFO;
            case "verbose" -> FfmpegLogEvent.LogLevel.DEBUG;
            case "debug" -> FfmpegLogEvent.LogLevel.DEBUG;
            case "trace" -> FfmpegLogEvent.LogLevel.TRACE;
            case "warning", "warn" -> FfmpegLogEvent.LogLevel.WARNING;
            case "error" -> FfmpegLogEvent.LogLevel.ERROR;
            default -> FfmpegLogEvent.LogLevel.UNKNOWN;
        };
    }

    // ── Custom Exception ────────────────────────────────────────────────

    /**
     * Thrown when FFmpeg execution fails at the process level
     * (not to be confused with FFmpeg returning a non-zero exit code).
     */
    public static class FfmpegException extends RuntimeException {
        private static final long serialVersionUID = 7315240434659431544L;

		FfmpegException(String message) {
            super(message);
        }

        FfmpegException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
