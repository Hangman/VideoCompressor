package de.pottgames.videocompressor.engine;

/**
 * Enumeration of FFmpeg encoding presets that control the speed vs. quality
 * trade-off during video encoding.
 * These presets are commonly used with x264/x265 encoders.
 */
public enum FfmpegPreset {
    ULTRAFAST("ultrafast", "Ultrafast"),
    SUPERFAST("superfast", "Superfast"),
    VERYFAST("veryfast", "Veryfast"),
    FASTER("faster", "Faster"),
    FAST("fast", "Fast"),
    MEDIUM("medium", "Medium"),
    SLOW("slow", "Slow"),
    SLOWER("slower", "Slower"),
    VERYSLOW("veryslow", "Veryslow");

    private final String ffmpegName;
    private final String humanName;

    FfmpegPreset(String ffmpegName, String humanName) {
        this.ffmpegName = ffmpegName;
        this.humanName = humanName;
    }

    /**
     * Returns the preset name as understood by the FFmpeg CLI.
     *
     * @return the FFmpeg preset identifier, e.g. "medium"
     */
    public String getFfmpegName() {
        return ffmpegName;
    }

    /**
     * Returns the human-readable name of the preset.
     *
     * @return the display name, e.g. "Medium"
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * Looks up an FfmpegPreset by its FFmpeg CLI name.
     * Falls back to MEDIUM for unknown values (e.g. numeric SVT-AV1 presets).
     *
     * @param ffmpegName the FFmpeg preset identifier
     * @return the matching FfmpegPreset, or MEDIUM as fallback
     */
    public static FfmpegPreset fromFfmpegName(String ffmpegName) {
        for (FfmpegPreset preset : values()) {
            if (preset.ffmpegName.equals(ffmpegName)) {
                return preset;
            }
        }
        // Fallback for unknown or numeric presets (e.g. SVT-AV1 uses 0–13)
        return MEDIUM;
    }

    @Override
    public String toString() {
        return humanName;
    }
}
