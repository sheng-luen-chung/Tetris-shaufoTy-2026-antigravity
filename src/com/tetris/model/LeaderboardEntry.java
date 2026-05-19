package com.tetris.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LeaderboardEntry {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final int score;
    private final int secondsElapsed;
    private final String difficulty;
    private final long playedAt;

    public LeaderboardEntry(int score, int secondsElapsed, String difficulty, long playedAt) {
        this.score = score;
        this.secondsElapsed = secondsElapsed;
        this.difficulty = difficulty;
        this.playedAt = playedAt;
    }

    public int getScore() {
        return score;
    }

    public int getSecondsElapsed() {
        return secondsElapsed;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public long getPlayedAt() {
        return playedAt;
    }

    public String getPlayedAtDisplay() {
        return DISPLAY_FORMAT.format(Instant.ofEpochMilli(playedAt));
    }
}