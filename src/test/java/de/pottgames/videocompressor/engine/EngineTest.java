package de.pottgames.videocompressor.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EngineTest {

    private File tempMp4File;
    private File tempAviFile;
    private File tempMkvFile;
    private File tempMovFile;
    private File tempWmvFile;
    private File tempFlvFile;
    private File tempWebmFile;
    private File tempTxtFile;
    private File tempPdfFile;

    @BeforeEach
    void setUp() throws IOException {
        tempMp4File = File.createTempFile("test_mp4_", ".mp4");
        tempAviFile = File.createTempFile("test_avi_", ".avi");
        tempMkvFile = File.createTempFile("test_mkv_", ".mkv");
        tempMovFile = File.createTempFile("test_mov_", ".mov");
        tempWmvFile = File.createTempFile("test_wmv_", ".wmv");
        tempFlvFile = File.createTempFile("test_flv_", ".flv");
        tempWebmFile = File.createTempFile("test_webm_", ".webm");
        tempTxtFile = File.createTempFile("test_txt_", ".txt");
        tempPdfFile = File.createTempFile("test_pdf_", ".pdf");
    }

    @AfterEach
    void tearDown() {
        if (tempMp4File != null) tempMp4File.delete();
        if (tempAviFile != null) tempAviFile.delete();
        if (tempMkvFile != null) tempMkvFile.delete();
        if (tempMovFile != null) tempMovFile.delete();
        if (tempWmvFile != null) tempWmvFile.delete();
        if (tempFlvFile != null) tempFlvFile.delete();
        if (tempWebmFile != null) tempWebmFile.delete();
        if (tempTxtFile != null) tempTxtFile.delete();
        if (tempPdfFile != null) tempPdfFile.delete();
    }

    // ==================== isCompatible() Tests ====================

    @Test
    void isCompatible_returnsTrueForSupportedMp4Extension() {
        assertTrue(Engine.isCompatible(tempMp4File));
    }

    @Test
    void isCompatible_returnsTrueForSupportedAviExtension() {
        assertTrue(Engine.isCompatible(tempAviFile));
    }

    @Test
    void isCompatible_returnsTrueForSupportedMkvExtension() {
        assertTrue(Engine.isCompatible(tempMkvFile));
    }

    @Test
    void isCompatible_returnsTrueForSupportedMovExtension() {
        assertTrue(Engine.isCompatible(tempMovFile));
    }

    @Test
    void isCompatible_returnsTrueForSupportedWmvExtension() {
        assertTrue(Engine.isCompatible(tempWmvFile));
    }

    @Test
    void isCompatible_returnsTrueForSupportedFlvExtension() {
        assertTrue(Engine.isCompatible(tempFlvFile));
    }

    @Test
    void isCompatible_returnsTrueForSupportedWebmExtension() {
        assertTrue(Engine.isCompatible(tempWebmFile));
    }

    @Test
    void isCompatible_returnsTrueForUppercaseMp4Extension() throws IOException {
        File upperCaseFile = new File(tempMp4File.getParent(), "test.MP4");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsTrueForUppercaseAviExtension() throws IOException {
        File upperCaseFile = new File(tempAviFile.getParent(), "test.AVI");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsTrueForUppercaseMkvExtension() throws IOException {
        File upperCaseFile = new File(tempMkvFile.getParent(), "test.MKV");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsTrueForUppercaseMovExtension() throws IOException {
        File upperCaseFile = new File(tempMovFile.getParent(), "test.MOV");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsTrueForUppercaseWmvExtension() throws IOException {
        File upperCaseFile = new File(tempWmvFile.getParent(), "test.WMV");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsTrueForUppercaseFlvExtension() throws IOException {
        File upperCaseFile = new File(tempFlvFile.getParent(), "test.FLV");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsTrueForUppercaseWebmExtension()
        throws IOException {
        File upperCaseFile = new File(tempWebmFile.getParent(), "test.WEBM");
        try {
            assertTrue(upperCaseFile.createNewFile());
            assertTrue(Engine.isCompatible(upperCaseFile));
        } finally {
            upperCaseFile.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedTxtExtension() {
        assertFalse(Engine.isCompatible(tempTxtFile));
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedPdfExtension() {
        assertFalse(Engine.isCompatible(tempPdfFile));
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedJpgExtension()
        throws IOException {
        File jpgFile = File.createTempFile("test_jpg_", ".jpg");
        try {
            assertFalse(Engine.isCompatible(jpgFile));
        } finally {
            jpgFile.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedPngExtension()
        throws IOException {
        File pngFile = File.createTempFile("test_png_", ".png");
        try {
            assertFalse(Engine.isCompatible(pngFile));
        } finally {
            pngFile.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedZipExtension()
        throws IOException {
        File zipFile = File.createTempFile("test_zip_", ".zip");
        try {
            assertFalse(Engine.isCompatible(zipFile));
        } finally {
            zipFile.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedRarExtension()
        throws IOException {
        File rarFile = File.createTempFile("test_rar_", ".rar");
        try {
            assertFalse(Engine.isCompatible(rarFile));
        } finally {
            rarFile.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForUnsupportedDocxExtension()
        throws IOException {
        File docxFile = File.createTempFile("test_docx_", ".docx");
        try {
            assertFalse(Engine.isCompatible(docxFile));
        } finally {
            docxFile.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForNullFile() {
        assertFalse(Engine.isCompatible(null));
    }

    @Test
    void isCompatible_returnsFalseForNonExistentFile() {
        File file = new File("/nonexistent/path/test.mp4");
        assertFalse(Engine.isCompatible(file));
    }

    @Test
    void isCompatible_returnsFalseForDirectory() {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        assertFalse(Engine.isCompatible(dir));
    }

    @Test
    void isCompatible_returnsFalseForFileWithoutExtension() throws IOException {
        File file = File.createTempFile("test", "");
        try {
            assertFalse(Engine.isCompatible(file));
        } finally {
            file.delete();
        }
    }

    @Test
    void isCompatible_returnsFalseForFileEndingWithDot() throws IOException {
        File file = File.createTempFile("test", ".");
        try {
            assertFalse(Engine.isCompatible(file));
        } finally {
            file.delete();
        }
    }

    // ==================== Path Resolution Tests ====================

    @Test
    void getFfmpegPath_returnsNonNullPath() {
        Path ffmpegPath = Engine.getFfmpegPath();
        assertNotNull(ffmpegPath, "FFmpeg path should not be null");
    }

    @Test
    void getFfprobePath_returnsNonNullPath() {
        Path ffprobePath = Engine.getFfprobePath();
        assertNotNull(ffprobePath, "FFprobe path should not be null");
    }

    @Test
    void getFfmpegPath_containsFfmpegInName() {
        String ffmpegPath = Engine.getFfmpegPath().toString();
        assertTrue(
            ffmpegPath.toLowerCase().contains("ffmpeg"),
            "FFmpeg path should contain 'ffmpeg': " + ffmpegPath
        );
    }

    @Test
    void getFfprobePath_containsFfprobeInName() {
        String ffprobePath = Engine.getFfprobePath().toString();
        assertTrue(
            ffprobePath.toLowerCase().contains("ffprobe"),
            "FFprobe path should contain 'ffprobe': " + ffprobePath
        );
    }

    @Test
    void getFfmpegPath_isAbsolutePath() {
        assertTrue(
            Engine.getFfmpegPath().isAbsolute(),
            "FFmpeg path should be absolute"
        );
    }

    @Test
    void getFfprobePath_isAbsolutePath() {
        assertTrue(
            Engine.getFfprobePath().isAbsolute(),
            "FFprobe path should be absolute"
        );
    }

    @Test
    void getFfmpegPath_hasCorrectExecutableExtension() {
        String osName = System.getProperty("os.name").toLowerCase();
        String ffmpegPath = Engine.getFfmpegPath().toString();

        if (osName.contains("win")) {
            assertTrue(
                ffmpegPath.endsWith(".exe"),
                "On Windows, FFmpeg path should end with .exe: " + ffmpegPath
            );
        } else {
            assertFalse(
                ffmpegPath.endsWith(".exe"),
                "On non-Windows, FFmpeg path should not end with .exe: " +
                    ffmpegPath
            );
        }
    }

    @Test
    void getFfprobePath_hasCorrectExecutableExtension() {
        String osName = System.getProperty("os.name").toLowerCase();
        String ffprobePath = Engine.getFfprobePath().toString();

        if (osName.contains("win")) {
            assertTrue(
                ffprobePath.endsWith(".exe"),
                "On Windows, FFprobe path should end with .exe: " + ffprobePath
            );
        } else {
            assertFalse(
                ffprobePath.endsWith(".exe"),
                "On non-Windows, FFprobe path should not end with .exe: " +
                    ffprobePath
            );
        }
    }

    // ==================== Supported Extensions Constants Tests ====================

    @Test
    void supportedExtensions_containsAllExpectedExtensions() {
        List<String> expected = List.of(
            "mp4",
            "avi",
            "mkv",
            "mov",
            "wmv",
            "flv",
            "webm"
        );
        assertEquals(
            expected,
            Engine.SUPPORTED_EXTENSIONS,
            "SUPPORTED_EXTENSIONS should contain all expected video formats"
        );
    }

    @Test
    void supportedExtensionPatterns_containsAllExpectedPatterns() {
        List<String> expected = List.of(
            "*.mp4",
            "*.avi",
            "*.mkv",
            "*.mov",
            "*.wmv",
            "*.flv",
            "*.webm"
        );
        assertEquals(
            expected,
            Engine.SUPPORTED_EXTENSION_PATTERNS,
            "SUPPORTED_EXTENSION_PATTERNS should contain all expected patterns"
        );
    }

    @Test
    void supportedExtensionsAndPatternsHaveSameSize() {
        assertEquals(
            Engine.SUPPORTED_EXTENSIONS.size(),
            Engine.SUPPORTED_EXTENSION_PATTERNS.size(),
            "SUPPORTED_EXTENSIONS and SUPPORTED_EXTENSION_PATTERNS should have the same size"
        );
    }
}
