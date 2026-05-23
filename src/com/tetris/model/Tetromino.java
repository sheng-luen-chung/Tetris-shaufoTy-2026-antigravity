package com.tetris.model;

import java.awt.Color;

public enum Tetromino {
    /*
     * Define the relative coordinates of each shape (tetromino)
     * Seven basic tetromino types: I、O、T、S、Z、J、L
     * shapes are in a 4 x 4 grid
     */

    // Shape definition (relative coordinates)
    I(new int[][] { { 1, 0 }, { 1, 1 }, { 1, 2 }, { 1, 3 } }, Color.CYAN),
    J(new int[][] { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 1, 2 } }, Color.BLUE),
    L(new int[][] { { 0, 2 }, { 1, 0 }, { 1, 1 }, { 1, 2 } }, Color.ORANGE),
    O(new int[][] { { 0, 1 }, { 0, 2 }, { 1, 1 }, { 1, 2 } }, Color.YELLOW),
    S(new int[][] { { 0, 1 }, { 0, 2 }, { 1, 0 }, { 1, 1 } }, Color.GREEN),
    T(new int[][] { { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1, 2 } }, Color.MAGENTA),
    Z(new int[][] { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 2 } }, Color.RED);

    // Tetromino properties
    private final int[][] coords;
    private final Color color;

    // Tetromino constructor
    Tetromino(int[][] coords, Color color) {
        this.coords = coords;
        this.color = color;
    }

    // Get Tetromino coordinates (relative)
    public int[][] getCoords() {
        return coords;
    }

    // Get Tetromino color
    public Color getColor() {
        return color;
    }

}