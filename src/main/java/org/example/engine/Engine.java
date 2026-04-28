package org.example.engine;

import java.io.File;
import java.util.List;

/**
 * Central engine for video processing configuration and validation.
 * Defines supported video formats and provides compatibility checks.
 */
public class Engine {

    /**
     * Supported video file extensions (without the dot).
     */
    public static final List<String> SUPPORTED_EXTENSIONS = List.of(
        "mp4",
        "avi",
        "mkv",
        "mov",
        "wmv",
        "flv",
        "webm"
    );

    /**
     * Supported video MIME type patterns for FileChooser filters.
     */
    public static final List<String> SUPPORTED_EXTENSION_PATTERNS = List.of(
        "*.mp4",
        "*.avi",
        "*.mkv",
        "*.mov",
        "*.wmv",
        "*.flv",
        "*.webm"
    );

    /**
     * Checks whether the given file is a compatible video file
     * that FFMPEG (full) can process.
     *
     * @param file the file to check
     * @return {@code true} if the file has a supported extension
     */
    public static boolean isCompatible(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= name.length() - 1) {
            return false;
        }
        String extension = name.substring(dotIndex + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
}
