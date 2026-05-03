package de.pottgames.videocompressor.engine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Glue-Klasse, die die für {@link Ffmpeg#execute} benötigte Argument-Liste aus
 * einem {@link ProbeInfo}, einem {@link Preset} und einem Output-Verzeichnis
 * zusammenbaut.
 *
 * <p>Der Ausgabe-Dateiname wird aus dem Eingabe-Dateinamen, dem Prefix {@code vc_}
 * und der Dateierweiterung des im Preset konfigurierten {@link VideoContainer}
 * zusammengesetzt.</p>
 */
public final class FfmpegCommandBuilder {

    private FfmpegCommandBuilder() {
        // Utility-Klasse – keine Instanziierung
    }

    /**
     * Baut die vollständige Liste von FFmpeg-CLI-Argumenten für einen
     * Kompressions-Job.
     *
     * @param probeInfo  die Probe-Informationen (enthält die Input-Datei)
     * @param preset     das zu verwendende Encoding-Preset
     * @param outputDir  das Verzeichnis, in das die Ausgabe geschrieben wird
     * @return eine {@code List&lt;String&gt;}, die direkt an
     *         {@link Ffmpeg#execute(List, java.util.function.Consumer)} übergeben
     *         werden kann
     */
    public static List<String> buildCommand(
        ProbeInfo probeInfo,
        Preset preset,
        Path outputDir
    ) {
        List<String> args = new ArrayList<>();

        // ── Input ──────────────────────────────────────────────────────
        File inputFile = probeInfo.file();
        args.add("-i");
        args.add(inputFile.getAbsolutePath());

        // ── Video-Codec ────────────────────────────────────────────────
        args.add("-c:v");
        args.add(preset.videoCodec().getFfmpegName());

        // ── CRF / Quality ──────────────────────────────────────────────
        // VP9 uses -cq instead of -crf
        if (preset.videoCodec() == VideoCodec.VP9) {
            args.add("-cq");
        } else {
            args.add("-crf");
        }
        args.add(String.valueOf(preset.crf()));

        // ── FFmpeg Preset (Speed/Quality Trade-off) ────────────────────
        // SVT-AV1 uses numeric presets (0-13), others use textual names
        args.add("-preset");
        if (preset.videoCodec() == VideoCodec.AV1) {
            args.add(String.valueOf(preset.ffmpegPreset().getAv1Preset()));
        } else {
            args.add(preset.ffmpegPreset().getFfmpegName());
        }

        // ── Tune ───────────────────────────────────────────────────────
        if (preset.tune() != Tune.NONE) {
            args.add("-tune");
            args.add(preset.tune().getFfmpegName());
        }

        // ── Video-Filter (Skalierung + eventuelle Normalisierung) ──────
        String vfParts = new String();
        if (!preset.keepSourceResolution()) {
            vfParts =
                "scale=" +
                preset.resolutionWidth() +
                ":" +
                preset.resolutionHeight() +
                ":force_original_aspect_ratio=decrease";
        }
        if (!vfParts.isEmpty()) {
            args.add("-vf");
            args.add(vfParts);
        }

        // ── FPS ────────────────────────────────────────────────────────
        args.add("-r");
        args.add(String.format(java.util.Locale.US, "%.3f", preset.fps()));

        // ── Audio ──────────────────────────────────────────────────────
        if (preset.keepSourceAudio()) {
            args.add("-c:a");
            args.add("copy");
        } else {
            args.add("-c:a");
            args.add(preset.audioCodec().getFfmpegName());
            args.add("-b:a");
            args.add(preset.audioBitrate() + "k");

            if (preset.mixToMono()) {
                args.add("-ac");
                args.add("1");
            }
        }

        // ── Audio-Filter (Lautstärken-Normalisierung) ──────────────────
        if (preset.audioNormalize()) {
            args.add("-af");
            args.add("loudnorm");
        }

        // ── Fast-Start (moov atom voranstellen) ────────────────────────
        if (preset.fastStart()) {
            args.add("-movflags");
            args.add("+faststart");
        }

        // ── Output-Datei ───────────────────────────────────────────────
        String outputFileName = buildOutputFileName(
            inputFile,
            preset.container()
        );
        Path outputPath = outputDir.resolve(outputFileName);
        args.add(outputPath.toString());

        return args;
    }

    /**
     * Baut den Ausgabe-Dateinamen aus dem Eingabe-Dateinamen, dem Prefix
     * {@code vc_} und der Dateierweiterung des Ziel-Containers.
     *
     * <p>Beispiel: {@code "urlaub.mp4"} → {@code "vc_urlaub.mkv"}</p>
     *
     * @param inputFile die Quelldatei
     * @param container das Ziel-Container-Format
     * @return der Ausgabe-Dateiname (kein Pfad)
     */
    public static String buildOutputFileName(
        File inputFile,
        VideoContainer container
    ) {
        String name = inputFile.getName();
        int lastDot = name.lastIndexOf('.');
        String baseName = lastDot > 0 ? name.substring(0, lastDot) : name;
        return "vc_" + baseName + "." + container.getExtension();
    }
}
