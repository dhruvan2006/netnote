package client.scenes;

import client.Language;
import client.LanguageManager;
import client.Main;
import client.controllers.*;
import client.entities.Action;
import client.entities.ActionType;
import client.services.CollectionFilter;
import client.services.NoteFilterProcessor;
import client.services.SearchFilter;
import client.services.TagFilter;
import client.ui.CustomTreeCell;
import client.ui.DialogStyler;
import client.ui.NoteListItem;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
//import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Controls all logic for the main dashboard.
 */
@SuppressWarnings("rawtypes")
public class DashboardCtrl implements Initializable {

    // Utilities
    //TODO: This is just a temporary solution, to be changed with something smarter
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Getter private final ServerUtils server;
    @Getter private final MainCtrl mainCtrl;
    @Getter private final NotificationsCtrl notificationsCtrl;
    @Getter @Setter @Inject private Config config;
    @Getter private LanguageManager languageManager;
    private ResourceBundle bundle;
    @Getter private final DialogStyler dialogStyler;

    // Controllers
    @Inject
    @Getter private final MarkdownCtrl markdownCtrl;
    @Getter private final CollectionCtrl collectionCtrl;
    @Getter private final NoteCtrl noteCtrl;
    @Getter private final SearchCtrl searchCtrl;
    @Getter private final FilesCtrl filesCtrl;
    @Getter private final TagCtrl tagCtrl;

    // FXML Components
    @FXML private VBox root;
    @FXML private Label contentBlocker;
    @FXML @Getter private TextArea noteBody;
//    @FXML private WebView markdownView;
    @FXML private Label markdownViewBlocker;
    @FXML @Getter private Label noteTitle;
    @FXML @Getter public ListView collectionView;
    @FXML @Getter public TreeView allNotesView;
    @FXML @Getter Button addButton;
    @FXML @Getter private Label noteTitleMD;
    @FXML private Button deleteButton;
    @FXML private Button clearSearchButton;
    @FXML @Getter private TextField searchField;
    @FXML private MenuButton currentCollectionTitle;
    @FXML private MenuItem allNotesButton;
    @FXML private ToggleGroup collectionSelect;
    @FXML private Button deleteCollectionButton;
    @FXML private MenuItem editCollectionTitle;
    @FXML private MenuButton moveNotesButton;
    @FXML private Button addFileButton;
    @FXML private HBox filesView;
    @FXML private HBox notificationsBar;
    @FXML private Label notificationsLabel;
    @FXML private Label filesViewBlocker;
    @FXML private HBox tagsBox;
    @FXML private MenuButton languageButton;
    @FXML private ScrollPane fileScrollPane;
    @FXML private Text filesText;
    @FXML private Button accessibilityButton;
    @FXML private Button refreshButton;
    @FXML private Button searchButton;

    // Variables
    @Getter @Setter private Note currentNote = null;
    @Getter @Setter private Collection currentCollection = null;
    @Getter private Collection defaultCollection = null;
    @Getter @Setter private Collection destinationCollection = null;
    @Getter @Setter public ObservableList<Collection> collections;
    @Getter @Setter private ObservableList<Note> allNotes;
    @Getter @Setter public ObservableList<Note> collectionNotes;

    private TreeItem<Object> noNotesItem;
    @Getter @Setter private boolean isProgrammaticChange = false;
    @Getter @Setter private Deque<Action> actionHistory = new ArrayDeque<>();
    private boolean isUndoBodyChange = false; // Flag for undo-triggered body changes
    @Getter private boolean isAccessible = false;
    @Getter private String currentCss;

    @Inject
    public DashboardCtrl(ServerUtils server,
                         MainCtrl mainCtrl,
                         MarkdownCtrl markdownCtrl,
                         NotificationsCtrl notificationsCtrl,
                         CollectionCtrl collectionCtrl,
                         NoteCtrl noteCtrl,
                         SearchCtrl searchCtrl,
                         FilesCtrl filesCtrl,
                         DialogStyler dialogStyler,
                         TagCtrl tagCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
        this.markdownCtrl = markdownCtrl;
        this.notificationsCtrl = notificationsCtrl;
        this.collectionCtrl = collectionCtrl;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
        this.filesCtrl = filesCtrl;
        this.dialogStyler = dialogStyler;
        this.tagCtrl = tagCtrl;
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = languageManager.getBundle();
        noNotesItem = new TreeItem<>(this.bundle.getString("noNotesInCollection.text"));
    }

    @SneakyThrows
    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        if(config.isFileErrorStatus()) onClose();

        languageManager = LanguageManager.getInstance(config);
        setupLanguageButton();
        currentCss = getClass().getResource("/css/color-styles.css").toExternalForm();

        // Tooltips
        Tooltip refreshTooltip = new Tooltip(bundle.getString("refresh.text"));
        refreshTooltip.setShowDelay(Duration.seconds(0.2));
        refreshButton.setTooltip(refreshTooltip);

        Tooltip addNoteTooltip = new Tooltip(bundle.getString("addNote.text"));
        addNoteTooltip.setShowDelay(Duration.seconds(0.2));
        addButton.setTooltip(addNoteTooltip);

        Tooltip clearSearchTooltip = new Tooltip(bundle.getString("clearSearch.text"));
        clearSearchTooltip.setShowDelay(Duration.seconds(0.2));
        clearSearchButton.setTooltip(clearSearchTooltip);

        Tooltip searchTooltip = new Tooltip(bundle.getString("search.text"));
        searchTooltip.setShowDelay(Duration.seconds(0.2));
        searchButton.setTooltip(searchTooltip);

        Tooltip deleteNoteTooltip = new Tooltip(bundle.getString("deleteNote.text"));
        deleteNoteTooltip.setShowDelay(Duration.seconds(0.2));
        deleteButton.setTooltip(deleteNoteTooltip);

        Tooltip languageTooltip = new Tooltip(bundle.getString("selectLanguage.text"));
        languageTooltip.setShowDelay(Duration.seconds(0.2));
        languageButton.setTooltip(languageTooltip);

        Tooltip moveNotesTooltip = new Tooltip(bundle.getString("moveSelectedNotes.text"));
        moveNotesTooltip.setShowDelay(Duration.seconds(0.2));
        moveNotesButton.setTooltip(moveNotesTooltip);

        // ---------


        allNotes = FXCollections.observableArrayList(server.getAllNotes());
        collectionNotes = allNotes;
        markdownCtrl.setReferences(collectionView, allNotesView, markdownViewBlocker, noteBody);
        markdownCtrl.setDashboardCtrl(this);
        searchCtrl.setReferences(this, searchField);
        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    search();
                }
                default -> {
                }
            }
        });

        collectionCtrl.setReferences(collectionView,
                allNotesView,
                currentCollectionTitle,
                allNotesButton,
                editCollectionTitle,
                deleteCollectionButton,
                moveNotesButton
        );
        collectionSelect = collectionCtrl.getCollectionSelect();

        collectionCtrl.setDashboardCtrl(this);

        notificationsCtrl.setReferences(notificationsBar, notificationsLabel, this);

        noteCtrl.setReferences(
                collectionView,
                allNotesView,
                noteTitle,
                noteTitleMD,
                noteBody,
                contentBlocker,
                searchField,
                filesViewBlocker,
                moveNotesButton
        );
        noteCtrl.setDashboardCtrl(this);

        collectionCtrl.setUp();

        collectionView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        BooleanBinding isNoteSelected = collectionView.getSelectionModel().selectedItemProperty().isNull()
                .and(allNotesView.getSelectionModel().selectedItemProperty().isNull()
                        .or(Bindings.createBooleanBinding(() -> {
                            TreeItem<Object> selectedItem = (TreeItem<Object>) allNotesView.getSelectionModel().getSelectedItem();
                            return selectedItem == null || !(selectedItem.getValue() instanceof Note); // Disable if no selection OR not a leaf
                        }, allNotesView.getSelectionModel().selectedItemProperty())));

        moveNotesButton.disableProperty().bind(isNoteSelected);

        deleteButton.disableProperty().bind(isNoteSelected);

        collectionCtrl.moveNotesInitialization();


        listViewSetup(collectionNotes);
        treeViewSetup();


        filesCtrl.setDashboardCtrl(this);
        filesCtrl.setReferences(filesView);

        AnchorPane.setLeftAnchor(fileScrollPane, filesText.getLayoutX() + filesText.getBoundsInParent().getWidth() + 5);
        fileScrollPane.prefWidthProperty().bind(
                addFileButton.layoutXProperty()
                        .subtract(fileScrollPane.layoutXProperty())
                        .subtract(10) // 5px gap from each side
        );

        tagCtrl.setReferences(this, tagsBox, allNotes);

        viewAllNotes();

        // Temporary solution
        scheduler.scheduleAtFixedRate(() -> noteCtrl.saveAllPendingNotes(),
                10,10, TimeUnit.SECONDS);
    }

    private void setupLanguageButton() {
        ImageView currentFlag = createLanguageImageView(languageManager.getCurrentLanguage().getImagePath());
        languageButton.setGraphic(currentFlag);
        languageButton.setText(languageManager.getCurrentLanguage().getDisplayName());


        for (Language lang : Language.values()) {
            MenuItem item = new MenuItem(lang.getDisplayName());

            ImageView flagImage = createLanguageImageView(lang.getImagePath());
            item.setGraphic(flagImage);

            item.setOnAction(e -> switchLanguage(lang));
            languageButton.getItems().add(item);
        }
    }

    private ImageView createLanguageImageView(String imagePath) {
        Image flagImage = new Image(getClass().getResourceAsStream(imagePath));
        ImageView imageView = new ImageView(flagImage);
        imageView.setFitHeight(16);
        imageView.setFitWidth(24);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void switchLanguage(Language language) {
        languageManager.switchLanguage(language);

        ImageView newFlag = createLanguageImageView(language.getImagePath());
        languageButton.setGraphic(newFlag);
        languageButton.setText(language.getDisplayName());

        updateLanguage();
    }

    private void updateLanguage() {
        Stage primaryStage = (Stage) languageButton.getScene().getWindow();
        try {
            // save state
            double x = primaryStage.getX();
            double y = primaryStage.getY();
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();
            boolean isMaximized = primaryStage.isMaximized();
            boolean isFullScreen = primaryStage.isFullScreen();

            noteCtrl.saveAllPendingNotes();

            Collection backupCurrentCollection = currentCollection;
            Note backupCurrentNote = currentNote;
            String backupSearchText = searchField.getText();
            boolean visibilityTreeView = allNotesView.isVisible();
            boolean visibilityCollectionView = collectionView.isVisible();
            List<String> selectedTags = tagCtrl.getSelectedTags();

            // restart the UI
            Main main = new Main();
            main.start(primaryStage);
            DashboardCtrl dashboardCtrl = main.getDashboardCtrl();

            dashboardCtrl.isAccessible = !isAccessible;
            dashboardCtrl.toggleAccessibility();

            // transfer state
            if (isFullScreen) {
                primaryStage.setFullScreen(true);
            } else if (isMaximized) {
                primaryStage.setMaximized(true);
            } else {
                primaryStage.setX(x);
                primaryStage.setY(y);
                primaryStage.setWidth(width);
                primaryStage.setHeight(height);
            }

            dashboardCtrl.setCurrentCollection(backupCurrentCollection);
            dashboardCtrl.setCurrentNote(backupCurrentNote);
            dashboardCtrl.getSearchField().setText(backupSearchText);
            dashboardCtrl.getAllNotesView().setVisible(visibilityTreeView);
            dashboardCtrl.getCollectionView().setVisible(visibilityCollectionView);
            dashboardCtrl.getNoteCtrl().showCurrentNote(backupCurrentNote);
            dashboardCtrl.selectNoteInTreeView(backupCurrentNote);
            dashboardCtrl.getTagCtrl().selectTags(selectedTags);
            Platform.runLater(() -> dashboardCtrl.getCollectionView().getSelectionModel().select(backupCurrentNote));
            dashboardCtrl.notificationsCtrl.pushNotification(dashboardCtrl.bundle.getString("newLanguage"), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void noteAdditionSync(String url) {
        server.registerForMessages("/topic/notes", Note.class, note -> {
            Platform.runLater(() -> {
                noteCtrl.updateViewAfterAdd(currentCollection, allNotes, collectionNotes, note);
            });
        }, url);
    }

    public void noteTitleSync(String url) {
        String topic = "/topic/notes/title";
        server.registerForTopic(url, topic, Note.class, "noteTitle", note -> {
            Platform.runLater(() -> {
                Note toUpdate = allNotes.stream()
                        .filter(n -> n.id.equals(note.id))
                        .findFirst()
                        .orElse(null);

                if (toUpdate != null) {
                    toUpdate.setTitle(note.getTitle());
                    if (toUpdate.equals(currentNote)) {
                        noteTitle.setText(note.getTitle());
                    }
                    collectionView.refresh();
                    refreshTreeView();
                }
            });
        });
    }

    public void noteDeletionSync(String url) {
        server.registerForMessages("/topic/notes/delete", Note.class, note -> {
            Platform.runLater(() -> {
                noteCtrl.updateAfterDelete(note, allNotes, collectionNotes);
            });
        }, url);
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup(ObservableList notes) {

        // Set required settings
        collectionView.setItems(notes);
        collectionView.setEditable(true);
        collectionView.setFixedCellSize(35);
        collectionView.setCellFactory(TextFieldListCell.forListView());

        // Set ListView entry as Title (editable)
        collectionView.setCellFactory(lv -> new NoteListItem(noteTitle, noteTitleMD, noteBody, this, noteCtrl, server,notificationsCtrl));

        collectionView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (!isProgrammaticChange) actionHistory.clear();

                currentNote = (Note) newValue;
                noteCtrl.showCurrentNote(currentNote);

                markdownViewBlocker.setVisible(false);

                String serverURL = currentNote.collection.serverURL;

                // Unregister previous subscriptions (if any)
                server.unregisterNoteSubscriptions(serverURL);

                // FILE WEBSOCKETS
                server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/files", UUID.class, "embeddedFiles", embeddedFileId -> {
                    Platform.runLater(() -> filesCtrl.updateViewAfterAdd(currentNote, embeddedFileId));
                });

                server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/files/deleteFile", UUID.class, "embeddedFilesDelete", embeddedFileId -> {
                    Platform.runLater(() -> filesCtrl.updateViewAfterDelete(currentNote, embeddedFileId));
                });

                server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/files/renameFile", Object[].class, "embeddedFilesRename", newFileName -> {
                    Platform.runLater(() -> filesCtrl.updateViewAfterRename(currentNote, newFileName));
                });

                // NOTE CONTENT WEBSOCKETS
                server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/body", Note.class, "noteBody", newContent -> {
                    Platform.runLater(() -> {
                        onNoteUpdate(newContent);
                        refreshTreeView();
                    });
                });

            } else {
                showBlockers();
            }
        });

        // Clear the selection after setup
        collectionView.getSelectionModel().clearSelection();
    }

    /**
     * Method used to set up the treeView
     * This method should only be used once in the initialization.
     */
    private void treeViewSetup() {
        // Create a virtual root item (you can use this if you don't want the root to be visible)
        TreeItem<Object> virtualRoot = new TreeItem<>(null);
        virtualRoot.setExpanded(true); // Optional: if you want the root to be expanded by default

        syncTreeView(virtualRoot, collections, allNotes, false);

        // Add listener to the ObservableList to dynamically update the TreeView
        allNotes.addListener((ListChangeListener<Note>) change -> {
            while (change.next()) {
                syncTreeView(virtualRoot, collections, allNotes, false);
            }
        });

        collections.addListener((ListChangeListener<Collection>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Collection collection : change.getAddedSubList()) {
                        String url = collection.serverURL;

                        // Establish WebSocket connection and register for updates
                        server.getWebSocketURL(url);
                        noteAdditionSync(url);

                        server.registerForTopic(url, "/topic/notes/title", Note.class, "noteTitle", note -> {
                            Platform.runLater(() -> noteTitleSync(url));
                        });

                        server.registerForTopic(url, "/topic/notes/delete", Note.class, "noteDelete", note -> {
                            Platform.runLater(() -> noteDeletionSync(url));
                        });
                    }
                } else if (change.wasRemoved()) {
                    for (Collection collection : change.getRemoved()) {
                        String url = collection.serverURL;

                        if (collections.stream().noneMatch(c -> c.serverURL.equals(url))) {
                            // Disconnect and unregister all subscriptions
                            server.disconnect(url);
                        }
                    }
                }
                syncTreeView(virtualRoot, collections, allNotes, false);
            }
        });

        // Add selection change listener
        allNotesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (currentCollection == null) {
                if (newValue != null && ((TreeItem<Object>) newValue).getValue() instanceof Note note) {
                    allNotesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    if (!isProgrammaticChange) actionHistory.clear();

                    currentNote = note;
                    noteCtrl.showCurrentNote(currentNote);

                    markdownViewBlocker.setVisible(false);
                    allNotesView.getFocusModel().focus(0);

                    String serverURL = note.collection.serverURL;

                    // Unregister any previous subscriptions
                    server.unregisterNoteSubscriptions(serverURL);

                    // Register for updates
                    server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/files", UUID.class, "embeddedFiles", embeddedFileId -> {
                        Platform.runLater(() -> filesCtrl.updateViewAfterAdd(currentNote, embeddedFileId));
                    });

                    server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/files/deleteFile", UUID.class, "embeddedFilesDelete", embeddedFileId -> {
                        Platform.runLater(() -> filesCtrl.updateViewAfterDelete(currentNote, embeddedFileId));
                    });

                    server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/files/renameFile", Object[].class, "embeddedFilesRename", newFileName -> {
                        Platform.runLater(() -> filesCtrl.updateViewAfterRename(currentNote, newFileName));
                    });

                    server.registerForTopic(serverURL, "/topic/notes/" + currentNote.getId() + "/body", Note.class, "noteBody", newContent -> {
                        Platform.runLater(() -> {
                            onNoteUpdate(newContent);
                            refreshTreeView();
                        });
                    });
                } else {
                    showBlockers();
                }
            }
        });

        // Set the virtual root as the root of the TreeView
        allNotesView.setRoot(virtualRoot);
        allNotesView.setShowRoot(false); // To hide the root item if it's just a container

        // Set custom TreeCell factory for NoteTreeItem
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl, dialogStyler, notificationsCtrl, server));
    }

    public void onNoteUpdate(Note newContent) {
        if (currentNote != null && currentNote.id.equals(newContent.id)) {
            if (!currentNote.getBody().equals(newContent.getBody())) {
                notificationsCtrl.pushNotification(bundle.getString("newContent"), false);
            }
            int caretPosition = noteBody.getCaretPosition();
            noteBody.setText(newContent.getBody());
            noteBody.positionCaret(caretPosition);
            currentNote.setBody(newContent.getBody());
        }
    }

    public void showBlockers() {
        currentNote = null;
        allNotesView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // Show content blockers when no item is selected
        contentBlocker.setVisible(true);
        markdownViewBlocker.setVisible(true);
        moveNotesButton.setText(bundle.getString("moveNote.text"));
        filesViewBlocker.setVisible(true);

//        server.unregisterFromEmbeddedFileUpdates();
//        server.unregisterFromNoteBodyUpdates();
    }

    /**
     * This method refreshes the treeView
     * Any new, changed, or deleted items will be reflected in the tree view after this methood
     */
    public void refreshTreeView(List<Collection> filteredCollections, List<Note> filteredNotes, boolean expanded) {
        syncTreeView(allNotesView.getRoot(), filteredCollections, filteredNotes, expanded);
    }
    public void refreshTreeView() {
        refreshTreeView(collections, allNotes, false);
    }

    private void syncTreeView(TreeItem<Object> virtualRoot, List<Collection> filteredCollections, List<Note> filteredNotes, boolean expanded) {
        // Map existing tree items for quick lookup
        Map<Collection, TreeItem<Object>> existingCollections = new HashMap<>();
        Map<Note, TreeItem<Object>> existingNotes = new HashMap<>();

        // Build the mapping of current tree structure
        for (TreeItem<Object> collectionItem : virtualRoot.getChildren()) {
            if (collectionItem.getValue() instanceof Collection collection) {
                existingCollections.put(collection, collectionItem);
                for (TreeItem<Object> noteItem : collectionItem.getChildren()) {
                    if (noteItem.getValue() instanceof Note note) {
                        existingNotes.put(note, noteItem);
                    }
                }
            }
        }

        // Add or update collections
        for (Collection collection : filteredCollections) {
            TreeItem<Object> collectionItem = existingCollections.get(collection);
            if (collectionItem == null) {
                // Collection doesn't exist, add it
                collectionItem = new TreeItem<>(collection);
                collectionItem.setExpanded(expanded);
                virtualRoot.getChildren().add(collectionItem);
            }
            existingCollections.remove(collection); // Mark as handled

            // Add or update notes within the collection
            Map<Note, TreeItem<Object>> collectionNotes = new HashMap<>();
            for (TreeItem<Object> noteItem : collectionItem.getChildren()) {
                if (noteItem.getValue() instanceof Note note) {
                    collectionNotes.put(note, noteItem);
                }
            }

            boolean hasNotes = false;
            for (Note note : filteredNotes) {
                if (note.collection.equals(collection)) {
                    TreeItem<Object> noteItem = collectionNotes.get(note);
                    if (noteItem == null) {
                        // Note doesn't exist, add it
                        collectionItem.getChildren().add(new TreeItem<>(note));
                    } else {
                        collectionNotes.remove(note); // Mark as handled
                    }
                    hasNotes = true;
                }
            }

            // Remove notes that are no longer present
            for (TreeItem<Object> unusedNoteItem : collectionNotes.values()) {
                collectionItem.getChildren().remove(unusedNoteItem);
            }

            // Manage "no notes" item
            if (hasNotes) {
                collectionItem.getChildren().remove(noNotesItem);
            } else if (!collectionItem.getChildren().contains(noNotesItem)) {
                collectionItem.getChildren().add(noNotesItem);
            }
        }

        // Remove collections that are no longer present
        for (TreeItem<Object> unusedCollectionItem : new ArrayList<>(virtualRoot.getChildren())) {
            if (unusedCollectionItem.getValue() instanceof Collection collection &&
                    !filteredCollections.contains(collection)) {
                virtualRoot.getChildren().remove(unusedCollectionItem);
            }
        }


        // Reapply the custom cell factory
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl, dialogStyler, notificationsCtrl, server));
    }



    /**
     * This method is used to pars through and select a note from the tree view
     * @param targetNote the targeted note
     */
    public void selectNoteInTreeView(Note targetNote) {
        // Call the helper method to find and select the item
        TreeItem<Object> itemToSelect = findItem(targetNote);
        if (itemToSelect != null) {
            // Select the TreeItem
            allNotesView.getSelectionModel().select(itemToSelect);
            allNotesView.scrollTo(allNotesView.getSelectionModel().getSelectedIndex());
        }
    }

    /**
     * This method takes in the current TreeItem that is selected in the treeView
     * And a target object that we want to find and it returns the targets respective TreeItem
     * @param currentItem currently selected TreeItem
     * @param targetObject object to be found
     * @return the TreeItem that represents the targetObject
     */
    private TreeItem<Object> findItem(TreeItem<Object> currentItem, Object targetObject) {
        // Check if the current item matches the target note
        if (currentItem.getValue() != null) {
            if (currentItem.getValue() instanceof Note currentNote &&
                    targetObject instanceof Note targetNote &&
                    currentNote.title.equals(targetNote.title)) {
                return currentItem;
            }

            if (currentItem.getValue() instanceof Collection currentCollection &&
                    targetObject instanceof Collection targetCollection &&
                    currentCollection.title.equals(targetCollection.title)) {
                return currentItem;
            }
        }

        // Recursively check children of the current item
        for (TreeItem<Object> child : currentItem.getChildren()) {
            TreeItem<Object> result = findItem(child, targetObject);
            if (result != null) {
                return result; // Found in child subtree
            }
        }

        return null; // If no match is found
    }

    private TreeItem<Object> findItem(Object targetObject) {
        return findItem(allNotesView.getRoot(), targetObject);
    }

    public void addNote() {
        Collection targetCollection = (currentCollection == null) ? defaultCollection : currentCollection;

        // Check if the server is available for the target collection
        if (server.isServerAvailable(targetCollection.serverURL)) {
            // Synchronize notes if the collection was previously unavailable
            if (ServerUtils.getUnavailableCollections().contains(targetCollection)) {
                allNotes.removeIf(note -> note.collection.equals(targetCollection));
                allNotes.addAll(server.getNotesByCollection(targetCollection));
            }

            // Ensure WebSocket connection is established for the target collection
            server.getWebSocketURL(targetCollection.serverURL);
            noteAdditionSync(targetCollection.serverURL);
            noteTitleSync(targetCollection.serverURL);
            noteDeletionSync(targetCollection.serverURL);

            ServerUtils.getUnavailableCollections().remove(targetCollection);
        } else {
            // Mark the collection as unavailable and show an error dialog
            if (!ServerUtils.getUnavailableCollections().contains(targetCollection)) {
                ServerUtils.getUnavailableCollections().add(targetCollection);
            }

            dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("serverError.text"),
                    bundle.getString("serverError.text"),
                    bundle.getString("addNoteError")
            ).showAndWait();
            return;
        }

        // Finalize adding the note
        setSearchIsActive(false);
        clearTags(null);
        currentNote = noteCtrl.addNote(currentCollection, allNotes, collectionNotes);
        noteCtrl.showCurrentNote(currentNote);
    }


    @FXML
    public void addCollection(){
        setSearchIsActive(false);
        clearTags(null);
        collectionCtrl.addCollection();
        updateTagList();
    }

    // This overloaded method is used when you already have the collection from the editCollections stage
    public void addCollection(Collection collection) {
        currentCollection = collectionCtrl.addInputtedCollection(collection, currentCollection, collections);
        collectionNotes = collectionCtrl.viewNotes();
        updateTagList();
    }


    public void openEditCollections() {
        collectionCtrl.editCollections();
    }

    public void viewAllNotes() {
        currentCollection = null;
        refreshTreeView();
        collectionNotes = collectionCtrl.viewNotes();
        updateTagList();

        if (defaultCollection != null && server.isServerAvailable(defaultCollection.serverURL)) {
            server.getWebSocketURL(defaultCollection.serverURL);
            noteAdditionSync(defaultCollection.serverURL);
            noteTitleSync(defaultCollection.serverURL);
            noteDeletionSync(defaultCollection.serverURL);
        }
    }

    @FXML
    public void onBodyChanged() {
        if (isUndoBodyChange) {
            isUndoBodyChange = false; // Reset the flag
            return; // Skip recording this change
        }

        String previousBody = currentNote != null? currentNote.getBody() : ""; // Get the current body before change
        if (currentNote == null) {
            return;
        }
        noteCtrl.onBodyChanged(currentNote);
        updateTagList();
        String newBody = currentNote != null? currentNote.getBody() : ""; // Get the new body after change
        newBody = currentNote.getBody();


        if (!previousBody.equals(newBody) && currentNote != null) {
            // Compute the diff between the previous body and the new body
            int startIndex = 0;
            int endIndexPrev = previousBody.length();
            int endIndexNew = newBody.length();

            // Find the first differing character from the start
            while (startIndex < endIndexPrev && startIndex < endIndexNew &&
                    previousBody.charAt(startIndex) == newBody.charAt(startIndex)) {
                startIndex++;
            }

            // Find the first differing character from the end
            while (endIndexPrev > startIndex && endIndexNew > startIndex &&
                    previousBody.charAt(endIndexPrev - 1) == newBody.charAt(endIndexNew - 1)) {
                endIndexPrev--;
                endIndexNew--;
            }

            String removedSegment = previousBody.substring(startIndex, endIndexPrev);
            String addedSegment = newBody.substring(startIndex, endIndexNew);

            boolean isWithinWord = removedSegment.matches("\\w*") && addedSegment.matches("\\w*");

            if (!newBody.isBlank() && (!actionHistory.isEmpty() && ActionType.EDIT_BODY.equals(actionHistory.peek().getType()) &&
                    actionHistory.peek().getNote().equals(currentNote))) {

                if (isWithinWord) {
                    // Merge changes into the last action if they are within a word
                    Action lastAction = actionHistory.pop();
                    actionHistory.push(new Action(ActionType.EDIT_BODY, currentNote, lastAction.getPreviousState(), null, newBody));
                } else {
                    // Create a new action for changes across words or lines
                    actionHistory.push(new Action(ActionType.EDIT_BODY, currentNote, previousBody, null, newBody));
                }
            } else {
                // Create a new action for the first change
                actionHistory.push(new Action(ActionType.EDIT_BODY, currentNote, previousBody, null, newBody));
            }
        }
    }



    public void deleteSelectedNote() {
        if(!server.isServerAvailable(currentNote.collection.serverURL)) {
            String alertText = bundle.getString("noteUpdateError") + "\n" + currentNote.title;
            dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("serverCouldNotBeReached.text"),
                    bundle.getString("serverCouldNotBeReached.text"),
                    alertText
            ).showAndWait();
        }

        if (currentCollection == null) {
            if (allNotesView.getSelectionModel().getSelectedItems().size() == 1) {
                noteCtrl.deleteSelectedNote(currentNote, collectionNotes, allNotes);
            } else {
                noteCtrl.deleteMultipleNotesInTreeView(allNotes,
                        allNotesView.getSelectionModel().getSelectedItems(),
                        collectionNotes);
            }
        } else {
            if(collectionView.getSelectionModel().getSelectedItems().size() == 1) {
                noteCtrl.deleteSelectedNote(currentNote, collectionNotes, allNotes);
            } else {
                noteCtrl.deleteMultipleNotes(allNotes, collectionView.getSelectionModel().getSelectedItems(), collectionNotes);
            }
        }
        updateTagList();
    }

    public void search() {
        filter();
    }

    public void setSearchIsActive(boolean b) {
        searchCtrl.setSearchIsActive(b);
    }

    public void clearSearch() {
        searchCtrl.setSearchIsActive(false);
        updateTagList();
    }

    public void refresh() {
        noteCtrl.saveAllPendingNotes();
        ObservableList<Note> allNotesRefreshed = FXCollections.observableArrayList(server.getAllNotes());
        allNotes.clear();
        allNotes.addAll(allNotesRefreshed);

        clearSearch();
        viewAllNotes();
    }

    @FXML
    public void deleteCollection() {
        if (collectionCtrl.showDeleteConfirmation()) {
            Collection collectionToDelete = currentCollection;
            viewAllNotes();
            collectionCtrl.removeCollectionFromClient(true, collectionToDelete, collections, collectionNotes, allNotes);
        }
        filter();
        updateTagList();
        actionHistory.clear();

    }

    public void connectToCollection(Collection collection) {
        collections.add(collection);
        List<Note> newCollectionNotes = server.getNotesByCollection(collection);
        for (Note note : newCollectionNotes) {
            allNotes.add(note);
        }

        if (currentCollection != null) {
            collectionNotes = FXCollections.observableArrayList(newCollectionNotes);
            currentCollection = collection;
        }

        config.writeToFile(collection);
        if (defaultCollection == null) {
            setDefaultCollection(collection);
        }

        // add entry in collections menu
        //right now in createCollectionButton they are not added to any menu
        RadioMenuItem radioMenuItem = createCollectionButton(collection, currentCollectionTitle, collectionSelect);
        collectionSelect.selectToggle(radioMenuItem);

        collectionCtrl.viewNotes();
    }

    public void addFile() {
        // Make sure notes are saved on the server
        noteCtrl.saveAllPendingNotes();
        EmbeddedFile newFile = filesCtrl.addFile(currentNote);
        if (newFile != null) {
            filesCtrl.showFiles(currentNote);
            // Save the file addition action to the history
            actionHistory.push(new Action(ActionType.ADD_FILE, currentNote, null, null, null, newFile));
        }
    }

    public void setDefaultCollection(Collection collection) {
        defaultCollection = collection;
        config.setDefaultCollection(collection);
    }

    // Temporary solution
    @FXML
    public void onClose() {
        noteCtrl.saveAllPendingNotes();
        // Ensure the scheduler is shut down when the application closes
        scheduler.shutdown();
    }

    public RadioMenuItem createCollectionButton(Collection c, MenuButton currentCollectionTitle, ToggleGroup collectionSelect) {
        RadioMenuItem radioMenuItem = new RadioMenuItem(c.title);
        radioMenuItem.setOnAction(event -> {
            currentCollection = c;
            collectionNotes = collectionCtrl.viewNotes();
        });
        radioMenuItem.setToggleGroup(collectionSelect);
        //here they are not added
        //currentCollectionTitle.getItems().addFirst(radioMenuItem);
        return radioMenuItem;
    }

    // ----------------------- HCI - Keyboard shortcuts -----------------------

    /**
     * ALT + RIGHT ARROW - cycles through collections
     */
    public void selectNextCollection() {
        if (currentCollection == null) {
            currentCollection = collections.getFirst();
        } else {
            if (collections.indexOf(currentCollection) == collections.size() - 1) {
                currentCollection = null;
            } else {
                currentCollection = collections.get(collections.indexOf(currentCollection) + 1);
            }
        }
        collectionNotes = collectionCtrl.viewNotes();
        updateTagList();
    }

    /**
     * ALT + LEFT ARROW - cycles through collections
     */
    public void selectPreviousCollection() {
        if (currentCollection == null) {
            currentCollection = collections.getLast();
        } else {
            if (collections.indexOf(currentCollection) == 0) {
                currentCollection = null;
            } else {
                currentCollection = collections.get(collections.indexOf(currentCollection) - 1);
            }
        }
        collectionNotes = collectionCtrl.viewNotes();
    }

    /**
     * ALT + DOWN ARROW - cycles through notes
     */
    public void selectNextNote() {
        selectNoteInDirection(1); // Direction 1 for "next"
    }

    /**
     * ALT + UP ARROW - cycles through notes
     */
    public void selectPreviousNote() {
        selectNoteInDirection(-1); // Direction -1 for "previous"
    }

    /**
     * ALT/CTRL + E - starts editing the current note name
     */
    public void editCurrentNoteName() {
        if (currentCollection == null) {
            // Handle TreeView selection
            TreeItem<Object> selectedItem = (TreeItem<Object>) allNotesView.getSelectionModel().getSelectedItem();

            if (selectedItem != null && selectedItem.getValue() instanceof Note) {
                Note selectedNote = (Note) selectedItem.getValue();
                if (!server.isServerAvailable(selectedNote.collection.serverURL)) {
                    String alertText = bundle.getString("noteUpdateError") + "\n" + currentNote.title;
                    dialogStyler.createStyledAlert(
                            Alert.AlertType.INFORMATION,
                            bundle.getString("serverCouldNotBeReached.text"),
                            bundle.getString("serverCouldNotBeReached.text"),
                            alertText
                    ).showAndWait();
                    return;
                }
                CustomTreeCell customTreeCell = (CustomTreeCell) allNotesView.lookup(".tree-cell:selected");
                if (customTreeCell != null) {
                    customTreeCell.startEditing();
                }
            }
        } else {
            // Handle ListView selection
            Note selectedNote = (Note) collectionView.getSelectionModel().getSelectedItem();
            if (selectedNote != null) {
                if (!server.isServerAvailable(selectedNote.collection.serverURL)) {
                    String alertText = bundle.getString("noteUpdateError") + "\n" + currentNote.title;
                    dialogStyler.createStyledAlert(
                            Alert.AlertType.INFORMATION,
                            bundle.getString("serverCouldNotBeReached.text"),
                            bundle.getString("serverCouldNotBeReached.text"),
                            alertText
                    ).showAndWait();
                    return;
                }
                NoteListItem noteListItem = (NoteListItem) collectionView.lookup(".list-cell:selected");
                if (noteListItem != null) {
                    noteListItem.startEditing();
                }
            }
        }
    }

    /**
     * CTRL + Z - Undoes the last action done to a note
     */
    public void undoLastAction(KeyEvent event) {
        if(actionHistory.isEmpty()){
            return;
        }

        Action lastAction = actionHistory.pop();

        switch (lastAction.getType()) {
            case ActionType.EDIT_BODY -> {
                isUndoBodyChange = true; // Set the flag
                currentNote.setBody((String) lastAction.getPreviousState());
                noteBody.setText(currentNote.getBody());
                // Add any edited but already existing note to the pending list
                if (!noteCtrl.getUpdatePendingNotes().contains(currentNote)) {
                    noteCtrl.getUpdatePendingNotes().add(currentNote);
                }
                noteBody.positionCaret(currentNote.getBody().length());
            }
            case ActionType.EDIT_TITLE -> {
                String oldTitle = (String) lastAction.getPreviousState();
                changeTitle(currentNote, currentNote.title, oldTitle);

                refreshTreeView();
                collectionView.setCellFactory(lv-> new NoteListItem(noteTitle, noteTitleMD, noteBody, this, noteCtrl, server,notificationsCtrl));
            }
            case ActionType.ADD_FILE -> {
                EmbeddedFile addedFile = lastAction.getEmbeddedFile().get();
                filesCtrl.deleteFile(currentNote, addedFile);
            }
            case ActionType.DELETE_FILE -> {
                EmbeddedFile embeddedFile = lastAction.getEmbeddedFile().get();
                filesCtrl.addDeletedFile(currentNote, embeddedFile);
            }
            case ActionType.EDIT_FILE_NAME -> {
                EmbeddedFile changedFile = lastAction.getEmbeddedFile().get();
                String previousName = (String) lastAction.getPreviousState();
                String newName = (String) lastAction.getNewState();
                filesCtrl.renameFileByName(newName,previousName, currentNote);
            }
            case ActionType.MOVE_NOTE -> {
                isProgrammaticChange = true;
                Note note = currentNote;
                String noteBody = currentNote.getBody();
                Note movedNote = collectionCtrl.moveNoteFromCollection(currentNote, (Collection) lastAction.getPreviousState());
                movedNote.body = noteBody;
                Platform.runLater(() -> {
                    if (currentCollection != null) {
                        collectionView.getSelectionModel().clearSelection();
                        collectionView.getSelectionModel().select(
                                collectionView.getItems().stream()
                                        .filter(obj -> obj instanceof Note n && n.title.equals(note.title))
                                        .findFirst().orElse(null)
                        );
                    } else {
                        allNotesView.getSelectionModel().clearSelection();
                        selectNoteInTreeView(note);
                    }
                    isProgrammaticChange = false;
                });
            }
            case ActionType.MOVE_MULTIPLE_NOTES -> {
                collectionCtrl.moveMultipleNotes((Collection)lastAction.getPreviousState());
                if(currentCollection == null){
                    allNotesView.scrollTo(allNotesView.getSelectionModel().getSelectedIndex());
                } else {
                    collectionView.scrollTo(collectionView.getSelectionModel().getSelectedIndex());
                }
            }

            case ActionType.MOVE_MULTIPLE_NOTES_TREE -> {
                collectionCtrl.moveMultipleNotesInTreeView(
                        null,
                        true,
                        (ObservableList<TreeItem<Note>>) lastAction.getPreviousState());

            }
            default -> throw new UnsupportedOperationException(bundle.getString("undoUnsupported.text") + lastAction.getType());
        }
        event.consume();
    }



    // ----------------------- HCI - Helper methods -----------------------

    private void selectNoteInDirection(int direction) {
        if (currentCollection == null) {
            SelectionMode currentMode = allNotesView.getSelectionModel().getSelectionMode();

            TreeItem<Object> root = allNotesView.getRoot();
            if (root == null || root.getChildren().isEmpty()) {
                return; // No items in the TreeView, do nothing
            }

            TreeItem<Object> selectedItem = (TreeItem<Object>) allNotesView.getSelectionModel().getSelectedItem();
            TreeItem<Object> nextItem;
            //we cannot select both collections and notes, fixes bugs
            if(selectedItem == null || selectedItem.getValue() instanceof Collection) {
                allNotesView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            }
            if (selectedItem == null) {
                // No item selected, select the first valid item
                nextItem = findFirstValidItem(root);
            } else {
                // Find the next or previous valid item
                nextItem = findValidItemInDirection(root, selectedItem, direction);
            }

            if (nextItem != null) {
                if(allNotesView.getSelectionModel().getSelectedItems().contains(nextItem)) {
                    allNotesView.getSelectionModel().clearSelection(allNotesView.getSelectionModel().getSelectedIndex());
                }

                    allNotesView.getSelectionModel().select(nextItem);
                // If the selected item is of type Note, set it as the current note and show it
                if (nextItem.getValue() instanceof Note note) {
                    currentNote = note;
                    noteCtrl.showCurrentNote(currentNote);
                }
            }
        } else {
            collectionNotes = FXCollections.observableArrayList(getFilteredNotes());
            // Collection-specific behavior
            if (currentNote == null) {
                currentNote = (direction > 0) ? collectionNotes.getFirst() : collectionNotes.getLast();
            } else {
                int currentIndex = collectionNotes.indexOf(currentNote);

                int nextIndex = (currentIndex + direction + collectionNotes.size()) % collectionNotes.size();
                if(collectionView.getSelectionModel().isSelected(nextIndex)){
                    collectionView.getSelectionModel().clearSelection(currentIndex);
                }
                currentNote = collectionNotes.get(nextIndex);
            }
            collectionView.getSelectionModel().select(currentNote);
            noteCtrl.showCurrentNote(currentNote);
        }
    }

    private TreeItem<Object> findFirstValidItem(TreeItem<Object> root) {
        for (TreeItem<Object> child : flattenTree(root)) {
            if (child.getValue() instanceof Note) {
                return child;
            }
        }
        return null; // No valid items found
    }

    private TreeItem<Object> findValidItemInDirection(TreeItem<Object> root, TreeItem<Object> currentItem, int direction) {
        List<TreeItem<Object>> flatList = flattenTree(root);

        // Find the index of the currently selected item
        int currentIndex = flatList.indexOf(currentItem);

        // Traverse the list in the specified direction
        for (int i = 1; i <= flatList.size(); i++) {
            int nextIndex = (currentIndex + (i * direction) + flatList.size()) % flatList.size();
            TreeItem<Object> nextItem = flatList.get(nextIndex);
            if (nextItem.getValue() instanceof Note && nextItem != currentItem) {
                return nextItem;
            }
        }

        return null; // No valid items found
    }

    private List<TreeItem<Object>> flattenTree(TreeItem<Object> root) {
        List<TreeItem<Object>> items = new ArrayList<>();
        flattenTreeRecursive(root, items);
        return items;
    }

    private void flattenTreeRecursive(TreeItem<Object> node, List<TreeItem<Object>> items) {
        if (node.getValue() != null) {
            items.add(node);
        }
        for (TreeItem<Object> child : node.getChildren()) {
            flattenTreeRecursive(child, items);
        }
    }

    /**
     * A method used for deleting multiple notes in a collection view
     *
     * @param selectedItems selected notes
     */
    public void deleteMultipleNotes(ObservableList<Note> selectedItems) {
        noteCtrl.deleteMultipleNotes(allNotes, selectedItems, collectionNotes);
    }



    // ----------------------- Tag - Methods -----------------------
    public void updateTagList() {
        tagCtrl.updateTagList();
    }

    public void clearTags(ActionEvent actionEvent) {
        tagCtrl.clearTags();
    }

    public void selectTag(String tag) {
        tagCtrl.selectTag(tag);
    }

    // ----------------------- Filtering - Collection & Search & Tag -----------------------
    /**
     * Updates the view with filtered notes using all filters
     */
    public List<Note> getFilteredNotes() {
        NoteFilterProcessor filterProcessor = new NoteFilterProcessor();
        filterProcessor.addFilter(new CollectionFilter(currentCollection));
        filterProcessor.addFilter(new TagFilter(tagCtrl.getSelectedTags(), tagCtrl.getTagService()));
        filterProcessor.addFilter(new SearchFilter(searchField.getText().trim().toLowerCase()));

        return filterProcessor.applyFilters(allNotes);
    }

    /**
     * Returns notes filtered by all criteria
     */
    public List<Note> getFilteredNotesWithCustomTags(List<String> tags) {
        NoteFilterProcessor filterProcessor = new NoteFilterProcessor();
        filterProcessor.addFilter(new CollectionFilter(currentCollection));
        filterProcessor.addFilter(new TagFilter(tags, tagCtrl.getTagService()));

        return filterProcessor.applyFilters(allNotes);
    }

    /**
     * Returns notes filtered by collection only
     */
    public List<Note> getFilteredNotesByCollection() {
        NoteFilterProcessor filterProcessor = new NoteFilterProcessor();
        filterProcessor.addFilter(new CollectionFilter(currentCollection));
        return filterProcessor.applyFilters(allNotes);
    }

    public ObservableList<Note> filter() {
        showBlockers();
        filterInTreeView();
        return filterInCollectionView();
    }

    private ObservableList<Note> filterInCollectionView() {
        ObservableList<Note> collectionNotes;

        if (currentCollection == null) {
            collectionNotes = allNotes;
            currentCollectionTitle.setText(bundle.getString("allNotes.text"));
            collectionView.setVisible(false);
            allNotesView.setVisible(true);
            collectionView.getSelectionModel().clearSelection();
        } else {
            collectionNotes = FXCollections.observableList(getFilteredNotes());
            currentCollectionTitle.setText(currentCollection.title);
            collectionView.setVisible(true);
            allNotesView.setVisible(false);
            allNotesView.getSelectionModel().clearSelection();
            collectionView.setItems(collectionNotes);
        }

        //collectionView.setItems(collectionNotes);
        Platform.runLater(() -> {
            collectionView.refresh();
        });
        //collectionView.getSelectionModel().clearSelection();

        deleteCollectionButton.setDisable(currentCollection == null);

        //collectionView.getSelectionModel().clearSelection();
        return collectionNotes!=null? collectionNotes : FXCollections.observableArrayList();
    }

    private void filterInTreeView() {
        if (searchField.getText().trim().isEmpty() && tagCtrl.getSelectedTags().isEmpty()) {
            refreshTreeView(collections, allNotes, true);
            return;
        }

        List<Note> filteredNotes = getFilteredNotes();
        List<Collection> filteredCollections = collections.stream()
                .filter(collection -> filteredNotes.stream()
                        .anyMatch(note -> note.collection.equals(collection)))
                .toList();

        refreshTreeView(filteredCollections, filteredNotes, true);

        collectionView.getSelectionModel().clearSelection();
    }

    @FXML
    public void toggleAccessibility(){
        if(isAccessible) {
            accessibilityButton.getStyleClass().remove("no-accessibility-icon");
            if (!accessibilityButton.getStyleClass().contains("accessibility-icon")) {
                accessibilityButton.getStyleClass().add("accessibility-icon");
            }

            root.getStylesheets().add(getClass().getResource("/css/color-styles.css").toExternalForm());
            root.getStylesheets().remove(getClass().getResource("/css/accessible-styles.css").toExternalForm());
            currentCss = getClass().getResource("/css/color-styles.css").toExternalForm();
        }else{
            accessibilityButton.getStyleClass().remove("accessibility-icon");
            if (!accessibilityButton.getStyleClass().contains("no-accessibility-icon")) {
                accessibilityButton.getStyleClass().add("no-accessibility-icon");
            }

            root.getStylesheets().add(getClass().getResource("/css/accessible-styles.css").toExternalForm());
            root.getStylesheets().remove(getClass().getResource("/css/color-styles.css").toExternalForm());
            currentCss = getClass().getResource("/css/accessible-styles.css").toExternalForm();
        }
        notificationsCtrl.pushNotification(bundle.getString("toggled.accessibility"), false);
        isAccessible = !isAccessible;
    }

    public void showHelpMenu() {
        mainCtrl.showHelpMenu();
    }

    public void changeTitle(Note note, String oldTitle, String uniqueTitle) {
        note.setTitle(uniqueTitle);
        noteCtrl.getUpdatePendingNotes().add(note);
        handleReferenceTitleChange(note, oldTitle, uniqueTitle);
        noteCtrl.saveAllPendingNotes();

        noteTitle.setText(uniqueTitle);
        noteTitleMD.setText(uniqueTitle);

        notificationsCtrl.pushNotification(bundle.getString("validRename"), false);

        noteCtrl.updateTitleWebsocket(note);
    }

    private void handleReferenceTitleChange(Note item, String oldTitle, String uniqueTitle) {
        collectionNotes.stream()
                .filter(note -> note.collection.equals(item.collection))
                .filter(note -> note.body.contains("[[" + oldTitle + "]]"))
                .forEach(note -> {
                    note.body = note.body.replace("[[" + oldTitle + "]]", "[[" + uniqueTitle + "]]");
                    noteCtrl.getUpdatePendingNotes().add(note);
                });
    }
}
