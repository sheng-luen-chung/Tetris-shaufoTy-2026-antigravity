package com.tetris.controller;

import com.tetris.model.LeaderboardEntry;
import com.tetris.model.GameMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardManager {
    private static final int MAX_ENTRIES = 10;

    public LeaderboardManager() {
        // Perform migration if old single leaderboard file exists
        Path oldFile = Paths.get(System.getProperty("user.home"), ".tetris", "leaderboard.csv");
        if (Files.exists(oldFile)) {
            List<LeaderboardEntry> allEntries = loadEntries(oldFile);
            if (!allEntries.isEmpty()) {
                List<LeaderboardEntry> easy = new ArrayList<>();
                List<LeaderboardEntry> normal = new ArrayList<>();
                List<LeaderboardEntry> hard = new ArrayList<>();

                for (LeaderboardEntry entry : allEntries) {
                    String diff = entry.getDifficulty().toUpperCase();
                    if (diff.contains("EASY")) {
                        easy.add(entry);
                    } else if (diff.contains("HARD")) {
                        hard.add(entry);
                    } else {
                        normal.add(entry);
                    }
                }

                Path easyFile = getLeaderboardFile("EASY", GameMode.ENDLESS);
                Path normalFile = getLeaderboardFile("NORMAL", GameMode.ENDLESS);
                Path hardFile = getLeaderboardFile("HARD", GameMode.ENDLESS);

                if (!Files.exists(easyFile) && !easy.isEmpty()) {
                    easy.sort(entryComparator(GameMode.ENDLESS));
                    saveEntries(easyFile, easy);
                }
                if (!Files.exists(normalFile) && !normal.isEmpty()) {
                    normal.sort(entryComparator(GameMode.ENDLESS));
                    saveEntries(normalFile, normal);
                }
                if (!Files.exists(hardFile) && !hard.isEmpty()) {
                    hard.sort(entryComparator(GameMode.ENDLESS));
                    saveEntries(hardFile, hard);
                }
            }

            try {
                Files.move(oldFile, oldFile.resolveSibling("leaderboard.csv.bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Failed to backup old leaderboard: " + e.getMessage());
            }
        }
    }

    private Path getLeaderboardFile(String difficultyLabel, GameMode mode) {
        String prefix;
        if (mode == GameMode.SPRINT) {
            prefix = "leaderboard_sprint_";
        } else if (mode == GameMode.ULTRA) {
            prefix = "leaderboard_ultra_";
        } else if (mode == GameMode.SURVIVAL) {
            prefix = "leaderboard_survival_";
        } else if (mode == GameMode.STAGE) {
            prefix = "leaderboard_stage_";
        } else {
            prefix = "leaderboard_";
        }
        String fileName = prefix + difficultyLabel.toLowerCase() + ".csv";
        return Paths.get(System.getProperty("user.home"), ".tetris", fileName);
    }

    public synchronized void recordScore(int score, int secondsElapsed, GameEngine.Difficulty difficulty, GameMode mode) {
        recordScore(score, secondsElapsed, 0, difficulty, mode, "PLAYER");
    }

    public synchronized void recordScore(int score, int secondsElapsed, int linesCleared, GameEngine.Difficulty difficulty, GameMode mode) {
        recordScore(score, secondsElapsed, linesCleared, difficulty, mode, "PLAYER");
    }

    public synchronized void recordScore(int score, int secondsElapsed, int linesCleared, GameEngine.Difficulty difficulty, GameMode mode, String playerName) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode);
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.add(new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), playerName));
        entries.sort(entryComparator(mode));
        saveEntries(file, entries);
    }

    public synchronized List<LeaderboardEntry> getTopEntries(GameEngine.Difficulty difficulty, GameMode mode) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode);
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.sort(entryComparator(mode));
        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        return entries;
    }

    public synchronized boolean qualifiesForLeaderboard(int score, int secondsElapsed, int linesCleared, GameEngine.Difficulty difficulty, GameMode mode) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode);
        List<LeaderboardEntry> entries = loadEntries(file);
        if (entries.size() < MAX_ENTRIES) {
            return true;
        }
        LeaderboardEntry last = entries.get(entries.size() - 1);
        Comparator<LeaderboardEntry> comp = entryComparator(mode);
        LeaderboardEntry temp = new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), "TEMP");
        return comp.compare(temp, last) < 0;
    }

    private Comparator<LeaderboardEntry> entryComparator(GameMode mode) {
        if (mode == GameMode.SPRINT) {
            return (a, b) -> {
                boolean aCompleted = a.getLinesCleared() >= 40;
                boolean bCompleted = b.getLinesCleared() >= 40;
                
                if (aCompleted && bCompleted) {
                    int timeCompare = Integer.compare(a.getSecondsElapsed(), b.getSecondsElapsed());
                    if (timeCompare != 0) return timeCompare;
                } else if (aCompleted) {
                    return -1;
                } else if (bCompleted) {
                    return 1;
                } else {
                    int linesCompare = Integer.compare(b.getLinesCleared(), a.getLinesCleared());
                    if (linesCompare != 0) return linesCompare;
                }
                
                int scoreCompare = Integer.compare(b.getScore(), a.getScore());
                if (scoreCompare != 0) return scoreCompare;
                return Long.compare(b.getPlayedAt(), a.getPlayedAt());
            };
        } else if (mode == GameMode.SURVIVAL) {
            return Comparator.comparingInt(LeaderboardEntry::getSecondsElapsed).reversed()
                    .thenComparing(Comparator.comparingInt(LeaderboardEntry::getScore).reversed())
                    .thenComparing(Comparator.comparingLong(LeaderboardEntry::getPlayedAt).reversed());
        } else {
            return Comparator.comparingInt(LeaderboardEntry::getScore).reversed()
                    .thenComparingInt(LeaderboardEntry::getSecondsElapsed)
                    .thenComparing(Comparator.comparingLong(LeaderboardEntry::getPlayedAt).reversed());
        }
    }

    private List<LeaderboardEntry> loadEntries(Path file) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!Files.exists(file)) {
            return entries;
        }

        boolean isSprint = file.getFileName().toString().contains("sprint");

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                LeaderboardEntry entry = parseLine(line, isSprint);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load leaderboard: " + e.getMessage());
        }
        return entries;
    }

    private void saveEntries(Path file, List<LeaderboardEntry> entries) {
        List<String> lines = new ArrayList<>();
        int limit = Math.min(entries.size(), MAX_ENTRIES);
        for (int i = 0; i < limit; i++) {
            LeaderboardEntry entry = entries.get(i);
            lines.add(entry.getScore() + "," + entry.getSecondsElapsed() + "," + escape(entry.getDifficulty()) + ","
                    + entry.getPlayedAt() + "," + entry.getLinesCleared() + "," + escape(entry.getPlayerName()));
        }

        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save leaderboard: " + e.getMessage());
        }
    }

    private LeaderboardEntry parseLine(String line, boolean isSprint) {
        String[] parts = line.split(",", -1);
        if (parts.length < 4) {
            return null;
        }

        try {
            int score = Integer.parseInt(parts[0]);
            int secondsElapsed = Integer.parseInt(parts[1]);
            String difficulty = unescape(parts[2]);
            long playedAt = Long.parseLong(parts[3]);
            int linesCleared = 0;
            if (parts.length >= 5) {
                linesCleared = Integer.parseInt(parts[4]);
            } else {
                linesCleared = isSprint ? 40 : 0;
            }
            String playerName = "PLAYER";
            if (parts.length >= 6) {
                playerName = unescape(parts[5]);
            }
            return new LeaderboardEntry(score, secondsElapsed, linesCleared, difficulty, playedAt, playerName);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace(",", "\\,");
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                builder.append(ch);
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                builder.append(ch);
            }
        }

        if (escaping) {
            builder.append('\\');
        }

        return builder.toString();
    }
}