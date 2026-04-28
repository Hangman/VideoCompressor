package de.pottgames.videocompressor.engine;

import java.nio.file.Path;

/**
 * Wrapper for the ffmpeg executable.
 * Provides access to the path of the bundled ffmpeg binary.
 */
public class Ffmpeg {

    /**
     * Returns the path to the ffmpeg executable.
     * The executable is located relative to the project structure.
     *
     * @return the path to the ffmpeg executable
     */
    public static Path path() {
        return Engine.getFfmpegPath();
    }
}
