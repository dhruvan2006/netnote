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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import commons.Collection;
import commons.Note;
import org.glassfish.jersey.client.ClientConfig;

import commons.Quote;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;

public class ServerUtils {

	private static final String SERVER = "http://localhost:8080/";

	public void getQuotesTheHardWay() throws IOException, URISyntaxException {
		var url = new URI("http://localhost:8080/api/quotes").toURL();
		var is = url.openConnection().getInputStream();
		var br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
	}

	public List<Quote> getQuotes() {
		return ClientBuilder.newClient(new ClientConfig()) //
				.target(SERVER).path("api/quotes") //
				.request(APPLICATION_JSON) //
				.get(new GenericType<List<Quote>>() {});
	}

	public Quote addQuote(Quote quote) {
		return ClientBuilder.newClient(new ClientConfig()) //
				.target(SERVER).path("api/quotes") //
				.request(APPLICATION_JSON) //
				.post(Entity.entity(quote, APPLICATION_JSON), Quote.class);
	}


	public Note addNote(Note note) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes")
				.request(APPLICATION_JSON)
				.post(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public List<Note> getAllNotes() {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Note>>() {});
	}

	public Note getNoteById(long id) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes/" + id)
				.request(APPLICATION_JSON)
				.get(Note.class);
	}

	public Note updateNote(Note note) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes/" + note.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public void deleteNote(long id) {
		ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes/" + id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public List<Note> getNotesByCollection(Collection collection) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection/" + collection.title)
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Note>>() {});
	}

	public Collection addCollection(Collection collection) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection")
				.request(APPLICATION_JSON)
				.post(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}

	public List<Collection> getCollections() {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Collection>>() {});
	}

	public Collection updateCollection(Collection collection) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection/" + collection.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}

	public void deleteCollection(long id) {
		ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection/" + id)
				.request(APPLICATION_JSON)
				.delete();
	}


	public boolean isServerAvailable() {
		try {
			ClientBuilder.newClient(new ClientConfig()) //
					.target(SERVER) //
					.request(APPLICATION_JSON) //
					.get();
		} catch (ProcessingException e) {
			if (e.getCause() instanceof ConnectException) {
				return false;
			}
		}
		return true;
	}
}