# NetNote

This repository contains the NetNote application, developed as part of the CSE project. NetNote is a distributed note-taking application designed to run in a client-server architecture, enabling seamless note organization, collaboration, and synchronization.

![NetNote](https://github.com/user-attachments/assets/72dd9618-38a6-4cdc-8f42-12827c9a506e)

---

## Getting Started

### Prerequisites
To run the project, you need:
- [Maven](https://maven.apache.org/install.html) installed on your system or the provided Maven wrapper (`mvnw`).
- Java 23 or later.

### Steps to Build and Run
1. Clone the repository.
2. Use Maven to build the project:
   ```
   mvn clean install
   ```
   This will package and install the artifacts for the three subprojects (client, server, and shared).
3. To start the server:
   ```
   cd server
   mvn spring-boot:run
   ```
4. To start the client:
   ```
   cd client
   mvn javafx:run
   ```

### First actions
- To start using the app and taking notes, you must create your first collection. You can do this by selecting the ```All notes``` dropdown label and then selecting ```Add Collection```, or by pressing ```CTRL + N```, and then entering a valid name and server.
- To create your first note, click on the ```Add Note``` button to the right of the ```All notes``` dropdown label.

---

## Features

### 1. Basic Requirements
- Host notes on a server for access from multiple clients.
- View all existing notes on the server.
- Create, edit, and delete notes with unique titles.
- Synchronize note changes automatically with the server.
- Markdown support, including a rendered preview.
- Search for notes by keywords.
- Automatic rendering updates and prevention of duplicate titles.

---

### 2. Multi-Collection Support
- Organize notes into multiple collections.
- Create, connect, edit, or delete collections.
- Filter notes by collection or view all collections simultaneously.
- Migrate collections between servers.
- Server status indicators for collection operations (e.g., server unreachable, collection exists).
- Configuration persisted locally using JSON.
### Meaningful addition
- **Multi-server** functionality is implemented, together with a set of extra actions regarding collections, including:
    - creating new collections;
    - connecting to already existing collections
    - deleting collections;
    - forgetting a collection from the client, without deleting it on the server
    - migrating collections from one server to another

---

### 3. Embedded Files
- Embed files (e.g., images) into notes.
- Rename and delete embedded files.
- Automatically remove embedded files when deleting a note.
- Refer to embedded images in Markdown previews.
- Dedicated URLs for embedded files, ensuring accessibility.
- Simplified metadata tooltips displayed on hover.
### Meaningful addition
- Hovering above added notes displays a tooltip with meta-data about the file

---

### 4. Interconnected Content
- Use tags (e.g., `#tag`) for categorizing notes.
- Link notes using `[[Note Title]]` syntax, with automatic updates on title changes.
- Render links and tags in Markdown previews for easy navigation.
- Visual indicators for unresolved references or missing tags.
- Multi-tag filtering with intuitive dropdown suggestions.

### Meaningful addition
- After typing '[[', a context menu pops up with recomendations for references, kind of like autocomplete
- Hovering over a reference shows a tooltip with details about the referred note

---

### 5. Automated Change Synchronization
- Real-time synchronization of changes, including:
    - Note content.
    - Note addition or deletion.
    - Title updates.
    - Embedded file changes.
- WebSocket-based push updates ensure efficiency.

### Meaningful addition
- Changes made to Embedded files are also synchronized

---

### 6. Live Language Switching
- Change the application language at runtime.
- Persist language preferences across sessions.
- Supports English, Dutch, and an additional proof-of-concept language.
- Language indicator with flag icons for easy identification.

### Meaningful addition
- Added a fourth language

---

## HCI
1. A high-contrast mode was implemented, which can be toggled on and off using the button at the bottom of the app.
2. Tooltips have been added to all icon-buttons in order to clearly inform the user about the actions of these buttons.

---

## Other considerations
1. As per the requirements, the ```config.json``` and ```markdown.css``` files can be found:
    - At ```"C:\Users\username\AppData\Roaming\NetNote\"```on Windows;
    - At ```"home/username/.netnote/``` on Linux and Mac;
   
    And can be edited by the user.

---

## Keyboard Shortcuts

### Dashboard
- **Create/Edit:**
    - `CTRL + N` / `ALT + N` - New Note
    - `CTRL + SHIFT + N` / `ALT + SHIFT + N` - New Collection
    - `CTRL + E` / `ALT + E` - Edit current note name
    - `CTRL + SHIFT + E` / `ALT + SHIFT + E` - Edit collection
- **Delete:**
    - `ALT + DELETE` - Delete current note
- **Navigation:**
    - `ALT + UP/DOWN ARROW` - Navigate notes
    - `ALT + SHIFT +UP/DOWN ARROW` - Select multiple notes
    - `ALT + LEFT/RIGHT ARROW` - Navigate collections
- **Other Actions:**
    - ```CTRL + Z```or ```ALT + Z``` - Undo last note action
        - Actions include:
            1. Note title changes
            2. Note body changes
            3. Moving notes between collections
            4. Moving multiple notes at the same time
            5. Adding / Editing / Deleting embedded files
    - `ESCAPE` - Clear search and focus search field
    - `F5` - Refresh
    - `F11` / `ALT + ENTER` - Fullscreen

#### Collections Stage
- ```CTRL + N``` or ```ALT + N``` - Add new collection
- ```CTRL + S``` or ```ALT + S``` - Save collection
- ```ALT + C``` - Create/Connect collection
- ```ALT + DELETE``` - Delete collection
- ```ALT + SHIFT + DELETE``` - Forget collection


- ```UP ARROW``` or ```DOWN ARROW``` - Navigate through collections
- ```TAB``` or ```SHIFT + TAB``` - Navigate through input fields & buttons


- ```ESCAPE``` - Close Stage
