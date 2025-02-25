package client.scenes;

import client.LanguageManager;
import client.controllers.MarkdownCtrl;
import client.utils.Config;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
//import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class HelpMenuCtrl implements Initializable {

    @FXML private HBox menuBar;
    private double dragStartX, dragStartY;
//    @FXML private WebView webView;
    private Stage stage;

    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;
    private MarkdownCtrl markdownCtrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
    }
    public void setReferences(Stage stage, MarkdownCtrl markdownCtrl) {
        this.stage = stage;
        this.markdownCtrl = markdownCtrl;
        setupDraggableWindow();
        addText();
    }

    private void addText() {
        try {
            String resourcePath = "help/HELP_" +
                switch (languageManager.getCurrentLanguage()) {
                    case ENGLISH -> "EN";
                    case DUTCH -> "NL";
                    case ROMANIAN -> "RO";
                    case POLISH -> "PL";
                }
                + ".md";

            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(resourcePath);

            // Read theckout e README.md file
            String markdownContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Convert Markdown to HTML
            String htmlContent = markdownCtrl.convertMarkdownToHtml(markdownContent);

            // Load the HTML into the WebView
//            webView.getEngine().loadContent(htmlContent);
        } catch (IOException e) {
            e.printStackTrace();
//            webView.getEngine().loadContent("<h1>Error</h1><p>Unable to load the README file.</p>");
        }
    }

    private void setupDraggableWindow() {
        menuBar.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });

        menuBar.setOnMouseDragged(event -> {
            if (stage != null) { // Ensure stage is set
                stage.setX(event.getScreenX() - dragStartX);
                stage.setY(event.getScreenY() - dragStartY);
            }
        });
    }

    @FXML
    private void onExitMenu() {
        if (stage != null) {
            stage.close(); // Close the popup window
        }
    }
}
