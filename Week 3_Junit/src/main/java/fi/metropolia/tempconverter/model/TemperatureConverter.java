package fi.metropolia.tempconverter.model;

public class TemperatureConverter {

    public double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    public double celsiusToFahrenheit(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    public double kelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    public double celsiusToKelvin(double celsius) {
        return celsius + 273.15;
    }

    public double toCelsius(double value, String fromKey) {
        return switch (fromKey) {
            case "celsius"    -> value;
            case "fahrenheit" -> fahrenheitToCelsius(value);
            case "kelvin"     -> kelvinToCelsius(value);
            default -> throw new IllegalArgumentException("Unknown unit: " + fromKey);
        };
    }

    public double fromCelsius(double celsius, String toKey) {
        return switch (toKey) {
            case "celsius"    -> celsius;
            case "fahrenheit" -> celsiusToFahrenheit(celsius);
            case "kelvin"     -> celsiusToKelvin(celsius);
            default -> throw new IllegalArgumentException("Unknown unit: " + toKey);
        };
    }

    public double convert(double value, String fromKey, String toKey) {
        return fromCelsius(toCelsius(value, fromKey), toKey);
    }

    public boolean isExtremeTemperature(double celsius) {
        return celsius < -40 || celsius > 50;
    }
}