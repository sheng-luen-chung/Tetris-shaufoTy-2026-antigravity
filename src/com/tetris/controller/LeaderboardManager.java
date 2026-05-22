package com.tetris.controller;

import com.tetris.model.LeaderboardEntry;

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

                Path easyFile = getLeaderboardFile("EASY");
                Path normalFile = getLeaderboardFile("NORMAL");
                Path hardFile = getLeaderboardFile("HARD");

                if (!Files.exists(easyFile) && !easy.isEmpty()) {
                    easy.sort(entryComparator());
                    saveEntries(easyFile, easy);
                }
                if (!Files.exists(normalFile) && !normal.isEmpty()) {
                    normal.sort(entryComparator());
                    saveEntries(normalFile, normal);
                }
                if (!Files.exists(hardFile) && !hard.isEmpty()) {
                    hard.sort(entryComparator());
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

    private Path getLeaderboardFile(String difficultyLabel) {
        String fileName = "leaderboard_" + difficultyLabel.toLowerCase() + ".csv";
        return Paths.get(System.getProperty("user.home"), ".tetris", fileName);
    }

    public synchronized void recordScore(int score, int secondsElapsed, GameEngine.Difficulty difficulty) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label);
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.add(new LeaderboardEntry(score, secondsElapsed, label, System.currentTimeMillis()));
        entries.sort(entryComparator());
        saveEntries(file, entries);
    }

    public synchronized List<LeaderboardEntry> getTopEntries(GameEngine.Difficulty difficulty) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label);
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.sort(entryComparator());
        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        return entries;
    }

    private Comparator<LeaderboardEntry> entryComparator() {
        return Comparator.comparingInt(LeaderboardEntry::getScore).reversed()
                .thenComparingInt(LeaderboardEntry::getSecondsElapsed)
                .thenComparing(Comparator.comparingLong(LeaderboardEntry::getPlayedAt).reversed());
    }

    private List<LeaderboardEntry> loadEntries(Path file) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!Files.exists(file)) {
            return entries;
        }

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                LeaderboardEntry entry = parseLine(line);
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
                    + entry.getPlayedAt());
        }

        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save leaderboard: " + e.getMessage());
        }
    }

    private LeaderboardEntry parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 4) {
            return null;
        }

        try {
            int score = Integer.parseInt(parts[0]);
            int secondsElapsed = Integer.parseInt(parts[1]);
            String difficulty = unescape(parts[2]);
            long playedAt = Long.parseLong(parts[3]);
            return new LeaderboardEntry(score, secondsElapsed, difficulty, playedAt);
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