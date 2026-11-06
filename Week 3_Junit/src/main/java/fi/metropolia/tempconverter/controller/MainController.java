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

    private static final int HISTORY_SIZE = 5;

    @FXML private TextField inputField;
    @FXML private ComboBox<TemperatureUnit> fromUnitBox;
    @FXML private ComboBox<TemperatureUnit> toUnitBox;
    @FXML private Label descriptionLabel;
    @FXML private Label resultLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<TempRecord> table;
    @FXML private TableColumn<TempRecord, Integer> idCol;
    @FXML private TableColumn<TempRecord, Double> inputCol;
    @FXML private TableColumn<TempRecord, String> fromCol;
    @FXML private TableColumn<TempRecord, Double> convertedCol;
    @FXML private TableColumn<TempRecord, String> toCol;
    @FXML private TableColumn<TempRecord, String> langCol;
    @FXML private TableColumn<TempRecord, Object> createdCol;

    private final TemperatureConverter converter = new TemperatureConverter();
    private final TempRecordDAO recordDAO = new TempRecordDAO();
    private final TemperatureUnitDAO unitDAO = new TemperatureUnitDAO();

    private final ObservableList<TempRecord> data = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        inputCol.setCellValueFactory(new PropertyValueFactory<>("inputValue"));
        fromCol.setCellValueFactory(new PropertyValueFactory<>("fromUnitName"));
        convertedCol.setCellValueFactory(new PropertyValueFactory<>("convertedValue"));
        toCol.setCellValueFactory(new PropertyValueFactory<>("toUnitName"));
        langCol.setCellValueFactory(new PropertyValueFactory<>("lang"));
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        table.setItems(data);
        resultLabel.setText(Messages.get("label.result") + ": -");

        // Show the localized description of whichever unit is selected
        fromUnitBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showDescription(selected));

        loadUnits();
        loadRecords();
    }

    /** Fills both dropdowns from the DB in the currently selected language. */
    private void loadUnits() {
        try {
            List<TemperatureUnit> units = unitDAO.findAllLocalized(Messages.getLang());

            fromUnitBox.getItems().setAll(units);
            toUnitBox.getItems().setAll(units);

            if (units.size() >= 2) {
                fromUnitBox.getSelectionModel().select(1);  // Fahrenheit
                toUnitBox.getSelectionModel().select(0);    // Celsius
            }
            showStatus(Messages.get("status.connected", units.size()), false);
        } catch (SQLException ex) {
            showStatus(Messages.get("status.noConnection", ex.getMessage()), true);
        }
    }

    private void showDescription(TemperatureUnit unit) {
        descriptionLabel.setText(unit == null ? "" : unit.getDescription());
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

        TemperatureUnit from = fromUnitBox.getSelectionModel().getSelectedItem();
        TemperatureUnit to = toUnitBox.getSelectionModel().getSelectedItem();
        if (from == null || to == null) {
            showStatus(Messages.get("status.selectRow"), true);
            return;
        }
        if (from.getUnitId() == to.getUnitId()) {
            showStatus(Messages.get("status.sameUnit"), true);
            return;
        }

        double result = converter.convert(value, from.getUnitKey(), to.getUnitKey());
        double celsius = converter.toCelsius(value, from.getUnitKey());

        String extra = converter.isExtremeTemperature(celsius)
                ? "  " + Messages.get("result.extreme")
                : "";
        resultLabel.setText(String.format("%s: %.2f %s%s",
                Messages.get("label.result"), result, to.getSymbol(), extra));

        try {
            recordDAO.insert(new TempRecord(value, result,
                    from.getUnitId(), to.getUnitId(), Messages.getLang()));
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

    /** Loads the last 5 conversions with unit names in the selected language. */
    private void loadRecords() {
        try {
            List<TempRecord> records =
                    recordDAO.findLatest(HISTORY_SIZE, Messages.getLang());
            data.setAll(records);
            showStatus(Messages.get("status.loaded", records.size()), false);
        } catch (SQLException ex) {
            showStatus(Messages.get("status.cannotLoad", ex.getMessage()), true);
        }
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}