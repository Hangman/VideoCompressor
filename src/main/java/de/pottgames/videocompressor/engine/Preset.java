package de.pottgames.videocompressor.engine;

import de.pottgames.videocompressor.i18n.I18n;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    @Override
    public int hashCode() {
        return Objects.hash(
            audioBitrate,
            audioCodec,
            audioNormalize,
            container,
            crf,
            fastStart,
            ffmpegPreset,
            fps,
            keepSourceAudio,
            keepSourceResolution,
            mixToMono,
            resolutionHeight,
            resolutionWidth,
            tune,
            videoCodec
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Preset other = (Preset) obj;
        return (
            audioBitrate == other.audioBitrate &&
            audioCodec == other.audioCodec &&
            audioNormalize == other.audioNormalize &&
            container == other.container &&
            crf == other.crf &&
            fastStart == other.fastStart &&
            ffmpegPreset == other.ffmpegPreset &&
            Double.doubleToLongBits(fps) ==
            Double.doubleToLongBits(other.fps) &&
            keepSourceAudio == other.keepSourceAudio &&
            keepSourceResolution == other.keepSourceResolution &&
            mixToMono == other.mixToMono &&
            resolutionHeight == other.resolutionHeight &&
            resolutionWidth == other.resolutionWidth &&
            tune == other.tune &&
            videoCodec == other.videoCodec
        );
    }

    /**
     * Returns a copy of this preset with a different name.
     * Used to mark a modified preset as "Custom".
     */
    public Preset withName(String newName) {
        return new Preset(
            newName,
            description,
            videoCodec,
            crf,
            keepSourceResolution,
            resolutionWidth,
            resolutionHeight,
            fps,
            container,
            keepSourceAudio,
            audioCodec,
            audioBitrate,
            audioNormalize,
            mixToMono,
            fastStart,
            ffmpegPreset,
            tune
        );
    }

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
        // Resolve name: if isNameI18nKey=true, treat as i18n key; otherwise use raw text
        String nameRaw = props.getProperty("preset.name", "Default");
        String name = Boolean.parseBoolean(
            props.getProperty("preset.isNameI18nKey", "false")
        )
            ? I18n.getOrDefault(nameRaw, nameRaw)
            : nameRaw;

        // Resolve description: if isDescriptionI18nKey=true, treat as i18n key; otherwise use raw text
        String descRaw = props.getProperty("preset.description", "");
        String description = Boolean.parseBoolean(
            props.getProperty("preset.isDescriptionI18nKey", "false")
        )
            ? I18n.getOrDefault(descRaw, descRaw)
            : descRaw;

        return new Preset(
            name,
            description,
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
        {
            int crfMin = videoCodec.getCrfMin();
            int crfMax = videoCodec.getCrfMax();
            if (crf < crfMin) {
                errors.add(
                    I18n.get("preset.validate.crf_too_low", crfMin, crf)
                );
            }
            if (crf > crfMax) {
                errors.add(
                    I18n.get(
                        "preset.validate.crf_too_high",
                        crfMax,
                        videoCodec.getHumanName(),
                        crf
                    )
                );
            }
        }
        if (crf > 0 && crf <= 10) {
            warnings.add(I18n.get("preset.validate.crf_lossless", crf));
        }
        if (crf >= 35) {
            warnings.add(I18n.get("preset.validate.crf_low_quality", crf));
        }

        // --- Auflösung ---
        if (!keepSourceResolution) {
            if (resolutionWidth <= 0 || resolutionHeight <= 0) {
                errors.add(
                    I18n.get(
                        "preset.validate.resolution_not_positive",
                        resolutionWidth,
                        resolutionHeight
                    )
                );
            } else {
                // Gerade Werte erforderlich für die meisten Codecs
                if (resolutionWidth % 2 != 0 || resolutionHeight % 2 != 0) {
                    errors.add(
                        I18n.get(
                            "preset.validate.resolution_not_even",
                            resolutionWidth,
                            resolutionHeight
                        )
                    );
                }

                // Warnung: nicht durch 4 teilbar (suboptimal für viele Codecs)
                if (resolutionWidth % 4 != 0 || resolutionHeight % 4 != 0) {
                    warnings.add(
                        I18n.get(
                            "preset.validate.resolution_not_div4",
                            resolutionWidth,
                            resolutionHeight
                        )
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
                        I18n.get(
                            "preset.validate.resolution_very_high",
                            resolutionWidth,
                            resolutionHeight
                        )
                    );
                }
                if (resolutionWidth < 160 || resolutionHeight < 120) {
                    warnings.add(
                        I18n.get(
                            "preset.validate.resolution_very_low",
                            resolutionWidth,
                            resolutionHeight
                        )
                    );
                }
            }
        }

        // --- FPS ---
        if (fps <= 0) {
            errors.add(I18n.get("preset.validate.fps_not_positive", fps));
        } else if (fps > 120) {
            warnings.add(I18n.get("preset.validate.fps_very_high", fps));
        }

        // --- Audio-Bitrate ---
        if (!keepSourceAudio) {
            if (audioBitrate <= 0) {
                errors.add(
                    I18n.get(
                        "preset.validate.audio_bitrate_not_positive",
                        audioBitrate
                    )
                );
            } else if (audioBitrate > 9999) {
                errors.add(
                    I18n.get(
                        "preset.validate.audio_bitrate_too_high",
                        audioBitrate
                    )
                );
            } else {
                if (!mixToMono && audioBitrate < 64) {
                    warnings.add(
                        I18n.get(
                            "preset.validate.audio_bitrate_stereo_low",
                            audioBitrate
                        )
                    );
                }
                if (mixToMono && audioBitrate < 32) {
                    warnings.add(
                        I18n.get(
                            "preset.validate.audio_bitrate_mono_low",
                            audioBitrate
                        )
                    );
                }
            }
        }

        // --- Codec-Container-Kompatibilität ---
        checkCodecContainerCompatibility(errors, warnings);

        // --- Tune-Codec-Kompatibilität ---
        if (tune != Tune.NONE && videoCodec == VideoCodec.VP9) {
            warnings.add(I18n.get("preset.validate.tune_unsupported_vp9"));
        }
        if (tune != Tune.NONE && videoCodec == VideoCodec.AV1) {
            warnings.add(I18n.get("preset.validate.tune_unsupported_av1"));
        }
        if (videoCodec == VideoCodec.H265 && !tune.isSupportedByH265()) {
            errors.add(
                I18n.get(
                    "preset.validate.tune_unsupported_h265",
                    tune.getHumanName()
                )
            );
        }

        // --- Audio-Normalisierung mit Opus/Vorbis ---
        if (
            !keepSourceAudio &&
            audioNormalize &&
            (audioCodec == AudioCodec.OPUS || audioCodec == AudioCodec.VORBIS)
        ) {
            warnings.add(
                I18n.get(
                    "preset.validate.normalize_unreliable",
                    audioCodec.getHumanName()
                )
            );
        }

        // --- Fast-Start nur bei MP4 sinnvoll ---
        if (fastStart && container != VideoContainer.MP4) {
            warnings.add(I18n.get("preset.validate.faststart_mp4_only"));
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
                    I18n.get(
                        "preset.validate.resolution_typo",
                        w,
                        h,
                        std.w,
                        std.h
                    )
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
                    I18n.get(
                        "preset.validate.codec_mp4_limited",
                        videoCodec.getHumanName()
                    )
                );
            }
            // AAC ist der Standard-Audio-Codec für MP4; Opus/Vorbis sind problematisch
            if (!keepSourceAudio) {
                if (
                    audioCodec == AudioCodec.OPUS ||
                    audioCodec == AudioCodec.VORBIS
                ) {
                    warnings.add(
                        I18n.get(
                            "preset.validate.audio_mp4_unsupported",
                            audioCodec.getHumanName()
                        )
                    );
                }
            }
        }

        // WebM: AAC ist nicht gut unterstützt
        if (container == VideoContainer.WEBM && !keepSourceAudio) {
            if (audioCodec == AudioCodec.AAC) {
                warnings.add(I18n.get("preset.validate.aac_webm_unsupported"));
            }
        }

        // H.264/H.265 in WebM ist nicht Standard
        if (container == VideoContainer.WEBM) {
            if (
                videoCodec == VideoCodec.H264 || videoCodec == VideoCodec.H265
            ) {
                warnings.add(
                    I18n.get(
                        "preset.validate.codec_webm_nonstandard",
                        videoCodec.getHumanName()
                    )
                );
            }
        }
    }
}
