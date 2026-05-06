package de.pottgames.videocompressor.engine;

/**
 * Enumeration of common frame rates used in video production.
 * Covers film, PAL, NTSC, high-frame-rate, and slow-motion standards.
 */
public enum Fps {
    FILM_NTSC("23.976 fps", 23.976),
    FILM("24 fps", 24.0),
    PAL("25 fps", 25.0),
    NTSC("29.97 fps", 29.97),
    NTSC_ROUND("30 fps", 30.0),
    HIGH_FILM("48 fps", 48.0),
    PAL_HIGH("50 fps", 50.0),
    HIGH_NTSC("59.94 fps", 59.94),
    HIGH("60 fps", 60.0),
    SLOW_MOTION("120 fps", 120.0);

    private final String humanName;
    private final double value;

    Fps(String humanName, double value) {
        this.humanName = humanName;
        this.value = value;
    }

    /**
     * Returns the human-readable name for display in the UI.
     *
     * @return the display name, e.g. "24 fps"
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * Returns the actual frame rate value as a double.
     *
     * @return the frame rate, e.g. 23.976
     */
    public double getValue() {
        return value;
    }

    /**
     * Finds the enum constant whose value is closest to the given
     * double. This is useful when loading presets from files or
     * reading source video properties where the exact value may
     * not match an enum constant perfectly.
     *
     * @param fps the frame rate value to match
     * @return the closest matching Fps constant
     */
    public static Fps fromValue(double fps) {
        Fps closest = FILM;
        double minDiff = Double.MAX_VALUE;

        for (Fps candidate : values()) {
            double diff = Math.abs(candidate.value - fps);
            if (diff < minDiff) {
                minDiff = diff;
                closest = candidate;
            }
        }

        return closest;
    }

    @Override
    public String toString() {
        return humanName;
    }
}
