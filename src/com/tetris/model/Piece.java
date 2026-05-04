package com.tetris.model;

import java.awt.Point;

public class Piece {
    private Tetromino type;
    private int x, y;
    private int rotation;
}

public Piece(Tetromino type) {
    this.type = type;
    this.rotationIndex = 0;

    // Initialize position at the top-center
    this.x = 3;
    this.y = 0;
}

public void move(int dx, int dy) {
    this.x += dx;
    this.y += dy;
}

public void rotate() {
    rotationIndex = (rotationIndex + 1) % 4;
}