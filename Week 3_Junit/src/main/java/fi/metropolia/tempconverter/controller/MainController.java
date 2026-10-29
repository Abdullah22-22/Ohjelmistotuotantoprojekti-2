package fi.metropolia.tempconverter.controller;

import fi.metropolia.tempconverter.dao.TempRecordDAO;
import fi.metropolia.tempconverter.dao.TemperatureUnitDAO;
import fi.metropolia.tempconverter.i18n.Messages;
import fi.metropolia.tempconverter.model.TempRecord;
import fi.metropolia.tempconverter.model.TemperatureConverter;
import fi.metropolia.tempconverter.model.TemperatureUnit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;


public class MainController {

    @FXML private TextField inputField;
    @FXML private ComboBox<String> conversionBox;
    @FXML private Label resultLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<TempRecord> table;
    @FXML private TableColumn<TempRecord, Integer> idCol;
    @FXML private TableColumn<TempRecord, Double> inputCol;
    @FXML private TableColumn<TempRecord, Double> convertedCol;
    @FXML private TableColumn<TempRecord, Integer> unitCol;
    @FXML private TableColumn<TempRecord, Object> createdCol;

    private final TemperatureConverter converter = new TemperatureConverter();
    private final TempRecordDAO recordDAO = new TempRecordDAO();
    private final TemperatureUnitDAO unitDAO = new TemperatureUnitDAO();

    private final ObservableList<TempRecord> data = FXCollections.observableArrayList();

    /** Called automatically by FXMLLoader once the FXML is loaded. */
    @FXML
    private void initialize() {
        conversionBox.getItems().addAll(
                Messages.get("conv.f2c"),
                Messages.get("conv.c2f"),
                Messages.get("conv.k2c"),
                Messages.get("conv.c2k"));
        conversionBox.getSelectionModel().selectFirst();

        idCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        inputCol.setCellValueFactory(new PropertyValueFactory<>("inputValue"));
        convertedCol.setCellValueFactory(new PropertyValueFactory<>("convertedValue"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unitId"));
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        table.setItems(data);

        resultLabel.setText(Messages.get("label.result") + ": -");

        loadUnits();
        loadRecords();
    }

    @FXML
    private void onConvert() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) {
            showStatus(Messages.get("status.enterValue"), true);
            return;
        }

        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            showStatus(Messages.get("status.invalidNumber"), true);
            return;
        }

        int choice = conversionBox.getSelectionModel().getSelectedIndex();
        double result;
        int unitId;

        switch (choice) {
            case 0 -> {
                result = converter.fahrenheitToCelsius(value);
                unitId = 1;
            }
            case 1 -> {
                result = converter.celsiusToFahrenheit(value);
                unitId = 2;
            }
            case 2 -> {
                result = converter.kelvinToCelsius(value);
                unitId = 1;
            }
            default -> {
                result = converter.celsiusToKelvin(value);
                unitId = 3;
            }
        }

        String extra = converter.isExtremeTemperature(result)
                ? "  " + Messages.get("result.extreme")
                : "";
        resultLabel.setText(String.format("%s: %.2f%s",
                Messages.get("label.result"), result, extra));

        try {
            recordDAO.insert(new TempRecord(value, result, unitId));
            showStatus(Messages.get("status.saved"), false);
            loadRecords();
        } catch (SQLException ex) {
            showStatus(Messages.get("status.dbError", ex.getMessage()), true);
        }
    }

    @FXML
    private void onRefresh() {
        loadRecords();
    }

    @FXML
    private void onDelete() {
        TempRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus(Messages.get("status.selectRow"), true);
            return;
        }
        try {
            recordDAO.delete(selected.getRecordId());
            showStatus(Messages.get("status.deleted", selected.getRecordId()), false);
            loadRecords();
        } catch (SQLException ex) {
            showStatus(Messages.get("status.deleteFailed", ex.getMessage()), true);
        }
    }

    private void loadRecords() {
        try {
            List<TempRecord> records = recordDAO.findAll();
            data.setAll(records);
            showStatus(Messages.get("status.loaded", records.size()), false);
        } catch (SQLException ex) {
            showStatus(Messages.get("status.cannotLoad", ex.getMessage()), true);
        }
    }

    private void loadUnits() {
        try {
            List<TemperatureUnit> units = unitDAO.findAll();
            showStatus(Messages.get("status.connected", units.size()), false);
        } catch (SQLException ex) {
            showStatus(Messages.get("status.noConnection", ex.getMessage()), true);
        }
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}