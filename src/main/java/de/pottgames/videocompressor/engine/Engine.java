package de.pottgames.videocompressor.engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Central engine for video processing configuration and validation.
 * Manages preset loading and provides access to FFMPEG tool paths.
 *
 * Initialize asynchronously via {@link #initialize()} before use.
 */
public class Engine {

    // ── Static utility members (unchanged) ──────────────────────────────

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
     * that FFMPEG can process.
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

    // ── Instance state ──────────────────────────────────────────────────

    private final List<Preset> presets;
    private final boolean initialized;

    private Engine(List<Preset> presets, boolean initialized) {
        this.presets = presets;
        this.initialized = initialized;
    }

    // ── Async initialization ────────────────────────────────────────────

    /**
     * Asynchronously initializes the engine by loading all preset
     * configuration files from the presets folder.
     *
     * @return a CompletableFuture that completes with the initialized Engine instance
     */
    public static CompletableFuture<Engine> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[Engine] Loading presets...");

            List<Preset> loadedPresets;
            try (var stream = Files.list(Preset.PRESET_FOLDER_PATH)) {
                loadedPresets = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".properties"))
                    .map(p -> {
                        try {
                            return Preset.fromFile(p);
                        } catch (Exception e) {
                            System.out.println(
                                "[Engine] Warning: Failed to load preset from " +
                                    p.getFileName() +
                                    ": " +
                                    e.getMessage()
                            );
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            } catch (java.io.IOException e) {
                throw new RuntimeException(
                    "Failed to read presets folder: " +
                        Preset.PRESET_FOLDER_PATH,
                    e
                );
            }

            if (loadedPresets.isEmpty()) {
                System.out.println(
                    "[Engine] Warning: No preset files found. Using empty list."
                );
            } else {
                System.out.println(
                    "[Engine] Loaded " + loadedPresets.size() + " preset(s)."
                );
            }

            return new Engine(List.copyOf(loadedPresets), true);
        });
    }

    // ── Instance accessors ──────────────────────────────────────────────

    /**
     * @return {@code true} if this engine instance has been fully initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return an unmodifiable list of all loaded presets
     */
    public List<Preset> getPresets() {
        return presets;
    }

    /**
     * Returns the first preset (default) if available.
     *
     * @return the default preset or {@code null} if no presets were loaded
     */
    public Preset getDefaultPreset() {
        return presets.isEmpty() ? null : presets.get(0);
    }
}
