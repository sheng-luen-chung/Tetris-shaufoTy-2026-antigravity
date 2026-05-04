package com.tetris.model;

import java.awt.Color;

public enum Tetromino {
    /*  Define the relative coordinates of each shape (tetromino)
        1. Seven basic tetromino types: I、O、T、S、Z、J、L
        2. The four coordinates are (r1, c1), (r2, c2), (r3, c3), (r4, c4)
    */
    I(new int[][] {{0, 1}, {1, 1}, {2, 1}, {3, 1}}, Color.CYAN);
    J(new int[][] {{0, 1}, {1, 1}, {2, 0}, {2, 1}}, Color.BLUE);
    L(new int[][] {{0, 0}, {1, 0}, {2, 0}, {2, 1}}, Color.ORANGE);
    O(new int[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}}, Color.YELLOW);
    S(new int[][] {{0, 1}, {0, 2}, {1, 0}, {1, 1}}, Color.GREEN);
    T(new int[][] {{0, 0}, {0, 1}, {0, 2}, {1, 1}}, Color.MAGENTA);
    Z(new int[][] {{0, 0}, {0, 1}, {1, 1}, {1, 2}}, Color.RED);

    private final int[][] coords;
    private final Color color;

    Tetromino(int [][] coords, Color color) {
        this.coords = coords;
        this.color = color;
    }

    public int[][] getCoords() {
        return coords;
    }

    public Color getColor() {
        return color;
    }

}