package de.pottgames.videocompressor.engine;

/**
 * Enumeration of commonly used audio codecs supported by FFmpeg.
 * Each codec provides its FFmpeg CLI identifier and a human-readable name.
 */
public enum AudioCodec {
    AAC("aac", "AAC"),
    OPUS("libopus", "Opus"),
    VORBIS("libvorbis", "Vorbis");

    private final String ffmpegName;
    private final String humanName;

    AudioCodec(String ffmpegName, String humanName) {
        this.ffmpegName = ffmpegName;
        this.humanName = humanName;
    }

    /**
     * Returns the codec name as understood by the FFmpeg CLI.
     *
     * Returns the FFmpeg codec identifier, e.g. "aac"
     */
    public String getFfmpegName() {
        return ffmpegName;
    }

    /**
     * Returns the human-readable, common name of the codec.
     *
     * @return the display name, e.g. "AAC"
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * Looks up an AudioCodec by its FFmpeg CLI name.
     *
     * @param ffmpegName the FFmpeg codec identifier
     * @return the matching AudioCodec
     * @throws IllegalArgumentException if no codec matches the given name
     */
    public static AudioCodec fromFfmpegName(String ffmpegName) {
        for (AudioCodec codec : values()) {
            if (codec.ffmpegName.equals(ffmpegName)) {
                return codec;
            }
        }
        throw new IllegalArgumentException(
            "Unknown FFmpeg codec name: " + ffmpegName
        );
    }

    @Override
    public String toString() {
        return humanName;
    }
}
