# Tetris

This is a Java and Swing-based Tetris project. The goal is to provide a complete, playable Tetris experience with game logic, UI, sound, save/load, leaderboard, achievements, and multiple game modes.

## Features

- Full Tetris core gameplay: piece generation, movement, rotation, collision, locking, and line clears
- Multiple game modes: `STAGE`, `ENDLESS`, `SURVIVAL`, `SPRINT`, `ULTRA`, `VS_AI`, `LOCAL_PVP`, `NET_PVP`
- In-game difficulty switching: `EASY`, `NORMAL`, `HARD`
- Advanced rules: `T-Spin`, `Combo`, `Back-to-Back`, and garbage line handling
- UI and visual effects: next piece preview, Hold, sidebar information, score popups, particle effects, and achievement toasts
- Extra systems: local leaderboard, save/load, sound and background music, language switching, theme switching, and color-blind support
- Test support: built-in game logic tests that can be run independently to verify core rules

## Controls

- Default key bindings:
  - Left / Right Arrow: move the piece
  - Up Arrow: rotate the piece
  - Down Arrow: soft drop
  - Space: hard drop, instantly place the piece
  - P or Esc: pause or resume the game

- If you do not want to use the default bindings, you can rebind keys in the settings.

## Implemented Features

- Seven Tetromino pieces with basic movement and rotation
- Line clear and scoring
- Difficulty-based drop speed control
- Next piece preview and Hold
- Pause, game over detection, and restart flow
- Leaderboard management
- Save and settings persistence
- Sound effects and background music management
- AI battle and network battle related modules
- Theme, language, and accessibility customization
- Unit tests and a test launcher

## Project Structure

```text
Tetris/
  src/
    com/tetris/main/Main.java
    com/tetris/controller/
      GameEngine.java
      InputHandler.java
      LeaderboardManager.java
    com/tetris/model/
      Board.java
      GameMode.java
      LeaderboardEntry.java
      Piece.java
      Tetromino.java
    com/tetris/util/
      AchievementManager.java
      LanguageManager.java
      NetworkManager.java
      SaveManager.java
      SoundManager.java
      TetrisAI.java
      ThemeManager.java
    com/tetris/view/
      GamePanel.java
      IPInputDialog.java
      MessageDialog.java
    com/tetris/test/
      GameLogicTest.java
      TestRunner.java
  README.md
  README_ZH.md
  README_EN.md
  build_and_run.bat
  run_tests.bat
  manifest.txt
```

## Requirements

- To run `Tetris.jar` directly: `JRE 17` or later
- To compile the source code yourself: `JDK 17` or later
- This workspace is configured to prefer `JDK 25` on Windows
- On Windows, you can use the provided batch files
- On other platforms, you can compile and run manually with `javac` and `java`

## How to Run

### Option 1: Use the batch file

Run the following from the project root:

```bat
build_and_run.bat
```

The batch file prefers `C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot` when it is available, so `java` and `javac` stay on the same JDK version.

### Option 2: Compile and run manually

```bash
mkdir bin
javac -d bin src/com/tetris/main/Main.java src/com/tetris/controller/*.java src/com/tetris/model/*.java src/com/tetris/view/*.java src/com/tetris/util/*.java src/com/tetris/test/*.java
java -cp bin com.tetris.main.Main
```

If you want to package the project as an executable JAR according to `manifest.txt`, set `Main-Class` to `com.tetris.main.Main`.

## Testing

To run the game logic tests, use the provided test script:

```bat
run_tests.bat
```

You can also run the test entry point directly with `com.tetris.test.TestRunner`.

## Version

- Current project version: `3.0.2`

## Notes
