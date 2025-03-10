package server.api;

import commons.EmbeddedFile;
import commons.Note;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.database.NoteRepository;
import server.service.CollectionService;
import server.service.EmbeddedFileService;
import server.service.NoteService;

import java.io.IOException;
import java.util.*;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService noteService;
    private final CollectionService collectionService;
    private final EmbeddedFileService embeddedFileService;
    private final NoteRepository noteRepository;

    public NoteController(NoteService noteService, CollectionService collectionService, EmbeddedFileService embeddedFileService, NoteRepository noteRepository) {
        this.noteService = noteService;
        this.collectionService = collectionService;
        this.embeddedFileService = embeddedFileService;
        this.noteRepository = noteRepository;
    }

    @MessageMapping("/notes")
    @SendTo("/topic/notes")
    public Note addMessage(Note note) {
        ResponseEntity<Note> response = createNote(note);
        if (response.getStatusCode().is2xxSuccessful()) {
            Note addedNote = response.getBody();
            return addedNote;
        }
        throw new RuntimeException("Failed to create note on server");
    }

    @MessageMapping("/notes/{noteId}/body")
    @SendTo("/topic/notes/{noteId}/body")
    public Note updateBody(Note note) {
        return note;
    }

    @MessageMapping("/notes/title")
    @SendTo("/topic/notes/title")
    public Note updateTitle(Note note) {
        return note;
    }

    @MessageMapping("/deleteNote")
    @SendTo("/topic/notes/delete")
    public Note deleteNoteHandler(Note note) {
        ResponseEntity<Void> response = deleteNote(note.id);
        if (response.getStatusCode().is2xxSuccessful()) {
            return note;
        }
        throw new RuntimeException("Failed to delete note on server");
    }

    @MessageMapping("/notes/{noteId}/files")
    @SendTo("/topic/notes/{noteId}/files")
    public UUID sendEmbeddedFileUpdate(@DestinationVariable UUID noteId, UUID embeddedFileId) {
        return embeddedFileId;
    }

    @MessageMapping("/notes/{noteId}/files/deleteFile")
    @SendTo("/topic/notes/{noteId}/files/deleteFile")
    public UUID sendMessageAfterDelete(@DestinationVariable UUID noteId, UUID embeddedFileId) {
        return embeddedFileId;
    }

    @MessageMapping("/notes/{noteId}/files/renameFile")
    @SendTo("/topic/notes/{noteId}/files/renameFile")
    public Object[] sendMessageAfterRename(@DestinationVariable UUID noteId,
                                                                        Object[] newFileName) {
        return newFileName;
    }

    @PostMapping(path = {"/", ""})
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        if (note == null || note.collection == null || note.title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (!collectionService.getAllCollections().contains(note.collection)) {
            return ResponseEntity.badRequest().build();
        }
        boolean isDuplicateTitle = noteService
                .getAllNotes()
                .stream()
                .anyMatch(existingNote -> existingNote.title.equals(note.title) && existingNote.collection.equals(note.collection));

        if (isDuplicateTitle) {
            return ResponseEntity.badRequest().build();
        }

        Note createdNote = noteService.save(note);
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable UUID id, @RequestBody Note note) {
        if (note == null || note.collection == null) {
            return ResponseEntity.badRequest().body("Invalid request");
        } else if (note.title.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Note title cannot be blank");
        }
        Optional<Note> existingNote = noteService.findById(id);
        if (existingNote.isPresent()) {
            note.id = id; // Ensure the note's ID is set
            Note updatedNote = noteService.save(note);
            return ResponseEntity.ok(updatedNote);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID id) {
        Optional<Note> note = noteService.findById(id);
        if (note.isPresent()) {
            try {
                embeddedFileService.deleteFilesByNoteId(id);
                noteService.deleteById(id);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable UUID id) {
        Optional<Note> note = noteService.findById(id);
        return note.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(path={"","/"})
    public ResponseEntity<List<Note>> getAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        return ResponseEntity.ok(notes);
    }

    @PostMapping(path = "/{id}/files", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        Optional<Note> noteOpt = noteService.findById(id);
        if (noteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            EmbeddedFile savedFile = embeddedFileService.saveFile(noteOpt.get(), file);
            return ResponseEntity.ok(savedFile);
        } catch (IOException e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    @GetMapping("/{noteId}/files/{fileName}")
    public ResponseEntity<byte[]> getFileByName(@PathVariable UUID noteId, @PathVariable String fileName) {
        List<EmbeddedFile> files = embeddedFileService.getFilesByNoteId(noteId);
        files = files.stream().filter(e -> Objects.equals(e.getFileName(), fileName)).toList();
        if (files.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(files.getFirst().getFileType()))
                .body(files.getFirst().getFileContent());
    }

    @GetMapping("/{noteId}/files/{fileId}/getFile")
    public ResponseEntity<EmbeddedFile> getFileById(@PathVariable UUID noteId, @PathVariable UUID fileId) {
        Optional<EmbeddedFile> file = embeddedFileService.findById(fileId);
        if (file.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(file.get());
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<List<EmbeddedFile>> getFiles(@PathVariable UUID id) {
        List<EmbeddedFile> files = embeddedFileService.getFilesByNoteId(id);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{noteId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID noteId, @PathVariable UUID fileId) {
        embeddedFileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{noteId}/files/{fileId}/rename")
    public ResponseEntity<EmbeddedFile> renameFile(@PathVariable UUID noteId, @PathVariable UUID fileId, @RequestParam String newFileName) {
        Optional<EmbeddedFile> embeddedFileOpt = embeddedFileService.findById(fileId);
        if (embeddedFileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EmbeddedFile embeddedFile = embeddedFileOpt.get();

        // rename the file by updating its name in the database
        embeddedFile.setFileName(newFileName);
        EmbeddedFile updatedFile = embeddedFileService.save(embeddedFile);

        return ResponseEntity.ok(updatedFile);
    }
}
