package de.pottgames.videocompressor.engine;

/**
 * Enumeration of supported video container formats.
 * Each container provides its FFmpeg CLI identifier and a human-readable name.
 */
public enum VideoContainer {
    MP4("mp4", "MP4", "mp4", true),
    MOV("mov", "MOV", "mov", true),
    MKV("matroska", "MKV", "mkv", false),
    WEBM("webm", "WebM", "webm", false);

    private final String ffmpegName;
    private final String humanName;
    private final String extension;
    private final boolean supportsFastStart;

    VideoContainer(
        String ffmpegName,
        String humanName,
        String extension,
        boolean supportsFastStart
    ) {
        this.ffmpegName = ffmpegName;
        this.humanName = humanName;
        this.extension = extension;
        this.supportsFastStart = supportsFastStart;
    }

    /**
     * Returns whether this container supports the -movflags faststart option.
     *
     * @return true for MP4 and MOV containers
     */
    public boolean supportsFastStart() {
        return supportsFastStart;
    }

    /**
     * Returns the container name as understood by the FFmpeg CLI.
     *
     * @return the FFmpeg container identifier, e.g. "mp4"
     */
    public String getFfmpegName() {
        return ffmpegName;
    }

    /**
     * Returns the human-readable, common name of the container.
     *
     * @return the display name, e.g. "MP4"
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * Returns the common file extension for this container format.
     *
     * @return the file extension, e.g. ".mp4"
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Looks up a VideoContainer by its FFmpeg CLI name.
     *
     * @param ffmpegName the FFmpeg container identifier
     * @return the matching VideoContainer
     * @throws IllegalArgumentException if no container matches the given name
     */
    public static VideoContainer fromFfmpegName(String ffmpegName) {
        for (VideoContainer container : values()) {
            if (container.ffmpegName.equals(ffmpegName)) {
                return container;
            }
        }
        throw new IllegalArgumentException(
            "Unknown FFmpeg container name: " + ffmpegName
        );
    }

    /**
     * Looks up a VideoContainer by a case-insensitive name match.
     * Tries matching against both the FFmpeg name and the human-readable name.
     * This is useful for loading from config files where the user may write
     * "mkv" even though FFmpeg calls it "matroska".
     *
     * @param name the container identifier to match
     * @return the matching VideoContainer
     * @throws IllegalArgumentException if no container matches the given name
     */
    public static VideoContainer fromName(String name) {
        for (VideoContainer container : values()) {
            if (
                container.ffmpegName.equalsIgnoreCase(name) ||
                container.humanName.equalsIgnoreCase(name)
            ) {
                return container;
            }
        }
        throw new IllegalArgumentException("Unknown container name: " + name);
    }

    @Override
    public String toString() {
        return humanName;
    }
}
