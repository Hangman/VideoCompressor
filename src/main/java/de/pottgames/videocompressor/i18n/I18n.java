package de.pottgames.videocompressor.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Central helper for internationalization (I18N).
 *
 * Loads message bundles from {@code de/pottgames/videocompressor/i18n/Messages}
 * using the system locale by default, with English as the fallback.
 *
 * Thread-safe: the bundle reference is volatile and replaced atomically
 * when the locale is changed at runtime.
 */
public final class I18n {

    private static final String BUNDLE_BASE_NAME =
        "de.pottgames.videocompressor.i18n.Messages";

    // English is the fallback locale
    private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;

    // Current bundle – volatile for safe publication across threads
    private static volatile ResourceBundle bundle;

    // Current locale – volatile for safe publication across threads
    private static volatile Locale currentLocale;

    // Static initializer – load bundle on first access
    static {
        //setLocale(Locale.getDefault()); // TODO: uncomment after testing
        setLocale(Locale.CHINESE); // temporarily for testing, remove after testing
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private I18n() {
        // intentionally empty
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Locale management
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the currently active locale.
     */
    public static Locale getLocale() {
        return currentLocale;
    }

    /**
     * Switches to the given locale at runtime.
     *
     * If the requested locale is not available, falls back to English.
     *
     * @param locale the new locale to activate
     */
    public static void setLocale(Locale locale) {
        Objects.requireNonNull(locale, "locale must not be null");

        Locale effective = resolveLocale(locale);

        ResourceBundle newBundle = ResourceBundle.getBundle(
            BUNDLE_BASE_NAME,
            effective,
            I18n.class.getClassLoader()
        );

        bundle = newBundle;
        currentLocale = effective;
    }

    /**
     * Resolves the requested locale against available bundles.
     * Falls back to English if the locale is not supported.
     */
    private static Locale resolveLocale(Locale requested) {
        // Try the requested locale
        if (
            ResourceBundle.getBundle(
                BUNDLE_BASE_NAME,
                requested,
                I18n.class.getClassLoader()
            ) !=
            null
        ) {
            return requested;
        }

        // Try the language-only variant (e.g. zh_CN -> zh)
        Locale languageOnly = Locale.of(requested.getLanguage());
        if (
            ResourceBundle.getBundle(
                BUNDLE_BASE_NAME,
                languageOnly,
                I18n.class.getClassLoader()
            ) !=
            null
        ) {
            return languageOnly;
        }

        // Fall back to English
        return FALLBACK_LOCALE;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Message retrieval
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the localized string for the given key.
     *
     * @param key the message key
     * @return the localized string
     * @throws java.util.MissingResourceException if the key is not found
     */
    public static String get(String key) {
        return bundle.getString(key);
    }

    /**
     * Returns the localized string for the given key, formatted with the
     * provided arguments using {@link java.text.MessageFormat#format(String, Object...)}.
     *
     * Placeholders follow {@code java.text.MessageFormat} syntax, e.g.
     * {@code Bearbeite Video {0} von {1}}.
     *
     * @param key  the message key
     * @param args the format arguments
     * @return the formatted localized string
     */
    public static String get(String key, Object... args) {
        String pattern = bundle.getString(key);
        return MessageFormat.format(pattern, args);
    }

    /**
     * Returns the localized string for the given key, or the fallback
     * value if the key does not exist.
     *
     * @param key     the message key
     * @param fallback the value to return if the key is missing
     * @return the localized string or the fallback
     */
    public static String getOrDefault(String key, String fallback) {
        try {
            return bundle.getString(key);
        } catch (java.util.MissingResourceException e) {
            return fallback;
        }
    }
}
