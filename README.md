# VideoCompressor

A desktop application for compressing videos using FFmpeg. Built with **JavaFX**, it provides a graphical interface to apply compression presets and manage video encoding workflows.

---

## Overview

This project was developed to meet the specific needs of a particular customer. It is not intended as a general-purpose, highly configurable video compressor, but rather as a tailored solution for a defined use case. While active development is currently completed, I am happy to incorporate changes or adaptations if needed.

---

## Prerequisites

- **Java 25** (or higher)
- **Maven 3.6+**
- **FFmpeg** (GPL version) — must be present as a local subfolder `ffmpeg/` in the project directory. The application **does not** rely on the system `PATH` environment variable; it exclusively expects FFmpeg binaries inside this subfolder.

---

## Usage

### Running the application

```bash
mvn javafx:run
```

### Building a custom image

You can create a compact, standalone runtime image using the `javafx:jlink` plugin:

```bash
mvn javafx:jlink
```

The resulting image will be located in the `target/` directory. It can be executed without a local JDK installation, provided that the `ffmpeg/` and `presets/` folder is copied alongside the generated image.
