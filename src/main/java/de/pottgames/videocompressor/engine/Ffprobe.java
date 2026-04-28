package de.pottgames.videocompressor.engine;

import java.nio.file.Path;

/**
 * Wrapper for the ffprobe executable.
 * Provides access to the path of the bundled ffprobe binary.
 */
public class Ffprobe {

    /**
     * Returns the path to the ffprobe executable.
     * The executable is located relative to the project structure.
     *
     * @return the path to the ffprobe executable
     */
    public static Path path() {
        return Engine.getFfprobePath();
    }
}
