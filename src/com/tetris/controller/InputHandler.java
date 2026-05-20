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
        GameEngine.GameState state = engine.getGameState();

        if (state == GameEngine.GameState.MENU) {
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    engine.navigateMenuUp();
                    break;
                case KeyEvent.VK_DOWN:
                    engine.navigateMenuDown();
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    engine.selectMenuItem();
                    break;
                case KeyEvent.VK_ESCAPE:
                    System.exit(0);
                    break;
            }
        } else if (state == GameEngine.GameState.LEADERBOARD) {
            switch (keyCode) {
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_ESCAPE:
                    engine.returnToMenu();
                    break;
            }
        } else if (state == GameEngine.GameState.PLAYING) {
            if (engine.isGameOver()) {
                if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_SPACE) {
                    engine.returnToMenu();
                }
                return;
            }

            if (engine.isPaused()) {
                switch (keyCode) {
                    case KeyEvent.VK_UP:
                        engine.navigatePauseMenu(-1);
                        break;
                    case KeyEvent.VK_DOWN:
                        engine.navigatePauseMenu(1);
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE:
                        engine.selectPauseMenuItem();
                        break;
                    case KeyEvent.VK_P:
                    case KeyEvent.VK_ESCAPE:
                        engine.togglePause();
                        break;
                }
                return;
            }

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

                case KeyEvent.VK_C:
                case KeyEvent.VK_SHIFT:
                    engine.holdPiece();
                    break;
            }
        }
    }
}
