package com.tetris.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputHandler extends KeyAdapter {
    private final GameEngine engine;

    // Movement state flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean downPressed = false;

    // Hold time accumulators (ms)
    private int leftHoldTime = 0;
    private int rightHoldTime = 0;
    private int downHoldTime = 0;

    // Repeat rate counters
    private int leftRepeatCounter = 0;
    private int rightRepeatCounter = 0;

    // Professional Tetris Tuning Constants
    private static final int DAS_DELAY_MS = 180;        // Delay before auto-repeat begins
    private static final int ARR_RATE_MS = 40;          // Auto-repeat rate (moves per ms)
    private static final int SOFT_DROP_INTERVAL_MS = 45; // Soft drop vertical speed

    private final javax.swing.Timer inputTimer;

    public InputHandler(GameEngine engine) {
        this.engine = engine;

        // 60 FPS high-frequency input polling loop
        this.inputTimer = new javax.swing.Timer(16, e -> {
            if (engine.getGameState() == GameEngine.GameState.PLAYING && 
                !engine.isGameOver() && !engine.isPaused()) {
                updateInputs();
                engine.tickLockDelay(); // Tick lock delay every frame
            } else {
                resetKeyStates();
            }
        });
        this.inputTimer.start();
    }

    private void updateInputs() {
        // Continuous left movement (DAS)
        if (leftPressed && !rightPressed) {
            leftHoldTime += 16;
            if (leftHoldTime >= DAS_DELAY_MS) {
                leftRepeatCounter += 16;
                if (leftRepeatCounter >= ARR_RATE_MS) {
                    engine.movePieceLeft();
                    leftRepeatCounter = 0;
                }
            }
        } else {
            leftHoldTime = 0;
            leftRepeatCounter = 0;
        }

        // Continuous right movement (DAS)
        if (rightPressed && !leftPressed) {
            rightHoldTime += 16;
            if (rightHoldTime >= DAS_DELAY_MS) {
                rightRepeatCounter += 16;
                if (rightRepeatCounter >= ARR_RATE_MS) {
                    engine.movePieceRight();
                    rightRepeatCounter = 0;
                }
            }
        } else {
            rightHoldTime = 0;
            rightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (downPressed) {
            downHoldTime += 16;
            if (downHoldTime >= SOFT_DROP_INTERVAL_MS) {
                engine.update();
                downHoldTime = 0;
            }
        } else {
            downHoldTime = 0;
        }
    }

    private void resetKeyStates() {
        leftPressed = false;
        rightPressed = false;
        downPressed = false;
        leftHoldTime = 0;
        rightHoldTime = 0;
        downHoldTime = 0;
        leftRepeatCounter = 0;
        rightRepeatCounter = 0;
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
                    engine.handleBackAction();
                    break;
            }
        } else if (state == GameEngine.GameState.LEADERBOARD) {
            switch (keyCode) {
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_ESCAPE:
                    engine.handleBackAction();
                    break;
                case KeyEvent.VK_LEFT:
                    engine.navigateLeaderboardTabs(-1);
                    break;
                case KeyEvent.VK_RIGHT:
                    engine.navigateLeaderboardTabs(1);
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
                        engine.togglePause();
                        resetKeyStates();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        engine.handleBackAction();
                        resetKeyStates();
                        break;
                }
                return;
            }

            switch (keyCode) {
                case KeyEvent.VK_LEFT:
                    if (!leftPressed) {
                        leftPressed = true;
                        leftHoldTime = 0;
                        leftRepeatCounter = 0;
                        engine.movePieceLeft();
                    }
                    break;

                case KeyEvent.VK_RIGHT:
                    if (!rightPressed) {
                        rightPressed = true;
                        rightHoldTime = 0;
                        rightRepeatCounter = 0;
                        engine.movePieceRight();
                    }
                    break;

                case KeyEvent.VK_DOWN:
                    if (!downPressed) {
                        downPressed = true;
                        downHoldTime = 0;
                        engine.update();
                    }
                    break;

                case KeyEvent.VK_UP:
                    engine.rotatePiece();
                    break;

                case KeyEvent.VK_SPACE:
                    engine.dropPiece();
                    resetKeyStates(); // Reset keys after hard drop to prevent instant repeats
                    break;

                case KeyEvent.VK_P:
                case KeyEvent.VK_ESCAPE:
                    engine.togglePause();
                    resetKeyStates();
                    break;

                case KeyEvent.VK_C:
                case KeyEvent.VK_SHIFT:
                    engine.holdPiece();
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (engine.getGameState() == GameEngine.GameState.PLAYING) {
            switch (keyCode) {
                case KeyEvent.VK_LEFT:
                    leftPressed = false;
                    break;
                case KeyEvent.VK_RIGHT:
                    rightPressed = false;
                    break;
                case KeyEvent.VK_DOWN:
                    downPressed = false;
                    break;
            }
        }
    }
}
