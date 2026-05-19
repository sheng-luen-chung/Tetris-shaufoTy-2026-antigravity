package com.tetris.controller;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import com.tetris.view.GamePanel;
import javax.swing.Timer;
import java.util.Random;

public class GameEngine {
    public enum Difficulty {
        EASY("EASY", 700),
        NORMAL("NORMAL", 500),
        HARD("HARD", 300);

        private final String label;
        private final int fallDelayMs;

        Difficulty(String label, int fallDelayMs) {
            this.label = label;
            this.fallDelayMs = fallDelayMs;
        }

        public String getLabel() {
            return label;
        }

        public int getFallDelayMs() {
            return fallDelayMs;
        }
    }

    private Board board;
    private GamePanel panel;
    private Piece currentPiece;
    private Piece nextPiece;
    private Timer gameLoop;
    private Timer secondTimer;
    private boolean isGameOver = false;
    private boolean isPaused = false;
    private int score = 0;
    private int secondsElapsed = 0;
    private Difficulty difficulty = Difficulty.NORMAL;

    public GameEngine(Board board, GamePanel panel) {
        this.board = board;
        this.panel = panel;

        // Initialize pieces
        nextPiece = generateRandomPiece();
        spawnNewPiece();

        // Set game loop (falling piece)
        gameLoop = new Timer(difficulty.getFallDelayMs(), e -> {
            if (!isPaused) {
                update();
            }
        });

        // Set second timer (game time)
        secondTimer = new Timer(1000, e -> {
            if (!isGameOver && !isPaused) {
                secondsElapsed++;
                panel.repaint(); // Refresh UI to show timer
            }
        });
    }

    // Start the game
    public void start() {
        gameLoop.start();
        secondTimer.start();
    }

    // Toggle pause
    public void togglePause() {
        if (isGameOver)
            return;
        isPaused = !isPaused;
        panel.repaint();
    }

    // Update the piece (down)
    public void update() {
        if (isGameOver || isPaused)
            return;

        // 1. Try to move down
        currentPiece.move(1, 0);

        // 2. Check collision
        if (!board.isValidMove(currentPiece)) { // Collision
            currentPiece.move(-1, 0); // Reposition (move back up)
            freezeAndSpawn(); // Freeze and spawn
        }

        panel.repaint(); // Repaint
    }

    // Freeze and spawn piece
    private void freezeAndSpawn() {
        int popupCol = getCurrentPieceCenterCol();
        int popupRow = getCurrentPieceCenterRow();
        board.freezePiece(currentPiece); // Freeze
        int lines = board.clearLines(); // Clear lines
        if (lines > 0) {
            int points = getLineClearPoints(lines);
            updateScore(points);
            panel.addScorePopup(popupCol, popupRow, points);
        }
        spawnNewPiece(); // Spawn
    }

    private int getLineClearPoints(int lines) {
        switch (lines) {
            case 1:
                return 100;
            case 2:
                return 300;
            case 3:
                return 500;
            case 4:
                return 800;
            default:
                return 0;
        }
    }

    private void updateScore(int points) {
        score += points;
        panel.setScore(score);
    }

    private int getCurrentPieceCenterCol() {
        int sum = 0;
        int count = 0;
        for (int[] coord : currentPiece.getAbsoluteCoords()) {
            sum += coord[0];
            count++;
        }
        return count == 0 ? 0 : Math.round(sum / (float) count);
    }

    private int getCurrentPieceCenterRow() {
        int sum = 0;
        int count = 0;
        for (int[] coord : currentPiece.getAbsoluteCoords()) {
            sum += coord[1];
            count++;
        }
        return count == 0 ? 0 : Math.round(sum / (float) count);
    }

    // Generate a random piece
    private Piece generateRandomPiece() {
        Tetromino[] types = Tetromino.values();
        Tetromino randomType = types[new Random().nextInt(types.length)];
        return new Piece(randomType);
    }

    // Spawn new piece
    private void spawnNewPiece() {
        // Use nextPiece and generate new nextPiece
        currentPiece = nextPiece;
        nextPiece = generateRandomPiece();

        // Update view
        panel.setCurrentPiece(currentPiece);

        // If new piece collision, game over
        if (!board.isValidMove(currentPiece)) {
            isGameOver = true;
            gameLoop.stop();
            secondTimer.stop();
            System.out.println("Game Over!");
        }
    }

    // Getters for UI
    public int getScore() {
        return score;
    }

    public int getSecondsElapsed() {
        return secondsElapsed;
    }

    public Piece getNextPiece() {
        return nextPiece;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty == null || this.difficulty == difficulty) {
            return;
        }
        this.difficulty = difficulty;
        gameLoop.setDelay(difficulty.getFallDelayMs());
        gameLoop.setInitialDelay(difficulty.getFallDelayMs());
        panel.repaint();
    }

    // Move piece left
    public void movePieceLeft() {
        if (isGameOver || isPaused)
            return;
        handleMove(0, -1);
    }

    // Move piece right
    public void movePieceRight() {
        if (isGameOver || isPaused)
            return;
        handleMove(0, 1);
    }

    // Rotate piece
    public void rotatePiece() {
        if (isGameOver || isPaused)
            return;
        currentPiece.rotate();
        if (!board.isValidMove(currentPiece)) {
            // If collision, cancel rotation
            currentPiece.undoRotate();
        }
        panel.repaint();
    }

    // Drop piece
    public void dropPiece() {
        if (isGameOver || isPaused)
            return;
        while (board.isValidMove(currentPiece)) {
            currentPiece.move(1, 0);
        }
        // Move back to last valid position
        currentPiece.move(-1, 0);
        freezeAndSpawn();
    }

    // Handle move
    private void handleMove(int dx, int dy) {
        currentPiece.move(dx, dy);
        if (!board.isValidMove(currentPiece)) {
            currentPiece.move(-dx, -dy); // Collision
        }
        panel.repaint();
    }

}