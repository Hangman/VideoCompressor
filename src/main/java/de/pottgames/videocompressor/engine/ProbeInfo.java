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
     * Returns an abbreviated resolution string for standard 16:9 formats,
     * or "widthxheight" for non-standard aspect ratios.
     * Follows UHD/DCI 4K distinction per technical specification.
     */
    private String getAbbreviatedResolution() {
        double ratio = (double) resolutionWidth / resolutionHeight;
        double invRatio = (double) resolutionHeight / resolutionWidth;

        // Prüfe auf 16:9 ODER 9:16 (Toleranz 0.02 ist okay)
        boolean isStandardRatio =
            Math.abs(ratio - 16.0 / 9.0) < 0.02 ||
            Math.abs(invRatio - 16.0 / 9.0) < 0.02;

        if (!isStandardRatio) {
            return resolutionWidth + "x" + resolutionHeight;
        }

        // Bestimme das Label basierend auf der kürzeren Seite bei Standard-Ratios
        // (Oder strikt nach Höhe, wenn du dem Standard treu bleiben willst)
        int referenceHeight = (resolutionHeight > resolutionWidth)
            ? resolutionWidth
            : resolutionHeight;
        String suffix = (resolutionHeight > resolutionWidth)
            ? " (Vertical)"
            : "";

        return switch (resolutionHeight) {
            case 720 -> "720p" + suffix;
            case 1080 -> "1080p" + suffix;
            case 1440 -> "1440p" + suffix;
            case 2160 -> (resolutionWidth == 3840 || resolutionHeight == 3840)
                ? "4K" + suffix
                : "2160p" + suffix;
            default -> resolutionWidth + "x" + resolutionHeight;
        };
    }
}
