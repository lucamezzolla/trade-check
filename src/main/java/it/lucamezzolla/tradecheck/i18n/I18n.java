package it.lucamezzolla.tradecheck.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public final class I18n {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String LANGUAGE_KEY = "ui.language";

    /*
     * New stable node for TradeCheck 1.0.0.
     * A new user starts in English. Once changed, the choice persists.
     */
    private static final Preferences PREFS =
            Preferences.userRoot().node("/it/lucamezzolla/tradecheck/1.0.0");

    private static Locale locale = DEFAULT_LOCALE;
    private static ResourceBundle bundle;

    private I18n() {
    }

    public static void initialize() {
        String savedLanguage = PREFS.get(LANGUAGE_KEY, null);
        Locale initialLocale = savedLanguage == null || savedLanguage.isBlank()
                ? DEFAULT_LOCALE
                : Locale.forLanguageTag(savedLanguage);

        applyLocale(initialLocale, false);
    }

    public static void setLocale(Locale newLocale) {
        applyLocale(newLocale, true);
    }

    private static void applyLocale(Locale newLocale, boolean persist) {
        locale = normalize(newLocale);

        try {
            bundle = ResourceBundle.getBundle(
                    "messages",
                    locale,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            );
        } catch (MissingResourceException ex) {
            locale = DEFAULT_LOCALE;
            bundle = ResourceBundle.getBundle(
                    "messages",
                    DEFAULT_LOCALE,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            );
        }

        if (persist) {
            PREFS.put(LANGUAGE_KEY, locale.toLanguageTag());
        }
    }

    private static Locale normalize(Locale candidate) {
        if (candidate == null) {
            return DEFAULT_LOCALE;
        }

        return switch (candidate.getLanguage()) {
            case "it" -> Locale.ITALIAN;
            case "es" -> Locale.forLanguageTag("es");
            case "fr" -> Locale.FRENCH;
            case "pt" -> Locale.forLanguageTag("pt");
            default -> DEFAULT_LOCALE;
        };
    }

    public static Locale getLocale() {
        return locale;
    }

    public static String text(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return "!" + key + "!";
        }
    }
}
