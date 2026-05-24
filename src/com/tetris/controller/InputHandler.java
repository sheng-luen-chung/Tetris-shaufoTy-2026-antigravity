package com.tetris.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputHandler extends KeyAdapter {
    private final GameEngine engine;

    private GameEngine engine2 = null;

    // Movement state flags for Player 1
    private boolean p1LeftPressed = false;
    private boolean p1RightPressed = false;
    private boolean p1DownPressed = false;
    private int p1LeftHoldTime = 0;
    private int p1RightHoldTime = 0;
    private int p1DownHoldTime = 0;
    private int p1LeftRepeatCounter = 0;
    private int p1RightRepeatCounter = 0;

    // Movement state flags for Player 2
    private boolean p2LeftPressed = false;
    private boolean p2RightPressed = false;
    private boolean p2DownPressed = false;
    private int p2LeftHoldTime = 0;
    private int p2RightHoldTime = 0;
    private int p2DownHoldTime = 0;
    private int p2LeftRepeatCounter = 0;
    private int p2RightRepeatCounter = 0;

    // Backward compatibility for single player
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean downPressed = false;
    private int leftHoldTime = 0;
    private int rightHoldTime = 0;
    private int downHoldTime = 0;
    private int leftRepeatCounter = 0;
    private int rightRepeatCounter = 0;

    // Professional Tetris Tuning Constants
    private static final int DAS_DELAY_MS = 170;        // Slightly increased for better control
    private static final int ARR_RATE_MS = 45;          // Increased to slow down horizontal movement
    private static final int SOFT_DROP_INTERVAL_MS = 30; // Kept fast for soft drop responsiveness

    private final javax.swing.Timer inputTimer;
    private long lastTickTime = System.currentTimeMillis();

    public InputHandler(GameEngine engine) {
        this.engine = engine;

        // High-frequency input polling loop (10ms for lower perceived latency)
        this.inputTimer = new javax.swing.Timer(10, e -> {
            long now = System.currentTimeMillis();
            int deltaTime = (int) (now - lastTickTime);
            lastTickTime = now;

            GameEngine.GameState state = engine.getGameState();
            boolean isActiveState = (state == GameEngine.GameState.PLAYING || state == GameEngine.GameState.TUTORIAL);
            if (isActiveState) {
                if (engine.getGameMode() == com.tetris.model.GameMode.PVP) {
                    if (engine2 != null && !engine.isPaused()) {
                        if (!engine.isGameOver()) {
                            updateP1Inputs(deltaTime);
                            engine.tickLockDelay();
                        } else {
                            resetP1KeyStates();
                        }

                        if (!engine2.isGameOver()) {
                            updateP2Inputs(deltaTime);
                            engine2.tickLockDelay();
                        } else {
                            resetP2KeyStates();
                        }
                    }
                } else {
                    if (!engine.isGameOver() && !engine.isPaused() && !engine.isTransitioning()) {
                        updateInputs(deltaTime);
                        engine.tickLockDelay();
                    } else {
                        resetKeyStates();
                        lastTickTime = now;
                    }
                }
            } else {
                resetKeyStates();
                lastTickTime = now;
            }
        });
        this.inputTimer.start();
    }

    public void setEngine2(GameEngine engine2) {
        this.engine2 = engine2;
    }

    private void updateP1Inputs(int deltaTime) {
        // Continuous left movement (DAS)
        if (p1LeftPressed && !p1RightPressed) {
            int oldHoldTime = p1LeftHoldTime;
            p1LeftHoldTime += deltaTime;
            if (p1LeftHoldTime >= DAS_DELAY_MS) {
                if (oldHoldTime < DAS_DELAY_MS) {
                    engine.movePieceLeft();
                }
                p1LeftRepeatCounter += deltaTime;
                while (p1LeftRepeatCounter >= ARR_RATE_MS) {
                    engine.movePieceLeft();
                    p1LeftRepeatCounter -= ARR_RATE_MS;
                }
            }
        } else {
            p1LeftHoldTime = 0;
            p1LeftRepeatCounter = 0;
        }

        // Continuous right movement (DAS)
        if (p1RightPressed && !p1LeftPressed) {
            int oldHoldTime = p1RightHoldTime;
            p1RightHoldTime += deltaTime;
            if (p1RightHoldTime >= DAS_DELAY_MS) {
                if (oldHoldTime < DAS_DELAY_MS) {
                    engine.movePieceRight();
                }
                p1RightRepeatCounter += deltaTime;
                while (p1RightRepeatCounter >= ARR_RATE_MS) {
                    engine.movePieceRight();
                    p1RightRepeatCounter -= ARR_RATE_MS;
                }
            }
        } else {
            p1RightHoldTime = 0;
            p1RightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (p1DownPressed) {
            p1DownHoldTime += deltaTime;
            while (p1DownHoldTime >= SOFT_DROP_INTERVAL_MS) {
                engine.softDrop();
                p1DownHoldTime -= SOFT_DROP_INTERVAL_MS;
            }
        } else {
            p1DownHoldTime = 0;
        }
    }

    private void updateP2Inputs(int deltaTime) {
        if (engine2 == null) return;
        
        // Continuous left movement (DAS)
        if (p2LeftPressed && !p2RightPressed) {
            int oldHoldTime = p2LeftHoldTime;
            p2LeftHoldTime += deltaTime;
            if (p2LeftHoldTime >= DAS_DELAY_MS) {
                if (oldHoldTime < DAS_DELAY_MS) {
                    engine2.movePieceLeft();
                }
                p2LeftRepeatCounter += deltaTime;
                while (p2LeftRepeatCounter >= ARR_RATE_MS) {
                    engine2.movePieceLeft();
                    p2LeftRepeatCounter -= ARR_RATE_MS;
                }
            }
        } else {
            p2LeftHoldTime = 0;
            p2LeftRepeatCounter = 0;
        }

        // Continuous right movement (DAS)
        if (p2RightPressed && !p2LeftPressed) {
            int oldHoldTime = p2RightHoldTime;
            p2RightHoldTime += deltaTime;
            if (p2RightHoldTime >= DAS_DELAY_MS) {
                if (oldHoldTime < DAS_DELAY_MS) {
                    engine2.movePieceRight();
                }
                p2RightRepeatCounter += deltaTime;
                while (p2RightRepeatCounter >= ARR_RATE_MS) {
                    engine2.movePieceRight();
                    p2RightRepeatCounter -= ARR_RATE_MS;
                }
            }
        } else {
            p2RightHoldTime = 0;
            p2RightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (p2DownPressed) {
            p2DownHoldTime += deltaTime;
            while (p2DownHoldTime >= SOFT_DROP_INTERVAL_MS) {
                engine2.softDrop();
                p2DownHoldTime -= SOFT_DROP_INTERVAL_MS;
            }
        } else {
            p2DownHoldTime = 0;
        }
    }

    private void updateInputs(int deltaTime) {
        // Continuous left movement (DAS)
        if (leftPressed && !rightPressed) {
            int oldHoldTime = leftHoldTime;
            leftHoldTime += deltaTime;
            if (leftHoldTime >= DAS_DELAY_MS) {
                // If we just passed DAS threshold, trigger a move immediately
                if (oldHoldTime < DAS_DELAY_MS) {
                    engine.movePieceLeft();
                }
                
                leftRepeatCounter += deltaTime;
                while (leftRepeatCounter >= ARR_RATE_MS) {
                    engine.movePieceLeft();
                    leftRepeatCounter -= ARR_RATE_MS;
                }
            }
        } else {
            leftHoldTime = 0;
            leftRepeatCounter = 0;
        }

        // Continuous right movement (DAS)
        if (rightPressed && !leftPressed) {
            int oldHoldTime = rightHoldTime;
            rightHoldTime += deltaTime;
            if (rightHoldTime >= DAS_DELAY_MS) {
                // If we just passed DAS threshold, trigger a move immediately
                if (oldHoldTime < DAS_DELAY_MS) {
                    engine.movePieceRight();
                }

                rightRepeatCounter += deltaTime;
                while (rightRepeatCounter >= ARR_RATE_MS) {
                    engine.movePieceRight();
                    rightRepeatCounter -= ARR_RATE_MS;
                }
            }
        } else {
            rightHoldTime = 0;
            rightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (downPressed) {
            downHoldTime += deltaTime;
            while (downHoldTime >= SOFT_DROP_INTERVAL_MS) {
                engine.softDrop();
                downHoldTime -= SOFT_DROP_INTERVAL_MS;
            }
        } else {
            downHoldTime = 0;
        }
    }

    private void resetP1KeyStates() {
        p1LeftPressed = false;
        p1RightPressed = false;
        p1DownPressed = false;
        p1LeftHoldTime = 0;
        p1RightHoldTime = 0;
        p1DownHoldTime = 0;
        p1LeftRepeatCounter = 0;
        p1RightRepeatCounter = 0;
    }

    private void resetP2KeyStates() {
        p2LeftPressed = false;
        p2RightPressed = false;
        p2DownPressed = false;
        p2LeftHoldTime = 0;
        p2RightHoldTime = 0;
        p2DownHoldTime = 0;
        p2LeftRepeatCounter = 0;
        p2RightRepeatCounter = 0;
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
        resetP1KeyStates();
        resetP2KeyStates();
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
                case KeyEvent.VK_UP:
                    engine.navigateLeaderboardModes(-1);
                    break;
                case KeyEvent.VK_DOWN:
                    engine.navigateLeaderboardModes(1);
                    break;
            }
        } else if (state == GameEngine.GameState.PLAYING || state == GameEngine.GameState.TUTORIAL) {
            if (engine.isTransitioning()) {
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    engine.returnToMenu();
                }
                return;
            }

            if (engine.isEnteringName()) {
                char keyChar = e.getKeyChar();
                if (keyCode == KeyEvent.VK_ENTER) {
                    if (engine.getNameInputBuffer().length() > 0) {
                        engine.submitLeaderboardName();
                    }
                } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                    engine.backspaceNameInput();
                } else if (keyCode == KeyEvent.VK_ESCAPE) {
                    engine.submitDefaultName();
                } else {
                    if (Character.isLetterOrDigit(keyChar) || keyChar == ' ' || keyChar == '_' || keyChar == '-') {
                        engine.appendNameInput(keyChar);
                    }
                }
                return;
            }

            boolean bothGameOver = false;
            if (engine.getGameMode() == com.tetris.model.GameMode.PVP && engine2 != null) {
                bothGameOver = engine.isGameOver() && engine2.isGameOver();
            } else {
                bothGameOver = engine.isGameOver();
            }

            if (bothGameOver) {
                if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_SPACE) {
                    engine.returnToMenu();
                }
                return;
            }

            if (engine.isPaused()) {
                switch (keyCode) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        engine.navigatePauseMenu(-1);
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        engine.navigatePauseMenu(1);
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE:
                        engine.selectPauseMenuItem();
                        break;
                    case KeyEvent.VK_P:
                    case KeyEvent.VK_ESCAPE:
                        engine.togglePause();
                        resetKeyStates();
                        break;
                }
                return;
            }

            if (engine.getGameMode() == com.tetris.model.GameMode.PVP) {
                // PVP Controls
                // Player 1 (WASD)
                if (keyCode == KeyEvent.VK_A) {
                    if (!p1LeftPressed) {
                        p1LeftPressed = true;
                        p1LeftHoldTime = 0;
                        p1LeftRepeatCounter = 0;
                        engine.movePieceLeft();
                    }
                } else if (keyCode == KeyEvent.VK_D) {
                    if (!p1RightPressed) {
                        p1RightPressed = true;
                        p1RightHoldTime = 0;
                        p1RightRepeatCounter = 0;
                        engine.movePieceRight();
                    }
                } else if (keyCode == KeyEvent.VK_S) {
                    if (!p1DownPressed) {
                        p1DownPressed = true;
                        p1DownHoldTime = 0;
                        engine.softDrop();
                    }
                } else if (keyCode == KeyEvent.VK_W) {
                    engine.rotatePiece();
                } else if (keyCode == KeyEvent.VK_SPACE) {
                    engine.dropPiece();
                    resetP1KeyStates();
                } else if (keyCode == KeyEvent.VK_C || keyCode == KeyEvent.VK_SHIFT) {
                    engine.holdPiece();
                }
                
                // Player 2 (Arrows)
                if (engine2 != null) {
                    if (keyCode == KeyEvent.VK_LEFT) {
                        if (!p2LeftPressed) {
                            p2LeftPressed = true;
                            p2LeftHoldTime = 0;
                            p2LeftRepeatCounter = 0;
                            engine2.movePieceLeft();
                        }
                    } else if (keyCode == KeyEvent.VK_RIGHT) {
                        if (!p2RightPressed) {
                            p2RightPressed = true;
                            p2RightHoldTime = 0;
                            p2RightRepeatCounter = 0;
                            engine2.movePieceRight();
                        }
                    } else if (keyCode == KeyEvent.VK_DOWN) {
                        if (!p2DownPressed) {
                            p2DownPressed = true;
                            p2DownHoldTime = 0;
                            engine2.softDrop();
                        }
                    } else if (keyCode == KeyEvent.VK_UP) {
                        engine2.rotatePiece();
                    } else if (keyCode == KeyEvent.VK_ENTER) {
                        engine2.dropPiece();
                        resetP2KeyStates();
                    } else if (keyCode == KeyEvent.VK_NUMPAD0 || keyCode == KeyEvent.VK_SLASH) {
                        engine2.holdPiece();
                    }
                }

                // Global PVP pause
                if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE) {
                    engine.togglePause();
                    resetKeyStates();
                }
                return;
            }

            if (keyCode == KeyEvent.VK_A) {
                engine.setAiPlay(!engine.isAiPlay());
                return;
            }

            if (engine.isAiPlay()) {
                if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE) {
                    engine.togglePause();
                    resetKeyStates();
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
                        engine.softDrop();
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
        GameEngine.GameState state = engine.getGameState();
        if (state == GameEngine.GameState.PLAYING || state == GameEngine.GameState.TUTORIAL) {
            if (engine.getGameMode() == com.tetris.model.GameMode.PVP) {
                switch (keyCode) {
                    case KeyEvent.VK_A:
                        p1LeftPressed = false;
                        break;
                    case KeyEvent.VK_D:
                        p1RightPressed = false;
                        break;
                    case KeyEvent.VK_S:
                        p1DownPressed = false;
                        break;
                    case KeyEvent.VK_LEFT:
                        p2LeftPressed = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                        p2RightPressed = false;
                        break;
                    case KeyEvent.VK_DOWN:
                        p2DownPressed = false;
                        break;
                }
            } else {
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
}
