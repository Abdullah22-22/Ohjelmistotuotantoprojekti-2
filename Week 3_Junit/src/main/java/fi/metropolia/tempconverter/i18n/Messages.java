package fi.metropolia.tempconverter.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class Messages {

    public static final Locale ENGLISH  = new Locale("en", "US");
    public static final Locale FINNISH  = new Locale("fi", "FI");
    public static final Locale SWEDISH  = new Locale("sv", "SE");
    public static final Locale JAPANESE = new Locale("ja", "JP");
    public static final Locale ARABIC   = new Locale("ar", "SA");

    private static final String BASE_NAME = "MessagesBundle";

    private static ResourceBundle bundle = load(ENGLISH);
    private static Locale current = ENGLISH;

    private Messages() {
    }

    public static void setLocale(Locale locale) {
        current = locale;
        bundle = load(locale);
    }

    public static Locale getLocale() {
        return current;
    }

    public static boolean isRightToLeft() {
        return ARABIC.getLanguage().equals(current.getLanguage());
    }

    public static String get(String key) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return "!" + key + "!";
    }

    public static String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    private static ResourceBundle load(Locale locale) {
        return ResourceBundle.getBundle(BASE_NAME, locale, new Utf8Control());
    }

    private static class Utf8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IOException {

            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");

            try (InputStream in = loader.getResourceAsStream(resourceName)) {
                if (in == null) {
                    return null;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    }
}