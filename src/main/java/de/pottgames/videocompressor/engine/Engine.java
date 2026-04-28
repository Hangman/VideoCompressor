package de.pottgames.videocompressor.engine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Central engine for video processing configuration and validation.
 * Defines supported video formats and provides compatibility checks.
 */
public class Engine {

    private static final String FFMPEG_EXECUTABLE =
        "ffmpeg" + getExecutableExtension();
    private static final String FFPROBE_EXECUTABLE =
        "ffprobe" + getExecutableExtension();

    private static final Path FFMPEG_PATH = findExecutable(FFMPEG_EXECUTABLE);
    private static final Path FFPROBE_PATH = findExecutable(FFPROBE_EXECUTABLE);

    private static String getExecutableExtension() {
        return System.getProperty("os.name").toLowerCase().contains("win")
            ? ".exe"
            : "";
    }

    private static Path findExecutable(String executableName) {
        // Try from class file location (compiled JAR or target/classes)
        try {
            Path classPath = Paths.get(
                Engine.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            );
            // If running from target/classes directory
            if (
                classPath
                    .toString()
                    .contains("target" + File.separator + "classes")
            ) {
                Path projectRoot = classPath
                    .getParent()
                    .getParent()
                    .getParent();
                Path localPath = projectRoot.resolve(
                    "ffmpeg/bin/" + executableName
                );
                if (localPath.toFile().exists()) {
                    return localPath.toAbsolutePath();
                }
            }
            // If running from a JAR
            else if (classPath.toString().endsWith(".jar")) {
                Path jarPath = classPath.getParent();
                Path localPath = jarPath.resolve(
                    "../../ffmpeg/bin/" + executableName
                );
                if (localPath.toFile().exists()) {
                    return localPath.toAbsolutePath();
                }
            }
        } catch (Exception e) {
            // Ignore and try next method
        }

        // Try relative path from working directory (project root)
        Path relativePath = Paths.get("ffmpeg/bin/" + executableName);
        if (relativePath.toFile().exists()) {
            return relativePath.toAbsolutePath();
        }

        // Return the relative path even if it doesn't exist yet
        // This allows the application to fail gracefully with a clear error message
        return relativePath.toAbsolutePath();
    }

    public static Path getFfmpegPath() {
        return FFMPEG_PATH;
    }

    public static Path getFfprobePath() {
        return FFPROBE_PATH;
    }

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
