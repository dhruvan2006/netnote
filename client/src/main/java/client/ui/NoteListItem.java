package client.ui;

import client.LanguageManager;
import client.controllers.NoteCtrl;
import client.controllers.NotificationsCtrl;
import client.entities.Action;
import client.entities.ActionType;
import client.scenes.DashboardCtrl;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ResourceBundle;

public class NoteListItem extends ListCell<Note> {

    // Utillities
    private final DialogStyler dialogStyler = new DialogStyler();

    // References
    private Label overviewTitle;
    private Label markdownTitle;
    private TextArea overviewBody;
    private DashboardCtrl controller;
    private NoteCtrl noteCtrl;

    // List cell content
    private final Label noteTitle;
    private final Region spacer;
    private final Button deleteButton;
    private final Button editButton;
    private final HBox hBox;
    private TextField textField;


    @Inject
    private Config config;
    private LanguageManager manager;
    private ResourceBundle bundle;
    private final ServerUtils server;
    private final NotificationsCtrl notificationsCtrl;

    // Variables
    private String originalTitle;
    private long lastClickTime = 0;
    private static final int DOUBLE_CLICK_TIMEFRAME = 400;

    public NoteListItem(Label overviewTitle, Label markdownTitle, TextArea overviewBody,
                        DashboardCtrl controller, NoteCtrl noteCtrl, ServerUtils server, NotificationsCtrl notificationsCtrl) {
        this.notificationsCtrl = notificationsCtrl;
        this.overviewTitle = overviewTitle;
        this.markdownTitle = markdownTitle;
        this.overviewBody = overviewBody;
        this.controller = controller;
        this.noteCtrl = noteCtrl;
        this.server = server;

        this.manager = LanguageManager.getInstance(this.config);
        this.bundle = this.manager.getBundle();

        // Initialize the note title
        noteTitle = new Label();
        noteTitle.setTextOverrun(OverrunStyle.ELLIPSIS);
        noteTitle.setWrapText(false);
        noteTitle.setMinWidth(0);

        // Initialize the delete button
        deleteButton = new Button();
        deleteButton.getStyleClass().addAll("icon", "note_list_icon", "delete_icon");
        deleteButton.setCursor(Cursor.HAND);
        Tooltip deleteNoteTooltip = new Tooltip(bundle.getString("deleteNote.text"));
        deleteNoteTooltip.setShowDelay(Duration.seconds(0.2));
        deleteButton.setTooltip(deleteNoteTooltip);

        // Initialize the edit button
        editButton = new Button();
        editButton.getStyleClass().addAll("icon", "note_list_icon", "edit_icon");
        editButton.setCursor(Cursor.HAND);
        Tooltip editNoteTooltip = new Tooltip(bundle.getString("editNote.text"));
        editNoteTooltip.setShowDelay(Duration.seconds(0.2));
        editButton.setTooltip(editNoteTooltip);

        // Create layout
        hBox = new HBox(0);
        spacer = new Region();
        hBox.getChildren().addAll(noteTitle, spacer, editButton, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER_LEFT);

        configureEventHandlers();
    }

    private void configureEventHandlers() {
        deleteButton.setOnAction(event -> {
            if (controller.getCollectionView().getSelectionModel().getSelectedItems().size() > 1) {
                controller.deleteMultipleNotes(controller.getCollectionView().getSelectionModel().getSelectedItems());
            } else {
                Note note = (Note) getItem();
                if (note != null) {
                    controller.deleteSelectedNote();
                }
            }
        });
        editButton.setOnAction(event -> {
            startEditing();
        });

        hBox.setOnMouseClicked(event -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime <= DOUBLE_CLICK_TIMEFRAME) {
                startEditing();
            }
            lastClickTime = currentTime;
        });
    }

    @Override
    protected void updateItem(Note item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            noteTitle.setText(item.getTitle());
            // Adjust layout based on selection
            deleteButton.setVisible(isSelected());
            editButton.setVisible(isSelected());

            if (isSelected()) {
                noteTitle.maxWidthProperty().bind(controller.getCollectionView().widthProperty().subtract(60));
                if (!hBox.getChildren().contains(editButton) || !hBox.getChildren().contains(deleteButton)) {
                    if (!hBox.getChildren().contains(editButton)) {
                        hBox.getChildren().add(editButton);
                    }
                    if (!hBox.getChildren().contains(deleteButton)) {
                        hBox.getChildren().add(deleteButton);
                    }
                }
            } else {
                noteTitle.maxWidthProperty().bind(controller.getCollectionView().widthProperty().subtract(10));
                hBox.getChildren().remove(editButton);
                hBox.getChildren().remove(deleteButton);
            }

            // Set the cell content
            setGraphic(hBox);
        }
    }

    public void startEditing() {
        Note item = getItem();
        if (item != null) {
            if (!server.isServerAvailable(item.collection.serverURL)) {
                String alertText = bundle.getString("noteUpdateError") + "\n" + item.title;
                dialogStyler.createStyledAlert(
                        Alert.AlertType.INFORMATION,
                        bundle.getString("serverCouldNotBeReached.text"),
                        bundle.getString("serverCouldNotBeReached.text"),
                        alertText
                ).showAndWait();
                return;
            }

            originalTitle = item.getTitle();

            textField = new TextField(originalTitle);

            // Set width dynamically to match the container minus 5px
            textField.prefWidthProperty().bind(getListView().widthProperty().subtract(10));


            final boolean isCommited[] = {false};

            // The whole isCommited logic is needed bc otherwise when you click "ENTER" both triggers are set
            // Pretty ugly, but can be improved later
            textField.setOnAction(e -> {
                if (!isCommited[0]) {
                    isCommited[0] = true;
                    commitTitleChange(item, textField.getText());
                }
            });

            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && !isCommited[0]) {
                    isCommited[0] = true;
                    commitTitleChange(item, textField.getText());
                }
            });

            textField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    isCommited[0] = true;
                    revertToLabel();
                }
            });

            hBox.getChildren().clear();
            hBox.getChildren().add(textField);

            textField.requestFocus();
        }
    }

    private void commitTitleChange(Note item, String newTitle) {
        if (item == null) return;
        String oldTitle = item.getTitle();

        // Ensure the title is unique in the current collection
        String uniqueTitle = noteCtrl.generateUniqueTitle(controller.getAllNotes(), item, newTitle, false);
        try {
            if (uniqueTitle.equals(oldTitle)) {
                throw new IllegalArgumentException("Title cannot be the same as the current title");
            }
            noteTitle.setText(uniqueTitle);

            controller.changeTitle(item, item.title, uniqueTitle);

            controller.getActionHistory().push(new Action(ActionType.EDIT_TITLE, item, oldTitle, null ,uniqueTitle));

        } catch (IllegalArgumentException | ClientErrorException e) {
            if (e instanceof IllegalArgumentException) {
                notificationsCtrl.pushNotification(bundle.getString("sameName"), true);
            } else {
                notificationsCtrl.pushNotification(bundle.getString("invalidName"), true);
            }
            item.setTitle(oldTitle);
            noteTitle.setText(oldTitle);
        }

        revertToLabel();
    }


    private void revertToLabel() {
        hBox.getChildren().clear();
        hBox.getChildren().addAll(noteTitle, spacer, editButton, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        overviewTitle.setText(getItem().getTitle());
        markdownTitle.setText(getItem().getTitle());
        overviewBody.setText(getItem().getBody());
    }
}
