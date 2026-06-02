# `build_and_run.bat` Flow

## Question

When running `build_and_run.bat`, it first runs tests and then continues through several build steps. The flow feels a bit complex. What does each step do, and why is it needed?

## Answer

Think of `build_and_run.bat` as a one-click release-style build script, not only a simple compile command.

The current flow is:

1. Switch to the project root.

   The script uses `%~dp0` to find the folder where the batch file is located, then changes into that directory. This prevents path problems if the script is launched from another working directory.

2. Select and configure a JDK automatically.

   The script calls `:choose_jdk` and `:configure_java` so Windows does not accidentally mix different `java` and `javac` versions. For example, it avoids compiling with JDK 25 but running with an older Java runtime.

3. Run tests first.

   The script calls:

   ```bat
   call run_tests.bat
   ```

   If tests fail, the build stops. This follows a CI/CD-style habit: do not package a jar if core game logic is already failing.

4. Compile Java source files.

   The script runs `javac` and outputs compiled classes into `bin`.

   It also uses:

   ```bat
   -encoding UTF-8
   ```

   This prevents Windows from compiling UTF-8 Java source files with the local default encoding, which can cause errors when source files contain non-ASCII text.

5. Copy resources.

   The script copies:

   ```text
   src/resources
   ```

   into:

   ```text
   bin/resources
   ```

   This step matters because the jar command packages content from `bin`. The resources must be copied into `bin/resources` before packaging, otherwise they will not be included in `Tetris.jar`.

6. Package `Tetris.jar`.

   The script uses:

   ```bat
   jar cvfm Tetris.jar manifest.txt -C bin .
   ```

   `manifest.txt` provides the main class, so the jar can be launched with:

   ```bat
   java -jar Tetris.jar
   ```

7. Run the generated jar.

   After packaging, the script immediately starts the game with the selected JDK:

   ```bat
   "%JAVA_CMD%" -jar Tetris.jar
   ```

## Why It Feels Complex

The script is doing multiple jobs in one command:

- test
- compile
- copy resources
- package jar
- run the game

That is useful for a full local verification flow, but it is more than a minimal build script.

## Possible Future Split

If the workflow becomes annoying, the scripts can be split later:

- `build_jar.bat`: compile, copy resources, and package `Tetris.jar`
- `build_and_run.bat`: call `build_jar.bat`, then run the game
- `run_tests.bat`: compile and run tests only
