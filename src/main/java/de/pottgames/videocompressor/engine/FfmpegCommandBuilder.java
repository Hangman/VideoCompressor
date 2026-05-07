package de.pottgames.videocompressor.engine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that builds the argument list required for {@link Ffmpeg#execute}
 * from a {@link ProbeInfo}, a {@link Preset}, and an output directory.
 *
 * <p>The output filename retains the original name of the input file.
 * Only if the output file would overwrite the source file (same
 * absolute path), the prefix {@code vc_} is prepended to prevent
 * overwriting.</p>
 */
public final class FfmpegCommandBuilder {

    private FfmpegCommandBuilder() {
        // Utility class – no instantiation
    }

    /**
     * Builds the complete list of FFmpeg CLI arguments for a
     * compression job.
     *
     * @param probeInfo  the probe information (contains the input file)
     * @param preset     the encoding preset to use
     * @param outputDir  the directory where the output is written
     * @return a {@code List&lt;String&gt;} that can be passed directly to
     *         {@link Ffmpeg#execute(List, java.util.function.Consumer)}
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

        // ── Video Codec ────────────────────────────────────────────────
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

        // ── Video Filters (Scaling + Optional Normalization) ───────────
        String vfParts = "";
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

        // ── Audio Filters (Loudness Normalization) ─────────────────────
        if (preset.audioNormalize()) {
            args.add("-af");
            args.add("loudnorm");
        }

        // ── Fast Start (move moov atom to front) ───────────────────────
        if (preset.fastStart()) {
            args.add("-movflags");
            args.add("+faststart");
        }

        // ── Output File ────────────────────────────────────────────────
        String outputFileName = buildOutputFileName(
            inputFile,
            preset.container(),
            outputDir
        );
        Path outputPath = outputDir.resolve(outputFileName);
        args.add(outputPath.toString());

        return args;
    }

    /**
     * Builds the output filename. The original filename is retained.
     * Only if the output file would overwrite the source file,
     * the prefix {@code vc_} is prepended.
     *
     * <p>Example (no overwriting): {@code "urlaub.mp4"} → {@code "urlaub.mkv"}</p>
     * <p>Example (overwriting would occur): {@code "urlaub.mp4"} → {@code "vc_urlaub.mp4"}</p>
     *
     * @param inputFile the source file
     * @param container the target container format
     * @param outputDir the output directory
     * @return the output filename (no path)
     */
    public static String buildOutputFileName(
        File inputFile,
        VideoContainer container,
        Path outputDir
    ) {
        String name = inputFile.getName();
        int lastDot = name.lastIndexOf('.');
        String baseName = lastDot > 0 ? name.substring(0, lastDot) : name;
        String outputName = baseName + "." + container.getExtension();

        // Check if output path would overwrite the input file
        Path outputPath = outputDir.resolve(outputName);
        boolean wouldOverwrite = outputPath
            .toAbsolutePath()
            .equals(inputFile.toPath().toAbsolutePath());

        if (wouldOverwrite) {
            return "vc_" + outputName;
        }
        return outputName;
    }
}
