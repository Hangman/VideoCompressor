package de.pottgames.videocompressor.engine;

/**
 * Enumeration of FFmpeg encoding presets that control the speed vs. quality
 * trade-off during video encoding.
 * These presets are commonly used with x264/x265 encoders.
 */
public enum FfmpegPreset {
    ULTRAFAST("ultrafast", "Ultrafast", 13),
    SUPERFAST("superfast", "Superfast", 11),
    VERYFAST("veryfast", "Veryfast", 9),
    FASTER("faster", "Faster", 7),
    FAST("fast", "Fast", 5),
    MEDIUM("medium", "Medium", 4),
    SLOW("slow", "Slow", 3),
    SLOWER("slower", "Slower", 2),
    VERYSLOW("veryslow", "Veryslow", 1);

    private final String ffmpegName;
    private final String humanName;
    private final int av1Preset;

    FfmpegPreset(String ffmpegName, String humanName, int av1Preset) {
        this.ffmpegName = ffmpegName;
        this.humanName = humanName;
        this.av1Preset = av1Preset;
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
     * Returns the numeric preset value for SVT-AV1 encoding (0–13).
     * 0 = slowest/best quality, 13 = fastest/lowest quality.
     *
     * @return the SVT-AV1 preset number
     */
    public int getAv1Preset() {
        return av1Preset;
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

    /**
     * Looks up an FfmpegPreset by a case-insensitive name match.
     * Tries matching against both the FFmpeg name and the human-readable name.
     * Falls back to MEDIUM for unknown values (e.g. numeric SVT-AV1 presets).
     *
     * @param name the preset identifier to match
     * @return the matching FfmpegPreset, or MEDIUM as fallback
     */
    public static FfmpegPreset fromName(String name) {
        for (FfmpegPreset preset : values()) {
            if (
                preset.ffmpegName.equalsIgnoreCase(name) ||
                preset.humanName.equalsIgnoreCase(name)
            ) {
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
