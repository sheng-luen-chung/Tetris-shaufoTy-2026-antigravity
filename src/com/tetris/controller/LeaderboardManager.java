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
        // If the game still has the old single leaderboard file, split it into the newer
        // per-mode files once so existing player scores are not lost after the format change.
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

                // Only create the new files when they do not already exist, so a later
                // run does not overwrite a newer leaderboard with older migrated data.
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
                // Rename the legacy file after migration to keep a backup for recovery.
                Files.move(oldFile, oldFile.resolveSibling("leaderboard.csv.bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Failed to backup old leaderboard: " + e.getMessage());
            }
        }
    }

    private Path getLeaderboardFile(String difficultyLabel, GameMode mode) {
        // Build the file name from the mode and difficulty so each leaderboard stays isolated.
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
        // Add the new result, re-rank the list, and write back only the top slice.
        // This keeps the file size bounded and guarantees the saved order matches the UI.
        entries.add(new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), playerName));
        entries.sort(entryComparator(mode));
        saveEntries(file, entries);
    }

    public synchronized List<LeaderboardEntry> getTopEntries(GameEngine.Difficulty difficulty, GameMode mode) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode);
        List<LeaderboardEntry> entries = loadEntries(file);
        // The caller should always receive a display-ready list, even if the on-disk
        // order was edited or loaded from an older format.
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
        // Since the list is already ranked, the last entry is the weakest one currently
        // saved; if the new score beats it, the player should enter the leaderboard.
        LeaderboardEntry last = entries.get(entries.size() - 1);
        Comparator<LeaderboardEntry> comp = entryComparator(mode);
        LeaderboardEntry temp = new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), "TEMP");
        return comp.compare(temp, last) < 0;
    }

    private Comparator<LeaderboardEntry> entryComparator(GameMode mode) {
        if (mode == GameMode.SPRINT) {
            // Sprint ranking has a special rule set:
            // 1) completed 40-line runs always outrank incomplete runs,
            // 2) among completed runs, lower clear time is better,
            // 3) tie-breakers use score and then recency.
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
            // Survival mode is time-based, so longer survival comes first.
            // Score and playedAt are only used as tie-breakers.
            return Comparator.comparingInt(LeaderboardEntry::getSecondsElapsed).reversed()
                    .thenComparing(Comparator.comparingInt(LeaderboardEntry::getScore).reversed())
                    .thenComparing(Comparator.comparingLong(LeaderboardEntry::getPlayedAt).reversed());
        } else {
            // Standard ranking is score-first, then faster completion time, then newer runs.
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

        // Older files may not contain the lines-cleared column, especially legacy sprint
        // data, so the reader reconstructs the missing value instead of failing.
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
            // Write a compact CSV row so the file stays easy to inspect and migrate.
            // The order matches parseLine(), which keeps read and write behavior aligned.
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
        // Parse one saved CSV row.
        // Invalid or partially broken rows are skipped so a bad line does not block
        // the rest of the leaderboard from loading.
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