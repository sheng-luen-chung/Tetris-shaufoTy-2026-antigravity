package com.tetris.model;

public class Piece {
    /*
     * Piece class: Represents the current active tetromino
     */

    // Properties
    private Tetromino type;
    private int row, col;
    private int rotationIndex;
    
    // Cache absolute coordinates to reduce GC pressure
    private final int[][] absoluteCoords = new int[4][2];
    private boolean coordsDirty = true;

    // Piece constructor
    public Piece(Tetromino type) {
        this.type = type;
        this.rotationIndex = 0;

        // Initialize position at the top-center (for 4x4 grid)
        this.row = 0;
        this.col = 3;
        this.coordsDirty = true;
    }

    // Create a piece with explicit position/rotation (ghost piece)
    public Piece(Tetromino type, int row, int col, int rotationIndex) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.rotationIndex = ((rotationIndex % 4) + 4) % 4;
        this.coordsDirty = true;
    }

    // Piece move methods
    public void move(int dr, int dc) {
        this.row += dr;
        this.col += dc;
        this.coordsDirty = true;
    }

    // Piece rotate methods
    public void rotate() {
        rotationIndex = (rotationIndex + 1) % 4;
        this.coordsDirty = true;
    }

    // Piece undo rotate methods
    public void undoRotate() {
        rotationIndex = (rotationIndex - 1 + 4) % 4;
        this.coordsDirty = true;
    }

    // Get tetromino type
    public Tetromino getType() {
        return type;
    }

    // Get the relative coordinates of the tetromino
    public int[][] getRotatedCoords() {
        int[][] baseCoords = type.getCoords();

        if (type == Tetromino.O) { // Special case: O (doesn't rotate)
            return baseCoords;
        }

        if (type == Tetromino.I) { // Special case: I (Standard SRS rotation to prevent wobble)
            int[][][] iRotations = {
                { { 1, 0 }, { 1, 1 }, { 1, 2 }, { 1, 3 } }, // 0
                { { 0, 2 }, { 1, 2 }, { 2, 2 }, { 3, 2 } }, // 90 CW
                { { 2, 0 }, { 2, 1 }, { 2, 2 }, { 2, 3 } }, // 180
                { { 0, 1 }, { 1, 1 }, { 2, 1 }, { 3, 1 } }  // 270 CW
            };
            return iRotations[rotationIndex];
        }

        int[][] rotated = new int[4][2];
        // Rotate algorithm for 3x3 pieces (T, J, L, S, Z) around (1,1)
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
        if (coordsDirty) {
            int[][] relative = getRotatedCoords();
            for (int i = 0; i < 4; i++) {
                // relative[i][0] is row, relative[i][1] is col
                // absolute[i][0] should be col, absolute[i][1] should be row
                absoluteCoords[i][0] = this.col + relative[i][1];
                absoluteCoords[i][1] = this.row + relative[i][0];
            }
            coordsDirty = false;
        }
        return absoluteCoords;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getRotationIndex() {
        return rotationIndex;
    }
}