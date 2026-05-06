package de.pottgames.videocompressor.engine;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generic utility class for opening a file or folder path in the system's
 * native handler.
 *
 * <p>For directories, this opens the system file explorer. For files, this
 * opens the default application associated with the file type (e.g., video
 * players for video files, text editors for text files, etc.).</p>
 *
 * <p>Supports all major operating systems via the Java {@code Desktop} API
 * with platform-specific CLI fallbacks ({@code xdg-open}, {@code open},
 * {@code explorer}). Execution is asynchronous to avoid blocking the UI.</p>
 */
public final class PathOpener {

    /** Prevents instantiation – this is a pure utility class. */
    private PathOpener() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Opens the given path (file or directory) in the system's native handler.
     *
     * <p>This method runs asynchronously and does not block the caller.
     * It first attempts to use the Java {@link Desktop} API and falls back
     * to platform-specific CLI commands if that fails.</p>
     *
     * @param path the file or directory path to open
     * @return a {@link CompletableFuture} that completes when the path
     *         has been opened (or a failure has been logged)
     */
    public static CompletableFuture<Void> openAsync(Path path) {
        File file = path.toFile();

        return CompletableFuture.runAsync(() -> {
            try {
                if (
                    Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
                ) {
                    Desktop.getDesktop().open(file);
                    return;
                }
            } catch (IOException e) {
                System.err.println("Desktop.open() failed: " + e.getMessage());
            }

            // Fallback to platform-specific CLI
            try {
                openWithFallback(path);
            } catch (Exception e) {
                System.err.println(
                    "Failed to open path: " +
                        e.getMessage() +
                        " | Path: " +
                        path
                );
            }
        });
    }

    /**
     * Opens the given path using a platform-specific CLI command.
     *
     * <p>Supported platforms:</p>
     * <ul>
     *   <li>Linux — {@code xdg-open}</li>
     *   <li>macOS — {@code open}</li>
     *   <li>Windows — {@code explorer}</li>
     * </ul>
     *
     * @param path the file or directory path to open
     * @throws IOException          if the command cannot be executed or
     *                              returns a non-zero exit code
     * @throws InterruptedException if the process is interrupted while waiting
     */
    static void openWithFallback(Path path)
        throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();

        ProcessBuilder pb;
        if (os.contains("linux")) {
            pb = new ProcessBuilder("xdg-open", path.toString());
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", path.toString());
        } else if (os.contains("win")) {
            // On Windows, explorer opens directories directly.
            // For files, we need to use /select or just open the file.
            // explorer <file> works for most file types.
            pb = new ProcessBuilder("explorer", path.toString());
        } else {
            throw new IOException("Unknown OS: " + os);
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new IOException(
                "Fallback command exited with code " + process.exitValue()
            );
        }
    }
}
