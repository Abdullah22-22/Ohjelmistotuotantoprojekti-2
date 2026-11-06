package fi.metropolia.tempconverter.model;

import java.time.LocalDateTime;

public class TempRecord {

    private int recordId;
    private double inputValue;
    private double convertedValue;
    private int fromUnitId;
    private int toUnitId;
    private String lang;
    private LocalDateTime createdAt;

    private String fromUnitName;
    private String toUnitName;

    public TempRecord() {
    }

    public TempRecord(double inputValue, double convertedValue,
                      int fromUnitId, int toUnitId, String lang) {
        this.inputValue = inputValue;
        this.convertedValue = convertedValue;
        this.fromUnitId = fromUnitId;
        this.toUnitId = toUnitId;
        this.lang = lang;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public double getInputValue() {
        return inputValue;
    }

    public void setInputValue(double inputValue) {
        this.inputValue = inputValue;
    }

    public double getConvertedValue() {
        return convertedValue;
    }

    public void setConvertedValue(double convertedValue) {
        this.convertedValue = convertedValue;
    }

    public int getFromUnitId() {
        return fromUnitId;
    }

    public void setFromUnitId(int fromUnitId) {
        this.fromUnitId = fromUnitId;
    }

    public int getToUnitId() {
        return toUnitId;
    }

    public void setToUnitId(int toUnitId) {
        this.toUnitId = toUnitId;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFromUnitName() {
        return fromUnitName;
    }

    public void setFromUnitName(String fromUnitName) {
        this.fromUnitName = fromUnitName;
    }

    public String getToUnitName() {
        return toUnitName;
    }

    public void setToUnitName(String toUnitName) {
        this.toUnitName = toUnitName;
    }
}