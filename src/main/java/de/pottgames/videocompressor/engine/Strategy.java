package de.pottgames.videocompressor.engine;

/**
 * Strategy utilities for computing and validating encoding presets
 * against the source video's actual properties.
 *
 * The central method {@link #calcEditingPreset(ProbeInfo, Preset)} performs
 * the final feasibility check and optimization before FFmpeg encoding.
 * It produces an "editing preset" that is guaranteed to be safe and
 * sensible for the given source material.
 */
public class Strategy {

    // ────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────

    /**
     * Computes an "editing preset" by starting from the desired preset and
     * applying two categories of modifications:
     *
     * <ol>
     *   <li><b>Sensible adjustments</b> – fix values that would be
     *       meaningless, harmful, or impossible given the source video's
     *       properties (no upscaling, no FPS increase, codec/container
     *       compatibility, etc.).</li>
     *   <li><b>Simplifications</b> – collapse redundant settings back to
     *       their "keep source" equivalents when the effective result is
     *       identical to the source.</li>
     * </ol>
     *
     * The returned preset is ready to be fed directly into FFmpeg without
     * further validation.
     *
     * @param sourceInfo     the probed source video information
     * @param desiredPreset  the user-chosen preset before adaptation
     * @return a corrected and simplified preset safe for FFmpeg encoding
     */
    public static Preset calcEditingPreset(
        ProbeInfo sourceInfo,
        Preset desiredPreset
    ) {
        // ── Start with copies of every field ────────────────────────────
        String name = "Editing Preset: " + sourceInfo.file().getName();
        String description =
            "Automatisch angepasstes Preset für " + sourceInfo.file().getName();

        VideoCodec videoCodec = desiredPreset.videoCodec();
        int crf = desiredPreset.crf();
        boolean keepSourceResolution = desiredPreset.keepSourceResolution();
        int resolutionWidth = desiredPreset.resolutionWidth();
        int resolutionHeight = desiredPreset.resolutionHeight();
        double fps = desiredPreset.fps();
        VideoContainer container = desiredPreset.container();

        boolean keepSourceAudio = desiredPreset.keepSourceAudio();
        AudioCodec audioCodec = desiredPreset.audioCodec();
        int audioBitrate = desiredPreset.audioBitrate();
        boolean audioNormalize = desiredPreset.audioNormalize();
        boolean mixToMono = desiredPreset.mixToMono();
        boolean fastStart = desiredPreset.fastStart();
        FfmpegPreset ffmpegPreset = desiredPreset.ffmpegPreset();
        Tune tune = desiredPreset.tune();

        // =================================================================
        // 1. SENSIBLE ADJUSTMENTS
        // =================================================================

        // ── 1a) Resolution: no upscaling ────────────────────────────────
        if (!keepSourceResolution) {
            // Clamp width and height so we never upscale
            int targetW = Math.min(
                resolutionWidth,
                sourceInfo.resolutionWidth()
            );
            int targetH = Math.min(
                resolutionHeight,
                sourceInfo.resolutionHeight()
            );

            // If the aspect ratio of the target resolution differs
            // significantly from the source, fall back to source resolution
            // entirely to avoid stretching / letterboxing artefacts.
            double sourceAR =
                (double) sourceInfo.resolutionWidth() /
                sourceInfo.resolutionHeight();
            double targetAR = (double) targetW / targetH;

            if (Math.abs(sourceAR - targetAR) > 0.05) {
                // Aspect ratios don't match – use source resolution
                targetW = sourceInfo.resolutionWidth();
                targetH = sourceInfo.resolutionHeight();
            }

            resolutionWidth = targetW;
            resolutionHeight = targetH;

            // Ensure even dimensions (required by most codecs)
            resolutionWidth = resolutionWidth - (resolutionWidth % 2);
            resolutionHeight = resolutionHeight - (resolutionHeight % 2);

            // Guard against zero dimensions after rounding
            if (resolutionWidth < 2) resolutionWidth = 2;
            if (resolutionHeight < 2) resolutionHeight = 2;
        }

        // ── 1b) FPS: no increase (with film-rate tolerance) ─────────────
        int sourceFps = sourceInfo.fps();
        if (sourceFps > 0) {
            // Allow small NTSC/film-rate rounding:
            //   23.976 → 24, 29.97 → 30, 59.94 → 60
            double effectiveSourceFps = roundFilmRate(sourceFps);
            double effectiveTargetFps = roundFilmRate((int) fps);

            if (fps > effectiveSourceFps) {
                // Check if this is a harmless film-rate rounding
                if (Math.abs(effectiveTargetFps - effectiveSourceFps) < 0.5) {
                    // e.g. 30 vs 29.97 – OK, keep target
                } else {
                    // Real increase – clamp down
                    fps = sourceFps;
                }
            }

            // Safety: never go below 1 fps
            if (fps < 1.0) fps = 1.0;
        }

        // ── 1c) CRF: clamp to codec-specific valid range ────────────────
        if (videoCodec == VideoCodec.AV1 || videoCodec == VideoCodec.VP9) {
            if (crf > 63) crf = 63;
        } else {
            if (crf > 51) crf = 51;
        }
        if (crf < 0) crf = 0;

        // ── 1d) Tune: VP9 / AV1 do not support tune ────────────────────
        if (
            (videoCodec == VideoCodec.VP9 || videoCodec == VideoCodec.AV1) &&
            tune != Tune.NONE
        ) {
            tune = Tune.NONE;
        }

        // ── 1e) fastStart: only meaningful for MP4 ─────────────────────
        if (fastStart && container != VideoContainer.MP4) {
            fastStart = false;
        }

        // ── 1f) Container-codec compatibility fixes ────────────────────
        // VP9 / AV1 in MP4 → switch to MKV for broad compatibility
        if (
            container == VideoContainer.MP4 &&
            (videoCodec == VideoCodec.VP9 || videoCodec == VideoCodec.AV1)
        ) {
            container = VideoContainer.MKV;
            // fastStart is MP4-only, so disable it now
            fastStart = false;
        }

        // H.264 / H.265 in WebM → switch to MP4 (standard container)
        if (
            container == VideoContainer.WEBM &&
            (videoCodec == VideoCodec.H264 || videoCodec == VideoCodec.H265)
        ) {
            container = VideoContainer.MP4;
        }

        // ── 1g) Audio codec-container compatibility ────────────────────
        // AAC in WebM is poorly supported → switch to Opus
        if (
            container == VideoContainer.WEBM &&
            !keepSourceAudio &&
            audioCodec == AudioCodec.AAC
        ) {
            audioCodec = AudioCodec.OPUS;
        }

        // Opus / Vorbis in MP4 is problematic → switch to AAC
        if (
            container == VideoContainer.MP4 &&
            !keepSourceAudio &&
            (audioCodec == AudioCodec.OPUS || audioCodec == AudioCodec.VORBIS)
        ) {
            audioCodec = AudioCodec.AAC;
        }

        // ── 1h) audioNormalize with Opus/Vorbis is unreliable ──────────
        if (
            !keepSourceAudio &&
            audioNormalize &&
            (audioCodec == AudioCodec.OPUS || audioCodec == AudioCodec.VORBIS)
        ) {
            audioNormalize = false;
        }

        // ── 1j) audioBitrate: clamp to reasonable bounds ───────────────
        if (!keepSourceAudio) {
            if (audioBitrate > 9999) audioBitrate = 9999;
            if (audioBitrate <= 0) audioBitrate = 128;
        }

        // =================================================================
        // 2. SIMPLIFICATIONS
        // =================================================================

        // ── 2a) Resolution matches source → use keepSourceResolution ────
        if (
            !keepSourceResolution &&
            resolutionWidth == sourceInfo.resolutionWidth() &&
            resolutionHeight == sourceInfo.resolutionHeight()
        ) {
            keepSourceResolution = true;
        }

        // ── 2b) FPS matches source → no adjustment needed (keep value) ─
        // Nothing to simplify here; the fps value is already correct.

        // ── 2c) keepSourceAudio overrides meaningless audio settings ────
        if (keepSourceAudio) {
            // When keeping source audio, normalize and mixToMono are
            // meaningless because we skip audio re-encoding entirely.
            audioNormalize = false;
            mixToMono = false;
        }

        // ── 2e) If everything matches source, describe it in the name ──
        if (
            keepSourceResolution &&
            fps == sourceInfo.fps() &&
            keepSourceAudio &&
            !fastStart &&
            tune == Tune.NONE
        ) {
            description =
                "Quellmaterial wird weitgehend beibehalten (Codec/Qualität ggf. angepasst)";
        }

        // =================================================================
        // Return
        // =================================================================
        return new Preset(
            name,
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

    // ────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────

    /**
     * Rounds common NTSC / film-rate values to their nominal integer
     * equivalents for comparison purposes.
     *
     * <ul>
     *   <li>23 → 24 (23.976 film)</li>
     *   <li>29 → 30 (NTSC)</li>
     *   <li>59 → 60 (NTSC double)</li>
     * </ul>
     *
     * @param fps the integer FPS value from ffprobe
     * @return the "effective" FPS for comparison
     */
    private static double roundFilmRate(int fps) {
        return switch (fps) {
            case 23 -> 24.0;
            case 29 -> 30.0;
            case 59 -> 60.0;
            default -> fps;
        };
    }
}
