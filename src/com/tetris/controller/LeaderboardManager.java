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
        return getLeaderboardFile(difficultyLabel, mode, false);
    }

    private Path getLeaderboardFile(String difficultyLabel, GameMode mode, boolean isGlobal) {
        String prefix;
        if (isGlobal) {
            if (mode == GameMode.SPRINT) {
                prefix = "leaderboard_global_sprint_";
            } else if (mode == GameMode.ULTRA) {
                prefix = "leaderboard_global_ultra_";
            } else if (mode == GameMode.SURVIVAL) {
                prefix = "leaderboard_global_survival_";
            } else if (mode == GameMode.STAGE) {
                prefix = "leaderboard_global_stage_";
            } else {
                prefix = "leaderboard_global_";
            }
        } else {
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
        Path file = getLeaderboardFile(label, mode, false);
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.add(new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), playerName));
        entries.sort(entryComparator(mode));
        saveEntries(file, entries);
    }

    public synchronized List<LeaderboardEntry> getTopEntries(GameEngine.Difficulty difficulty, GameMode mode) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode, false);
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.sort(entryComparator(mode));
        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        return entries;
    }

    public synchronized List<LeaderboardEntry> getGlobalTopEntries(GameEngine.Difficulty difficulty, GameMode mode) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode, true);
        if (!Files.exists(file)) {
            initializeGlobalMockFile(file, label, mode);
        }
        List<LeaderboardEntry> entries = loadEntries(file);
        entries.sort(entryComparator(mode));
        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        return entries;
    }

    public synchronized boolean qualifiesForLeaderboard(int score, int secondsElapsed, int linesCleared, GameEngine.Difficulty difficulty, GameMode mode) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode, false);
        List<LeaderboardEntry> entries = loadEntries(file);
        if (entries.size() < MAX_ENTRIES) {
            return true;
        }
        LeaderboardEntry last = entries.get(entries.size() - 1);
        Comparator<LeaderboardEntry> comp = entryComparator(mode);
        LeaderboardEntry temp = new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), "TEMP");
        return comp.compare(temp, last) < 0;
    }

    public void submitGlobalScoreAsync(int score, int secondsElapsed, int linesCleared, GameEngine.Difficulty difficulty, GameMode mode, String name) {
        String label = (difficulty == null ? "NORMAL" : difficulty.name());
        Path file = getLeaderboardFile(label, mode, true);
        
        synchronized (this) {
            List<LeaderboardEntry> entries = loadEntries(file);
            entries.add(new LeaderboardEntry(score, secondsElapsed, linesCleared, label, System.currentTimeMillis(), name));
            entries.sort(entryComparator(mode));
            saveEntries(file, entries);
        }

        new Thread(() -> {
            try {
                // Background network request mockup
                String cloudUrl = "https://api.example.com/tetris/leaderboard";
                Thread.sleep(1000);
            } catch (Exception e) {
                // Ignore network exceptions in mock simulation
            }
        }).start();
    }

    private void initializeGlobalMockFile(Path file, String difficultyLabel, GameMode mode) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String[] mockNames = {
            "[TW] T-Spin大師", "[JP] BlockKing", "[US] PixelZen", 
            "[KR] DoubleClear", "[TW] 俄羅斯神手", "[US] TetrisPro", 
            "[DE] SpeedRacer", "[TW] 快樂消方塊"
        };
        
        int baseScore;
        int baseTime;
        if ("HARD".equals(difficultyLabel)) {
            baseScore = 280000;
            baseTime = 120;
        } else if ("EASY".equals(difficultyLabel)) {
            baseScore = 70000;
            baseTime = 300;
        } else {
            baseScore = 150000;
            baseTime = 200;
        }
        
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < mockNames.length; i++) {
            int score = baseScore - i * 15000 - rand.nextInt(5000);
            if (score < 0) score = 0;
            int seconds = baseTime + i * 25 + rand.nextInt(10);
            int lines = score / 200;
            if (mode == GameMode.SPRINT) {
                lines = 40;
                seconds = 45 + i * 12 + rand.nextInt(5);
            } else if (mode == GameMode.ULTRA) {
                seconds = 120;
            } else if (mode == GameMode.SURVIVAL) {
                seconds = baseTime - i * 20 + rand.nextInt(5);
                if (seconds < 10) seconds = 10;
            } else if (mode == GameMode.STAGE) {
                seconds = baseTime - i * 15 + rand.nextInt(5);
                if (seconds < 20) seconds = 20;
            }
            
            entries.add(new LeaderboardEntry(
                score, seconds, lines, difficultyLabel, 
                System.currentTimeMillis() - (long)i * 3600000L * 24L - rand.nextInt(3600000), 
                mockNames[i]
            ));
        }
        
        entries.sort(entryComparator(mode));
        saveEntries(file, entries);
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