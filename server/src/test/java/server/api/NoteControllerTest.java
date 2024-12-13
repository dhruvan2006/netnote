package server.api;

import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import server.service.CollectionService;
import server.service.EmbeddedFileService;
import server.service.NoteService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class NoteControllerTest {

    private NoteController noteController;
    private CollectionController collectionController;

    private NoteService noteService;
    private CollectionService collectionService;
    private EmbeddedFileService embeddedFileService;

    private TestNoteRepository noteRepo;
    private TestCollectionRepository collectionRepo;
    private TestEmbeddedFileRepository embeddedFileRepository;

    Collection collection1, collection2;
    Note note1, note2, note3, note4;
    EmbeddedFile embeddedFile;

    @BeforeEach
    void setUp() {
        noteRepo = new TestNoteRepository();
        collectionRepo = new TestCollectionRepository();
        embeddedFileRepository = new TestEmbeddedFileRepository();

        noteService = new NoteService(noteRepo);
        collectionService = new CollectionService(collectionRepo);
        embeddedFileService = new EmbeddedFileService(embeddedFileRepository);

        noteController = new NoteController(noteService, collectionService, embeddedFileService);
        collectionController = new CollectionController(noteService, collectionService);


        collection1 = new Collection("collection1");
        collection2 = new Collection("collection2");

        note1 = new Note("note1", "bla", collection1);
        note2 = new Note("note2", "bla", collection1);
        note3 = new Note("note3", "bla", collection2);
        note4 = new Note("note4", "bla", collection2);

        embeddedFile = new EmbeddedFile(note1, "file.txt", "text/plain", new byte[]{1, 2, 3, 4});
        embeddedFile.setId(1L);
    }

    @Test
    public void createNoteTest() {
        collectionController.createCollection(collection1);
        var actual = noteController.createNote(note1);
        assertNotNull(actual);
        assertEquals(ResponseEntity.ok(note1), actual);
    }

    @Test
    public void cannotAddNullNote() {
        var actual = noteController.createNote(null);
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void cannotAddNoteWithNullCollection() {
        var actual = noteController.createNote(new Note("test", "bla", null));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void cannotAddNoteWithUnknownCollection() {
        var actual = noteController.createNote(new Note("test", "bla", collection1));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void cannotAddNoteWithEmptyTitle() {
        collectionController.createCollection(collection1);
        var actual = noteController.createNote(new Note("", "bla", collection1));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void getNoteByIdTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);
        noteController.createNote(note2);

        var actual1 = noteController.getNoteById(1);
        var actual2 = noteController.getNoteById(2);

        assertEquals(ResponseEntity.ok(note1), actual1);
        assertEquals(ResponseEntity.ok(note2), actual2);
    }

    @Test
    public void getNoteByIdNotFoundTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var actual = noteController.getNoteById(2);

        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void deleteNoteTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var response = noteController.deleteNote(1);

        assertEquals(ResponseEntity.noContent().build(), response);

        var actual = noteController.getNoteById(1);
        assertEquals(ResponseEntity.notFound().build(), actual);

    }

    @Test
    public void updateNoteTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var response = noteController.updateNote(1, note2);
        assertEquals(ResponseEntity.ok(note2), response);

        var actual = noteController.getNoteById(1);
        assertEquals(ResponseEntity.ok(note2), actual);
    }

    @Test
    public void updateNoteNotFoundTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var actual = noteController.updateNote(2, note3);
        assertEquals(ResponseEntity.notFound().build(), actual);

        var actual2 = noteController.getNoteById(1);
        assertEquals(ResponseEntity.ok(note1), actual2);
    }


    @Test
    void getAllNotesTest() {
        collectionController.createCollection(collection1);
        collectionController.createCollection(collection2);

        noteController.createNote(note1);
        noteController.createNote(note2);
        noteController.createNote(note3);
        noteController.createNote(note4);

        var response = noteController.getAllNotes();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());

        assertEquals("note1", response.getBody().get(0).title);
        assertEquals("note2", response.getBody().get(1).title);
        assertEquals("note3", response.getBody().get(2).title);
        assertEquals("note4", response.getBody().get(3).title);

        assertEquals(collection1, response.getBody().get(0).collection);
        assertEquals(collection1, response.getBody().get(1).collection);
        assertEquals(collection2, response.getBody().get(2).collection);
        assertEquals(collection2, response.getBody().get(3).collection);
    }

    @Test
    public void getFilesTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        noteController.uploadFile(1L, mockFile);

        var response = noteController.getFiles(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        EmbeddedFile retrievedFile = response.getBody().get(0);
        assertEquals("test.txt", retrievedFile.getFileName());
        assertEquals("text/plain", retrievedFile.getFileType());
    }

    @Test
    public void getFilesForNonExistentNoteTest() {
        var response = noteController.getFiles(999L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void uploadFileTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        var response = noteController.uploadFile(1L, mockFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        EmbeddedFile uploadedFile = (EmbeddedFile) response.getBody();
        assertNotNull(uploadedFile);
        assertEquals("test.txt", uploadedFile.getFileName());
        assertEquals("text/plain", uploadedFile.getFileType());
    }

    @Test
    public void uploadFileNoteNotFoundTest() {
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        var response = noteController.uploadFile(1L, mockFile);

        assertEquals(ResponseEntity.notFound().build(), response);
    }

    @Test
    public void deleteFileTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);
        embeddedFileRepository.save(embeddedFile);

        var response = noteController.deleteFile(1L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(embeddedFileRepository.findById(1L).isEmpty());
    }

    @Test
    public void renameFileTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);
        embeddedFileRepository.save(embeddedFile);

        var response = noteController.renameFile(1L, 1L, "new_name.txt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        EmbeddedFile renamedFile = response.getBody();
        assertNotNull(renamedFile);
        assertEquals("new_name.txt", renamedFile.getFileName());
    }

    @Test
    public void renameFileNotFoundTest() {
        var response = noteController.renameFile(1L, 1L, "new_name.txt");

        assertEquals(ResponseEntity.notFound().build(), response);
    }
}