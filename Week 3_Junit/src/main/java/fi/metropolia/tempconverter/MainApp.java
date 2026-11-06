package fi.metropolia.tempconverter;

import fi.metropolia.tempconverter.i18n.Messages;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MainApp extends Application {

    /** First font on this machine that can draw Japanese and Arabic. */
    private static final String UI_FONT = pickFont();

    private static String pickFont() {
        List<String> installed = Font.getFamilies();
        String[] wanted = {
                "Noto Sans CJK JP", "Noto Sans JP", "Noto Sans Mono CJK JP",
                "IPAGothic", "VL Gothic", "TakaoGothic",
                "Meiryo", "Yu Gothic", "MS Gothic",   // Windows
                "Arial Unicode MS"
        };
        for (String f : wanted) {
            if (installed.contains(f)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public void start(Stage stage) throws IOException {

        askForLanguage();

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/main-view.fxml"),
                Messages.getBundle());

        Parent root = loader.load();

        if (UI_FONT != null) {
            root.setStyle("-fx-font-family: \"" + UI_FONT + "\";");
        }

        Scene scene = new Scene(root, 700, 560);

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
        if (UI_FONT != null) {
            dialog.getDialogPane().setStyle("-fx-font-family: \"" + UI_FONT + "\";");
        }
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