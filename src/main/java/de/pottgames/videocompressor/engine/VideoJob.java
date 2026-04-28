package de.pottgames.videocompressor.engine;

import java.io.File;

public record VideoJob(
    File outputFile,
    ProbeInfo sourceInfo,
    Preset preset,
    String codec,
    int crf,
    int resolutionWidth,
    int resolutionHeight,
    int fps,
    long maxFileSize,
    int audioBitrate,
    boolean normalizeAudio,
    boolean fastStart,
    String ffmpegPreset,
    String tune,
    VideoJobStatus status
) {}
