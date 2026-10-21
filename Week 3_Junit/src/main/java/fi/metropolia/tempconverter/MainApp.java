package fi.metropolia.tempconverter;

import fi.metropolia.tempconverter.dao.TempRecordDAO;
import fi.metropolia.tempconverter.dao.TemperatureUnitDAO;
import fi.metropolia.tempconverter.i18n.Messages;
import fi.metropolia.tempconverter.model.TempRecord;
import fi.metropolia.tempconverter.model.TemperatureConverter;
import fi.metropolia.tempconverter.model.TemperatureUnit;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MainApp extends Application {

    private final TemperatureConverter converter = new TemperatureConverter();
    private final TempRecordDAO recordDAO = new TempRecordDAO();
    private final TemperatureUnitDAO unitDAO = new TemperatureUnitDAO();

    private TextField inputField;
    private ComboBox<String> conversionBox;
    private Label resultLabel;
    private Label statusLabel;
    private TableView<TempRecord> table;

    private final ObservableList<TempRecord> data = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        // Step 2 of the assignment: ask for the language BEFORE building the window.
        askForLanguage();

        // From here on every text comes from the chosen .properties file.
        stage.setTitle(Messages.get("app.title"));

        Label title = new Label(Messages.get("app.heading"));
        title.setFont(new Font("Arial", 22));

        inputField = new TextField();
        inputField.setPromptText(Messages.get("field.prompt"));

        conversionBox = new ComboBox<>();
        conversionBox.getItems().addAll(
                Messages.get("conv.f2c"),
                Messages.get("conv.c2f"),
                Messages.get("conv.k2c"),
                Messages.get("conv.c2k"));
        conversionBox.getSelectionModel().selectFirst();

        Button convertBtn = new Button(Messages.get("button.convert"));
        convertBtn.setOnAction(e -> convertAndSave());

        Button refreshBtn = new Button(Messages.get("button.refresh"));
        refreshBtn.setOnAction(e -> loadRecords());

        Button deleteBtn = new Button(Messages.get("button.delete"));
        deleteBtn.setOnAction(e -> deleteSelected());

        resultLabel = new Label(Messages.get("label.result") + ": -");
        resultLabel.setFont(new Font("Arial", 16));

        statusLabel = new Label(Messages.get("status.ready"));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label(Messages.get("label.value")), 0, 0);
        form.add(inputField, 1, 0);
        form.add(new Label(Messages.get("label.conversion")), 0, 1);
        form.add(conversionBox, 1, 1);

        HBox buttons = new HBox(10, convertBtn, refreshBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        buildTable();

        VBox root = new VBox(12,
                title,
                form,
                buttons,
                resultLabel,
                new Label(Messages.get("label.savedRecords")),
                table,
                statusLabel);
        root.setPadding(new Insets(20));

        stage.setScene(new Scene(root, 650, 520));
        stage.show();

        loadUnits();
        loadRecords();
    }

    /**
     * Shows the language dialog at startup. The choice decides which
     * MessagesBundle_*.properties file is used for the whole window.
     */
    private void askForLanguage() {
        // LinkedHashMap keeps the order, so English is always first in the list.
        Map<String, Locale> options = new LinkedHashMap<>();
        options.put("English", Messages.ENGLISH);
        options.put("Suomi (Finnish)", Messages.FINNISH);
        options.put("Svenska (Swedish)", Messages.SWEDISH);
        options.put("日本語 (Japanese)", Messages.JAPANESE);

        String first = options.keySet().iterator().next();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(first, options.keySet());
        dialog.setTitle(Messages.get("lang.title"));
        dialog.setHeaderText(Messages.get("lang.header"));
        dialog.setContentText(Messages.get("lang.content"));

        Optional<String> answer = dialog.showAndWait();
        // If the dialog is closed without choosing, English stays as the default.
        answer.ifPresent(choice -> Messages.setLocale(options.get(choice)));
    }

    private void buildTable() {
        table = new TableView<>();

        TableColumn<TempRecord, Integer> idCol = new TableColumn<>(Messages.get("col.id"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));

        TableColumn<TempRecord, Double> inCol = new TableColumn<>(Messages.get("col.input"));
        inCol.setCellValueFactory(new PropertyValueFactory<>("inputValue"));

        TableColumn<TempRecord, Double> outCol = new TableColumn<>(Messages.get("col.converted"));
        outCol.setCellValueFactory(new PropertyValueFactory<>("convertedValue"));

        TableColumn<TempRecord, Integer> unitCol = new TableColumn<>(Messages.get("col.unit"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unitId"));

        TableColumn<TempRecord, Object> dateCol = new TableColumn<>(Messages.get("col.created"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateCol.setPrefWidth(160);

        table.getColumns().add(idCol);
        table.getColumns().add(inCol);
        table.getColumns().add(outCol);
        table.getColumns().add(unitCol);
        table.getColumns().add(dateCol);

        table.setItems(data);
        table.setPrefHeight(220);
    }

    private void convertAndSave() {
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

        // The INDEX is used instead of the text, because the text is translated.
        int choice = conversionBox.getSelectionModel().getSelectedIndex();
        double result;
        int unitId;

        switch (choice) {
            case 0 -> {                                   // Fahrenheit -> Celsius
                result = converter.fahrenheitToCelsius(value);
                unitId = 1;
            }
            case 1 -> {                                   // Celsius -> Fahrenheit
                result = converter.celsiusToFahrenheit(value);
                unitId = 2;
            }
            case 2 -> {                                   // Kelvin -> Celsius
                result = converter.kelvinToCelsius(value);
                unitId = 1;
            }
            default -> {                                  // Celsius -> Kelvin
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

    private void deleteSelected() {
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

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}