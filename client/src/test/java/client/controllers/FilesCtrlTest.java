package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.EmbeddedFile;
import commons.Note;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FilesCtrlTest {

    private FilesCtrl filesCtrl;
    private ServerUtils serverUtils;
    private DashboardCtrl dashboardCtrl;
    private DialogStyler dialogStyler;
    private Config config;
    private HBox filesView;

    private Note sampleNote;
    private EmbeddedFile sampleFile;

    @BeforeEach
    void setup() {
        serverUtils = mock(ServerUtils.class);
        dashboardCtrl = mock(DashboardCtrl.class);
        dialogStyler = mock(DialogStyler.class);
        config = mock(Config.class);
        filesView = new HBox();

        filesCtrl = new FilesCtrl(serverUtils, new FileChooser(), config);
        filesCtrl.setDashboardCtrl(dashboardCtrl);
        filesCtrl.setReferences(filesView);
        filesCtrl.setDialogStyler(dialogStyler);

        sampleNote = new Note("Sample Note", "This is a test note.", null);
        sampleFile = new EmbeddedFile(sampleNote, "test.txt", "text/plain", new byte[]{});
        sampleFile.setId(1L);
        sampleNote.getEmbeddedFiles().add(sampleFile);
    }

    @Test
    void checkFileNameTrue() {
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of());

        assertTrue(filesCtrl.checkFileName(sampleNote, "newfile.txt"));
    }

    @Test
    void checkFileNameFalse() {
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of(sampleFile));

        assertFalse(filesCtrl.checkFileName(sampleNote, "test.txt"));
    }

    @Test
    void addFile() throws IOException {
        FileChooser mockFileChooser = mock(FileChooser.class);
        File mockFile = mock(File.class);

        when(mockFileChooser.showOpenDialog(any())).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("valid.txt");
        when(mockFile.isDirectory()).thenReturn(false);

        EmbeddedFile embeddedFile = new EmbeddedFile(sampleNote, "valid.txt", "text/plain", new byte[]{});
        embeddedFile.setId(2L);
        when(serverUtils.addFile(eq(sampleNote), eq(mockFile))).thenReturn(embeddedFile);

        filesCtrl.setFileChooser(mockFileChooser);

        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doNothing().when(filesCtrlSpy).updateView(any());
        doAnswer(invocationOnMock -> {
            sampleNote.getEmbeddedFiles().add(embeddedFile);
            return null;
        }).when(serverUtils).send(any(), any());

        EmbeddedFile result = filesCtrlSpy.addFile(sampleNote);

        assertNotNull(result);
        assertEquals("valid.txt", result.getFileName());
        assertTrue(sampleNote.getEmbeddedFiles().contains(result));

        verify(serverUtils, times(1)).addFile(eq(sampleNote), eq(mockFile));
    }

    @Test
    void addFileNullNote() throws IOException {
        Alert mockAlert = mock(Alert.class);
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        EmbeddedFile result = filesCtrl.addFile(null);

        assertNull(result);
        verify(mockAlert, times(1)).showAndWait();
    }

    @Test
    void addFileDirectory() throws IOException {
        Alert mockAlert = mock(Alert.class);
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        FileChooser mockFileChooser = mock(FileChooser.class);
        File mockFile = mock(File.class);

        when(mockFileChooser.showOpenDialog(any())).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("valid.txt");
        when(mockFile.isDirectory()).thenReturn(true);

        filesCtrl.setFileChooser(mockFileChooser);

        EmbeddedFile result = filesCtrl.addFile(sampleNote);

        assertNull(result);
        verify(mockAlert, times(1)).showAndWait();
    }

    @Test
    void showFilesNullNote() {
        filesView = mock(HBox.class);
        filesCtrl.setReferences(filesView);
        filesCtrl.showFiles(null);
        assertNull(filesView.getChildren());
    }

    @Test
    void showFiles_ShouldAddEntries_WhenNoteHasFiles() {
        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doReturn(new HBox()).when(filesCtrlSpy).createFileEntry(any(Note.class), any(EmbeddedFile.class));

        filesView = mock(HBox.class);
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(filesView.getChildren()).thenReturn(mockChildren);  // mock getChildren method

        filesCtrlSpy.setReferences(filesView);

        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of(sampleFile));

        doReturn(new HBox()).when(filesCtrlSpy).createFileEntry(sampleNote, sampleFile);

        filesCtrlSpy.showFiles(sampleNote);


        verify(filesView, times(3)).getChildren();
        verify(filesView.getChildren(), times(1)).clear();
        verify(filesView.getChildren(), times(2)).add(any());
    }


    @Test
    void deleteFileCancelled() {
        Alert mockAlert = mock(Alert.class);
        when(mockAlert.showAndWait()).thenReturn(Optional.of(ButtonType.CANCEL));
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        filesCtrl.deleteFile(sampleNote, sampleFile);

        verify(serverUtils, never()).deleteFile(any(), any());
    }

    @Test
    void deleteFile() {
        EmbeddedFile sampleFile2 = new EmbeddedFile(sampleNote, "test.txt", "text/plain", new byte[]{});
        sampleFile2.setId(2L);
        sampleNote.getEmbeddedFiles().add(sampleFile2);

        Alert mockAlert = mock(Alert.class);
        when(mockAlert.showAndWait()).thenReturn(Optional.of(ButtonType.OK));
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doNothing().when(filesCtrlSpy).updateView(any());
        doAnswer(invocationOnMock -> {
            filesCtrlSpy.updateViewAfterDelete(sampleNote, sampleFile2.getId());
            return null;
        }).when(serverUtils).send(any(), any());

        filesCtrl.deleteFile(sampleNote, sampleFile2);

        assertFalse(sampleNote.getEmbeddedFiles().contains(sampleFile2));
        verify(serverUtils, times(1)).deleteFile(eq(sampleNote), eq(sampleFile2));
    }

    @Test
    void renameFileExistingFilename() {
        Alert mockAlert = mock(Alert.class);
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);
        TextInputDialog mockDialog = mock(TextInputDialog.class);
        when(mockDialog.showAndWait()).thenReturn(Optional.of("test.txt"));
        when(dialogStyler.createStyledTextInputDialog(any(), any(), any())).thenReturn(mockDialog);
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of(sampleFile));

        filesCtrl.renameFile(sampleNote, sampleFile);

        verify(dialogStyler, times(1)).createStyledAlert(
                eq(Alert.AlertType.INFORMATION), any(), any(), eq("A file with this name already exists!")
        );
    }

    @Test
    void renameFile() {
        String originalFileName = sampleFile.getFileName();
        TextInputDialog mockDialog = mock(TextInputDialog.class);
        when(mockDialog.showAndWait()).thenReturn(Optional.of("newfile.txt"));
        when(dialogStyler.createStyledTextInputDialog(any(), any(), any())).thenReturn(mockDialog);

        EmbeddedFile renamedFile = new EmbeddedFile(sampleNote, "newfile.txt", "text/plain", new byte[]{});
        when(serverUtils.renameFile(eq(sampleNote), eq(sampleFile), eq("newfile.txt"))).thenReturn(renamedFile);

        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doNothing().when(filesCtrlSpy).persistFileName(any(), any(), any());
        doNothing().when(filesCtrlSpy).updateView(any());
        doAnswer(invocationOnMock -> {
            filesCtrlSpy.updateViewAfterRename(sampleNote, new Object[]{sampleFile.getId(), "newfile.txt"});
            return null;
        }).when(serverUtils).send(any(), any());

        filesCtrlSpy.renameFile(sampleNote, sampleFile);

        assertFalse(
                sampleNote.getEmbeddedFiles()
                        .stream().filter(e -> e.getFileName().equals("newfile.txt"))
                        .toList().isEmpty()
        );
        assertTrue(
                sampleNote.getEmbeddedFiles()
                        .stream().filter(e -> e.getFileName().equals(originalFileName))
                        .toList().isEmpty()
        );
    }

    @Test
    void persistFileName() {
        sampleNote.setBody("![File](test.txt)");

        NoteCtrl noteCtrlMock = mock(NoteCtrl.class);
        when(dashboardCtrl.getNoteCtrl()).thenReturn(noteCtrlMock);
        doNothing().when(noteCtrlMock).showCurrentNote(any());

        filesCtrl.persistFileName(sampleNote, "test.txt", "newfile.txt");

        assertEquals("![File](newfile.txt)", sampleNote.getBody());
    }

    @Test
    void notPersistFileName() {
        sampleNote.setBody("![File](otherfile.txt)");

        NoteCtrl noteCtrlMock = mock(NoteCtrl.class);
        when(dashboardCtrl.getNoteCtrl()).thenReturn(noteCtrlMock);
        doNothing().when(noteCtrlMock).showCurrentNote(any());

        filesCtrl.persistFileName(sampleNote, "test.txt", "newfile.txt");

        assertEquals("![File](otherfile.txt)", sampleNote.getBody());
    }

    @Test
    void updateView() {
        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doReturn(new HBox()).when(filesCtrlSpy).createFileEntry(any(Note.class), any(EmbeddedFile.class));

        filesView = mock(HBox.class);
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(filesView.getChildren()).thenReturn(mockChildren);  // mock getChildren method

        filesCtrlSpy.setReferences(filesView);

        doReturn(new HBox()).when(filesCtrlSpy).createFileEntry(sampleNote, sampleFile);

        filesCtrlSpy.updateView(sampleNote);


        verify(filesView, times(3)).getChildren();
        verify(filesView.getChildren(), times(1)).clear();
        verify(filesView.getChildren(), times(2)).add(any());
    }

    @Test
    void updateViewAfterAdd() {
        FilesCtrl filesCtrlSpy = spy(filesCtrl);

        Long fileId = 2L;
        EmbeddedFile newFile = new EmbeddedFile();
        newFile.setId(fileId);
        when(serverUtils.getFileById(sampleNote, fileId)).thenReturn(newFile);
        doNothing().when(filesCtrlSpy).updateView(any());

        filesCtrlSpy.updateViewAfterAdd(sampleNote, fileId);

        assertTrue(sampleNote.getEmbeddedFiles().contains(newFile));
        verify(filesCtrlSpy, times(1)).updateView(sampleNote);
    }

    @Test
    void updateViewAfterDelete() {
        FilesCtrl filesCtrlSpy = spy(filesCtrl);

        Long fileId = 2L;
        EmbeddedFile fileToRemove = new EmbeddedFile(sampleNote, "test.txt", "text/plain", new byte[]{});
        fileToRemove.setId(fileId);

        doNothing().when(filesCtrlSpy).updateView(any());

        filesCtrlSpy.updateViewAfterDelete(sampleNote, fileId);

        assertFalse(sampleNote.getEmbeddedFiles().contains(fileToRemove));
        verify(filesCtrlSpy, times(1)).updateView(sampleNote);
    }

    @Test
    void updateViewAfterRename() {
        FilesCtrl filesCtrlSpy = spy(filesCtrl);

        long fileId = 1L;
        String newFileName = "new_name.txt";
        Object[] newFileNameArray = {fileId, newFileName};

        doNothing().when(filesCtrlSpy).updateView(any());

        filesCtrlSpy.updateViewAfterRename(sampleNote, newFileNameArray);

        assertEquals(sampleNote.getEmbeddedFiles().getFirst().getFileName(), newFileName);
        verify(filesCtrlSpy, times(1)).updateView(sampleNote);
    }
}
