package fi.metropolia.tempconverter.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @AfterEach
    void resetToEnglish() {
        Messages.setLocale(Messages.ENGLISH);
    }

    @Test
    void englishIsTheDefault() {
        Messages.setLocale(Messages.ENGLISH);
        assertEquals("Convert & Save", Messages.get("button.convert"));
    }

    @Test
    void finnishBundleIsLoaded() {
        Messages.setLocale(Messages.FINNISH);
        assertEquals(Messages.FINNISH, Messages.getLocale());
        assertEquals("Päivitä", Messages.get("button.refresh"));
    }

    @Test
    void swedishBundleIsLoaded() {
        Messages.setLocale(Messages.SWEDISH);
        assertEquals("Språk", Messages.get("lang.title"));
    }

    @Test
    void japaneseBundleIsLoadedAsUtf8() {
        Messages.setLocale(Messages.JAPANESE);
        assertEquals("更新", Messages.get("button.refresh"));
        assertEquals("日本語", Messages.get("lang.japanese"));
    }

    @Test
    void arabicBundleIsLoadedAsUtf8() {
        Messages.setLocale(Messages.ARABIC);
        assertEquals("تحديث", Messages.get("button.refresh"));
        assertEquals("اللغة", Messages.get("lang.title"));
    }

    @Test
    void placeholdersAreReplaced() {
        Messages.setLocale(Messages.ENGLISH);
        assertEquals("Loaded 3 record(s)", Messages.get("status.loaded", 3));
    }

    @Test
    void missingKeyIsMarked() {
        Messages.setLocale(Messages.ENGLISH);
        assertEquals("!no.such.key!", Messages.get("no.such.key"));
    }

    @Test
    void onlyArabicIsRightToLeft() {
        Messages.setLocale(Messages.ARABIC);
        assertTrue(Messages.isRightToLeft());

        Messages.setLocale(Messages.ENGLISH);
        assertFalse(Messages.isRightToLeft());

        Messages.setLocale(Messages.JAPANESE);
        assertFalse(Messages.isRightToLeft());
    }

    @Test
    void getLangReturnsTheDatabaseLanguageCode() {
        Messages.setLocale(Messages.ENGLISH);
        assertEquals("en", Messages.getLang());

        Messages.setLocale(Messages.FINNISH);
        assertEquals("fi", Messages.getLang());

        Messages.setLocale(Messages.SWEDISH);
        assertEquals("sv", Messages.getLang());

        Messages.setLocale(Messages.JAPANESE);
        assertEquals("ja", Messages.getLang());

        Messages.setLocale(Messages.ARABIC);
        assertEquals("ar", Messages.getLang());
    }

    @Test
    void everyLocaleHasAllKeys() {
        String[] keys = {
                "app.title", "app.heading",
                "label.value", "label.fromUnit", "label.toUnit",
                "label.result", "label.lastFive",
                "button.convert", "button.refresh", "button.delete",
                "col.id", "col.input", "col.fromUnit", "col.converted",
                "col.toUnit", "col.lang", "col.created",
                "status.ready", "status.invalidNumber", "status.sameUnit",
                "result.extreme"
        };

        for (Locale locale : new Locale[]{
                Messages.ENGLISH, Messages.FINNISH, Messages.SWEDISH,
                Messages.JAPANESE, Messages.ARABIC}) {

            Messages.setLocale(locale);
            for (String key : keys) {
                String value = Messages.get(key);
                assertNotNull(value);
                assertFalse(value.startsWith("!"), "Missing key " + key + " in " + locale);
                assertFalse(value.isEmpty());
            }
        }
    }
}