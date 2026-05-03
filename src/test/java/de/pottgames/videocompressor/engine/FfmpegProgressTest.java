package de.pottgames.videocompressor.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FfmpegProgressTest {

    // ────────────────────────────────────────────────────────────────────
    // isProgress() – should match real FFmpeg stderr lines
    // ────────────────────────────────────────────────────────────────────

    @Test
    void isProgress_matchesStandardLineWithoutDupDrop() {
        String line =
            "frame=123 fps=25.0 q=28.0 size=12345kB time=00:01:23.45 bitrate=1234.5kbits/s speed=1.2x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Standard progress line should match"
        );
    }

    @Test
    void isProgress_matchesLineWithDupAndDrop() {
        String line =
            "frame=123 fps=25.0 q=28.0 size=12345kB time=00:01:23.45 bitrate=1234.5kbits/s speed=1.2x dup=0 drop=0";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Line with dup/drop should match"
        );
    }

    @Test
    void isProgress_matchesLineWithOnlyDup() {
        String line =
            "frame=123 fps=25.0 q=28.0 size=12345kB time=00:01:23.45 bitrate=1234.5kbits/s speed=1.2x dup=2";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Line with only dup should match"
        );
    }

    @Test
    void isProgress_matchesLineWithOnlyDrop() {
        String line =
            "frame=123 fps=25.0 q=28.0 size=12345kB time=00:01:23.45 bitrate=1234.5kbits/s speed=1.2x drop=1";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Line with only drop should match"
        );
    }

    @Test
    void isProgress_matchesBitsPerSecondUnit() {
        String line =
            "frame=10 fps=30.0 q=20.0 size=100kB time=00:00:05.00 bitrate=500000bits/s speed=2.0x";
        assertTrue(FfmpegProgress.isProgress(line), "bits/s unit should match");
    }

    @Test
    void isProgress_matchesMbitsPerSecondUnit() {
        String line =
            "frame=10 fps=30.0 q=20.0 size=100kB time=00:00:05.00 bitrate=5mbits/s speed=2.0x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "mbits/s unit should match"
        );
    }

    @Test
    void isProgress_matchesNoBitrateUnit() {
        String line =
            "frame=10 fps=30.0 q=20.0 size=100kB time=00:00:05.00 bitrate=500000 speed=2.0x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "No bitrate unit should match"
        );
    }

    @Test
    void isProgress_matchesHighFrameNumbers() {
        String line =
            "frame=99999 fps=60.0 q=30.0 size=999999kB time=01:23:45.67 bitrate=9999.9kbits/s speed=3.5x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "High frame numbers should match"
        );
    }

    @Test
    void isProgress_matchesDecimalFps() {
        String line =
            "frame=500 fps=29.970 q=27.5 size=5000kB time=00:16:40.00 bitrate=4096.0kbits/s speed=1.0x";
        assertTrue(FfmpegProgress.isProgress(line), "Decimal fps should match");
    }

    @Test
    void isProgress_matchesNegativeQValue() {
        String line =
            "frame=100 fps=25.0 q=-0.0 size=1000kB time=00:00:04.00 bitrate=2000.0kbits/s speed=1.5x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Negative q value should match"
        );
    }

    @Test
    void isProgress_rejectsNonProgressLine() {
        String line = "[info] Some informational message";
        assertFalse(
            FfmpegProgress.isProgress(line),
            "Non-progress line should not match"
        );
    }

    @Test
    void isProgress_rejectsEmptyLine() {
        assertFalse(
            FfmpegProgress.isProgress(""),
            "Empty line should not match"
        );
    }

    @Test
    void isProgress_rejectsGarbageLine() {
        assertFalse(
            FfmpegProgress.isProgress(
                "this is not an ffmpeg progress line at all"
            ),
            "Garbage should not match"
        );
    }

    // ────────────────────────────────────────────────────────────────────
    // Constructor parsing – verify field extraction
    // ────────────────────────────────────────────────────────────────────

    @Test
    void constructor_parsesAllFields() {
        String line =
            "frame=500 fps=29.97 q=27.5 size=5000kB time=00:08:20.00 bitrate=4096.0kbits/s speed=1.5x";

        FfmpegProgress progress = new FfmpegProgress(line);

        assertEquals(500, progress.getFrame());
        assertEquals(29.97, progress.getFps(), 0.01);
        assertEquals(27.5, progress.getQ(), 0.01);
        assertEquals(5000, progress.getSize());
        assertEquals("00:08:20.00", progress.getTime());
        assertEquals(4096.0, progress.getBitrate(), 0.1);
        assertEquals(1.5, progress.getSpeed(), 0.01);
    }

    @Test
    void constructor_parsesWithDupDrop() {
        String line =
            "frame=200 fps=25.0 q=28.0 size=2000kB time=00:00:08.00 bitrate=2000.0kbits/s speed=1.0x dup=3 drop=1";

        FfmpegProgress progress = new FfmpegProgress(line);

        assertEquals(200, progress.getFrame());
        assertEquals(25.0, progress.getFps(), 0.01);
        assertEquals(1.0, progress.getSpeed(), 0.01);
    }

    @Test
    void constructor_throwsOnInvalidLine() {
        String line = "not a valid progress line";
        assertThrows(IllegalArgumentException.class, () ->
            new FfmpegProgress(line)
        );
    }

    // ────────────────────────────────────────────────────────────────────
    // timeToMs() – verify time string to milliseconds conversion
    // ────────────────────────────────────────────────────────────────────

    @Test
    void timeToMs_convertsSimpleSeconds() {
        assertEquals(5000, FfmpegProgress.timeToMs("00:00:05.00"));
    }

    @Test
    void timeToMs_convertsMinutesAndSeconds() {
        assertEquals(120500, FfmpegProgress.timeToMs("00:02:00.50"));
    }

    @Test
    void timeToMs_convertsHoursMinutesSeconds() {
        assertEquals(3665000, FfmpegProgress.timeToMs("01:01:05.00"));
    }

    @Test
    void timeToMs_convertsZero() {
        assertEquals(0, FfmpegProgress.timeToMs("00:00:00.00"));
    }

    @Test
    void timeToMs_convertsSingleDigitMs() {
        assertEquals(100, FfmpegProgress.timeToMs("00:00:00.1"));
    }

    @Test
    void timeToMs_convertsTwoDigitMs() {
        assertEquals(120, FfmpegProgress.timeToMs("00:00:00.12"));
    }

    @Test
    void timeToMs_convertsThreeDigitMs() {
        assertEquals(123, FfmpegProgress.timeToMs("00:00:00.123"));
    }

    @Test
    void timeToMs_throwsOnInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () ->
            FfmpegProgress.timeToMs("invalid")
        );
    }

    // ────────────────────────────────────────────────────────────────────
    // Real-world FFmpeg output samples
    // ────────────────────────────────────────────────────────────────────

    @Test
    void isProgress_matchesRealFfmpegH264Output() {
        // Typical libx264 progress line from FFmpeg 8.x
        String line =
            "frame=  234 fps=0.0 q=29.0 size=   1024kB time=00:00:09.34 bitrate= 894.5kbits/s speed=0.954x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Real H.264 output should match"
        );
    }

    @Test
    void isProgress_matchesRealFfmpegAv1Output() {
        // Typical libsvtav1 progress line
        String line =
            "frame= 1000 fps=15.2 q=35.0 size=   5120kB time=00:00:40.00 bitrate=1048.6kbits/s speed=0.507x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Real AV1 output should match"
        );
    }

    @Test
    void isProgress_matchesRealFfmpegVp9Output() {
        // Typical libvpx-vp9 progress line
        String line =
            "frame=  500 fps=20.1 q=31.0 size=   2560kB time=00:00:20.00 bitrate=1048.6kbits/s speed=0.67x";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Real VP9 output should match"
        );
    }

    @Test
    void isProgress_matchesRealFfmpegWithDupDrop() {
        // FFmpeg often outputs dup/drop at the end
        String line =
            "frame=  300 fps=25.0 q=28.0 size=   1536kB time=00:00:12.00 bitrate=1048.6kbits/s speed=0.833x dup=0 drop=0";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "Real output with dup/drop should match"
        );
    }

    // ────────────────────────────────────────────────────────────────────
    // KiB / MiB size units (newer FFmpeg versions use binary SI prefixes)
    // ────────────────────────────────────────────────────────────────────

    @Test
    void isProgress_matchesKiBSizeUnit_initialFrame() {
        // Real FFmpeg output: size=       0KiB with many leading spaces
        String line =
            "frame=   16 fps=0.0 q=28.0 size=       0KiB time=00:00:01.92 bitrate=   0.2kbits/s speed=3.84x elapsed=0:00:00.50";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "KiB size unit should match (initial frame)"
        );
    }

    @Test
    void isProgress_matchesKiBSizeUnit_laterFrame() {
        String line =
            "frame=  134 fps=134 q=28.0 size=     768KiB time=00:00:05.67 bitrate=1108.8kbits/s speed=5.67x elapsed=0:00:01.00";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "KiB size unit should match (later frame)"
        );
    }

    @Test
    void isProgress_matchesKiBSizeUnit_largeFile() {
        String line =
            "frame=  622 fps=207 q=28.0 size=    3584KiB time=00:00:21.17 bitrate=1386.7kbits/s speed=7.06x elapsed=0:00:03.00";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "KiB size unit should match (large file)"
        );
    }

    @Test
    void isProgress_matchesMiBSizeUnit() {
        // When file grows large enough, FFmpeg switches to MiB
        String line =
            "frame= 5000 fps=250 q=28.0 size=    10240MiB time=00:03:20.00 bitrate=4096.0kbits/s speed=10.0x elapsed=0:00:10.00";
        assertTrue(
            FfmpegProgress.isProgress(line),
            "MiB size unit should match"
        );
    }

    @Test
    void constructor_parsesKiBSizeUnit() {
        String line =
            "frame=  258 fps=172 q=28.0 size=    1536KiB time=00:00:09.37 bitrate=1342.1kbits/s speed=6.25x elapsed=0:00:01.50";

        FfmpegProgress progress = new FfmpegProgress(line);

        assertEquals(258, progress.getFrame());
        assertEquals(172.0, progress.getFps(), 0.01);
        assertEquals(28.0, progress.getQ(), 0.01);
        assertEquals(1536, progress.getSize());
        assertEquals("00:00:09.37", progress.getTime());
        assertEquals(1342.1, progress.getBitrate(), 0.1);
        assertEquals(6.25, progress.getSpeed(), 0.01);
    }

    @Test
    void isProgress_matchesLineWithNATimeAndBitrate() {
        // FFmpeg may output N/A for time/bitrate in some frames
        String line =
            "frame=  876 fps=219 q=-1.0 size=    4864KiB time=N/A bitrate=N/A speed=N/A elapsed=0:00:04.00";
        // This line should NOT match because time/bitrate/speed contain N/A
        // which the regex doesn't support. That's expected behavior.
        assertFalse(
            FfmpegProgress.isProgress(line),
            "Line with N/A values should not match (expected)"
        );
    }
}
