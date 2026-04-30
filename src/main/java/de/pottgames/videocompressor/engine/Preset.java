package de.pottgames.videocompressor.engine;

import java.io.InputStream;
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
    public static Preset getDefault() {
        Properties props = new Properties();
        try (
            InputStream is = Preset.class.getClassLoader().getResourceAsStream(
                "presets/default.properties"
            )
        ) {
            if (is == null) {
                // Fallback: try loading from file system (outside resources)
                java.io.File file = new java.io.File(
                    "presets/default.properties"
                );
                if (file.exists()) {
                    try (InputStream fis = new java.io.FileInputStream(file)) {
                        props.load(fis);
                    }
                } else {
                    throw new RuntimeException(
                        "Could not find presets/default.properties"
                    );
                }
            } else {
                props.load(is);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load default preset", e);
        }

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
