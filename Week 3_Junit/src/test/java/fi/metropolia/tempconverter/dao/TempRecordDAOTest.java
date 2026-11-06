package fi.metropolia.tempconverter.dao;

import fi.metropolia.tempconverter.db.DBConnection;
import fi.metropolia.tempconverter.model.TempRecord;

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

class TempRecordDAOTest {

    private TempRecordDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        DBConnection.setCredentials(
                "jdbc:h2:mem:recorddao;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");

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

            st.execute("CREATE TABLE temp_records ("
                    + "record_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "input_value DOUBLE NOT NULL,"
                    + "converted_value DOUBLE NOT NULL,"
                    + "from_unit_id INT,"
                    + "to_unit_id INT,"
                    + "lang VARCHAR(5) NOT NULL DEFAULT 'en',"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (from_unit_id) REFERENCES temperature_units(unit_id),"
                    + "FOREIGN KEY (to_unit_id) REFERENCES temperature_units(unit_id))");

            st.execute("INSERT INTO temperature_units (unit_id, unit_key, symbol) VALUES "
                    + "(1,'celsius','C'), (2,'fahrenheit','F'), (3,'kelvin','K')");

            // Kelvin has English only, to test the fallback here too
            st.execute("INSERT INTO unit_translation (unit_id, lang, name, description) VALUES "
                    + "(1,'en','Celsius','Water freezes at 0'),"
                    + "(1,'ja','\u6442\u6c0f','\u6c34\u306f0\u5ea6\u3067\u51cd\u308b'),"
                    + "(2,'en','Fahrenheit','Water freezes at 32'),"
                    + "(2,'ja','\u83ef\u6c0f','\u6c34\u306f32\u5ea6\u3067\u51cd\u308b'),"
                    + "(3,'en','Kelvin','Absolute scale')");
        }

        dao = new TempRecordDAO();
    }

    @AfterEach
    void tearDown() throws SQLException {
        DBConnection.closeConnection();
    }

    @Test
    void tableIsEmptyAtStart() throws SQLException {
        assertEquals(0, dao.count());
        assertTrue(dao.findLatest(5, "en").isEmpty());
    }

    @Test
    void insertReturnsGeneratedId() throws SQLException {
        int id = dao.insert(new TempRecord(212.0, 100.0, 2, 1, "en"));
        assertTrue(id > 0);
        assertEquals(1, dao.count());
    }

    @Test
    void insertStoresEveryField() throws SQLException {
        int id = dao.insert(new TempRecord(32.0, 0.0, 2, 1, "fi"));
        TempRecord saved = dao.findById(id, "en");

        assertNotNull(saved);
        assertEquals(32.0, saved.getInputValue(), 0.001);
        assertEquals(0.0, saved.getConvertedValue(), 0.001);
        assertEquals(2, saved.getFromUnitId());
        assertEquals(1, saved.getToUnitId());
        assertEquals("fi", saved.getLang());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("The language of the conversion is saved with it")
    void theLanguageIsStoredPerRecord() throws SQLException {
        int ja = dao.insert(new TempRecord(212.0, 100.0, 2, 1, "ja"));
        int ar = dao.insert(new TempRecord(32.0, 0.0, 2, 1, "ar"));

        assertEquals("ja", dao.findById(ja, "en").getLang());
        assertEquals("ar", dao.findById(ar, "en").getLang());
    }

    @Test
    @DisplayName("Unit names in the history are localized")
    void unitNamesAreLocalizedInTheHistory() throws SQLException {
        int id = dao.insert(new TempRecord(212.0, 100.0, 2, 1, "ja"));

        TempRecord inEnglish = dao.findById(id, "en");
        assertEquals("Fahrenheit", inEnglish.getFromUnitName());
        assertEquals("Celsius", inEnglish.getToUnitName());

        TempRecord inJapanese = dao.findById(id, "ja");
        assertEquals("\u83ef\u6c0f", inJapanese.getFromUnitName());
        assertEquals("\u6442\u6c0f", inJapanese.getToUnitName());
    }

    @Test
    @DisplayName("FALLBACK: a missing unit translation shows the English name")
    void missingUnitTranslationFallsBackInTheHistory() throws SQLException {
        // Kelvin has no Japanese row
        int id = dao.insert(new TempRecord(0.0, 273.15, 1, 3, "ja"));

        TempRecord record = dao.findById(id, "ja");
        assertNotNull(record);
        assertEquals("\u6442\u6c0f", record.getFromUnitName());  // Celsius in Japanese
        assertEquals("Kelvin", record.getToUnitName());          // fell back to English
    }

    @Test
    @DisplayName("Only the last 5 conversions are returned")
    void findLatestReturnsAtMostFive() throws SQLException {
        for (int i = 1; i <= 8; i++) {
            dao.insert(new TempRecord(i, i * 2, 2, 1, "en"));
        }

        List<TempRecord> latest = dao.findLatest(5, "en");
        assertEquals(5, latest.size());
        assertEquals(8, dao.count());
    }

    @Test
    @DisplayName("The newest conversion comes first")
    void findLatestIsNewestFirst() throws SQLException {
        dao.insert(new TempRecord(1.0, 2.0, 2, 1, "en"));
        dao.insert(new TempRecord(99.0, 100.0, 2, 1, "en"));

        List<TempRecord> latest = dao.findLatest(5, "en");
        assertEquals(99.0, latest.get(0).getInputValue(), 0.001);
        assertEquals(1.0, latest.get(1).getInputValue(), 0.001);
    }

    @Test
    void findByIdReturnsNullForUnknownId() throws SQLException {
        assertNull(dao.findById(999, "en"));
    }

    @Test
    void deleteRemovesRecord() throws SQLException {
        int id = dao.insert(new TempRecord(100.0, 212.0, 1, 2, "en"));
        assertTrue(dao.delete(id));
        assertNull(dao.findById(id, "en"));
        assertEquals(0, dao.count());
    }

    @Test
    void deleteReturnsFalseForUnknownId() throws SQLException {
        assertFalse(dao.delete(12345));
    }

    @Test
    void deleteAllClearsTable() throws SQLException {
        dao.insert(new TempRecord(1.0, 2.0, 2, 1, "en"));
        dao.insert(new TempRecord(3.0, 4.0, 2, 1, "en"));

        assertEquals(2, dao.deleteAll());
        assertEquals(0, dao.count());
    }

    @Test
    void negativeTemperaturesAreStored() throws SQLException {
        int id = dao.insert(new TempRecord(-40.0, -40.0, 2, 1, "en"));
        TempRecord saved = dao.findById(id, "en");
        assertNotNull(saved);
        assertEquals(-40.0, saved.getInputValue(), 0.001);
    }
}