package de.pottgames.videocompressor.engine;

import java.io.File;

/**
 * Record to store probe information for video files obtained via ffprobe.
 */
public record ProbeInfo(
    File file,
    int resolutionWidth,
    int resolutionHeight,
    int fps,
    int bitrate,
    double duration,
    String codec,
    long fileSize
) {
    /**
     * Returns a combined string of resolution and FPS.
     * Standard 16:9 resolutions are abbreviated (e.g., 1080p, 4K), while
     * non-standard aspect ratios are shown as raw dimensions (e.g., "3440x1440").
     *
     * @return formatted string like "1080p @ 30" or "3440x1440 @ 60"
     */
    public String getResolutionFpsString() {
        String resolution = getAbbreviatedResolution();
        return resolution + " @ " + fps;
    }

    /**
     * Returns an abbreviated resolution string for standard 16:9 formats (horizontal & vertical),
     * or "widthxheight" for non-standard aspect ratios.
     */
    private String getAbbreviatedResolution() {
        double ratio = (double) resolutionWidth / resolutionHeight;
        double invRatio = (double) resolutionHeight / resolutionWidth;
        
        // Check for 16:9 (approx. 1.77) or 9:16 (approx. 0.56)
        boolean isStandard16x9 = Math.abs(ratio - 16.0 / 9.0) < 0.02;
        boolean isVertical16x9 = Math.abs(invRatio - 16.0 / 9.0) < 0.02;

        if (!isStandard16x9 && !isVertical16x9) {
            return resolutionWidth + "x" + resolutionHeight;
        }

        // Use the smaller dimension to determine the "p" label
        int pValue = Math.min(resolutionWidth, resolutionHeight);
        String suffix = isVertical16x9 ? " (Vertical)" : "";

        return switch (pValue) {
            case 720 -> "720p" + suffix;
            case 1080 -> "1080p" + suffix;
            case 1440 -> "1440p" + suffix;
            case 2160 -> "4K" + suffix;
            case 4320 -> "8K" + suffix;
            default -> resolutionWidth + "x" + resolutionHeight;
        };
    }
}
