# Escape Game (JavaFX)

A JavaFX-based puzzle game with single-player and multiplayer modes. Content and player data are stored in simple JSON files under the `data/` folder for easy editing and persistence.

## Features
- **Single-player**: Choose a room and difficulty and solve puzzles.
- **Multiplayer (socket server)**: Host/join rooms; server filters puzzles by selected room and difficulty.
- **Profiles & avatars**: Basic user profiles stored locally.
- **Admin & puzzle manager**: Manage puzzles via UI.
- **Leaderboard & scores**: Progress and scores persisted to JSON.

## Tech Stack
- **Java 21**, **Maven**
- **JavaFX 21** (controls, fxml, web, media, swing)
- UI add-ons: ControlsFX, FormsFX, ValidatorFX, Ikonli, BootstrapFX, TilesFX
- **Gson** and **org.json** for JSON serialization
- FXGL (some UI/game utilities)

Module name: `com.example.escapeGame` (see `src/main/java/module-info.java`).

## Project Structure
```
Escape_Game/
├─ pom.xml
├─ data/                         # Runtime JSON data (editable)
│  ├─ users.json
│  ├─ puzzles.json
│  └─ leaderboard.json
└─ src/
   └─ main/
      ├─ java/
      │  ├─ com/example/escapeGame/
      │  │  ├─ Launcher.java            # Launches JavaFX app
      │  │  ├─ LogIn.java               # JavaFX Application
      │  │  ├─ PuzzleDataUtil.java      # Loads/saves puzzles from data/
      │  │  ├─ UserDataUtil.java        # Loads/saves users from data/
      │  │  ├─ LeaderboardDataUtil.java # Loads/saves leaderboard from data/
      │  │  ├─ ScoreDataUtil.java       # Loads/saves scores from data/
      │  │  └─ ... (controllers, models)
      │  └─ com/example/escapeGame/server/
      │     └─ ServerMain.java          # Multiplayer socket server (port 9090)
      └─ resources/
         └─ com/example/escapeGame/
            ├─ login.fxml, mode.fxml, game.fxml, ...
            └─ (FXML views / assets)
```

## Prerequisites
- JDK **21**
- Maven **3.9+**
- Windows/Linux/macOS (JavaFX Maven plugin manages platform dependencies)

## Run the Client (JavaFX)
From the project root (`Escape_Game`):

```bash
mvn clean javafx:run
```

This uses the plugin config in `pom.xml` to launch `com.example.escapeGame.LogIn`.

Alternatively, in an IDE:
- Run `com.example.escapeGame.Launcher` (it calls `Application.launch(LogIn.class, args)`), or
- Run `com.example.escapeGame.LogIn` directly.

## Run the Multiplayer Server
The server is a plain Java socket app; default port is **9090**.

- In an IDE: run `com.example.escapeGame.server.ServerMain`.
- From CLI:
  ```bash
  mvn -q -DskipTests package
  java -cp target/classes com.example.escapeGame.server.ServerMain 9090
  ```
  You can omit `9090` to use the default.

Start the server first, then run one or more clients to host/join rooms.

## Data and Persistence
Runtime data lives in the project-level `data/` directory:
- `data/users.json`
- `data/puzzles.json`
- `data/leaderboard.json`
- `data/scores.json` (created on first save)

The utilities ensure the `data/` folder exists and will create empty files if missing. Editing these JSON files updates the game content and users immediately.

> Note: Older copies of JSON under `src/main/resources/` are not used at runtime for persistence. Keep only the `data/` versions to avoid confusion.

## JSON Schemas
- `puzzles.json` (array)
  ```json
  {
    "id": 1,
    "room": "Riddle Chamber",
    "question": "...",
    "answer": "...",      // also accepts "correctAnswer" (backward compatible)
    "difficulty": "Easy"  // Easy | Medium | Hard
  }
  ```

- `users.json` (array)
  ```json
  {
    "username": "admin",
    "password": "adminpass",
    "email": "admin@gmail.com",
    "role": "admin",     // or "user"
    "avatar": "girl_2.png"  // optional
  }
  ```

- `leaderboard.json` / `scores.json`
  - Simple arrays of score/stat entries managed by the app (do not hand-edit unless needed).

## Common Classes
- `PuzzleDataUtil`, `UserDataUtil`, `ScoreDataUtil`, `LeaderboardDataUtil` – read/write JSON under `data/`.
- `Launcher`/`LogIn` – JavaFX entry points.
- `ServerMain` – multiplayer server entry point (defaults to port `9090`).

## Troubleshooting
- **App runs but shows no puzzles**: Ensure `data/puzzles.json` exists and has content. If it was auto-created on first run, it may be an empty `[]`.
- **JavaFX runtime errors**: Use `mvn javafx:run`. This configures module-path for you.
- **Deleting resource JSON shows IDE conflicts**: Prefer a plain Delete (not Safe Delete/Refactor), then commit the removal. The code only uses `data/`.
- **Server not reachable**: Verify the port (9090 by default) and firewall settings.

## Build & Test
```bash
mvn clean package    # build
mvn -q -DskipTests package  # faster build
mvn -Dtest=* test    # run tests (JUnit 5)
```

## License
Educational project. Add a license file if you plan to distribute.
