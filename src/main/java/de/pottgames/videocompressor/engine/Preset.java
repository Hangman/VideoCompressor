package de.pottgames.videocompressor.engine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Record representing a video encoding preset configuration.
 */
public record Preset(
    String name,
    String description,
    VideoCodec videoCodec,
    int crf,
    boolean keepSourceResolution,
    int resolutionWidth,
    int resolutionHeight,
    double fps,
    VideoContainer container,

    boolean keepSourceAudio,
    AudioCodec audioCodec,
    int audioBitrate,
    boolean audioNormalize,
    boolean mixToMono,
    boolean fastStart,
    FfmpegPreset ffmpegPreset,
    Tune tune
) {
    public static final Path PRESET_FOLDER_PATH = Path.of("presets");
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
            VideoCodec.fromName(
                props.getProperty("preset.videoCodec", "libx264")
            ),
            Integer.parseInt(props.getProperty("preset.crf", "23")),
            Boolean.parseBoolean(
                props.getProperty("preset.keepSourceResolution", "false")
            ),
            Integer.parseInt(
                props.getProperty("preset.resolutionWidth", "1920")
            ),
            Integer.parseInt(
                props.getProperty("preset.resolutionHeight", "1080")
            ),
            Double.parseDouble(props.getProperty("preset.fps", "30")),
            VideoContainer.fromName(
                props.getProperty("preset.container", "mp4")
            ),
            Boolean.parseBoolean(
                props.getProperty("preset.keepSourceAudio", "true")
            ),
            AudioCodec.fromName(props.getProperty("preset.audioCodec", "aac")),
            Integer.parseInt(props.getProperty("preset.audioBitrate", "192")),
            Boolean.parseBoolean(
                props.getProperty("preset.audioNormalize", "false")
            ),
            Boolean.parseBoolean(
                props.getProperty("preset.mixToMono", "false")
            ),
            Boolean.parseBoolean(
                props.getProperty("preset.fastStart", "false")
            ),
            FfmpegPreset.fromName(
                props.getProperty("preset.ffmpegPreset", "medium")
            ),
            Tune.fromName(props.getProperty("preset.tune", "none"))
        );
    }

    /**
     * Loads a preset from a .properties file.
     *
     * @param path the path to the properties file
     * @return the loaded Preset
     */
    public static Preset fromFile(Path path) {
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load preset from " + path, e);
        }
        return fromProperties(props);
    }

    /**
     * Result of a preset validation.
     *
     * @param valid whether the preset configuration is valid
     * @param warnings non-critical issues that may indicate a problem
     * @param errors critical issues that make the preset unusable
     */
    public record ValidationResult(
        boolean valid,
        List<String> warnings,
        List<String> errors
    ) {}

    /**
     * Validates this preset for realistic, ffmpeg-compatible values and
     * problem-free combinations.
     *
     * @return a ValidationResult describing any issues found
     */
    public ValidationResult validate() {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // --- CRF ---
        if (crf < 0) {
            errors.add("CRF muss 0 oder größer sein (ist " + crf + ").");
        } else if (videoCodec == VideoCodec.AV1) {
            if (crf > 63) {
                errors.add(
                    "CRF für AV1 darf maximal 63 sein (ist " + crf + ")."
                );
            }
        } else {
            if (crf > 51) {
                errors.add("CRF darf maximal 51 sein (ist " + crf + ").");
            }
        }
        if (crf > 0 && crf <= 10) {
            warnings.add(
                "CRF " +
                    crf +
                    " bedeutet nahezu verlustfrei – sehr große Dateien."
            );
        }
        if (crf >= 35) {
            warnings.add("CRF " + crf + " führt zu sehr geringer Qualität.");
        }

        // --- Auflösung ---
        if (!keepSourceResolution) {
            if (resolutionWidth <= 0 || resolutionHeight <= 0) {
                errors.add(
                    "Auflösung muss positiv sein (ist " +
                        resolutionWidth +
                        "x" +
                        resolutionHeight +
                        ")."
                );
            } else {
                // Gerade Werte erforderlich für die meisten Codecs
                if (resolutionWidth % 2 != 0 || resolutionHeight % 2 != 0) {
                    errors.add(
                        "Auflösung muss gerade Werte haben (ist " +
                            resolutionWidth +
                            "x" +
                            resolutionHeight +
                            ")."
                    );
                }

                // Warnung: nicht durch 4 teilbar (suboptimal für viele Codecs)
                if (resolutionWidth % 4 != 0 || resolutionHeight % 4 != 0) {
                    warnings.add(
                        "Auflösung " +
                            resolutionWidth +
                            "x" +
                            resolutionHeight +
                            " ist nicht durch 4 teilbar – suboptimal für viele Codecs."
                    );
                }

                // Tippfehler-Erkennung für gängige Auflösungen
                checkResolutionTypo(
                    resolutionWidth,
                    resolutionHeight,
                    warnings
                );

                // Extremwerte
                if (resolutionWidth > 7680 || resolutionHeight > 4320) {
                    warnings.add(
                        "Auflösung " +
                            resolutionWidth +
                            "x" +
                            resolutionHeight +
                            " ist sehr hoch – stelle Hardware-Kapazität sicher."
                    );
                }
                if (resolutionWidth < 160 || resolutionHeight < 120) {
                    warnings.add(
                        "Auflösung " +
                            resolutionWidth +
                            "x" +
                            resolutionHeight +
                            " ist sehr niedrig."
                    );
                }
            }
        }

        // --- FPS ---
        if (fps <= 0) {
            errors.add("FPS muss größer als 0 sein (ist " + fps + ").");
        } else if (fps > 120) {
            warnings.add(
                "FPS " +
                    fps +
                    " ist sehr hoch – meist unnötig für Video-Kompression."
            );
        }

        // --- Audio-Bitrate ---
        if (!keepSourceAudio) {
            if (audioBitrate <= 0) {
                errors.add(
                    "Audio-Bitrate muss größer als 0 sein (ist " +
                        audioBitrate +
                        ")."
                );
            } else if (audioBitrate > 9999) {
                errors.add(
                    "Audio-Bitrate " +
                        audioBitrate +
                        " kbit/s ist unüblich hoch."
                );
            } else {
                if (!mixToMono && audioBitrate < 64) {
                    warnings.add(
                        "Audio-Bitrate " +
                            audioBitrate +
                            " kbit/s ist für Stereo sehr niedrig."
                    );
                }
                if (mixToMono && audioBitrate < 32) {
                    warnings.add(
                        "Audio-Bitrate " +
                            audioBitrate +
                            " kbit/s ist für Mono sehr niedrig."
                    );
                }
            }
        }

        // --- Codec-Container-Kompatibilität ---
        checkCodecContainerCompatibility(errors, warnings);

        // --- Tune-Codec-Kompatibilität ---
        if (tune != Tune.NONE && videoCodec == VideoCodec.VP9) {
            warnings.add("Tune-Option wird von VP9 nicht unterstützt.");
        }
        if (tune != Tune.NONE && videoCodec == VideoCodec.AV1) {
            warnings.add("Tune-Option wird von SVT-AV1 nicht unterstützt.");
        }

        // --- Audio-Normalisierung mit Opus/Vorbis ---
        if (
            !keepSourceAudio &&
            audioNormalize &&
            (audioCodec == AudioCodec.OPUS || audioCodec == AudioCodec.VORBIS)
        ) {
            warnings.add(
                "Lautstärken-Normalisierung funktioniert mit " +
                    audioCodec.getHumanName() +
                    " möglicherweise nicht zuverlässig."
            );
        }

        // --- Fast-Start nur bei MP4 sinnvoll ---
        if (fastStart && container != VideoContainer.MP4) {
            warnings.add("Fast-Start (moov atom) ist nur bei MP4 sinnvoll.");
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, warnings, errors);
    }

    /**
     * Checks if the given resolution looks like a typo of a common standard
     * resolution (e.g. 1921x1080 instead of 1920x1080).
     */
    private void checkResolutionTypo(int w, int h, List<String> warnings) {
        record Resolution(int w, int h) {}

        Resolution[] standards = {
            new Resolution(3840, 2160),
            new Resolution(2560, 1440),
            new Resolution(1920, 1080),
            new Resolution(1280, 720),
            new Resolution(854, 480),
            new Resolution(640, 360),
            new Resolution(320, 180),
            new Resolution(720, 480),
            new Resolution(640, 480),
        };

        for (Resolution std : standards) {
            int diffW = Math.abs(w - std.w);
            int diffH = Math.abs(h - std.h);
            if (diffW <= 2 && diffH <= 2 && (diffW > 0 || diffH > 0)) {
                warnings.add(
                    "Auflösung " +
                        w +
                        "x" +
                        h +
                        " ähnelt " +
                        std.w +
                        "x" +
                        std.h +
                        " – Tippfehler?"
                );
                break;
            }
        }
    }

    /**
     * Checks codec-container compatibility and reports errors or warnings.
     */
    private void checkCodecContainerCompatibility(
        List<String> errors,
        List<String> warnings
    ) {
        // VP9 und AV1 in MP4 haben eingeschränkte Player-Unterstützung
        if (container == VideoContainer.MP4) {
            if (videoCodec == VideoCodec.VP9 || videoCodec == VideoCodec.AV1) {
                warnings.add(
                    videoCodec.getHumanName() +
                        " in MP4 hat eingeschränkte Player-Unterstützung. MKV oder WebM bevorzugen."
                );
            }
            // AAC ist der Standard-Audio-Codec für MP4; Opus/Vorbis sind problematisch
            if (!keepSourceAudio) {
                if (
                    audioCodec == AudioCodec.OPUS ||
                    audioCodec == AudioCodec.VORBIS
                ) {
                    warnings.add(
                        audioCodec.getHumanName() +
                            " in MP4 wird von vielen Playern nicht unterstützt. AAC bevorzugen."
                    );
                }
            }
        }

        // WebM: AAC ist nicht gut unterstützt
        if (container == VideoContainer.WEBM && !keepSourceAudio) {
            if (audioCodec == AudioCodec.AAC) {
                warnings.add(
                    "AAC in WebM wird nicht gut unterstützt. Opus oder Vorbis bevorzugen."
                );
            }
        }

        // H.264/H.265 in WebM ist nicht Standard
        if (container == VideoContainer.WEBM) {
            if (
                videoCodec == VideoCodec.H264 || videoCodec == VideoCodec.H265
            ) {
                warnings.add(
                    videoCodec.getHumanName() +
                        " in WebM ist nicht Standard. VP9 oder AV1 bevorzugen."
                );
            }
        }
    }
}
