package fi.metropolia.tempconverter;

import fi.metropolia.tempconverter.i18n.Messages;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        askForLanguage();

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/main-view.fxml"),
                Messages.getBundle());

        Parent root = loader.load();

        Scene scene = new Scene(root, 650, 520);

        if (Messages.isRightToLeft()) {
            scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        stage.setTitle(Messages.get("app.title"));
        stage.setScene(scene);
        stage.show();
    }

    private void askForLanguage() {
        Map<String, Locale> options = new LinkedHashMap<>();
        options.put("English", Messages.ENGLISH);
        options.put("Suomi (Finnish)", Messages.FINNISH);
        options.put("Svenska (Swedish)", Messages.SWEDISH);
        options.put("日本語 (Japanese)", Messages.JAPANESE);
        options.put("العربية (Arabic)", Messages.ARABIC);

        String first = options.keySet().iterator().next();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(first, options.keySet());
        dialog.setTitle(Messages.get("lang.title"));
        dialog.setHeaderText(Messages.get("lang.header"));
        dialog.setContentText(Messages.get("lang.content"));

        Optional<String> answer = dialog.showAndWait();
        answer.ifPresent(choice -> Messages.setLocale(options.get(choice)));
    }

    public static void main(String[] args) {
        launch(args);
    }
}