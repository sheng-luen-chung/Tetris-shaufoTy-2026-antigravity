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

    // Set up the board grid for T-spin tutorial levels
    public void setupTutorialLevel(int level) {
        clear();
        if (level == 1) { // TSS (T-Spin Single)
            // Row 17: Overhang on col 2, entry path at cols 3-4
            for (int c = 0; c < COLS; c++) {
                if (c != 3 && c != 4) {
                    grid[17][c] = Color.GRAY;
                }
            }
            // Row 18: Target line. Empty at cols 2, 3, 4.
            for (int c = 0; c < COLS; c++) {
                if (c != 2 && c != 3 && c != 4) {
                    grid[18][c] = Color.GRAY;
                }
            }
            // Row 19: Bottom line. Empty at col 3 (pointing part) and col 0 (prevent clear)
            for (int c = 0; c < COLS; c++) {
                if (c != 0 && c != 3) {
                    grid[19][c] = Color.GRAY;
                }
            }
        } else if (level == 2) { // TSD Right (T-Spin Double Right-facing)
            // Row 17: Overhang on col 2, entry path at cols 3-4
            for (int c = 0; c < COLS; c++) {
                if (c != 3 && c != 4) {
                    grid[17][c] = Color.GRAY;
                }
            }
            // Row 18: Target line 1. Empty at cols 2, 3, 4.
            for (int c = 0; c < COLS; c++) {
                if (c != 2 && c != 3 && c != 4) {
                    grid[18][c] = Color.GRAY;
                }
            }
            // Row 19: Target line 2. Empty at col 3.
            for (int c = 0; c < COLS; c++) {
                if (c != 3) {
                    grid[19][c] = Color.GRAY;
                }
            }
        } else if (level == 3) { // TSD Left (T-Spin Double Left-facing)
            // Row 17: Overhang on col 7, entry path at cols 5-6
            for (int c = 0; c < COLS; c++) {
                if (c != 5 && c != 6) {
                    grid[17][c] = Color.GRAY;
                }
            }
            // Row 18: Target line 1. Empty at cols 5, 6, 7.
            for (int c = 0; c < COLS; c++) {
                if (c != 5 && c != 6 && c != 7) {
                    grid[18][c] = Color.GRAY;
                }
            }
            // Row 19: Target line 2. Empty at col 6.
            for (int c = 0; c < COLS; c++) {
                if (c != 6) {
                    grid[19][c] = Color.GRAY;
                }
            }
        }
    }

    // Insert a garbage line at the bottom, shifting all other rows up.
    public void addGarbageLine(int holeCol) {
        // Shift rows 0 to ROWS-2 up by one (row 0 gets discarded)
        for (int r = 0; r < ROWS - 1; r++) {
            System.arraycopy(grid[r + 1], 0, grid[r], 0, COLS);
        }
        
        // Populate the bottom row (ROWS - 1) with gray blocks except for the hole
        for (int c = 0; c < COLS; c++) {
            if (c == holeCol) {
                grid[ROWS - 1][c] = null;
            } else {
                grid[ROWS - 1][c] = Color.GRAY;
            }
        }
    }
}