package de.pottgames.videocompressor.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the base path for exported video files.
 *
 * <p>The detection strategy is locale-independent and follows this order:</p>
 * <ol>
 *   <li>Linux: {@code XDG_VIDEOS_DIR} environment variable.</li>
 *   <li>Linux: Parse {@code ~/.config/user-dirs.dirs} for
 *       {@code XDG_VIDEOS_DIR} (handles localized folder names like
 *       "Filme", "Videolar", etc.).</li>
 *   <li>macOS: Query the Movies folder via {@code osascript}
 *       (locale-independent on macOS).</li>
 *   <li>Fallback: Check for hardcoded folder names
 *       ({@code Videos}, {@code Filme}) under the user's home directory.</li>
 *   <li>If none of the above succeed, fall back to the general user home
 *       directory ({@code USER_HOME}).</li>
 * </ol>
 *
 * <p>Then, {@code VideoCompressor/export/} is appended under the resolved
 * base folder and created if needed. As a last resort, a temporary
 * directory or the current working directory is used.</p>
 *
 * <p>The path is resolved once on the first call and then cached,
 * so subsequent calls return immediately.</p>
 */
public final class ExportPathResolver {

    /** Prevents instantiation – this is a pure utility class. */
    private ExportPathResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Once-resolved and cached export path.
     * Initialized lazily on the first call.
     */
    private static volatile Path cachedExportPath;

    /**
     * Returns the base path where exported videos are stored.
     *
     * <p>The returned path points to
     * {@code <video-folder>/VideoCompressor/export/} or, if the
     * video folder cannot be determined, to
     * {@code <user-home>/VideoCompressor/export/}.</p>
     *
     * @return the absolute exportable path
     */
    public static Path getExportPath() {
        if (cachedExportPath != null) {
            return cachedExportPath;
        }

        synchronized (ExportPathResolver.class) {
            if (cachedExportPath != null) {
                return cachedExportPath;
            }

            Path resolved = resolveExportPathInternal();
            cachedExportPath = resolved;
            return resolved;
        }
    }

    /**
     * Internal path resolution logic – executed exactly once.
     */
    private static Path resolveExportPathInternal() {
        Path videoFolder = findUserVideoFolder();

        Path base;
        if (videoFolder != null && isWritable(videoFolder)) {
            base = videoFolder;
        } else {
            // Fallback: user home
            base = getUserHome();
        }

        // Create VideoCompressor/export/
        Path exportDir = base.resolve("VideoCompressor").resolve("export");
        ensureDirectoryExists(exportDir);

        return exportDir;
    }

    /**
     * Attempts to detect the native video folder of the current user.
     *
     * @return the video folder or {@code null} if it cannot be determined
     */
    private static Path findUserVideoFolder() {
        Path home = getUserHome();

        // ---- Linux: XDG_VIDEOS_DIR environment variable ----
        String xdgVideos = System.getenv("XDG_VIDEOS_DIR");
        if (xdgVideos != null && !xdgVideos.isBlank()) {
            Path p = Paths.get(xdgVideos);
            if (Files.isDirectory(p)) {
                return p;
            }
        }

        // ---- Linux: Parse XDG user-dirs.dirs (locale-independent) ----
        Path xdgPath = findXdgVideoFolder(home);
        if (xdgPath != null) {
            return xdgPath;
        }

        // ---- macOS: Query Movies folder via osascript (locale-independent) ----
        Path macOsPath = findMacOsMoviesFolder();
        if (macOsPath != null) {
            return macOsPath;
        }

        // ---- Fallback: hardcoded folder names ----
        Path videos = home.resolve("Videos");
        if (Files.isDirectory(videos)) {
            return videos;
        }
        Path films = home.resolve("Filme");
        if (Files.isDirectory(films)) {
            return films;
        }

        return null;
    }

    /**
     * Parses the XDG user-dirs configuration file to find the video folder.
     * This is locale-independent because the config file contains the actual
     * folder names as set by the desktop environment (e.g. "Filme", "Videos",
     * "Videolar", etc.).
     *
     * @param home the user's home directory
     * @return the video folder path or {@code null} if not found
     */
    private static Path findXdgVideoFolder(Path home) {
        Path configPath = home.resolve(".config").resolve("user-dirs.dirs");
        if (!Files.exists(configPath)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("XDG_VIDEOS_DIR=")) {
                    String value = line.substring("XDG_VIDEOS_DIR=".length());
                    // Remove surrounding quotes
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    // Expand $HOME to actual home path
                    value = value.replace("$HOME", home.toString());
                    Path videoDir = Paths.get(value);
                    if (Files.isDirectory(videoDir)) {
                        return videoDir;
                    }
                    break;
                }
            }
        } catch (IOException ignored) {
            // Silently fall through to other detection methods
        }

        return null;
    }

    /**
     * Queries macOS for the Movies folder path using osascript.
     * This is locale-independent because macOS returns the actual folder
     * path regardless of the system language.
     *
     * @return the movies folder path or {@code null} if not on macOS or not found
     */
    private static Path findMacOsMoviesFolder() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "osascript",
                "-e",
                "path to movies folder as POSIX text"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                )
            ) {
                String path = reader.readLine();
                if (path != null && !path.isBlank()) {
                    Path moviesPath = Paths.get(path.trim());
                    if (Files.isDirectory(moviesPath)) {
                        return moviesPath;
                    }
                }
            }

            process.waitFor();
        } catch (Exception ignored) {
            // Silently fall through to other detection methods
        }

        return null;
    }

    /**
     * Returns the home path of the current user.
     */
    private static Path getUserHome() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            // Extremely rare fallback – should practically never happen
            return Paths.get(".");
        }
        return Paths.get(home);
    }

    /**
     * Checks whether a directory is writable.
     */
    private static boolean isWritable(Path dir) {
        if (!Files.exists(dir)) {
            return false;
        }
        return Files.isWritable(dir);
    }

    /**
     * Ensures that the specified directory exists.
     * If it cannot be created (e.g., missing permissions),
     * a fallback path in the temporary directory is used.
     */
    private static void ensureDirectoryExists(Path dir) {
        if (Files.exists(dir)) {
            return;
        }

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            // Fallback: temporary directory
            try {
                Path tempBase = Files.createTempDirectory(
                    "VideoCompressor_export_"
                );
                Path tempExport = tempBase.resolve("export");
                Files.createDirectories(tempExport);
                // Override the cached path with the fallback
                cachedExportPath = tempExport;
            } catch (IOException fallbackEx) {
                // Everything fails – use current directory
                Path lastResort = Paths.get("export");
                try {
                    Files.createDirectories(lastResort);
                } catch (IOException ignored) {
                    // There's nothing more we can do
                }
                cachedExportPath = lastResort;
            }
        }
    }

    /**
     * Resets the internal cache. Primarily intended for testing.
     */
    public static void resetCache() {
        cachedExportPath = null;
    }
}
