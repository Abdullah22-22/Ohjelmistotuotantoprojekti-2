package fi.metropolia.tempconverter.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureConverterTest {

    private TemperatureConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TemperatureConverter();
    }

    @Test
    @DisplayName("Fahrenheit to Celsius")
    void fahrenheitToCelsius() {
        assertEquals(0, converter.fahrenheitToCelsius(32), 0.01);
        assertEquals(100, converter.fahrenheitToCelsius(212), 0.01);
        assertEquals(-40, converter.fahrenheitToCelsius(-40), 0.01);
        assertEquals(37, converter.fahrenheitToCelsius(98.6), 0.01);
    }

    @Test
    @DisplayName("Celsius to Fahrenheit")
    void celsiusToFahrenheit() {
        assertEquals(32, converter.celsiusToFahrenheit(0), 0.01);
        assertEquals(212, converter.celsiusToFahrenheit(100), 0.01);
        assertEquals(-40, converter.celsiusToFahrenheit(-40), 0.01);
    }

    @Test
    @DisplayName("Kelvin to Celsius")
    void kelvinToCelsius() {
        assertEquals(26.85, converter.kelvinToCelsius(300), 0.01);
        assertEquals(0, converter.kelvinToCelsius(273.15), 0.01);
        assertEquals(-273.15, converter.kelvinToCelsius(0), 0.01);
    }

    @Test
    @DisplayName("Celsius to Kelvin")
    void celsiusToKelvin() {
        assertEquals(273.15, converter.celsiusToKelvin(0), 0.01);
        assertEquals(373.15, converter.celsiusToKelvin(100), 0.01);
    }

    @Test
    @DisplayName("Round trip conversion stays the same")
    void roundTripConversion() {
        double original = 25.0;
        double back = converter.fahrenheitToCelsius(converter.celsiusToFahrenheit(original));
        assertEquals(original, back, 0.0001);
    }

    @Test
    @DisplayName("Extreme temperature - above the upper limit")
    void isExtremeAboveUpperLimit() {
        assertTrue(converter.isExtremeTemperature(51));
        assertTrue(converter.isExtremeTemperature(1000));
    }

    @Test
    @DisplayName("Extreme temperature - below the lower limit")
    void isExtremeBelowLowerLimit() {
        assertTrue(converter.isExtremeTemperature(-41));
        assertTrue(converter.isExtremeTemperature(-273.15));
    }

    @Test
    @DisplayName("Extreme temperature - the boundaries themselves are not extreme")
    void boundariesAreNotExtreme() {
        assertFalse(converter.isExtremeTemperature(-40));
        assertFalse(converter.isExtremeTemperature(50));
    }

    @Test
    @DisplayName("Extreme temperature - normal values")
    void normalValuesAreNotExtreme() {
        assertFalse(converter.isExtremeTemperature(0));
        assertFalse(converter.isExtremeTemperature(25));
        assertFalse(converter.isExtremeTemperature(-39.9));
        assertFalse(converter.isExtremeTemperature(49.9));
    }
}