package fi.metropolia.tempconverter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemperatureUnitTest {

    @Test
    void emptyConstructorCreatesObject() {
        TemperatureUnit unit = new TemperatureUnit();
        assertNotNull(unit);
        assertEquals(0, unit.getUnitId());
        assertNull(unit.getUnitKey());
        assertNull(unit.getSymbol());
        assertNull(unit.getName());
        assertNull(unit.getDescription());
    }

    @Test
    void parameterizedConstructorSetsValues() {
        TemperatureUnit unit = new TemperatureUnit(
                1, "celsius", "C", "Celsius", "Water freezes at 0");

        assertEquals(1, unit.getUnitId());
        assertEquals("celsius", unit.getUnitKey());
        assertEquals("C", unit.getSymbol());
        assertEquals("Celsius", unit.getName());
        assertEquals("Water freezes at 0", unit.getDescription());
    }

    @Test
    void settersAndGettersWork() {
        TemperatureUnit unit = new TemperatureUnit();
        unit.setUnitId(3);
        unit.setUnitKey("kelvin");
        unit.setSymbol("K");
        unit.setName("Kelvin");
        unit.setDescription("Absolute scale");

        assertEquals(3, unit.getUnitId());
        assertEquals("kelvin", unit.getUnitKey());
        assertEquals("K", unit.getSymbol());
        assertEquals("Kelvin", unit.getName());
        assertEquals("Absolute scale", unit.getDescription());
    }

    @Test
    void toStringUsesTheLocalizedName() {
        TemperatureUnit unit = new TemperatureUnit(
                2, "fahrenheit", "F", "Fahrenheit", "Water freezes at 32");
        assertEquals("Fahrenheit (F)", unit.toString());
    }

    @Test
    void toStringShowsJapaneseNameWithTheSameSymbol() {
        TemperatureUnit unit = new TemperatureUnit(
                1, "celsius", "C", "摂氏", "水は0度で凍る");
        assertEquals("摂氏 (C)", unit.toString());
    }

    @Test
    void theKeyStaysTheSameWhateverTheLanguage() {
        TemperatureUnit en = new TemperatureUnit(1, "celsius", "C", "Celsius", "");
        TemperatureUnit ar = new TemperatureUnit(1, "celsius", "C", "سيليزيوس", "");

        assertEquals(en.getUnitKey(), ar.getUnitKey());
        assertEquals(en.getSymbol(), ar.getSymbol());
        assertEquals(en.getUnitId(), ar.getUnitId());
    }
}