package fi.metropolia.tempconverter.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TempRecordTest {

    @Test
    void emptyConstructorCreatesObject() {
        TempRecord record = new TempRecord();
        assertNotNull(record);
        assertEquals(0, record.getRecordId());
        assertEquals(0.0, record.getInputValue(), 0.001);
        assertEquals(0, record.getFromUnitId());
        assertEquals(0, record.getToUnitId());
        assertNull(record.getLang());
        assertNull(record.getCreatedAt());
    }

    @Test
    void parameterizedConstructorSetsValues() {
        TempRecord record = new TempRecord(212.0, 100.0, 2, 1, "ja");

        assertEquals(212.0, record.getInputValue(), 0.001);
        assertEquals(100.0, record.getConvertedValue(), 0.001);
        assertEquals(2, record.getFromUnitId());
        assertEquals(1, record.getToUnitId());
        assertEquals("ja", record.getLang());
    }

    @Test
    void settersAndGettersWork() {
        TempRecord record = new TempRecord();
        LocalDateTime now = LocalDateTime.now();

        record.setRecordId(5);
        record.setInputValue(32.0);
        record.setConvertedValue(0.0);
        record.setFromUnitId(2);
        record.setToUnitId(1);
        record.setLang("fi");
        record.setCreatedAt(now);

        assertEquals(5, record.getRecordId());
        assertEquals(32.0, record.getInputValue(), 0.001);
        assertEquals(0.0, record.getConvertedValue(), 0.001);
        assertEquals(2, record.getFromUnitId());
        assertEquals(1, record.getToUnitId());
        assertEquals("fi", record.getLang());
        assertEquals(now, record.getCreatedAt());
    }

    @Test
    void localizedUnitNamesAreNotStoredInTheRecord() {
        TempRecord record = new TempRecord(212.0, 100.0, 2, 1, "ar");

        assertNull(record.getFromUnitName());
        assertNull(record.getToUnitName());

        record.setFromUnitName("فهرنهايت");
        record.setToUnitName("سيليزيوس");

        assertEquals("فهرنهايت", record.getFromUnitName());
        assertEquals("سيليزيوس", record.getToUnitName());
    }

    @Test
    void negativeValuesAreAllowed() {
        TempRecord record = new TempRecord(-40.0, -40.0, 2, 1, "en");
        assertEquals(-40.0, record.getInputValue(), 0.001);
        assertEquals(-40.0, record.getConvertedValue(), 0.001);
    }
}