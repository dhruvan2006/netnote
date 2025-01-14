package client.controllers;

import client.scenes.DashboardCtrl;
import client.scenes.EditCollectionsCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;



public class CollectionCtrl {

    // Utilities
    private final ServerUtils server;
    private Config config;
    private NoteCtrl noteCtrl;
    private DashboardCtrl dashboardCtrl;
    private SearchCtrl searchCtrl;
    private DialogStyler dialogStyler = new DialogStyler();

    // References
    private ListView collectionView;
    private TreeView treeView;
    private MenuButton currentCollectionTitle;
    private ToggleGroup collectionSelect;
    private MenuItem allNotesButton;
    private MenuItem editCollectionTitle;
    private Button deleteCollectionButton;
    private MenuButton moveNotesButton;




    @Inject
    public CollectionCtrl(ServerUtils server, Config config, NoteCtrl noteCtrl, SearchCtrl searchCtrl) {
        this.server = server;
        this.config = config;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
    }


    public void setReferences(ListView collectionView,
                              TreeView treeView,
                              MenuButton currentCollectionTitle,
                              ToggleGroup collectionSelect,
                              MenuItem allNotesButton,
                              MenuItem editCollectionTitle,
                              Button deleteCollectionButton,
                              MenuButton moveNotesButton) {
        this.collectionView = collectionView;
        this.treeView = treeView;
        this.currentCollectionTitle = currentCollectionTitle;
        this.allNotesButton = allNotesButton;
        this.editCollectionTitle = editCollectionTitle;
        this.deleteCollectionButton = deleteCollectionButton;
        this.moveNotesButton = moveNotesButton;
        this.collectionSelect = new ToggleGroup();
    }
    /**
     * method used for selecting and switching the collection
     *
     * @param listView    listview
     * @param collections collections
     */

    private void listViewSetupForCollections(
            ListView<Collection> listView,
            ObservableList<Collection> collections
    ) {
        listViewDisplayOnlyTitles(listView, collections, false);

        //switching to collection
        listView.setOnMouseClicked(event -> {
            Collection selectedCollection = listView.getSelectionModel().getSelectedItem();
            // Check if collectionSelect is not null
            if (collectionSelect != null) {
                for (Toggle toggle : collectionSelect.getToggles()) {
                    if (toggle instanceof RadioMenuItem) {
                        RadioMenuItem item = (RadioMenuItem) toggle;
                        if (item.getText().equals(selectedCollection.title)) {
                            collectionSelect.selectToggle(item);
                            item.fire();
                            currentCollectionTitle.hide();
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * method that displays only collection titles in listview
     *
     * @param listView    listview
     * @param collections collections
     */
    private void listViewDisplayOnlyTitles(ListView<Collection> listView, ObservableList<Collection> collections, boolean removeSelf) {
        ObservableList<Collection> filteredCollections = FXCollections.observableArrayList(
                collections.stream()
                        .filter(collection -> !collection.equals(dashboardCtrl.getCurrentCollection()))
                        .toList()
        );
        if(dashboardCtrl.getCurrentNote() != null && removeSelf) {
            filteredCollections.remove(dashboardCtrl.getCurrentNote().collection);
        }
        listView.setItems(filteredCollections);
        listView.setFixedCellSize(35);
        listView.getStyleClass().add("collection-list-view");
        listView.setFocusTraversable(false);
        int maxVisibleItems = 6;
        listView.setPrefHeight(Math.min(maxVisibleItems, filteredCollections.size()) * listView.getFixedCellSize() + 2);
        listView.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Collection filteredCollection, boolean empty) {
                super.updateItem(filteredCollection, empty);
                if (empty || filteredCollection == null) {
                    setText(null);
                } else {

                    Label label = new Label(filteredCollection.title);
                    label.maxWidthProperty().bind(listView.widthProperty().subtract(10)); // Set maximum width in pixels
                    label.setTextOverrun(OverrunStyle.ELLIPSIS); // Set overrun to ellipsis
                    label.setStyle("-fx-text-fill: white;"); // Set text color to white
                    setGraphic(label);
                }
            }
        });
    }

    /**
     * a method used to move notes from listview
     *
     * @param listView    listview
     * @param collections collections
     */
    private void listViewSetupForMovingNotes(
            ListView<Collection> listView,
            ObservableList<Collection> collections
    ) {
        listViewDisplayOnlyTitles(listView, collections, true);
        //switching to collection
        listView.setOnMouseClicked(event -> {
            Collection selectedCollection = listView.getSelectionModel().getSelectedItem();
            for (Toggle toggle : collectionSelect.getToggles()) {
                if (toggle instanceof RadioMenuItem item) {
                    if (item.getText().equals(selectedCollection.title)) {
                        Note currentNote = dashboardCtrl.getCurrentNote();

                        moveNoteFromCollection(dashboardCtrl.getCurrentNote(), dashboardCtrl.getCurrentCollection(), selectedCollection);
                        if(dashboardCtrl.getCurrentCollection() != null ) {
                            item.fire();   // If not in all note view
                            dashboardCtrl.collectionView.getSelectionModel().select(currentNote);
                        }
                        else {
                            dashboardCtrl.refreshTreeView();
                            dashboardCtrl.selectNoteInTreeView(currentNote);
                        }

                        collectionSelect.selectToggle(item);
                        moveNotesButton.hide();
                        break;
                    }
                }
            }
        });
    }
    /**
     * dropout menu for collections + for moving notes
     */
    public void initializeDropoutCollectionLabel() {


        ListView<Collection> collectionListView = new ListView<>();
        ObservableList<Collection> collections = FXCollections.observableArrayList(dashboardCtrl.getCollections());
        listViewSetupForCollections(collectionListView, collections);

        CustomMenuItem scrollableCollectionsItem = new CustomMenuItem(collectionListView, false);


        Collection currentCollection = dashboardCtrl.getCurrentCollection();

        Label dynamicLabel = new Label();
        if (currentCollection != null && currentCollection.title != null) {
            dynamicLabel.setText("Current: " + currentCollection.title);
        } else {
            dynamicLabel.setText("No collection selected");
        }
        dynamicLabel.getStyleClass().add("current-collection-label");

        CustomMenuItem dynamicLabelItem = new CustomMenuItem(dynamicLabel, false);
        currentCollectionTitle.getItems().set(4, dynamicLabelItem);
        currentCollectionTitle.getItems().set(6, scrollableCollectionsItem);
        currentCollectionTitle.setOnHidden(event -> {
            initializeDropoutCollectionLabel();
        });
        currentCollectionTitle.setOnShowing(event -> {
            initializeDropoutCollectionLabel();
        });

    }

    public void moveNotesInitialization() {


        ListView<Collection> collectionListView2 = new ListView<>();
        ObservableList<Collection> collections = FXCollections.observableArrayList(dashboardCtrl.getCollections());
        listViewSetupForMovingNotes(collectionListView2, collections);
        CustomMenuItem scrollableCollectionsItem2 = new CustomMenuItem(collectionListView2, false);
        Collection currentCollectionForNote = null;
        if (dashboardCtrl.getCurrentNote() != null) {
            currentCollectionForNote = dashboardCtrl.getCurrentNote().collection;
        }

        Label dynamicLabel = new Label();
        Label pickNoteDestination = new Label();

        if (currentCollectionForNote != null && currentCollectionForNote.title != null) {
            dynamicLabel.setText("Assigned to: " + currentCollectionForNote.title);
        } else {
            dynamicLabel.setText("No collection assigned");
        }
        dynamicLabel.getStyleClass().add("current-collection-label");
        pickNoteDestination.getStyleClass().add("pick-note-destination");
        CustomMenuItem dynamicLabelItem = new CustomMenuItem(dynamicLabel, false);

        pickNoteDestination.setText("Pick Note Destination");
        CustomMenuItem pickNoteDestinationItem = new CustomMenuItem(pickNoteDestination, false);
        moveNotesButton.getItems().set(0, dynamicLabelItem);
        moveNotesButton.getItems().set(2, pickNoteDestinationItem);
        moveNotesButton.getItems().set(3, scrollableCollectionsItem2);
        moveNotesButton.setOnHidden(event -> {
            moveNotesInitialization();
        });
        moveNotesButton.setOnShowing(event -> {
            moveNotesInitialization();
        });

        moveNotesButton.maxWidthProperty().bind(
                dashboardCtrl.getNoteBody().widthProperty().divide(2).subtract(40)
        );
    }


    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    public void setUp() {

        // Set up the collections menu
        ObservableList<Collection> collections = FXCollections.observableArrayList(config.readFromFile());
        dashboardCtrl.setCollections(collections);

        if (!collections.isEmpty()) {
            server.getWebSocketURL(
                    config.readDefaultCollection().serverURL
            );
            dashboardCtrl.noteAdditionSync();
            dashboardCtrl.noteDeletionSync();
        }

        Collection defaultCollection = collections.stream()
                .filter(collection -> collection.equals(config.readDefaultCollection()))
                .findFirst().orElse(null);
        dashboardCtrl.setDefaultCollection(defaultCollection);

        dashboardCtrl.getAddButton().disableProperty().bind(
                Bindings.createBooleanBinding(
                        collections::isEmpty,
                        collections
                )
        );


        for (Collection c : collections) {
            dashboardCtrl.createCollectionButton(c, currentCollectionTitle, collectionSelect);
        }

        initializeDropoutCollectionLabel();
    }


    public ObservableList<Note> viewNotes(Collection currentCollection, ObservableList<Note> allNotes) {
        dashboardCtrl.setSearchIsActive(false);
        ObservableList<Note> collectionNotes;
        if (currentCollection == null) {
            collectionNotes = allNotes;
            currentCollectionTitle.setText("All Notes");
            collectionView.setVisible(false);
            treeView.setVisible(true);
            collectionView.getSelectionModel().clearSelection();
        } else {
            server.getWebSocketURL(currentCollection.serverURL);
            dashboardCtrl.noteAdditionSync();
            dashboardCtrl.noteDeletionSync();
            collectionNotes = FXCollections.observableArrayList(
                    allNotes.stream()
                            .filter(note -> note.collection.equals(currentCollection))
                            .distinct()
                            .collect(Collectors.toList())
            );
            currentCollectionTitle.setText(currentCollection.title);
            collectionView.setVisible(true);
            treeView.setVisible(false);
            treeView.getSelectionModel().clearSelection();
        }

        collectionView.setItems(collectionNotes);
        collectionView.getSelectionModel().clearSelection();

        deleteCollectionButton.setDisable(currentCollection == null);

        collectionView.getSelectionModel().clearSelection();
        return collectionNotes!=null?collectionNotes : FXCollections.observableArrayList();
    }


    public Collection deleteCollection(Collection currentCollection,
                                       List<Collection> collections,
                                       ObservableList<Note> collectionNotes,
                                       ObservableList<Note> allNotes)
    {
        if (!showDeleteConfirmation()) return currentCollection;

        List<Note> notesToDelete = collectionNotes.stream().toList();
        for (Note n : notesToDelete) {
            noteCtrl.deleteNote(n,collectionNotes,allNotes);
        }
        // delete collection from server
        server.deleteCollection(currentCollection);
        // delete collection from config file
        collections.remove(currentCollection);

        config.writeAllToFile(collections);
        // delete collection from collections menu
        //currentCollectionTitle.getItems().remove(
//.getSelectedToggle()
        //);
        return null;
    }

    private boolean showDeleteConfirmation() {
        return dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                "Delete collection",
                "Delete collection",
                "Are you sure you want to delete this collection? All notes in the collection will be deleted as well."
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /**
     * A method used to move note from one collection to the other
     */
    public Collection moveNoteFromCollection(Note currentNote, Collection currentCollection,
                                             Collection destinationCollection){
        if (currentNote == null) {
            return currentCollection;
        }
        currentNote.collection = destinationCollection;
        if(noteCtrl.isTitleDuplicate(dashboardCtrl.getAllNotes(), currentNote, currentNote.getTitle(), false)){
            currentNote.setTitle(noteCtrl.generateUniqueTitle(dashboardCtrl.getAllNotes(), currentNote, currentNote.getTitle(), false));
        }
        noteCtrl.getUpdatePendingNotes().add(currentNote);
        noteCtrl.saveAllPendingNotes();

        return destinationCollection;
    }


    public void updateCollection(Collection collection, List<Collection> collections) {
        server.updateCollection(collection);
        config.writeAllToFile(collections);
    }

    public Collection addInputtedCollection(Collection inputtedCollection, Collection currentCollection, List<Collection> collections) {
        Collection addedCollection;
        try {
            addedCollection = server.addCollection(inputtedCollection);
            if (addedCollection == null) return currentCollection;
            config.writeToFile(addedCollection);
            if (dashboardCtrl.getDefaultCollection() == null) {
                dashboardCtrl.setDefaultCollection(addedCollection);
                config.setDefaultCollection(addedCollection);
            }

            collections.add(addedCollection);
        } catch (ClientErrorException e) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error",
                    e.getResponse().readEntity(String.class)
            );
            alert.showAndWait();
            return currentCollection;
        }

        // add entry in collections menu
        RadioMenuItem radioMenuItem = dashboardCtrl.createCollectionButton(addedCollection, currentCollectionTitle, collectionSelect);
        collectionSelect.selectToggle(radioMenuItem);

        return addedCollection;
    }

    public void addCollection(){
        EditCollectionsCtrl editCollectionsCtrl = dashboardCtrl.getMainCtrl().showEditCollections();
        editCollectionsCtrl.addCollection();
    }

    public void editCollections() {
        dashboardCtrl.getMainCtrl().showEditCollections();
    }



}
