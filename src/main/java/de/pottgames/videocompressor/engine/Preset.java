package de.pottgames.videocompressor.engine;

/**
 * Record representing a video encoding preset configuration.
 */
public record Preset(
    String name,
    String description,
    String codec,
    int crf,
    int resolutionWidth,
    int resolutionHeight,
    int fps,
    int maxFileSize,
    int audioBitrate,
    boolean audioNormalize,
    boolean fastStart,
    String ffmpegPreset,
    String tune
) {}
