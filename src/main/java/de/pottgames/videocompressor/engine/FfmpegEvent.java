package de.pottgames.videocompressor.engine;

import java.util.List;

/**
 * Sealed interface for all events emitted during an FFmpeg execution.
 *
 * Each event type captures a specific kind of output or lifecycle milestone
 * from the FFmpeg process. Consumers can pattern-match on these events to
 * react to progress updates, log messages, completion, or errors.
 *
 * @see FfmpegStartEvent
 * @see FfmpegProgressEvent
 * @see FfmpegLogEvent
 * @see FfmpegCompleteEvent
 * @see FfmpegErrorEvent
 */
public sealed interface FfmpegEvent
    permits
        FfmpegStartEvent,
        FfmpegProgressEvent,
        FfmpegLogEvent,
        FfmpegCompleteEvent,
        FfmpegErrorEvent
{
    /**
     * Returns the time at which this event was created (epoch millis).
     */
    long timestamp();
}

/**
 * Emitted when an FFmpeg process has been successfully started.
 * Contains the resolved command line that was executed.
 */
record FfmpegStartEvent(
    long timestamp,
    String commandLine
) implements FfmpegEvent {}

/**
 * Emitted for each parsed progress line from FFmpeg stderr.
 *
 * FFmpeg outputs lines like:
 * <pre>
 *   frame=1234 fps=25.0 q=28.0 size=12345kB time=00:01:23.45 bitrate=1234.5kbits/s speed=1.2x
 * </pre>
 *
 * All numeric fields are nullable, since partial lines may be emitted
 * during startup or shutdown.
 */
record FfmpegProgressEvent(
    long timestamp,

    /** Processed frame count. */
    Long frameCount,

    /** Current encoding frame rate. */
    Double fps,

    /** Quality factor (codec-dependent). */
    Double quality,

    /** Current output file size in kilobytes. */
    Double sizeKb,

    /** Current time position in the stream (e.g. "00:01:23.45"). */
    String timeStr,

    /** Current time position parsed to milliseconds. */
    Long timeMs,

    /** Current encoding bitrate in kilobits per second. */
    Double bitrateKbps,

    /** Encoding speed relative to real-time (e.g. 1.2x). */
    Double speed
) implements FfmpegEvent {}

/**
 * Emitted for log, warning, or informational lines from FFmpeg that do
 * not match the progress pattern.
 */
record FfmpegLogEvent(
    long timestamp,

    /** Severity level of the log line. */
    LogLevel level,

    /** The raw log message. */
    String message
) implements FfmpegEvent {
    public enum LogLevel {
        INFO,
        WARNING,
        ERROR,
        DEBUG,
        TRACE,
        UNKNOWN,
    }
}

/**
 * Emitted when the FFmpeg process terminates (successfully or not).
 * The exit code determines whether the execution was successful.
 */
record FfmpegCompleteEvent(
    long timestamp,
    int exitCode,
    long durationMs,
    List<String> stderrLines
) implements FfmpegEvent {
    /**
     * @return {@code true} if the exit code indicates success (0).
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}

/**
 * Emitted when a fatal error prevents FFmpeg execution or causes an
 * unexpected failure (e.g. executable not found, I/O exception).
 */
record FfmpegErrorEvent(
    long timestamp,
    String message,
    Throwable cause
) implements FfmpegEvent {}
