package com.tetris.util;

import com.tetris.controller.GameEngine;
import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SaveManager {
    private static final String SAVE_FILE_NAME = "savegame.csv";

    private static Path getSaveFilePath() {
        return Paths.get(System.getProperty("user.home"), ".tetris", SAVE_FILE_NAME);
    }

    public static boolean hasSave() {
        return Files.exists(getSaveFilePath());
    }

    public static void deleteSave() {
        try {
            Files.deleteIfExists(getSaveFilePath());
        } catch (IOException e) {
            System.err.println("Failed to delete save file: " + e.getMessage());
        }
    }

    public static boolean save(int score, int secondsElapsed, GameEngine.Difficulty difficulty,
                               boolean canHoldThisTurn, Piece currentPiece, Piece nextPiece,
                               Piece heldPiece, Board board,
                               int piecesSpawned, int totalActions, int totalLinesCleared,
                               int tetrisClears, int tSpins, int maxCombo) {
        Path path = getSaveFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("score=" + score + "\n");
                writer.write("secondsElapsed=" + secondsElapsed + "\n");
                writer.write("difficulty=" + (difficulty != null ? difficulty.name() : "NORMAL") + "\n");
                writer.write("canHoldThisTurn=" + canHoldThisTurn + "\n");
                writer.write("currentPiece=" + serializePiece(currentPiece) + "\n");
                writer.write("nextPiece=" + serializePiece(nextPiece) + "\n");
                writer.write("heldPiece=" + serializePiece(heldPiece) + "\n");
                writer.write("piecesSpawned=" + piecesSpawned + "\n");
                writer.write("totalActions=" + totalActions + "\n");
                writer.write("totalLinesCleared=" + totalLinesCleared + "\n");
                writer.write("tetrisClears=" + tetrisClears + "\n");
                writer.write("tSpins=" + tSpins + "\n");
                writer.write("maxCombo=" + maxCombo + "\n");

                // Serialize board grid (20 rows x 10 cols)
                StringBuilder boardStr = new StringBuilder();
                Color[][] grid = board.getGrid();
                for (int r = 0; r < Board.ROWS; r++) {
                    for (int c = 0; c < Board.COLS; c++) {
                        if (grid[r][c] == null) {
                            boardStr.append("0");
                        } else {
                            boardStr.append(grid[r][c].getRGB());
                        }
                        if (r < Board.ROWS - 1 || c < Board.COLS - 1) {
                            boardStr.append(",");
                        }
                    }
                }
                writer.write("board=" + boardStr.toString() + "\n");
                return true;
            }
        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            return false;
        }
    }

    private static String serializePiece(Piece piece) {
        if (piece == null) {
            return "null";
        }
        return piece.getType().name() + "," + piece.getRow() + "," + piece.getCol() + "," + piece.getRotationIndex();
    }

    private static Piece deserializePiece(String str) {
        if (str == null || str.equals("null") || str.trim().isEmpty()) {
            return null;
        }
        String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }
        Tetromino type = Tetromino.valueOf(parts[0]);
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        int rotationIndex = Integer.parseInt(parts[3]);
        return new Piece(type, row, col, rotationIndex);
    }

    public static SaveState load() {
        Path path = getSaveFilePath();
        if (!Files.exists(path)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, String> data = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    data.put(key, val);
                }
            }

            SaveState state = new SaveState();
            state.score = Integer.parseInt(data.getOrDefault("score", "0"));
            state.secondsElapsed = Integer.parseInt(data.getOrDefault("secondsElapsed", "0"));
            
            String diffStr = data.getOrDefault("difficulty", "NORMAL");
            state.difficulty = GameEngine.Difficulty.valueOf(diffStr);
            
            state.canHoldThisTurn = Boolean.parseBoolean(data.getOrDefault("canHoldThisTurn", "true"));
            state.currentPiece = deserializePiece(data.get("currentPiece"));
            state.nextPiece = deserializePiece(data.get("nextPiece"));
            state.heldPiece = deserializePiece(data.get("heldPiece"));
            state.piecesSpawned = Integer.parseInt(data.getOrDefault("piecesSpawned", "0"));
            state.totalActions = Integer.parseInt(data.getOrDefault("totalActions", "0"));
            state.totalLinesCleared = Integer.parseInt(data.getOrDefault("totalLinesCleared", "0"));
            state.tetrisClears = Integer.parseInt(data.getOrDefault("tetrisClears", "0"));
            state.tSpins = Integer.parseInt(data.getOrDefault("tSpins", "0"));
            state.maxCombo = Integer.parseInt(data.getOrDefault("maxCombo", "0"));

            String boardStr = data.get("board");
            state.grid = new Color[Board.ROWS][Board.COLS];
            if (boardStr != null && !boardStr.isEmpty()) {
                String[] cells = boardStr.split(",");
                int idx = 0;
                for (int r = 0; r < Board.ROWS; r++) {
                    for (int c = 0; c < Board.COLS; c++) {
                        if (idx < cells.length) {
                            String val = cells[idx];
                            if (!val.equals("0")) {
                                int rgb = Integer.parseInt(val);
                                state.grid[r][c] = new Color(rgb, true);
                            }
                        }
                        idx++;
                    }
                }
            }
            return state;
        } catch (Exception e) {
            System.err.println("Failed to load save file: " + e.getMessage());
            return null;
        }
    }

    public static class SaveState {
        public int score;
        public int secondsElapsed;
        public GameEngine.Difficulty difficulty;
        public boolean canHoldThisTurn;
        public Piece currentPiece;
        public Piece nextPiece;
        public Piece heldPiece;
        public Color[][] grid;
        public int piecesSpawned;
        public int totalActions;
        public int totalLinesCleared;
        public int tetrisClears;
        public int tSpins;
        public int maxCombo;
    }

    private static final String SETTINGS_FILE_NAME = "settings.csv";

    private static Path getSettingsFilePath() {
        return Paths.get(System.getProperty("user.home"), ".tetris", SETTINGS_FILE_NAME);
    }

    public static void saveSettings(int das, int arr, int sdr, 
                                    boolean singleCustom, int[] singleKeys, 
                                    boolean pvpP1Custom, int[] pvpP1Keys,
                                    boolean pvpP2Custom, int[] pvpP2Keys,
                                    float bgmVol, float sfxVol,
                                    boolean bgmMute, boolean sfxMute,
                                    String theme, String colorBlind,
                                    String soundPack, String language) {
        Path path = getSettingsFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("das=" + das + "\n");
                writer.write("arr=" + arr + "\n");
                writer.write("sdr=" + sdr + "\n");
                writer.write("singleCustom=" + singleCustom + "\n");
                if (singleKeys != null && singleKeys.length >= 6) {
                    writer.write("singleKeys=" + singleKeys[0] + "," + singleKeys[1] + "," + singleKeys[2] + "," + singleKeys[3] + "," + singleKeys[4] + "," + singleKeys[5] + "\n");
                }
                writer.write("pvpP1Custom=" + pvpP1Custom + "\n");
                if (pvpP1Keys != null && pvpP1Keys.length >= 6) {
                    writer.write("pvpP1Keys=" + pvpP1Keys[0] + "," + pvpP1Keys[1] + "," + pvpP1Keys[2] + "," + pvpP1Keys[3] + "," + pvpP1Keys[4] + "," + pvpP1Keys[5] + "\n");
                }
                writer.write("pvpP2Custom=" + pvpP2Custom + "\n");
                if (pvpP2Keys != null && pvpP2Keys.length >= 6) {
                    writer.write("pvpP2Keys=" + pvpP2Keys[0] + "," + pvpP2Keys[1] + "," + pvpP2Keys[2] + "," + pvpP2Keys[3] + "," + pvpP2Keys[4] + "," + pvpP2Keys[5] + "\n");
                }
                writer.write("bgmVol=" + bgmVol + "\n");
                writer.write("sfxVol=" + sfxVol + "\n");
                writer.write("bgmMute=" + bgmMute + "\n");
                writer.write("sfxMute=" + sfxMute + "\n");
                writer.write("theme=" + theme + "\n");
                writer.write("colorBlind=" + colorBlind + "\n");
                writer.write("soundPack=" + soundPack + "\n");
                writer.write("language=" + language + "\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public static Map<String, String> loadSettings() {
        Path path = getSettingsFilePath();
        if (!Files.exists(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, String> data = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    data.put(line.substring(0, eqIdx).trim(), line.substring(eqIdx + 1).trim());
                }
            }
            return data;
        } catch (Exception e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            return null;
        }
    }
}

