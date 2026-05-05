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
    public static final String CSS_FG = "-color-fg-default"; // #f8f8f2

    /** Foreground subtle / muted text color. */
    public static final String CSS_FG_SUBTLE = "-color-fg-subtle"; // #7e7f86

    /** Success color (green). */
    public static final String CSS_SUCCESS = "-color-success-5"; // #8Aff80

    /** Warning color (amber). */
    public static final String CSS_WARNING = "-color-warning-5"; // #ffca80

    /** Danger / error color (red). */
    public static final String CSS_DANGER = "-color-danger-5"; // #ff9580

    /** Background — main surface (same as HEX_BG). */
    public static final String CSS_BG_DEFAULT = "-color-bg-default"; // #282a36

    /** Primary accent (purple) — same as HEX_ACCENT. */
    public static final String CSS_ACCENT_FG = "-color-accent-fg"; // #9580ff

    // ─────────────────────────────────────────────────────────────────────
    //  Dracula palette — hex values for -fx-background-color etc.
    // ─────────────────────────────────────────────────────────────────────

    /** Background — darker variant used for log/console areas. */
    public static final String HEX_BG_DARK = "#21222c";

    /** Accent variant — lighter purple used in some panels. */
    public static final String HEX_ACCENT_LIGHT = "#bd93f9";

    /** Red — error indicators. */
    public static final String HEX_DANGER = "#ff9580";

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

    // ── Font sizes (raw values, internal) ───────────────────────────────

    /** Small text, e.g. status badges, log area. */
    private static final String FONT_SMALL = "13px";

    /** Regular body text. */
    private static final String FONT_BASE = "14px";

    /** Large text for validation icons and emphasis. */
    private static final String FONT_LARGE = "16px";

    /** Extra-large text for hero values and headers. */
    private static final String FONT_XLARGE = "18px";

    /** Extra-extra-large text for stepper numbers. */
    private static final String FONT_XXLARGE = "20px";

    // ── Font-size CSS style strings (ready for inline styles) ───────────

    public static final String FONT_SMALL_STYLE =
        "-fx-font-size: " + FONT_SMALL + ";";
    public static final String FONT_BASE_STYLE =
        "-fx-font-size: " + FONT_BASE + ";";
    public static final String FONT_LARGE_STYLE =
        "-fx-font-size: " + FONT_LARGE + ";";
    public static final String FONT_XLARGE_STYLE =
        "-fx-font-size: " + FONT_XLARGE + ";";
    public static final String FONT_XXLARGE_STYLE =
        "-fx-font-size: " + FONT_XXLARGE + ";";

    // ─────────────────────────────────────────────────────────────────────
    //  Text-fill CSS style strings (ready for inline styles)
    // ─────────────────────────────────────────────────────────────────────

    // ── AtlantFX CSS variable based ─────────────────────────────────────

    /** Default foreground text fill. */
    public static final String TEXT_FILL_FG_STYLE =
        "-fx-text-fill: " + CSS_FG + ";";

    /** Subtle / muted text fill. */
    public static final String TEXT_FILL_FG_SUBTLE_STYLE =
        "-fx-text-fill: " + CSS_FG_SUBTLE + ";";

    /** Success text fill. */
    public static final String TEXT_FILL_SUCCESS_STYLE =
        "-fx-text-fill: " + CSS_SUCCESS + ";";

    /** Warning text fill. */
    public static final String TEXT_FILL_WARNING_STYLE =
        "-fx-text-fill: " + CSS_WARNING + ";";

    /** Danger / error text fill. */
    public static final String TEXT_FILL_DANGER_STYLE =
        "-fx-text-fill: " + CSS_DANGER + ";";

    /** Accent text fill. */
    public static final String TEXT_FILL_ACCENT_STYLE =
        "-fx-text-fill: " + CSS_ACCENT_FG + ";";

    /** Background default — used as text fill on accent backgrounds (e.g., badge). */
    public static final String TEXT_FILL_BG_DEFAULT_STYLE =
        "-fx-text-fill: " + CSS_BG_DEFAULT + ";";

    // ── Dracula hex based ───────────────────────────────────────────────

    /** Default foreground text fill (hex). */
    public static final String TEXT_FILL_HEX_FG_STYLE =
        "-fx-text-fill: " + HEX_FG + ";";

    /** Comment / muted text fill (hex). */
    public static final String TEXT_FILL_HEX_COMMENT_STYLE =
        "-fx-text-fill: " + HEX_COMMENT + ";";

    /** Accent text fill (hex). */
    public static final String TEXT_FILL_HEX_ACCENT_STYLE =
        "-fx-text-fill: " + HEX_ACCENT + ";";

    /** Error text fill (hex). */
    public static final String TEXT_FILL_HEX_RED_STYLE =
        "-fx-text-fill: " + HEX_DANGER + ";";

    /** Warning text fill (hex). */
    public static final String TEXT_FILL_HEX_ORANGE_STYLE =
        "-fx-text-fill: " + HEX_ORANGE + ";";
}
