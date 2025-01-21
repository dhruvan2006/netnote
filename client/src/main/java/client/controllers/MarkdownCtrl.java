package client.controllers;

import client.LanguageManager;
import client.scenes.DashboardCtrl;
import client.services.ReferenceService;
import client.services.TagService;
import client.ui.DialogStyler;
import client.utils.Config;
import com.google.inject.Inject;
import commons.Note;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.Getter;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles markdown rendering, reference validation, tag visualization, and tooltip interactions in the WebView.
 */
public class MarkdownCtrl {

    // Dashboard reference
    private DashboardCtrl dashboardCtrl;
    private ReferenceService referenceService;
    private TagService tagService;
    private DialogStyler dialogStyler = new DialogStyler();

    // Markdown parser and renderer
    private final Parser parser;
    private final HtmlRenderer renderer;

    // UI references
    private ListView<Note> collectionView;
    private TreeView<Note> treeView;
    private WebView markdownView;
    private Label markdownViewBlocker;
    private TextArea noteBody;

    private ContextMenu recommendationsMenu;


    private Note currentNote;


    @Getter
    private final String cssPath;
    private final String scriptPath;

    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;

    @Inject
    public MarkdownCtrl(Config config) {
        this.config = config;
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();

        this.referenceService = new ReferenceService(dashboardCtrl, noteBody, recommendationsMenu);
        this.tagService = new TagService();
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
            throw new RuntimeException(bundle.getString("markdownCssNotFound.text"));
        }

        URL scriptUrl = getClass().getResource("/script/referenceHandler.js");
        if (scriptUrl != null) {
            scriptPath = scriptUrl.toExternalForm();
        } else {
            throw new RuntimeException(bundle.getString("referenceJsNotFound.text"));
        }
    }

    public void setCurrentNote(Note currentNote) {
        this.currentNote = currentNote;
    }

    public String getCssPath() {
        return cssPath;
    }

    /**
     * Sets UI component references and initializes markdown rendering.
     */
    public void setReferences(ListView<Note> collectionView, TreeView<Note> treeView, WebView markdownView,
                              Label markdownViewBlocker, TextArea noteBody) {
        this.collectionView = collectionView;
        this.treeView = treeView;
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

        // Pass internationalized strings to JS
        WebEngine webEngine = markdownView.getEngine();
        webEngine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {
                String collectionLabel = bundle.getString("collection.text");
                String noteLabel = bundle.getString("note.text");
                String previewLabel = bundle.getString("preview.text");

                webEngine.executeScript(
                        "window.localizedStrings = {" +
                                "   collectionLabel: '" + escapeJsString(collectionLabel) + "'," +
                                "   noteLabel: '" + escapeJsString(noteLabel) + "'," +
                                "   previewLabel: '" + escapeJsString(previewLabel) + "'" +
                                "};"
                );
            }
        });

        // Handle javascript alerts from the WebView
        markdownView.getEngine().setOnAlert(event -> {
            String url = event.getData();

            if (url.startsWith("tag://")) {
                // Handle tag clicks
                String tag = url.substring("tag://".length());
                dashboardCtrl.selectTag(tag);
            } else if (url.startsWith("note://")) {
                // Handle internal note links
                String noteTitle = url.substring("note://".length());
                dashboardCtrl.getCollectionNotes().stream()
                        .filter(note -> note.title.equals(noteTitle))
                        .findFirst()
                        .ifPresent(selectedNote -> {
                            collectionView.getSelectionModel().select(selectedNote);
                            dashboardCtrl.selectNoteInTreeView(selectedNote);
                        });
            } else {
                // Handle external urls
                openUrlInBrowser(url);
            }
        });
    }

    private String escapeJsString(String input) {
        return input.replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
        this.referenceService = new ReferenceService(dashboardCtrl, noteBody, recommendationsMenu);
    }

    /**
     * Updates the markdown view with validated and rendered content.
     */
    public void updateMarkdownView(String markdown) {
        String validatedContent = tagService.replaceTagsInMarkdown(markdown);
        validatedContent = referenceService.validateAndReplaceReferences(validatedContent);
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
        markdown = convertFileNameToURL(markdown);
        String htmlContent = markdown == null || markdown.isEmpty() ? "" : renderer.render(parser.parse(markdown));

        markdownView.getEngine().setUserStyleSheetLocation(getClass().getResource("/css/markdown.css").toExternalForm());

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

    private String convertFileNameToURL(String markdown) {
        if (currentNote == null) {
            return markdown;
        }
        String regex = "!\\[([^)]+)]\\(([^)]+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(markdown);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while(matcher.find()) {
            result.append(markdown, lastEnd, matcher.start());
            String altText = matcher.group(1);
            String fileName = matcher.group(2);

            URI uri = null;
            try {
                uri = new URI(null, null, fileName, null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            String encodedFileName = uri.toASCIIString();

            String fileURL = currentNote.collection.serverURL + "api/notes/" + currentNote.getId() + "/files/" + encodedFileName;

            result.append(String.format("![%s](%s)", altText, fileURL));

            lastEnd = matcher.end();
        }

        result.append(markdown.substring(lastEnd));
        return result.toString();
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

    private void openUrlInBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                Alert alert = dialogStyler.createStyledAlert(Alert.AlertType.ERROR, bundle.getString("errorOpeningUrl.text"),
                        bundle.getString("failedToOpenUrl.text") + url, bundle.getString("checkUrlFormat.text"));
                alert.showAndWait();
            }
        } else {
            Alert alert = dialogStyler.createStyledAlert(Alert.AlertType.ERROR, "Desktop Not Supported",
                    bundle.getString("unableToOpenUrl.text"), bundle.getString("desktopNotSupported.text"));
            alert.showAndWait();
        }
    }
}
