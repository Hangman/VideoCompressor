package de.pottgames.videocompressor.engine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Record representing a video encoding preset configuration.
 */
public record Preset(
    String name,
    String description,
    String codec,
    int crf,
    int resolutionWidth,
    int resolutionHeight,
    int fps,
    int maxFileSize,
    int audioBitrate,
    boolean audioNormalize,
    boolean fastStart,
    String ffmpegPreset,
    String tune
) {
    private static final Path DEFAULT_PRESET_PATH = Path.of(
        "presets",
        "default.properties"
    );

    public static Preset getDefault() {
        Properties props = new Properties();
        try {
            props.load(
                Files.newBufferedReader(
                    DEFAULT_PRESET_PATH,
                    StandardCharsets.UTF_8
                )
            );
        } catch (java.io.IOException e) {
            throw new RuntimeException(
                "Failed to load default preset from " + DEFAULT_PRESET_PATH,
                e
            );
        }

        return fromProperties(props);
    }

    private static Preset fromProperties(Properties props) {
        return new Preset(
            props.getProperty("preset.name", "Default"),
            props.getProperty("preset.description", ""),
            props.getProperty("preset.codec", "libx264"),
            Integer.parseInt(props.getProperty("preset.crf", "23")),
            Integer.parseInt(
                props.getProperty("preset.resolutionWidth", "1920")
            ),
            Integer.parseInt(
                props.getProperty("preset.resolutionHeight", "1080")
            ),
            Integer.parseInt(props.getProperty("preset.fps", "30")),
            Integer.parseInt(props.getProperty("preset.maxFileSize", "0")),
            Integer.parseInt(props.getProperty("preset.audioBitrate", "192")),
            Boolean.parseBoolean(
                props.getProperty("preset.audioNormalize", "false")
            ),
            Boolean.parseBoolean(
                props.getProperty("preset.fastStart", "false")
            ),
            props.getProperty("preset.ffmpegPreset", "medium"),
            props.getProperty("preset.tune", "none")
        );
    }
}
