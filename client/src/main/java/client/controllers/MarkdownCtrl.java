package client.controllers;

import client.scenes.DashboardCtrl;
import client.services.ReferenceService;
import com.google.inject.Inject;
import commons.Note;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.Getter;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.net.URL;
import java.util.Arrays;

/**
 * Handles markdown rendering, reference validation, and tooltip interactions in the WebView.
 */
public class MarkdownCtrl {

    // Dashboard reference
    private DashboardCtrl dashboardCtrl;
    private ReferenceService referenceService;

    // Markdown parser and renderer
    private final Parser parser;
    private final HtmlRenderer renderer;

    // UI references
    private ListView<Note> collectionView;
    private WebView markdownView;
    private Label markdownViewBlocker;
    private TextArea noteBody;

    private ContextMenu recommendationsMenu;

    @Getter
    private final String cssPath;
    private final String scriptPath;

    @Inject
    public MarkdownCtrl() {
        this.referenceService = new ReferenceService(dashboardCtrl, noteBody, recommendationsMenu);
        var extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );
        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).build();

        URL cssUrl = getClass().getResource("/css/markdown.css");
        if (cssUrl != null) {
            cssPath = cssUrl.toExternalForm();
        } else {
            throw new RuntimeException("Markdown CSS file not found.");
        }

        URL scriptUrl = getClass().getResource("/script/referenceHandler.js");
        if (scriptUrl != null) {
            scriptPath = scriptUrl.toExternalForm();
        } else {
            throw new RuntimeException("Reference javascript file not found.");
        }
    }

    /**
     * Sets UI component references and initializes markdown rendering.
     */
    public void setReferences(ListView<Note> collectionView, WebView markdownView,
                              Label markdownViewBlocker, TextArea noteBody) {
        this.collectionView = collectionView;
        this.markdownView = markdownView;
        this.markdownViewBlocker = markdownViewBlocker;
        this.noteBody = noteBody;

        this.markdownView.getEngine().setJavaScriptEnabled(true);

        // Initialize the WebView
        updateMarkdownView("");

        // Add listeners for note body changes and scrolling
        noteBody.textProperty().addListener((_, _, newValue) -> {
            updateMarkdownView(newValue);
            PauseTransition pause = new PauseTransition(Duration.millis(100)); // Adjust duration as needed
            pause.setOnFinished(event -> {
                referenceService.handleReferenceRecommendations();
            });
            pause.play();
        });
        noteBody.scrollTopProperty().addListener((_, _, _) -> synchronizeScroll());

        // Add click listener for note links in the WebView
        markdownView.getEngine().setOnAlert(event -> {
            String noteTitle = event.getData();
            dashboardCtrl.getCollectionNotes().stream()
                    .filter(note -> note.title.equals(noteTitle))
                    .findFirst()
                    .ifPresent(selectedNote -> collectionView.getSelectionModel().select(selectedNote));
        });
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
        this.referenceService = new ReferenceService(dashboardCtrl, noteBody, recommendationsMenu);
    }

    /**
     * Updates the markdown view with validated and rendered content.
     */
    private void updateMarkdownView(String markdown) {
        String validatedContent = referenceService.validateAndReplaceReferences(markdown);
        String renderedHtml = convertMarkdownToHtml(validatedContent);

        Platform.runLater(() -> {
            markdownView.getEngine().loadContent(renderedHtml, "text/html");
            markdownViewBlocker.setVisible(markdown == null || markdown.isEmpty());
        });
    }

    /**
     * Converts markdown content to HTML, applying CSS and JavaScript for tooltips and interactivity.
     */
    private String convertMarkdownToHtml(String markdown) {
        String htmlContent = markdown == null || markdown.isEmpty() ? "" : renderer.render(parser.parse(markdown));

        return """
                <!DOCTYPE html>
                <html>
                    <head>
                        <link rel='stylesheet' type='text/css' href='""" + cssPath + "'>" +
                """
                    </head>
                    <body>
                """ + htmlContent  +
                """
                    <div id="tooltip" class="tooltip"></div>
                </body>
                <script src='""" + scriptPath + "'></script>" +
                """
            </html>""";
    }
    /**
     * Synchronizes scrolling between the note body and markdown view.
     */
    private void synchronizeScroll() {
        double percentage = computeScrollPercentage(noteBody);
        Platform.runLater(() -> markdownView.getEngine().executeScript(
                "document.body.scrollTop = document.body.scrollHeight * " + percentage + ";"
        ));
    }

    private double computeScrollPercentage(TextArea noteBody) {
        double scrollTop = noteBody.getScrollTop();
        double contentHeight = noteBody.lookup(".content").getBoundsInLocal().getHeight();
        double viewportHeight = noteBody.getHeight();
        return scrollTop / (contentHeight - viewportHeight);
    }
}
