package fi.metropolia.tempconverter.dao;

import fi.metropolia.tempconverter.db.DBConnection;
import fi.metropolia.tempconverter.model.TemperatureUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureUnitDAOTest {

    private TemperatureUnitDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        DBConnection.setCredentials(
                "jdbc:h2:mem:unitdao;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");

        try (Statement st = DBConnection.getConnection().createStatement()) {
            st.execute("DROP TABLE IF EXISTS temp_records");
            st.execute("DROP TABLE IF EXISTS unit_translation");
            st.execute("DROP TABLE IF EXISTS temperature_units");

            st.execute("CREATE TABLE temperature_units ("
                    + "unit_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "unit_key VARCHAR(20) NOT NULL UNIQUE,"
                    + "symbol VARCHAR(5) NOT NULL)");

            st.execute("CREATE TABLE unit_translation ("
                    + "unit_id INT NOT NULL,"
                    + "lang VARCHAR(5) NOT NULL,"
                    + "name VARCHAR(50) NOT NULL,"
                    + "description VARCHAR(200),"
                    + "PRIMARY KEY (unit_id, lang),"
                    + "FOREIGN KEY (unit_id) REFERENCES temperature_units(unit_id) "
                    + "ON DELETE CASCADE)");

            st.execute("INSERT INTO temperature_units (unit_id, unit_key, symbol) VALUES "
                    + "(1,'celsius','C'), (2,'fahrenheit','F'), (3,'kelvin','K')");

            // Celsius and Fahrenheit: full translations.
            // Kelvin: English only, on purpose, to test the fallback.
            st.execute("INSERT INTO unit_translation (unit_id, lang, name, description) VALUES "
                    + "(1,'en','Celsius','Water freezes at 0'),"
                    + "(1,'fi','Celsius','Vesi jaatyy 0:ssa'),"
                    + "(1,'ja','\u6442\u6c0f','\u6c34\u306f0\u5ea6\u3067\u51cd\u308b'),"
                    + "(1,'ar','\u0633\u064a\u0644\u064a\u0632\u064a\u0648\u0633','\u0648\u0635\u0641'),"
                    + "(2,'en','Fahrenheit','Water freezes at 32'),"
                    + "(2,'fi','Fahrenheit','Vesi jaatyy 32:ssa'),"
                    + "(2,'ja','\u83ef\u6c0f','\u6c34\u306f32\u5ea6\u3067\u51cd\u308b'),"
                    + "(3,'en','Kelvin','Absolute scale')");
        }

        dao = new TemperatureUnitDAO();
    }

    @AfterEach
    void tearDown() throws SQLException {
        DBConnection.closeConnection();
    }

    @Test
    void findAllReturnsThreeUnits() throws SQLException {
        assertEquals(3, dao.findAllLocalized("en").size());
    }

    @Test
    @DisplayName("Units come back in English")
    void unitsAreLocalizedInEnglish() throws SQLException {
        List<TemperatureUnit> units = dao.findAllLocalized("en");
        assertEquals("Celsius", units.get(0).getName());
        assertEquals("Fahrenheit", units.get(1).getName());
    }

    @Test
    @DisplayName("Units come back in Japanese")
    void unitsAreLocalizedInJapanese() throws SQLException {
        List<TemperatureUnit> units = dao.findAllLocalized("ja");
        assertEquals("\u6442\u6c0f", units.get(0).getName());   // Celsius
        assertEquals("\u83ef\u6c0f", units.get(1).getName());   // Fahrenheit
    }

    @Test
    @DisplayName("Arabic text survives the round trip")
    void arabicIsStoredAndReadAsUtf8() throws SQLException {
        TemperatureUnit celsius = dao.findByIdLocalized(1, "ar");
        assertNotNull(celsius);
        assertEquals("\u0633\u064a\u0644\u064a\u0632\u064a\u0648\u0633", celsius.getName());
    }

    @Test
    @DisplayName("FALLBACK: a missing translation falls back to English")
    void missingTranslationFallsBackToEnglish() throws SQLException {
        // Kelvin has no Finnish row, so the English one must be used
        TemperatureUnit kelvin = dao.findByIdLocalized(3, "fi");

        assertNotNull(kelvin);
        assertEquals("Kelvin", kelvin.getName());
        assertEquals("Absolute scale", kelvin.getDescription());
    }

    @Test
    @DisplayName("FALLBACK: a unit with no translation at all still appears")
    void unitWithoutAnyTranslationIsStillListed() throws SQLException {
        // Kelvin has no Japanese row either
        List<TemperatureUnit> units = dao.findAllLocalized("ja");

        assertEquals(3, units.size());
        assertEquals("Kelvin", units.get(2).getName());
    }

    @Test
    @DisplayName("FALLBACK: an unknown language gives the whole list in English")
    void unknownLanguageFallsBackCompletely() throws SQLException {
        List<TemperatureUnit> units = dao.findAllLocalized("de");

        assertEquals(3, units.size());
        assertEquals("Celsius", units.get(0).getName());
        assertEquals("Fahrenheit", units.get(1).getName());
        assertEquals("Kelvin", units.get(2).getName());
    }

    @Test
    void theKeyAndSymbolDoNotChangeWithTheLanguage() throws SQLException {
        TemperatureUnit en = dao.findByIdLocalized(1, "en");
        TemperatureUnit ja = dao.findByIdLocalized(1, "ja");

        assertEquals("celsius", en.getUnitKey());
        assertEquals("celsius", ja.getUnitKey());
        assertEquals("C", en.getSymbol());
        assertEquals("C", ja.getSymbol());
    }

    @Test
    void findByIdReturnsNullForUnknownId() throws SQLException {
        assertNull(dao.findByIdLocalized(999, "en"));
    }

    @Test
    void findByKeyReturnsTheRightUnit() throws SQLException {
        TemperatureUnit kelvin = dao.findByKeyLocalized("kelvin", "en");
        assertNotNull(kelvin);
        assertEquals(3, kelvin.getUnitId());
        assertEquals("K", kelvin.getSymbol());
    }

    @Test
    void findByKeyReturnsNullForUnknownKey() throws SQLException {
        assertNull(dao.findByKeyLocalized("rankine", "en"));
    }

    @Test
    void insertAddsNewUnitAndTranslation() throws SQLException {
        int id = dao.insert("rankine", "R");
        assertTrue(id > 0);

        assertTrue(dao.addTranslation(id, "en", "Rankine", "Absolute Fahrenheit scale"));

        TemperatureUnit saved = dao.findByIdLocalized(id, "en");
        assertNotNull(saved);
        assertEquals("Rankine", saved.getName());
        assertEquals(4, dao.findAllLocalized("en").size());
    }

    @Test
    @DisplayName("FALLBACK: a new unit with only English shows English in every language")
    void newUnitWithOnlyEnglishFallsBack() throws SQLException {
        int id = dao.insert("rankine", "R");
        dao.addTranslation(id, "en", "Rankine", "Absolute Fahrenheit scale");

        assertEquals("Rankine", dao.findByIdLocalized(id, "ja").getName());
        assertEquals("Rankine", dao.findByIdLocalized(id, "ar").getName());
        assertEquals("Rankine", dao.findByIdLocalized(id, "fi").getName());
    }

    @Test
    void deleteRemovesUnitAndItsTranslations() throws SQLException {
        int id = dao.insert("rankine", "R");
        dao.addTranslation(id, "en", "Rankine", "desc");

        assertTrue(dao.delete(id));
        assertNull(dao.findByIdLocalized(id, "en"));
    }

    @Test
    void deleteReturnsFalseForUnknownId() throws SQLException {
        assertFalse(dao.delete(999));
    }
}