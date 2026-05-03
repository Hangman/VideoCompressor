package de.pottgames.videocompressor.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FfmpegProgress {

    // raw
    // frame=\s*(?<nframe>[0-9]+)\s+fps=\s*(?<nfps>[0-9\.]+)\s+q=(?<nq>[0-9\.-]+)\s+(L?)\s*size=\s*(?<nsize>[0-9]+)(?<ssize>kB|mB|b)?\s*time=\s*(?<sduration>[0-9\:\.]+)\s*bitrate=\s*(?<nbitrate>[0-9\.]+)(?<sbitrate>bits\/s|mbits\/s|kbits\/s)?.*(dup=(?<ndup>\d+)\s*)?(drop=(?<ndrop>\d+)\s*)?speed=\s*(?<nspeed>[0-9\.]+)x
    private static final Pattern PATTERN_S = Pattern.compile(
        "" +
            "frame=\\s*(?<nframe>[0-9]+)\\s+" +
            "fps=\\s*(?<nfps>[0-9\\.]+)\\s+" +
            "q=(?<nq>[0-9\\.-]+)\\s+(L?)\\s*" +
            "size=\\s*(?<nsize>[0-9]+)(?<ssize>KiB|MiB|kB|mB|b)?\\s*" +
            "time=\\s*(?<sduration>[0-9\\:\\.]+)\\s*" +
            "bitrate=\\s*(?<nbitrate>[0-9\\.]+)(?<sbitrate>bits\\/s|mbits\\/s|kbits\\/s)?.*" +
            "(dup=(?<ndup>\\d+)\\s*)?" +
            "(drop=(?<ndrop>\\d+)\\s*)?" +
            "speed=\\s*(?<nspeed>[0-9\\.]+)x"
    );
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d+):(\\d+):(\\d+)\\.(\\d+)"
    );

    private long mFrame;
    private double mFps;
    private double mQ;
    private long mSize;
    private String mTime;
    private long mTimeMs;
    private double mBitrate;
    private double mSpeed;

    public FfmpegProgress(String rawProgress) {
        Matcher matcher = PATTERN_S.matcher(rawProgress);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Invalid raw progress string: " + rawProgress
            );
        }

        mFrame = Long.parseLong(matcher.group("nframe"));
        mFps = Double.parseDouble(matcher.group("nfps"));
        mQ = Double.parseDouble(matcher.group("nq"));
        mSize = Long.parseLong(matcher.group("nsize"));
        mTime = matcher.group("sduration");
        mTimeMs = timeToMs(mTime);
        mBitrate = Double.parseDouble(matcher.group("nbitrate"));
        mSpeed = Double.parseDouble(matcher.group("nspeed"));
    }

    public static boolean isProgress(String rawProgress) {
        return PATTERN_S.matcher(rawProgress).find();
    }

    public static long timeToMs(String time) {
        Matcher matcher = TIME_PATTERN.matcher(time);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }

        int hours = Integer.parseInt(matcher.group(1));
        int mins = Integer.parseInt(matcher.group(2));
        int seconds = Integer.parseInt(matcher.group(3));
        int ms = Integer.parseInt(matcher.group(4));

        long hoursMs = ((hours * 60) * 60) * 1000;
        long minsMs = (mins * 60) * 1000;
        long secondsMs = seconds * 1000;
        long msMs = 0;
        final int len = String.valueOf(
            Integer.parseInt(matcher.group(4))
        ).length();
        switch (len) {
            case 1:
                msMs = ms * 100;
                break;
            case 2:
                msMs = ms * 10;
                break;
            case 3:
                msMs = ms;
            default:
                if (len > 3) {
                    msMs = Integer.parseInt(matcher.group(4).substring(0, 2));
                }
                break;
        }

        return hoursMs + minsMs + secondsMs + msMs;
    }

    public long getFrame() {
        return mFrame;
    }

    public double getFps() {
        return mFps;
    }

    public double getQ() {
        return mQ;
    }

    public long getSize() {
        return mSize;
    }

    public String getTime() {
        return mTime;
    }

    public long getTimeMs() {
        return mTimeMs;
    }

    public double getBitrate() {
        return mBitrate;
    }

    public double getSpeed() {
        return mSpeed;
    }
}
