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
    private Timer timer;
    private boolean isGameOver = false;
    private int score = 0;

    public GameEngine(Board board, GamePanel panel) {
        this.board = board;
        this.panel = panel;
        spawnNewPiece();

        // Set timer
        timer = new Timer(500, e -> update());
    }

    // Start the game
    public void start() {
        timer.start();
    }

    // Update the piece (down)
    public void update() {
        if (isGameOver)
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
            case 1: score += 100; break;
            case 2: score += 300; break;
            case 3: score += 500; break;
            case 4: score += 800; break;
        }
        panel.setScore(score);
    }

    // Spawn new piece
    private void spawnNewPiece() {
        // Random type
        Tetromino[] types = Tetromino.values();
        Tetromino randomType = types[new Random().nextInt(types.length)];
        currentPiece = new Piece(randomType);

        // Update view
        panel.setCurrentPiece(currentPiece);

        // If new piece collision, game over
        if (!board.isValidMove(currentPiece)) {
            isGameOver = true;
            timer.stop();
            System.out.println("Game Over!");
        }
    }

    // Move piece left
    public void movePieceLeft() {
        handleMove(0, -1);
    }

    // Move piece right
    public void movePieceRight() {
        handleMove(0, 1);
    }

    // Rotate piece
    public void rotatePiece() {
        currentPiece.rotate();
        if (!board.isValidMove(currentPiece)) {
            // If collision, cancel rotation
            currentPiece.undoRotate();
        }
        panel.repaint();
    }

    // Drop piece
    public void dropPiece() {
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