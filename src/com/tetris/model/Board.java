package com.tetris.model;

import java.awt.Color;

public class Board {
    public static final int ROWS = 20;
    public static final int COLS = 10;

    // Use color matrix to represent the board
    private Color[][] grid;

    public Board() {
        grid = new Color[ROWS][COLS];
    }

    // Clear the board grid
    public void clear() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = null;
            }
        }
    }

    // Color getter for grid
    public Color[][] getGrid() {
        return grid;
    }

    // Check if a cell is occupied (out of bounds or colored)
    public boolean isOccupied(int row, int col) {
        if (col < 0 || col >= COLS || row >= ROWS) {
            return true; // Walls and floor count as occupied
        }
        if (row < 0) {
            return false; // Ceiling is not occupied
        }
        return grid[row][col] != null;
    }

    // Check if the move is valid
    public boolean isValidMove(Piece piece) {
        for (int[] cell : piece.getAbsoluteCoords()) {
            int col = cell[0];
            int row = cell[1];

            // 1. Check left & right boundaries
            if (col < 0 || col >= COLS)
                return false;

            // 2. Check bottom boundaries
            if (row >= ROWS)
                return false;

            // 3. Check collision with existing blocks
            if (row >= 0 && grid[row][col] != null)
                return false;
        }
        return true;
    }

    // Freeze piece
    public void freezePiece(Piece piece) {
        for (int[] cell : piece.getAbsoluteCoords()) {
            int col = cell[0];
            int row = cell[1];
            if (row >= 0) {
                grid[row][col] = piece.getType().getColor();
            }
        }
    }

    // Check if a line is full
    private boolean isLineFull(int row) {
        for (int col = 0; col < COLS; col++) {
            if (grid[row][col] == null)
                return false;
        }
        return true;
    }

    // Clear full lines
    public int clearLines() {
        int linesCleared = 0;

        for (int row = ROWS - 1; row >= 0; row--) {
            if (isLineFull(row)) {
                removeLine(row);
                linesCleared++;
                row++;
            }
        }
        return linesCleared;
    }

    // Remove a line
    private void removeLine(int row) {
        // Move all lines above this line down by one
        for (int r = row; r > 0; r--) {
            System.arraycopy(grid[r - 1], 0, grid[r], 0, COLS);
        }

        // Clear the top line
        for (int col = 0; col < COLS; col++) {
            grid[0][col] = null;
        }
    }
}