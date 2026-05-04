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

    // ─────────────────────────────────────────────────────────────────────
    //  Dracula palette — hex values for -fx-background-color etc.
    // ─────────────────────────────────────────────────────────────────────

    /** Background — main surface. */
    public static final String HEX_BG = "#282a36";

    /** Background — darker variant used for log/console areas. */
    public static final String HEX_BG_DARK = "#21222c";

    /** Comment / muted elements. */
    public static final String HEX_COMMENT = "#6272a4";

    /** Primary accent (purple). */
    public static final String HEX_ACCENT = "#9580ff";

    /** Accent variant — lighter purple used in some panels. */
    public static final String HEX_ACCENT_LIGHT = "#bd93f9";

    /** Cyan — used for highlights and output labels. */
    public static final String HEX_CYAN = "#8be9fd";

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
}
