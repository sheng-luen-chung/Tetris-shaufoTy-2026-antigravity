# Revision Log

## Since commit `8e5545d` (`Configure Windows JDK 25 repo runner`)

### Portable batch runner fixes

- Updated `build_and_run.bat` and `run_tests.bat` so they no longer depend on one hard-coded JDK 25 install path.
- The scripts now select a JDK in this order:
  1. Project-local JDK folders such as `.jdk` or `.jdk\jdk-*`.
  2. Common Windows install locations for Eclipse Adoptium or Microsoft JDK 25.
  3. `JAVA_HOME`, if it points to a folder with `bin\javac.exe`.
  4. Common Windows install locations for Eclipse Adoptium or Microsoft JDK 21 or 17.
  5. Common Oracle/Java JDK install folders under `C:\Program Files\Java\jdk-*`.
  6. The system `PATH`, if `javac` is available there.
- The scripts now run `java.exe` and `javac.exe` from the same selected JDK folder when possible, avoiding JDK/JRE mismatches.
- If no JDK is found, the scripts print a clear error asking the user to install a JDK or set `JAVA_HOME`.

### Source encoding fix

- Added `-encoding UTF-8` to the `javac` commands in both batch files.
- This prevents Windows from compiling UTF-8 Java source files with the local default encoding, such as `x-windows-950`, which caused many `unmappable character` errors.

### Verification

- `run_tests.bat` now uses `javac 25.0.2`.
- All game logic tests passed.
- `build_and_run.bat` compiled successfully and launched the game process with JDK 25.
- Remaining sound messages, such as missing `.wav` files, are resource warnings and are unrelated to the JDK/JRE version issue.
