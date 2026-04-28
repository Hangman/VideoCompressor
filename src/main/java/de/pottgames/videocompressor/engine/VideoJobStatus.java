package de.pottgames.videocompressor.engine;

/**
 * Status information for ffmpeg processing jobs.
 * Stores data that can be read during ffmpeg execution.
 */
public class VideoJobStatus {

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
    }

    private Status status = Status.PENDING;

    // Progress
    private long currentTimeMs = 0;
    private long totalDurationMs = 0;
    private double progressPercent = 0.0;

    // ffmpeg Statistics
    private long bitrateBps = 0;
    private long frameCount = 0;
    private double fps = 0.0;
    private long outputSizeBytes = 0;

    // Error information
    private String errorMessage = null;
    private int errorCode = 0;

    /**
     * Sets the general job status.
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Sets the current processing time in milliseconds.
     */
    public void setCurrentTimeMs(long currentTimeMs) {
        this.currentTimeMs = currentTimeMs;
    }

    public long getCurrentTimeMs() {
        return currentTimeMs;
    }

    /**
     * Sets the total video duration in milliseconds.
     */
    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    /**
     * Sets the progress in percent (0.0 - 100.0).
     */
    public void setProgressPercent(double progressPercent) {
        this.progressPercent = Math.max(0.0, Math.min(100.0, progressPercent));
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    /**
     * Sets the current bitrate in bits per second.
     */
    public void setBitrateBps(long bitrateBps) {
        this.bitrateBps = bitrateBps;
    }

    public long getBitrateBps() {
        return bitrateBps;
    }

    /**
     * Sets the processed frame count.
     */
    public void setFrameCount(long frameCount) {
        this.frameCount = frameCount;
    }

    public long getFrameCount() {
        return frameCount;
    }

    /**
     * Sets the current frame rate.
     */
    public void setFps(double fps) {
        this.fps = fps;
    }

    public double getFps() {
        return fps;
    }

    /**
     * Sets the current output file size in bytes.
     */
    public void setOutputSizeBytes(long outputSizeBytes) {
        this.outputSizeBytes = outputSizeBytes;
    }

    public long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    /**
     * Sets the error message for a failed job.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error code for a failed job.
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Checks whether the job is finished (regardless of success or failure).
     */
    public boolean isFinished() {
        return (
            status == Status.COMPLETED ||
            status == Status.FAILED ||
            status == Status.CANCELLED
        );
    }

    /**
     * Checks whether the job was completed successfully.
     */
    public boolean isSuccess() {
        return status == Status.COMPLETED;
    }

    @Override
    public String toString() {
        return (
            "VideoJobStatus{" +
            "status=" +
            status +
            ", progress=" +
            String.format("%.1f%%", progressPercent) +
            ", currentTime=" +
            currentTimeMs +
            "ms/" +
            totalDurationMs +
            "ms" +
            ", bitrate=" +
            bitrateBps +
            "bps" +
            ", frames=" +
            frameCount +
            ", fps=" +
            fps +
            ", size=" +
            outputSizeBytes +
            "bytes" +
            (errorMessage != null ? ", error='" + errorMessage + "'" : "") +
            '}'
        );
    }
}
