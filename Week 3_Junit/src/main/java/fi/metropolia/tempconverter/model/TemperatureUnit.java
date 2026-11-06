package fi.metropolia.tempconverter.model;

public class TemperatureUnit {

    private int unitId;
    private String unitKey;
    private String symbol;
    private String name;
    private String description;

    public TemperatureUnit() {
    }

    public TemperatureUnit(int unitId, String unitKey, String symbol,
                           String name, String description) {
        this.unitId = unitId;
        this.unitKey = unitKey;
        this.symbol = symbol;
        this.name = name;
        this.description = description;
    }

    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    public String getUnitKey() {
        return unitKey;
    }

    public void setUnitKey(String unitKey) {
        this.unitKey = unitKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }
}