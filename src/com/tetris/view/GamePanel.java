package com.tetris.view;

import javax.swing.JPanel;

import com.tetris.model.Board;
import com.tetris.model.Piece;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;

// Game panel
public class GamePanel extends JPanel {
    public static final int TILE_SIZE = 30;
    public static final int COLS = 10;
    public static final int ROWS = 20;

    private Board board;
    private Piece currentPiece;
    private int score = 0;

    // Game panel constructor
    public GamePanel(Board board) {
        this.board = board;

        // Set the size of the game panel
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        setBackground(Color.BLACK);
    }

    // Set the current piece
    public void setCurrentPiece(Piece piece) {
        this.currentPiece = piece;
    }

    // Set the score
    public void setScore(int score) {
        this.score = score;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawGrid(g);
        drawFixedBlocks(g);
        drawCurrentPiece(g);
        drawScore(g);
    }

    // Draw the grid
    private void drawGrid(Graphics g) {
        g.setColor(Color.GRAY);
        for (int i = 0; i <= COLS; i++) {
            g.drawLine(i * TILE_SIZE, 0, i * TILE_SIZE, getHeight());
        }
        for (int i = 0; i <= ROWS; i++) {
            g.drawLine(0, i * TILE_SIZE, getWidth(), i * TILE_SIZE);
        }
    }

    // Draw fixed blocks
    private void drawFixedBlocks(Graphics g) {
        Color[][] grid = board.getGrid();

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    drawSquare(g, c, r, grid[r][c]);
                }
            }
        }
    }

    // Draw current piece
    private void drawCurrentPiece(Graphics g) {
        if (currentPiece != null) {
            Color color = currentPiece.getType().getColor();

            // Get the absolute coordinates of the current piece after rotation
            for (int[] coord : currentPiece.getAbsoluteCoords()) {
                drawSquare(g, coord[0], coord[1], color);
            }
        }
    }

    // Draw score
    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 30);
    }

    // Helper method: To draw a square with a border
    private void drawSquare(Graphics g, int x, int y, Color color) {
        if (y < 0)
            return;

        g.setColor(color);
        g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE); // Draw square

        // To make the blocks easier to distinguish,
        // draw a dark border around each block.
        g.setColor(color.darker());
        g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE); // Draw border
    }
}