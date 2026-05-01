package de.pottgames.videocompressor.engine;

/**
 * Enumeration of commonly used video codecs supported by FFmpeg.
 * Each codec provides its FFmpeg CLI identifier and a human-readable name.
 */
public enum VideoCodec {
    H264("libx264", "H.264 / AVC"),
    H265("libx265", "H.265 / HEVC"),
    VP9("libvpx-vp9", "VP9"),
    AV1("libsvtav1", "AV1");

    private final String ffmpegName;
    private final String humanName;

    VideoCodec(String ffmpegName, String humanName) {
        this.ffmpegName = ffmpegName;
        this.humanName = humanName;
    }

    /**
     * Returns the codec name as understood by the FFmpeg CLI.
     *
     * @return the FFmpeg codec identifier, e.g. "libx264"
     */
    public String getFfmpegName() {
        return ffmpegName;
    }

    /**
     * Returns the human-readable, common name of the codec.
     *
     * @return the display name, e.g. "H.264 / AVC"
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * Looks up a VideoCodec by its FFmpeg CLI name.
     *
     * @param ffmpegName the FFmpeg codec identifier
     * @return the matching VideoCodec
     * @throws IllegalArgumentException if no codec matches the given name
     */
    public static VideoCodec fromFfmpegName(String ffmpegName) {
        for (VideoCodec codec : values()) {
            if (codec.ffmpegName.equals(ffmpegName)) {
                return codec;
            }
        }
        throw new IllegalArgumentException(
            "Unknown FFmpeg codec name: " + ffmpegName
        );
    }

    /**
     * Looks up a VideoCodec by a case-insensitive name match.
     * Tries matching against both the FFmpeg name and the human-readable name.
     *
     * @param name the codec identifier to match
     * @return the matching VideoCodec
     * @throws IllegalArgumentException if no codec matches the given name
     */
    public static VideoCodec fromName(String name) {
        for (VideoCodec codec : values()) {
            if (
                codec.ffmpegName.equalsIgnoreCase(name) ||
                codec.humanName.equalsIgnoreCase(name)
            ) {
                return codec;
            }
        }
        throw new IllegalArgumentException("Unknown codec name: " + name);
    }

    @Override
    public String toString() {
        return humanName;
    }
}
