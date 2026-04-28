package de.pottgames.videocompressor.engine;

import java.io.File;

/**
 * Record to store probe information for video files obtained via ffprobe.
 */
public record ProbeInfo(
    File file,
    int resolutionWidth,
    int resolutionHeight,
    int fps,
    int bitrate,
    double duration,
    String codec,
    long fileSize
) {}
