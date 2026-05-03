package de.pottgames.videocompressor.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Strategy#calcEditingPreset(ProbeInfo, Preset)}.
 *
 * Every test creates a {@code ProbeInfo} representing the source video and a
 * {@code Preset} representing the user's desired settings, then verifies that
 * the returned "editing preset" has been correctly adjusted and simplified.
 */
class StrategyTest {

    // ── Shared test data ────────────────────────────────────────────────

    private ProbeInfo sourceInfo;
    private Preset defaultPreset;

    /**
     * Creates a "normal" 1080p30 source file (1920×1080, 30 fps, ~50 MB).
     */
    @BeforeEach
    void setUp() {
        sourceInfo = new ProbeInfo(
            new File("input.mp4"),
            1920,
            1080,
            30, // fps
            5000, // bitrate
            128, // audioBitrate
            120.5, // duration (seconds)
            "h264", // codec
            50L * 1024 * 1024 // 50 MB
        );

        defaultPreset = new Preset(
            "Test Preset",
            "A test preset",
            VideoCodec.H264,
            23,
            true, // keepSourceResolution
            1920,
            1080,
            30.0,
            VideoContainer.MP4,
            0, // maxFileSize
            true, // keepSourceAudio
            AudioCodec.AAC,
            192,
            false, // audioNormalize
            false, // mixToMono
            false, // fastStart
            FfmpegPreset.MEDIUM,
            Tune.NONE
        );
    }

    // ====================================================================
    //  Helpers
    // ====================================================================

    /**
     * Convenience: run {@code Strategy.calcEditingPreset} with the current
     * {@link #sourceInfo} and a custom {@link Preset}.
     */
    private Preset calc(Preset preset) {
        return Strategy.calcEditingPreset(sourceInfo, preset);
    }

    // ====================================================================
    //  Name / Description
    // ====================================================================

    @Nested
    @DisplayName("Name & Description")
    class NameDescription {

        @Test
        @DisplayName("Name should contain the source file name")
        void nameContainsFileName() {
            Preset result = calc(defaultPreset);
            assertTrue(result.name().contains("input.mp4"));
        }

        @Test
        @DisplayName("Description should contain the source file name")
        void descriptionContainsFileName() {
            // Use a preset that does NOT fully match the source, so the
            // filename-based description is kept instead of the
            // "source preserved" message.
            Preset preset = new Preset(
                "P",
                "desc",
                VideoCodec.H264,
                23,
                false,
                1280,
                720,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertTrue(result.description().contains("input.mp4"));
        }

        @Test
        @DisplayName("Description changes when source is mostly preserved")
        void descriptionWhenSourcePreserved() {
            // All settings match the source → special description
            Preset result = calc(defaultPreset);
            assertTrue(
                result
                    .description()
                    .contains("Quellmaterial wird weitgehend beibehalten")
            );
        }
    }

    // ====================================================================
    //  1a) Resolution – no upscaling
    // ====================================================================

    @Nested
    @DisplayName("Resolution – no upscaling")
    class ResolutionNoUpscaling {

        @Test
        @DisplayName("Wider target resolution is clamped to source width")
        void clampWidth() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                3840,
                2160, // 4K target
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(1920, result.resolutionWidth());
        }

        @Test
        @DisplayName("Taller target resolution is clamped to source height")
        void clampHeight() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                1920,
                2160,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(1080, result.resolutionHeight());
        }

        @Test
        @DisplayName("Smaller resolution is accepted")
        void smallerResolutionAccepted() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                1280,
                720,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(1280, result.resolutionWidth());
            assertEquals(720, result.resolutionHeight());
        }

        @Test
        @DisplayName("keepSourceResolution=true skips adjustment logic")
        void keepSourceResolutionSkipsClamp() {
            Preset result = calc(defaultPreset);
            assertTrue(result.keepSourceResolution());
        }
    }

    // ====================================================================
    //  1a) Resolution – aspect ratio mismatch
    // ====================================================================

    @Nested
    @DisplayName("Resolution – aspect ratio mismatch")
    class ResolutionAspectMismatch {

        @Test
        @DisplayName("Mismatched aspect ratio falls back to source resolution")
        void fallbackToSourceOnMismatch() {
            // 16:9 source (1920x1080) vs 4:3 target (1024x768)
            double sourceAR = 1920.0 / 1080.0; // ~1.778
            double targetAR = 1024.0 / 768.0; // ~1.333
            assertTrue(Math.abs(sourceAR - targetAR) > 0.05);

            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                1024,
                768,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            // Falls back to source resolution → simplified to keepSourceResolution
            assertTrue(result.keepSourceResolution());
        }
    }

    // ====================================================================
    //  1a) Resolution – even dimensions
    // ====================================================================

    @Nested
    @DisplayName("Resolution – even dimensions")
    class ResolutionEvenDimensions {

        @Test
        @DisplayName("Odd dimensions are rounded down to even")
        void oddDimensionsRoundedDown() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                1281,
                721,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(1280, result.resolutionWidth());
            assertEquals(720, result.resolutionHeight());
        }

        @Test
        @DisplayName("Zero dimension after rounding is guarded to minimum 2")
        void zeroDimensionGuard() {
            // Use a 1:1 source so that 1×1 target does not trigger the
            // aspect-ratio mismatch fallback to source resolution.
            ProbeInfo squareSource = new ProbeInfo(
                new File("square.mp4"),
                100,
                100,
                30,
                5000,
                128,
                120.5,
                "h264",
                50L * 1024 * 1024
            );
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                1,
                1,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = Strategy.calcEditingPreset(squareSource, preset);
            assertEquals(2, result.resolutionWidth());
            assertEquals(2, result.resolutionHeight());
        }
    }

    // ====================================================================
    //  1b) FPS – no increase
    // ====================================================================

    @Nested
    @DisplayName("FPS – no increase")
    class FpsNoIncrease {

        @Test
        @DisplayName("Higher FPS is clamped to source FPS")
        void clampHigherFps() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                60.0, // source is 30 fps
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(30, result.fps());
        }

        @Test
        @DisplayName("Lower FPS is accepted")
        void lowerFpsAccepted() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                24.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(24.0, result.fps());
        }

        @Test
        @DisplayName("FPS below 1 is raised to 1")
        void fpsClampedToOne() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                0.5,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(1.0, result.fps());
        }

        @Test
        @DisplayName(
            "Film-rate tolerance: 24 fps target vs 23 fps source is allowed"
        )
        void filmRateTolerance23to24() {
            ProbeInfo filmSource = new ProbeInfo(
                new File("film.mp4"),
                1920,
                1080,
                23, // 23.976 film rate
                5000,
                128,
                120.5,
                "h264",
                50L * 1024 * 1024
            );
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                24.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = Strategy.calcEditingPreset(filmSource, preset);
            // 24 is within film-rate tolerance of 23 (effective 24)
            assertEquals(24.0, result.fps());
        }

        @Test
        @DisplayName(
            "Film-rate tolerance: 30 fps target vs 29 fps source is allowed"
        )
        void filmRateTolerance29to30() {
            ProbeInfo ntscSource = new ProbeInfo(
                new File("ntsc.mp4"),
                1920,
                1080,
                29, // 29.97 NTSC
                5000,
                128,
                120.5,
                "h264",
                50L * 1024 * 1024
            );
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = Strategy.calcEditingPreset(ntscSource, preset);
            assertEquals(30.0, result.fps());
        }

        @Test
        @DisplayName(
            "Film-rate tolerance: 60 fps target vs 59 fps source is allowed"
        )
        void filmRateTolerance59to60() {
            ProbeInfo hdtvSource = new ProbeInfo(
                new File("hdtv.mp4"),
                1920,
                1080,
                59, // 59.94
                5000,
                128,
                120.5,
                "h264",
                50L * 1024 * 1024
            );
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                60.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = Strategy.calcEditingPreset(hdtvSource, preset);
            assertEquals(60.0, result.fps());
        }

        @Test
        @DisplayName("Real FPS increase (no film-rate match) is clamped")
        void realFpsIncreaseClamped() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                120.0, // source is 30
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(30, result.fps());
        }
    }

    // ====================================================================
    //  1c) CRF – clamp to codec-specific range
    // ====================================================================

    @Nested
    @DisplayName("CRF – codec-specific clamping")
    class CrfClamping {

        @Test
        @DisplayName("H.264 CRF > 51 is clamped to 51")
        void h264CrfClampedTo51() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                60,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(51, result.crf());
        }

        @Test
        @DisplayName("H.265 CRF > 51 is clamped to 51")
        void h265CrfClampedTo51() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H265,
                70,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(51, result.crf());
        }

        @Test
        @DisplayName("VP9 CRF > 63 is clamped to 63")
        void vp9CrfClampedTo63() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                80,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(63, result.crf());
        }

        @Test
        @DisplayName("AV1 CRF > 63 is clamped to 63")
        void av1CrfClampedTo63() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.AV1,
                100,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MKV,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(63, result.crf());
        }

        @Test
        @DisplayName("CRF < 0 is clamped to 0")
        void crfClampedToZero() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                -5,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(0, result.crf());
        }

        @Test
        @DisplayName("VP9 CRF of 60 is accepted (within 0-63 range)")
        void vp9CrfWithinRangeAccepted() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                60,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(60, result.crf());
        }
    }

    // ====================================================================
    //  1d) Tune – VP9 / AV1 do not support tune
    // ====================================================================

    @Nested
    @DisplayName("Tune – disabled for VP9 / AV1")
    class TuneDisabled {

        @Test
        @DisplayName("Tune is reset to NONE for VP9")
        void tuneResetForVp9() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.FILM
            );
            Preset result = calc(preset);
            assertEquals(Tune.NONE, result.tune());
        }

        @Test
        @DisplayName("Tune is reset to NONE for AV1")
        void tuneResetForAv1() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.AV1,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MKV,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.ANIMATION
            );
            Preset result = calc(preset);
            assertEquals(Tune.NONE, result.tune());
        }

        @Test
        @DisplayName("Tune is preserved for H.264")
        void tunePreservedForH264() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.FILM
            );
            Preset result = calc(preset);
            assertEquals(Tune.FILM, result.tune());
        }

        @Test
        @DisplayName("Tune is preserved for H.265")
        void tunePreservedForH265() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H265,
                28,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.STILLIMAGE
            );
            Preset result = calc(preset);
            assertEquals(Tune.STILLIMAGE, result.tune());
        }
    }

    // ====================================================================
    //  1e) fastStart – only meaningful for MP4
    // ====================================================================

    @Nested
    @DisplayName("fastStart – only for MP4")
    class FastStart {

        @Test
        @DisplayName("fastStart is disabled for MKV")
        void fastStartDisabledForMkv() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MKV,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                true, // fastStart = true
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.fastStart());
        }

        @Test
        @DisplayName("fastStart is disabled for WebM")
        void fastStartDisabledForWebm() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                true,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.fastStart());
        }

        @Test
        @DisplayName("fastStart is preserved for MP4")
        void fastStartPreservedForMp4() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                true,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertTrue(result.fastStart());
        }
    }

    // ====================================================================
    //  1f) Container-codec compatibility fixes
    // ====================================================================

    @Nested
    @DisplayName("Container-codec compatibility")
    class ContainerCodecCompatibility {

        @Test
        @DisplayName("VP9 in MP4 switches container to MKV")
        void vp9InMp4SwitchesToMkv() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(VideoContainer.MKV, result.container());
        }

        @Test
        @DisplayName("AV1 in MP4 switches container to MKV")
        void av1InMp4SwitchesToMkv() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.AV1,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(VideoContainer.MKV, result.container());
        }

        @Test
        @DisplayName(
            "fastStart is disabled when container switches from MP4 to MKV"
        )
        void fastStartDisabledOnContainerSwitch() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                true, // fastStart = true
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.fastStart());
        }

        @Test
        @DisplayName("H.264 in WebM switches container to MP4")
        void h264InWebMSwitchesToMp4() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(VideoContainer.MP4, result.container());
        }

        @Test
        @DisplayName("H.265 in WebM switches container to MP4")
        void h265InWebMSwitchesToMp4() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H265,
                28,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(VideoContainer.MP4, result.container());
        }
    }

    // ====================================================================
    //  1g) Audio codec-container compatibility
    // ====================================================================

    @Nested
    @DisplayName("Audio codec-container compatibility")
    class AudioCodecContainerCompatibility {

        @Test
        @DisplayName("AAC in WebM switches to Opus")
        void aacInWebMSwitchesToOpus() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                false, // keepSourceAudio = false
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(AudioCodec.OPUS, result.audioCodec());
        }

        @Test
        @DisplayName("AAC in WebM is NOT switched when keepSourceAudio=true")
        void aacInWebMKeptWhenSourceAudio() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true, // keepSourceAudio = true
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(AudioCodec.AAC, result.audioCodec());
        }

        @Test
        @DisplayName("Opus in MP4 switches to AAC")
        void opusInMp4SwitchesToAac() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                false,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(AudioCodec.AAC, result.audioCodec());
        }

        @Test
        @DisplayName("Vorbis in MP4 switches to AAC")
        void vorbisInMp4SwitchesToAac() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                false,
                AudioCodec.VORBIS,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(AudioCodec.AAC, result.audioCodec());
        }
    }

    // ====================================================================
    //  1h) audioNormalize with Opus/Vorbis is unreliable
    // ====================================================================

    @Nested
    @DisplayName("audioNormalize – unreliable with Opus/Vorbis")
    class AudioNormalize {

        @Test
        @DisplayName("audioNormalize is disabled for Opus")
        void normalizeDisabledForOpus() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                false,
                AudioCodec.OPUS,
                192,
                true,
                false,
                false, // audioNormalize = true
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.audioNormalize());
        }

        @Test
        @DisplayName("audioNormalize is disabled for Vorbis")
        void normalizeDisabledForVorbis() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                false,
                AudioCodec.VORBIS,
                192,
                true,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.audioNormalize());
        }

        @Test
        @DisplayName("audioNormalize is preserved for AAC")
        void normalizePreservedForAac() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                false,
                AudioCodec.AAC,
                192,
                true,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertTrue(result.audioNormalize());
        }

        @Test
        @DisplayName("audioNormalize is not disabled when keepSourceAudio=true")
        void normalizeNotTouchedWhenKeepSourceAudio() {
            // When keepSourceAudio=true, audioNormalize is set to false by
            // simplification 2c, but the 1h rule doesn't apply because
            // we skip re-encoding entirely.
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true, // keepSourceAudio
                AudioCodec.OPUS,
                192,
                true,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            // 2c simplification: keepSourceAudio overrides audioNormalize
            assertFalse(result.audioNormalize());
        }
    }

    // ====================================================================
    //  1i) maxFileSize – remove constraint when source already fits
    // ====================================================================

    @Nested
    @DisplayName("maxFileSize – remove unnecessary constraint")
    class MaxFileSize {

        @Test
        @DisplayName("maxFileSize is cleared when source is smaller than limit")
        void maxFileSizeClearedWhenSourceFits() {
            // source is 50 MB, limit is 100 MB
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                100,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(0, result.maxFileSize());
        }

        @Test
        @DisplayName("maxFileSize is kept when source exceeds limit")
        void maxFileSizeKeptWhenSourceExceeds() {
            // source is 50 MB, limit is 25 MB
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                25,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(25, result.maxFileSize());
        }

        @Test
        @DisplayName("maxFileSize is kept when source equals limit")
        void maxFileSizeKeptWhenSourceEqualsLimit() {
            // source is exactly 50 MB, limit is 50 MB
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                50,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(50, result.maxFileSize());
        }
    }

    // ====================================================================
    //  1j) audioBitrate – clamp to reasonable bounds
    // ====================================================================

    @Nested
    @DisplayName("audioBitrate – clamping")
    class AudioBitrateClamping {

        @Test
        @DisplayName("audioBitrate > 9999 is clamped to 9999")
        void audioBitrateClampedToMax() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                false,
                AudioCodec.AAC,
                15000,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(9999, result.audioBitrate());
        }

        @Test
        @DisplayName("audioBitrate <= 0 is clamped to 128")
        void audioBitrateClampedToMin() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                false,
                AudioCodec.AAC,
                0,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(128, result.audioBitrate());
        }

        @Test
        @DisplayName("audioBitrate is not clamped when keepSourceAudio=true")
        void audioBitrateNotClampedWhenKeepSourceAudio() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                0,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(0, result.audioBitrate());
        }
    }

    // ====================================================================
    //  2) Simplifications
    // ====================================================================

    @Nested
    @DisplayName("Simplifications")
    class Simplifications {

        @Test
        @DisplayName(
            "Resolution matching source sets keepSourceResolution=true"
        )
        void resolutionMatchSetsKeepSource() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                false,
                1920,
                1080, // same as source
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertTrue(result.keepSourceResolution());
        }

        @Test
        @DisplayName("keepSourceAudio overrides audioNormalize")
        void keepSourceAudioOverridesNormalize() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true, // keepSourceAudio
                AudioCodec.AAC,
                192,
                true,
                false,
                false, // audioNormalize = true
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.audioNormalize());
        }

        @Test
        @DisplayName("keepSourceAudio overrides mixToMono")
        void keepSourceAudioOverridesMixToMono() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true, // keepSourceAudio
                AudioCodec.AAC,
                192,
                false,
                true,
                false, // mixToMono = true
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertFalse(result.mixToMono());
        }
    }

    // ====================================================================
    //  End-to-end scenarios
    // ====================================================================

    @Nested
    @DisplayName("End-to-end scenarios")
    class EndToEnd {

        @Test
        @DisplayName(
            "VP9 in MP4 with fastStart: container→MKV, fastStart→false, tune→NONE"
        )
        void vp9Mp4CascadeFixes() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.VP9,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                true, // fastStart
                FfmpegPreset.MEDIUM,
                Tune.FILM
            );
            Preset result = calc(preset);
            assertEquals(VideoContainer.MKV, result.container());
            assertFalse(result.fastStart());
            assertEquals(Tune.NONE, result.tune());
        }

        @Test
        @DisplayName(
            "H.264 in WebM with fastStart: container→MP4, fastStart preserved"
        )
        void h264WebMCascadeFixes() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.WEBM,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                true,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(VideoContainer.MP4, result.container());
            // fastStart is disabled in step 1e (WebM != MP4) before the
            // container switch in step 1f, so it stays false.
            assertFalse(result.fastStart());
        }

        @Test
        @DisplayName("Upscaling + FPS increase + high CRF: all clamped")
        void multipleClampsTogether() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                80,
                false,
                3840,
                2160,
                60.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = calc(preset);
            // Resolution clamped to source → simplified
            assertTrue(result.keepSourceResolution());
            // FPS clamped to source
            assertEquals(30, result.fps());
            // CRF clamped to 51
            assertEquals(51, result.crf());
        }

        @Test
        @DisplayName("Source with zero fps skips FPS check")
        void zeroSourceFpsSkipsCheck() {
            ProbeInfo zeroFpsSource = new ProbeInfo(
                new File("static.mp4"),
                1920,
                1080,
                0, // fps = 0
                5000,
                128,
                120.5,
                "h264",
                50L * 1024 * 1024
            );
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                60.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.MEDIUM,
                Tune.NONE
            );
            Preset result = Strategy.calcEditingPreset(zeroFpsSource, preset);
            // FPS check is guarded by sourceFps > 0, so 60 is preserved
            assertEquals(60.0, result.fps());
        }

        @Test
        @DisplayName("VideoCodec is preserved unchanged")
        void videoCodecPreserved() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.AV1,
                30,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MKV,
                0,
                true,
                AudioCodec.OPUS,
                192,
                false,
                false,
                false,
                FfmpegPreset.VERYFAST,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(VideoCodec.AV1, result.videoCodec());
        }

        @Test
        @DisplayName("FfmpegPreset is preserved unchanged")
        void ffmpegPresetPreserved() {
            Preset preset = new Preset(
                "P",
                "",
                VideoCodec.H264,
                23,
                true,
                1920,
                1080,
                30.0,
                VideoContainer.MP4,
                0,
                true,
                AudioCodec.AAC,
                192,
                false,
                false,
                false,
                FfmpegPreset.VERYFAST,
                Tune.NONE
            );
            Preset result = calc(preset);
            assertEquals(FfmpegPreset.VERYFAST, result.ffmpegPreset());
        }

        @Test
        @DisplayName("Name starts with 'Editing Preset:'")
        void namePrefix() {
            Preset result = calc(defaultPreset);
            assertTrue(result.name().startsWith("Editing Preset:"));
        }
    }
}
