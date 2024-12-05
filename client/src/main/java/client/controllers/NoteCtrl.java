package client.controllers;

import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteCtrl {

    // Utilities
    private final ServerUtils server;

    // References
    private ListView collectionView;
    private Label noteTitle;
    private Label noteTitleMd;
    private TextArea noteBody;
    private Label contentBlocker;
    private TextField searchField;

    // Variables
    public List<Note> createPendingNotes = new ArrayList<>();
    public List<Note> updatePendingNotes = new ArrayList<>();
    private long tempNoteId = -1;

    @Inject
    public NoteCtrl(ServerUtils server) {
        this.server = server;
    }

    public void setReferences(
            ListView collectionView,
            Label noteTitle,
            Label noteTitleMd,
            TextArea noteBody,
            Label contentBlocker,
            TextField searchField
    ) {
        this.collectionView = collectionView;
        this.noteTitle = noteTitle;
        this.noteTitleMd = noteTitleMd;
        this.noteBody = noteBody;
        this.contentBlocker = contentBlocker;
        this.searchField = searchField;
    }

    public void addNote(Collection currentCollection,
                        List<Collection> collections,
                        ObservableList<Note> allNotes,
                        ObservableList<Note> collectionNotes) {
        Collection collection = currentCollection;
        if (currentCollection == null) {
            collection = collections.stream().filter(c -> c.title.equals("Default")).findFirst().orElse(null);
        }

        // Generate a unique title
        String baseTitle = "New Note";
        Note newNote = new Note(baseTitle, "", collection);

        String newTitle = generateUniqueTitle(allNotes, newNote, baseTitle, true);

        newNote.title = newTitle;
        newNote.id = this.tempNoteId--;
        allNotes.add(newNote);

        if (currentCollection != null) {
            collectionNotes.add(newNote);
        }

        // Add the new note to a list of notes pending being sent to the server
        createPendingNotes.add(newNote);

        collectionView.getSelectionModel().select(collectionNotes.size() - 1);
        collectionView.getFocusModel().focus(collectionNotes.size() - 1);
        collectionView.edit(collectionNotes.size() - 1);

        noteTitle.setText(newTitle);
        noteTitleMd.setText(newTitle);

        noteBody.setText("");
    }

    public void showCurrentNote(Note selectedNote) {
        if (selectedNote == null) return;
        noteTitle.setText(selectedNote.title);
        noteTitleMd.setText(selectedNote.title);
        noteBody.setText(selectedNote.body);
        contentBlocker.setVisible(false);

        if (!searchField.isFocused()) {
            Platform.runLater(() -> noteBody.requestFocus());
        }
    }

    public void deleteSelectedNote(Note currentNote,
                                   ObservableList<Note> collectionNotes,
                                   ObservableList<Note> allNotes) {
        if (currentNote != null) {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm deletion");
            alert.setContentText("Do you really want to delete this note?");
            Optional<ButtonType> buttonType = alert.showAndWait();

            if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                deleteNote(currentNote, collectionNotes, allNotes);
                collectionView.getSelectionModel().clearSelection();
                noteBody.clear();
            }
        }
    }

    public void deleteNote(Note currentNote,
                           ObservableList<Note> collectionNotes,
                           ObservableList<Note> allNotes) {
        if (createPendingNotes.contains(currentNote)) {
            createPendingNotes.remove(currentNote);
        } else {
            updatePendingNotes.remove(currentNote);
            server.deleteNote(currentNote.id);
        }
        collectionNotes.remove(currentNote);
        allNotes.remove(currentNote);
    }

    public void saveAllPendingNotes() {
        try {
            for (Note note : createPendingNotes) {
                Note savedNote = server.addNote(note);
                note.id = savedNote.id;
            }
            createPendingNotes.clear();

            for (Note note : updatePendingNotes) {
                server.updateNote(note);
            }
            updatePendingNotes.clear();
        } catch (Exception e) {
            throw e;
        }
    }

    public void onBodyChanged(Note currentNote) {
        if (currentNote != null) {
            String rawText = noteBody.getText();
            currentNote.setBody(rawText);

            // Add any edited but already existing note to the pending list
            if (!createPendingNotes.contains(currentNote) && !updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
            }
        }
    }

    /**
     * Generates a unique title by appending "(1)", "(2)", etc., if needed.
     */
    public String generateUniqueTitle(ObservableList<Note> allNotes, Note note, String baseTitle, boolean isGlobal) {
        String uniqueTitle = baseTitle;
        int counter = 1;

        while (isTitleDuplicate(allNotes, note, uniqueTitle, isGlobal)) {
            uniqueTitle = baseTitle + " (" + counter + ")";
            counter++;
        }

        return uniqueTitle;
    }

    /**
     * Checks if the given title is already used in the collection, excluding the current note.
     */
    private boolean isTitleDuplicate(ObservableList<Note> allNotes, Note newNote, String title, boolean isGlobal) {
        return allNotes.stream()
                .filter(note -> isGlobal || (note.collection.equals(newNote.collection)))
                .anyMatch(note -> note != newNote && note.getTitle().equals(title));
    }
}
