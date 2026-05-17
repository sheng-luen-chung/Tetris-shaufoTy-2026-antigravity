package com.tetris.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputHandler extends KeyAdapter {
    private GameEngine engine;

    public InputHandler(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_LEFT:
                engine.movePieceLeft();
                break;

            case KeyEvent.VK_RIGHT:
                engine.movePieceRight();
                break;

            case KeyEvent.VK_UP:
                engine.rotatePiece();
                break;

            case KeyEvent.VK_DOWN:
                engine.update();
                break;
                
            case KeyEvent.VK_SPACE:
                engine.dropPiece();
                break;

            case KeyEvent.VK_P:
            case KeyEvent.VK_ESCAPE:
                engine.togglePause();
                break;
        }

    }
}
