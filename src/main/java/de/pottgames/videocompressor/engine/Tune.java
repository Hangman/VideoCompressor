package de.pottgames.videocompressor.engine;

/**
 * Enumeration of FFmpeg tune options for optimizing encoding for specific
 * content types.
 * Each entry provides its FFmpeg CLI identifier and a human-readable name.
 */
public enum Tune {
    NONE("none", "None"),
    FILM("film", "Film"),
    ANIMATION("animation", "Animation"),
    GRAIN("grain", "Grain"),
    STILLIMAGE("stillimage", "Stillimage"),
    FASTDECODE("fastdecode", "Fastdecode"),
    ZEROLATENCY("zerolatency", "Zerolatency");

    private final String ffmpegName;
    private final String humanName;

    Tune(String ffmpegName, String humanName) {
        this.ffmpegName = ffmpegName;
        this.humanName = humanName;
    }

    /**
     * Returns the tune option as understood by the FFmpeg CLI.
     *
     * @return the FFmpeg tune identifier, e.g. "film"
     */
    public String getFfmpegName() {
        return ffmpegName;
    }

    /**
     * Returns the human-readable name of the tune option.
     *
     * @return the display name, e.g. "Film"
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * Looks up a Tune by its FFmpeg CLI name.
     *
     * @param ffmpegName the FFmpeg tune identifier
     * @return the matching Tune
     * @throws IllegalArgumentException if no tune matches the given name
     */
    public static Tune fromFfmpegName(String ffmpegName) {
        for (Tune tune : values()) {
            if (tune.ffmpegName.equals(ffmpegName)) {
                return tune;
            }
        }
        throw new IllegalArgumentException(
            "Unknown FFmpeg tune name: " + ffmpegName
        );
    }

    /**
     * Looks up a Tune by a case-insensitive name match.
     * Tries matching against both the FFmpeg name and the human-readable name.
     *
     * @param name the tune identifier to match
     * @return the matching Tune
     * @throws IllegalArgumentException if no tune matches the given name
     */
    public static Tune fromName(String name) {
        for (Tune tune : values()) {
            if (
                tune.ffmpegName.equalsIgnoreCase(name) ||
                tune.humanName.equalsIgnoreCase(name)
            ) {
                return tune;
            }
        }
        throw new IllegalArgumentException("Unknown tune name: " + name);
    }

    /**
     * Returns whether this tune is supported by the libx265 (H.265/HEVC)
     * encoder. H.265 supports: none, animation, grain, fastdecode,
     * zerolatency. It does NOT support: film, stillimage.
     * (psnr/ssim are also supported by x265 but intentionally not exposed.)
     *
     * @return true if libx265 accepts this tune value
     */
    public boolean isSupportedByH265() {
        return this != Tune.FILM && this != Tune.STILLIMAGE;
    }

    @Override
    public String toString() {
        return humanName;
    }
}
