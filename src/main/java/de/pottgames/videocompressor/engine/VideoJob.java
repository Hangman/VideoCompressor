package de.pottgames.videocompressor.engine;

import java.io.File;

public record VideoJob(
    File outputFile,
    ProbeInfo sourceInfo,
    Preset preset,
    VideoJobStatus status
) {}
