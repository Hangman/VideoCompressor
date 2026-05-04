package de.pottgames.videocompressor.view;

/**
 * Central theme constants for the VideoCompressor UI.
 *
 * <p>All color values used in inline styles across the application are
 * defined here so that theming can be changed in one place.
 *
 * <h2>CSS Variables</h2>
 * <p>AtlantFX exposes CSS custom properties that resolve at runtime.
 * These are used for text colors and semantic colors where the theme
 * should adapt automatically.</p>
 *
 * <h2>Dracula Palette</h2>
 * <p>Hardcoded hex values from the Dracula color scheme. These are
 * needed for {@code -fx-background-color} and similar properties where
 * CSS variables do not resolve in JavaFX inline styles.</p>
 */
public final class Theme {

    private Theme() {
        // No instances
    }

    // ─────────────────────────────────────────────────────────────────────
    //  AtlantFX CSS custom properties (resolve at runtime)
    // ─────────────────────────────────────────────────────────────────────

    /** Foreground default text color. */
    public static final String CSS_FG = "-color-fg-default";

    /** Foreground subtle / muted text color. */
    public static final String CSS_FG_SUBTLE = "-color-fg-subtle";

    /** Success color (green). */
    public static final String CSS_SUCCESS = "-color-success-5";

    /** Warning color (amber). */
    public static final String CSS_WARNING = "-color-warning-5";

    /** Danger / error color (red). */
    public static final String CSS_DANGER = "-color-danger-5";

    /** Background — main surface (same as HEX_BG). */
    public static final String CSS_BG_DEFAULT = "-color-bg-default";

    /** Primary accent (purple) — same as HEX_ACCENT. */
    public static final String CSS_ACCENT_FG = "-color-accent-fg";

    // ─────────────────────────────────────────────────────────────────────
    //  Dracula palette — hex values for -fx-background-color etc.
    // ─────────────────────────────────────────────────────────────────────

    /** Background — darker variant used for log/console areas. */
    public static final String HEX_BG_DARK = "#21222c";

    /** Accent variant — lighter purple used in some panels. */
    public static final String HEX_ACCENT_LIGHT = "#bd93f9";

    /** Red — error indicators. */
    public static final String HEX_RED = "#ff5555";

    /** Orange / amber — warning indicators. */
    public static final String HEX_ORANGE = "#ffb86c";

    /** Card background — slightly elevated surface. */
    public static final String HEX_CARD_BG = "#2f3143";

    /** Card background — used in FileListCell root cards. */
    public static final String HEX_CARD_BG_CELL = "#343646";

    /** Panel background — used for info panels (e.g. Step3 video info). */
    public static final String HEX_PANEL_BG = "#44475a";

    /** Panel background — source side in comparison view. */
    public static final String HEX_PANEL_SOURCE = "#24263a";

    /** Panel background — output side in comparison view. */
    public static final String HEX_PANEL_OUTPUT = "#1d2338";

    /** Border color. */
    public static final String HEX_BORDER = "#3b3d57";

    /** Divider / separator color. */
    public static final String HEX_DIVIDER = "#3a3c52";

    // ── Stepper (Dracula palette) ─────────────────────────────────────

    /** Foreground text — used for active stepper numbers/labels. */
    public static final String HEX_FG = "#f8f8f2";

    /** Comment / muted — used for inactive stepper rings and labels. */
    public static final String HEX_COMMENT = "#6272a4";

    /** Accent — used for active stepper rings and connecting lines. */
    public static final String HEX_ACCENT = "#9580ff";

    /** Dimmed connecting line between inactive stepper steps. */
    public static final String HEX_STEPPER_LINE_DIMMED = "#4a5a8a";

    // ─────────────────────────────────────────────────────────────────────
    //  Font sizes
    // ─────────────────────────────────────────────────────────────────────

    /** Tiny text, e.g. labels in comparison panels. */
    public static final String FONT_SIZE_TINY = "10px";

    /** Small text, e.g. status badges, log area. */
    public static final String FONT_SIZE_SMALL = "12px";

    /** Regular body text. */
    public static final String FONT_SIZE_BASE = "13px";

    /** Slightly larger text for labels and headings. */
    public static final String FONT_SIZE_MEDIUM = "14px";

    /** Large text for validation icons and emphasis. */
    public static final String FONT_SIZE_LARGE = "16px";

    /** Extra-large text for hero values and headers. */
    public static final String FONT_SIZE_XLARGE = "18px";

    /** Extra-extra-large text for stepper numbers. */
    public static final String FONT_SIZE_XXLARGE = "20px";
}
