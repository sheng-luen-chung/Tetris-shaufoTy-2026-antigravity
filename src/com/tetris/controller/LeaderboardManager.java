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
    private static final int MAX_ENTRIES = 5;
    private final Path leaderboardFile;

    public LeaderboardManager() {
        this.leaderboardFile = Paths.get(System.getProperty("user.home"), ".tetris", "leaderboard.csv");
    }

    public synchronized void recordScore(int score, int secondsElapsed, GameEngine.Difficulty difficulty) {
        List<LeaderboardEntry> entries = loadEntries();
        entries.add(new LeaderboardEntry(score, secondsElapsed,
                difficulty == null ? "UNKNOWN" : difficulty.getLabel(), System.currentTimeMillis()));
        entries.sort(entryComparator());
        saveEntries(entries);
    }

    public synchronized List<LeaderboardEntry> getTopEntries() {
        List<LeaderboardEntry> entries = loadEntries();
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

    private List<LeaderboardEntry> loadEntries() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!Files.exists(leaderboardFile)) {
            return entries;
        }

        try {
            for (String line : Files.readAllLines(leaderboardFile, StandardCharsets.UTF_8)) {
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

    private void saveEntries(List<LeaderboardEntry> entries) {
        List<String> lines = new ArrayList<>();
        int limit = Math.min(entries.size(), MAX_ENTRIES);
        for (int i = 0; i < limit; i++) {
            LeaderboardEntry entry = entries.get(i);
            lines.add(entry.getScore() + "," + entry.getSecondsElapsed() + "," + escape(entry.getDifficulty()) + ","
                    + entry.getPlayedAt());
        }

        try {
            Files.createDirectories(leaderboardFile.getParent());
            Files.write(leaderboardFile, lines, StandardCharsets.UTF_8);
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