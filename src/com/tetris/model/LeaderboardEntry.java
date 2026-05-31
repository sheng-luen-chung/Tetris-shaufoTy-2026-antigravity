package com.tetris.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LeaderboardEntry {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final int score;
    private final int secondsElapsed;
    private final int linesCleared;
    private final String difficulty;
    private final long playedAt;
    private final String playerName;

    public LeaderboardEntry(int score, int secondsElapsed, int linesCleared, String difficulty, long playedAt, String playerName) {
        this.score = score;
        this.secondsElapsed = secondsElapsed;
        this.linesCleared = linesCleared;
        this.difficulty = difficulty;
        this.playedAt = playedAt;
        this.playerName = playerName != null ? playerName : "PLAYER";
    }

    public LeaderboardEntry(int score, int secondsElapsed, int linesCleared, String difficulty, long playedAt) {
        this(score, secondsElapsed, linesCleared, difficulty, playedAt, "PLAYER");
    }

    public LeaderboardEntry(int score, int secondsElapsed, String difficulty, long playedAt) {
        this(score, secondsElapsed, 0, difficulty, playedAt, "PLAYER");
    }

    public int getScore() {
        return score;
    }

    public int getSecondsElapsed() {
        return secondsElapsed;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public long getPlayedAt() {
        return playedAt;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayedAtDisplay() {
        return DISPLAY_FORMAT.format(Instant.ofEpochMilli(playedAt));
    }
}