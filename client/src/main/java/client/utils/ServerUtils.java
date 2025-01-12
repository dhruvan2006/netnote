/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client.utils;

import client.ui.DialogStyler;
import com.google.inject.Inject;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import javafx.scene.control.Alert;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.File;
import java.lang.reflect.Type;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;

public class ServerUtils {

	private final Config config;
	private List<Collection> collections;
	private DialogStyler dialogStyler;

	@Inject
	public ServerUtils(Config config, DialogStyler dialogStyler) {
		this.config = config;
		this.dialogStyler = dialogStyler;
		collections = config.readFromFile();
	}

	public void getWebSocketURL(String serverURL) {
		String webSocket = serverURL.replace("http://", "ws://");
		webSocket += "websocket";
		session = connect(webSocket);
	}

	private StompSession session = connect("ws://localhost:8080/websocket");

	private StompSession connect(String url) {
		var client = new StandardWebSocketClient();
		var stomp = new WebSocketStompClient(client);
		stomp.setMessageConverter(new MappingJackson2MessageConverter());
		try {
			return stomp.connectAsync(url, new StompSessionHandlerAdapter() {}).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		throw new IllegalStateException();
	}

	public <T> void registerForMessages(String dest, Class<T> type, Consumer<T> consumer) {
		session.subscribe(dest, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return type;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				consumer.accept((T) payload);
			}
		});
	}

	public void send(String dest, Object o) {
		session.send(dest, o);
	}

	public Note addNote(Note note) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes")
				.request(APPLICATION_JSON)
				.post(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public List<Note> getAllNotes() {
		collections = config.readFromFile();
		List<Note> allNotes = new ArrayList<>();
		if (collections != null) {
			for (Collection collection : collections) {
				allNotes.addAll(getNotesByCollection(collection));
			}
		}
		return allNotes;
	}

	public Note updateNote(Note note) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public void deleteNote(Note note) {
		if(!isServerAvailableWithAlert(note.collection.serverURL)) return;
		ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public List<Note> getNotesByCollection(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL)
				.path("/api/collection/{title}")
				.resolveTemplate("title", collection.title)
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Note>>() {});
	}

	public List<Collection> getCollectionsOnServer(String serverURL) {
		if (!isServerAvailableWithAlert(serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(serverURL).path("/api/collection/")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Collection>>() {});
	}

	public Collection addCollection(Collection collection) {
		if(!isServerAvailableWithAlert(collection.serverURL)) return null;
		if (!collection.serverURL.endsWith("/")) collection.serverURL = collection.serverURL + "/";
		return ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection")
				.request(APPLICATION_JSON)
				.post(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}


	public Collection updateCollection(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection/" + collection.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}

	public void deleteCollection(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return;

		List<Note> notesInCollection = getNotesByCollection(collection);
		for (Note note : notesInCollection) {
			deleteNote(note);
		}

		ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection/" + collection.id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public EmbeddedFile addFile(Note note, File file){
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		// Convert file to MultipartFile
		FormDataMultiPart multiPart = new FormDataMultiPart();
		multiPart.bodyPart(new FileDataBodyPart("file", file));

		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.post(Entity.entity(multiPart, MULTIPART_FORM_DATA_TYPE), EmbeddedFile.class);
	}

	public void deleteFile(Note note, EmbeddedFile file) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return;
		ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id + "/files/" + file.getId())
				.request(APPLICATION_JSON)
				.delete();
	}

	public EmbeddedFile renameFile(Note note, EmbeddedFile file, String newFileName) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id + "/files/" + file.getId() + "/rename")
				.queryParam("newFileName", newFileName)
				.request(APPLICATION_JSON)
				.put(Entity.entity(file, APPLICATION_JSON), EmbeddedFile.class);
	}

	public List<EmbeddedFile> getFilesByNote(Note note) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		List<EmbeddedFile> result = ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<EmbeddedFile>>() {});
		if (result == null)
			result = new ArrayList<>();
		return result;
	}

	public long getCollectionID(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return -1;
		Collection fetchedCollection = ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection/title/" + collection.title)
				.request(APPLICATION_JSON)
				.get(Collection.class);
		return fetchedCollection.id;
	}

	public boolean isServerAvailableWithAlert(String serverUrl) {
		if (!isServerAvailable(serverUrl)) {
			dialogStyler.createStyledAlert(
					Alert.AlertType.ERROR,
					"Server Unreachable.",
					"Server could not be reached.",
					"The server you tried to access is unreachable. Please try again later."
			).showAndWait();

			return false;
		}
		return true;
	}

	public boolean isServerAvailable(String serverUrl) {
		if (!isValidUrl(serverUrl)) return false;
		try {
			if (serverUrl == null);
			ClientBuilder.newClient(new ClientConfig()) //
					.target(serverUrl) //
					.request(APPLICATION_JSON) //
					.get();
			return true;
		} catch (ProcessingException e) {
			if (e.getCause() instanceof ConnectException) {
				return false;
			}
		}
		return false;
	}

	public boolean isValidUrl(String url) {
		try {
			URI uri = new URI(url);

			// Ensure the scheme is either http or https
			String scheme = uri.getScheme();
			if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
				return false;
			}

			// Ensure there is a valid host
			String host = uri.getHost();
			if (host == null || host.isEmpty()) {
				return false;
			}

			// Ensure the URL is not only scheme and host (e.g., "http://")
			if (uri.getPath() == null && uri.getQuery() == null && uri.getFragment() == null) {
				return true; // Acceptable for cases like "http://test.com"
			}

			return true; // URL is valid
		} catch (URISyntaxException e) {
			return false; // Invalid URL syntax
		}
	}

}