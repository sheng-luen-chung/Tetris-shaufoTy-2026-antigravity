package com.tetris.model;

public class Piece {
    /*
     * Piece class: Represents the current active tetromino
     */

    // Properties
    private Tetromino type;
    private int row, col;
    private int rotationIndex;

    // Piece constructor
    public Piece(Tetromino type) {
        this.type = type;
        this.rotationIndex = 0;

        // Initialize position at the top-center (for 4x4 grid)
        this.row = 0;
        this.col = 3;
    }

    // Piece move methods
    public void move(int dr, int dc) {
        this.row += dr;
        this.col += dc;
    }

    // Piece rotate methods
    public void rotate() {
        rotationIndex = (rotationIndex + 1) % 4;
    }

    // Piece undo rotate methods
    public void undoRotate() {
        rotationIndex = (rotationIndex - 1 + 4) % 4;
    }

    // Get tetromino type
    public Tetromino getType() {
        return type;
    }

    // Get the relative coordinates of the tetromino
    public int[][] getRotatedCoords() {
        int[][] baseCoords = type.getCoords();
        int[][] rotated = new int[4][2];

        if (type == Tetromino.O) { // Special case: O (doesn't rotate)
            return baseCoords;
        }

        // Rotate algorithm
        for (int i = 0; i < 4; i++) {
            int newR = baseCoords[i][0];
            int newC = baseCoords[i][1];

            /*
             * Rotate n times 90 degrees clockwise in (row, col) space
             * 0: 0 degrees
             * 1: 90 degrees
             * 2: 180 degrees
             * 3: 270 degrees
             */
            for (int r = 0; r < rotationIndex; r++) {
                int cR = 1; // Center Row
                int cC = 1; // Center Col

                int tempR = newR;
                newR = cR + (newC - cC);
                newC = cC - (tempR - cR);
            }

            rotated[i][0] = newR;
            rotated[i][1] = newC;
        }
        return rotated;
    }

    public int[][] getAbsoluteCoords() {
        int[][] relative = getRotatedCoords();
        int[][] absolute = new int[4][2];

        for (int i = 0; i < 4; i++) {
            // relative[i][0] is row, relative[i][1] is col
            // absolute[i][0] should be col, absolute[i][1] should be row
            absolute[i][0] = this.col + relative[i][1];
            absolute[i][1] = this.row + relative[i][0];
        }
        return absolute;
    }
}