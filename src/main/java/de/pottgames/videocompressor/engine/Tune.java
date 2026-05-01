package de.pottgames.videocompressor.engine;

/**
 * Enumeration of FFmpeg tune options for optimizing encoding for specific
 * content types.
 * Each entry provides its FFmpeg CLI identifier and a human-readable name.
 */
public enum Tune {
    NONE("none", "Keine"),
    FILM("film", "Film"),
    ANIMATION("animation", "Animation"),
    GRAIN("grain", "Filmkörnung"),
    STILLIMAGE("stillimage", "Standbild"),
    FASTDECODE("fastdecode", "Schnelles Decodieren"),
    ZEROLATENCY("zerolatency", "Keine Latenz");

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

    @Override
    public String toString() {
        return humanName;
    }
}
