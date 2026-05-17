package com.tetris.controller;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import com.tetris.view.GamePanel;
import javax.swing.Timer;
import java.util.Random;

public class GameEngine {
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

    public GameEngine(Board board, GamePanel panel) {
        this.board = board;
        this.panel = panel;

        // Initialize pieces
        nextPiece = generateRandomPiece();
        spawnNewPiece();

        // Set game loop (falling piece)
        gameLoop = new Timer(500, e -> {
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
        board.freezePiece(currentPiece); // Freeze
        int lines = board.clearLines(); // Clear lines
        if (lines > 0) {
            updateScore(lines);
        }
        spawnNewPiece(); // Spawn
    }

    private void updateScore(int lines) {
        switch (lines) {
            case 1:
                score += 100;
                break;
            case 2:
                score += 300;
                break;
            case 3:
                score += 500;
                break;
            case 4:
                score += 800;
                break;
        }
        panel.setScore(score);
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