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

    // Professional Tetris Tuning Constants (Configurable)
    private static int dasDelayMs = 170;
    private static int arrRateMs = 45;
    private static int softDropIntervalMs = 30;

    // Player 1 Custom Keys (Left, Right, Down, Rotate, Drop, Hold)
    private static boolean p1Customized = false;
    private static int p1KeyLeft = KeyEvent.VK_LEFT;
    private static int p1KeyRight = KeyEvent.VK_RIGHT;
    private static int p1KeyDown = KeyEvent.VK_DOWN;
    private static int p1KeyRotate = KeyEvent.VK_UP;
    private static int p1KeyDrop = KeyEvent.VK_SPACE;
    private static int p1KeyHold = KeyEvent.VK_C;

    // Player 2 Custom Keys
    private static boolean p2Customized = false;
    private static int p2KeyLeft = KeyEvent.VK_A;
    private static int p2KeyRight = KeyEvent.VK_D;
    private static int p2KeyDown = KeyEvent.VK_S;
    private static int p2KeyRotate = KeyEvent.VK_W;
    private static int p2KeyDrop = KeyEvent.VK_Q;
    private static int p2KeyHold = KeyEvent.VK_E;

    public static int getDasDelayMs() { return dasDelayMs; }
    public static void setDasDelayMs(int val) { dasDelayMs = val; saveConfig(); }
    public static int getArrRateMs() { return arrRateMs; }
    public static void setArrRateMs(int val) { arrRateMs = val; saveConfig(); }
    public static int getSoftDropIntervalMs() { return softDropIntervalMs; }
    public static void setSoftDropIntervalMs(int val) { softDropIntervalMs = val; saveConfig(); }

    public static boolean isP1Customized() { return p1Customized; }
    public static void setP1Customized(boolean val) { p1Customized = val; saveConfig(); }
    public static int getP1KeyLeft() { return p1KeyLeft; }
    public static int getP1KeyRight() { return p1KeyRight; }
    public static int getP1KeyDown() { return p1KeyDown; }
    public static int getP1KeyRotate() { return p1KeyRotate; }
    public static int getP1KeyDrop() { return p1KeyDrop; }
    public static int getP1KeyHold() { return p1KeyHold; }

    public static boolean isP2Customized() { return p2Customized; }
    public static void setP2Customized(boolean val) { p2Customized = val; saveConfig(); }
    public static int getP2KeyLeft() { return p2KeyLeft; }
    public static int getP2KeyRight() { return p2KeyRight; }
    public static int getP2KeyDown() { return p2KeyDown; }
    public static int getP2KeyRotate() { return p2KeyRotate; }
    public static int getP2KeyDrop() { return p2KeyDrop; }
    public static int getP2KeyHold() { return p2KeyHold; }

    public static void saveConfig() {
        int[] p1Keys = { p1KeyLeft, p1KeyRight, p1KeyDown, p1KeyRotate, p1KeyDrop, p1KeyHold };
        int[] p2Keys = { p2KeyLeft, p2KeyRight, p2KeyDown, p2KeyRotate, p2KeyDrop, p2KeyHold };
        com.tetris.util.SaveManager.saveSettings(dasDelayMs, arrRateMs, softDropIntervalMs, 
                                                p1Customized, p1Keys, 
                                                p2Customized, p2Keys,
                                                com.tetris.util.SoundManager.getBGMVolume(),
                                                com.tetris.util.SoundManager.getSFXVolume(),
                                                com.tetris.util.SoundManager.isBGMMuted(),
                                                com.tetris.util.SoundManager.isSFXMuted(),
                                                com.tetris.util.ThemeManager.getCurrentTheme().name(),
                                                com.tetris.util.ThemeManager.getCurrentColorBlindMode().name(),
                                                com.tetris.util.SoundManager.getCurrentSoundPack().name());
    }

    public static void resetToDefault() {
        dasDelayMs = 170;
        arrRateMs = 45;
        softDropIntervalMs = 30;
        p1Customized = false;
        p1KeyLeft = KeyEvent.VK_LEFT;
        p1KeyRight = KeyEvent.VK_RIGHT;
        p1KeyDown = KeyEvent.VK_DOWN;
        p1KeyRotate = KeyEvent.VK_UP;
        p1KeyDrop = KeyEvent.VK_SPACE;
        p1KeyHold = KeyEvent.VK_C;
        p2Customized = false;
        p2KeyLeft = KeyEvent.VK_A;
        p2KeyRight = KeyEvent.VK_D;
        p2KeyDown = KeyEvent.VK_S;
        p2KeyRotate = KeyEvent.VK_W;
        p2KeyDrop = KeyEvent.VK_Q;
        p2KeyHold = KeyEvent.VK_E;
        saveConfig();
    }

    private final javax.swing.Timer inputTimer;
    private long lastTickTime = System.currentTimeMillis();

    public InputHandler(GameEngine engine) {
        this.engine = engine;

        // Load custom settings
        java.util.Map<String, String> config = com.tetris.util.SaveManager.loadSettings();
        if (config != null) {
            try {
                if (config.containsKey("das")) dasDelayMs = Integer.parseInt(config.get("das"));
                if (config.containsKey("arr")) arrRateMs = Integer.parseInt(config.get("arr"));
                if (config.containsKey("sdr")) softDropIntervalMs = Integer.parseInt(config.get("sdr"));
                if (config.containsKey("p1Custom")) p1Customized = Boolean.parseBoolean(config.get("p1Custom"));
                if (config.containsKey("p1Keys")) {
                    String[] parts = config.get("p1Keys").split(",");
                    if (parts.length >= 6) {
                        p1KeyLeft = Integer.parseInt(parts[0]);
                        p1KeyRight = Integer.parseInt(parts[1]);
                        p1KeyDown = Integer.parseInt(parts[2]);
                        p1KeyRotate = Integer.parseInt(parts[3]);
                        p1KeyDrop = Integer.parseInt(parts[4]);
                        p1KeyHold = Integer.parseInt(parts[5]);
                    }
                }
                if (config.containsKey("p2Custom")) p2Customized = Boolean.parseBoolean(config.get("p2Custom"));
                if (config.containsKey("p2Keys")) {
                    String[] parts = config.get("p2Keys").split(",");
                    if (parts.length >= 6) {
                        p2KeyLeft = Integer.parseInt(parts[0]);
                        p2KeyRight = Integer.parseInt(parts[1]);
                        p2KeyDown = Integer.parseInt(parts[2]);
                        p2KeyRotate = Integer.parseInt(parts[3]);
                        p2KeyDrop = Integer.parseInt(parts[4]);
                        p2KeyHold = Integer.parseInt(parts[5]);
                    }
                }
                if (config.containsKey("bgmVol")) com.tetris.util.SoundManager.setBGMVolume(Float.parseFloat(config.get("bgmVol")));
                if (config.containsKey("sfxVol")) com.tetris.util.SoundManager.setSFXVolume(Float.parseFloat(config.get("sfxVol")));
                if (config.containsKey("bgmMute")) com.tetris.util.SoundManager.setBGMMuted(Boolean.parseBoolean(config.get("bgmMute")));
                if (config.containsKey("sfxMute")) com.tetris.util.SoundManager.setSFXMuted(Boolean.parseBoolean(config.get("sfxMute")));
                if (config.containsKey("theme")) com.tetris.util.ThemeManager.setCurrentTheme(com.tetris.util.ThemeManager.Theme.valueOf(config.get("theme")));
                if (config.containsKey("colorBlind")) com.tetris.util.ThemeManager.setCurrentColorBlindMode(com.tetris.util.ThemeManager.ColorBlindMode.valueOf(config.get("colorBlind")));
                if (config.containsKey("soundPack")) com.tetris.util.SoundManager.setCurrentSoundPack(com.tetris.util.SoundManager.SoundPack.valueOf(config.get("soundPack")));
            } catch (Exception ex) {
                System.err.println("Error parsing controls config: " + ex.getMessage());
            }
        }

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
            if (p1LeftHoldTime >= dasDelayMs) {
                if (oldHoldTime < dasDelayMs) {
                    engine.movePieceLeft();
                }
                p1LeftRepeatCounter += deltaTime;
                while (p1LeftRepeatCounter >= arrRateMs) {
                    engine.movePieceLeft();
                    p1LeftRepeatCounter -= arrRateMs;
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
            if (p1RightHoldTime >= dasDelayMs) {
                if (oldHoldTime < dasDelayMs) {
                    engine.movePieceRight();
                }
                p1RightRepeatCounter += deltaTime;
                while (p1RightRepeatCounter >= arrRateMs) {
                    engine.movePieceRight();
                    p1RightRepeatCounter -= arrRateMs;
                }
            }
        } else {
            p1RightHoldTime = 0;
            p1RightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (p1DownPressed) {
            p1DownHoldTime += deltaTime;
            while (p1DownHoldTime >= softDropIntervalMs) {
                engine.softDrop();
                p1DownHoldTime -= softDropIntervalMs;
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
            if (p2LeftHoldTime >= dasDelayMs) {
                if (oldHoldTime < dasDelayMs) {
                    engine2.movePieceLeft();
                }
                p2LeftRepeatCounter += deltaTime;
                while (p2LeftRepeatCounter >= arrRateMs) {
                    engine2.movePieceLeft();
                    p2LeftRepeatCounter -= arrRateMs;
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
            if (p2RightHoldTime >= dasDelayMs) {
                if (oldHoldTime < dasDelayMs) {
                    engine2.movePieceRight();
                }
                p2RightRepeatCounter += deltaTime;
                while (p2RightRepeatCounter >= arrRateMs) {
                    engine2.movePieceRight();
                    p2RightRepeatCounter -= arrRateMs;
                }
            }
        } else {
            p2RightHoldTime = 0;
            p2RightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (p2DownPressed) {
            p2DownHoldTime += deltaTime;
            while (p2DownHoldTime >= softDropIntervalMs) {
                engine2.softDrop();
                p2DownHoldTime -= softDropIntervalMs;
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
            if (leftHoldTime >= dasDelayMs) {
                // If we just passed DAS threshold, trigger a move immediately
                if (oldHoldTime < dasDelayMs) {
                    engine.movePieceLeft();
                }
                
                leftRepeatCounter += deltaTime;
                while (leftRepeatCounter >= arrRateMs) {
                    engine.movePieceLeft();
                    leftRepeatCounter -= arrRateMs;
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
            if (rightHoldTime >= dasDelayMs) {
                // If we just passed DAS threshold, trigger a move immediately
                if (oldHoldTime < dasDelayMs) {
                    engine.movePieceRight();
                }

                rightRepeatCounter += deltaTime;
                while (rightRepeatCounter >= arrRateMs) {
                    engine.movePieceRight();
                    rightRepeatCounter -= arrRateMs;
                }
            }
        } else {
            rightHoldTime = 0;
            rightRepeatCounter = 0;
        }

        // Continuous down movement (Soft Drop)
        if (downPressed) {
            downHoldTime += deltaTime;
            while (downHoldTime >= softDropIntervalMs) {
                engine.softDrop();
                downHoldTime -= softDropIntervalMs;
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

        // Intercept key events for rebinding
        if (engine.getPanel() != null && engine.getPanel().getRebindingAction() != null) {
            String action = engine.getPanel().getRebindingAction();
            int player = engine.getPanel().getRebindingPlayer();
            if (player == 1) {
                p1Customized = true;
                if ("LEFT".equals(action)) p1KeyLeft = keyCode;
                else if ("RIGHT".equals(action)) p1KeyRight = keyCode;
                else if ("ROTATE".equals(action)) p1KeyRotate = keyCode;
                else if ("DOWN".equals(action)) p1KeyDown = keyCode;
                else if ("DROP".equals(action)) p1KeyDrop = keyCode;
                else if ("HOLD".equals(action)) p1KeyHold = keyCode;
            } else if (player == 2) {
                p2Customized = true;
                if ("LEFT".equals(action)) p2KeyLeft = keyCode;
                else if ("RIGHT".equals(action)) p2KeyRight = keyCode;
                else if ("ROTATE".equals(action)) p2KeyRotate = keyCode;
                else if ("DOWN".equals(action)) p2KeyDown = keyCode;
                else if ("DROP".equals(action)) p2KeyDrop = keyCode;
                else if ("HOLD".equals(action)) p2KeyHold = keyCode;
            }
            engine.getPanel().setRebindingAction(null); // clear rebinding status
            saveConfig(); // save settings!
            engine.getPanel().repaint();
            return;
        }

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
                // Resolve dynamic keys
                int p1Left = p1Customized ? p1KeyLeft : KeyEvent.VK_LEFT;
                int p1Right = p1Customized ? p1KeyRight : KeyEvent.VK_RIGHT;
                int p1Down = p1Customized ? p1KeyDown : KeyEvent.VK_DOWN;
                int p1Rotate = p1Customized ? p1KeyRotate : KeyEvent.VK_UP;
                int p1Drop = p1Customized ? p1KeyDrop : KeyEvent.VK_SPACE;
                int p1Hold = p1Customized ? p1KeyHold : KeyEvent.VK_C;

                int p2Left = p2Customized ? p2KeyLeft : KeyEvent.VK_A;
                int p2Right = p2Customized ? p2KeyRight : KeyEvent.VK_D;
                int p2Down = p2Customized ? p2KeyDown : KeyEvent.VK_S;
                int p2Rotate = p2Customized ? p2KeyRotate : KeyEvent.VK_W;
                int p2Drop = p2Customized ? p2KeyDrop : KeyEvent.VK_Q;
                int p2Hold = p2Customized ? p2KeyHold : KeyEvent.VK_E;

                // Player 1
                if (keyCode == p1Left) {
                    if (!p1LeftPressed) {
                        p1LeftPressed = true;
                        p1LeftHoldTime = 0;
                        p1LeftRepeatCounter = 0;
                        engine.movePieceLeft();
                    }
                } else if (keyCode == p1Right) {
                    if (!p1RightPressed) {
                        p1RightPressed = true;
                        p1RightHoldTime = 0;
                        p1RightRepeatCounter = 0;
                        engine.movePieceRight();
                    }
                } else if (keyCode == p1Down) {
                    if (!p1DownPressed) {
                        p1DownPressed = true;
                        p1DownHoldTime = 0;
                        engine.softDrop();
                    }
                } else if (keyCode == p1Rotate) {
                    engine.rotatePiece();
                } else if (keyCode == p1Drop) {
                    engine.dropPiece();
                    resetP1KeyStates();
                } else if (keyCode == p1Hold) {
                    engine.holdPiece();
                }
                
                // Player 2 (Arrows)
                if (engine2 != null) {
                    if (keyCode == p2Left) {
                        if (!p2LeftPressed) {
                            p2LeftPressed = true;
                            p2LeftHoldTime = 0;
                            p2LeftRepeatCounter = 0;
                            engine2.movePieceLeft();
                        }
                    } else if (keyCode == p2Right) {
                        if (!p2RightPressed) {
                            p2RightPressed = true;
                            p2RightHoldTime = 0;
                            p2RightRepeatCounter = 0;
                            engine2.movePieceRight();
                        }
                    } else if (keyCode == p2Down) {
                        if (!p2DownPressed) {
                            p2DownPressed = true;
                            p2DownHoldTime = 0;
                            engine2.softDrop();
                        }
                    } else if (keyCode == p2Rotate) {
                        engine2.rotatePiece();
                    } else if (keyCode == p2Drop) {
                        engine2.dropPiece();
                        resetP2KeyStates();
                    } else if (keyCode == p2Hold) {
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

            // Single Player dynamic keys
            int left = p1Customized ? p1KeyLeft : KeyEvent.VK_LEFT;
            int right = p1Customized ? p1KeyRight : KeyEvent.VK_RIGHT;
            int down = p1Customized ? p1KeyDown : KeyEvent.VK_DOWN;
            int rotate = p1Customized ? p1KeyRotate : KeyEvent.VK_UP;
            int drop = p1Customized ? p1KeyDrop : KeyEvent.VK_SPACE;
            int hold = p1Customized ? p1KeyHold : KeyEvent.VK_C;

            if (keyCode == left) {
                if (!leftPressed) {
                    leftPressed = true;
                    leftHoldTime = 0;
                    leftRepeatCounter = 0;
                    engine.movePieceLeft();
                }
            } else if (keyCode == right) {
                if (!rightPressed) {
                    rightPressed = true;
                    rightHoldTime = 0;
                    rightRepeatCounter = 0;
                    engine.movePieceRight();
                }
            } else if (keyCode == down) {
                if (!downPressed) {
                    downPressed = true;
                    downHoldTime = 0;
                    engine.softDrop();
                }
            } else if (keyCode == rotate) {
                engine.rotatePiece();
            } else if (keyCode == drop) {
                engine.dropPiece();
                resetKeyStates(); // Reset keys after hard drop to prevent instant repeats
            } else if (keyCode == hold) {
                engine.holdPiece();
            } else if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE) {
                engine.togglePause();
                resetKeyStates();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        GameEngine.GameState state = engine.getGameState();
        if (state == GameEngine.GameState.PLAYING || state == GameEngine.GameState.TUTORIAL) {
            if (engine.getGameMode() == com.tetris.model.GameMode.PVP) {
                int p1Left = p1Customized ? p1KeyLeft : KeyEvent.VK_A;
                int p1Right = p1Customized ? p1KeyRight : KeyEvent.VK_D;
                int p1Down = p1Customized ? p1KeyDown : KeyEvent.VK_S;
                int p2Left = p2Customized ? p2KeyLeft : KeyEvent.VK_LEFT;
                int p2Right = p2Customized ? p2KeyRight : KeyEvent.VK_RIGHT;
                int p2Down = p2Customized ? p2KeyDown : KeyEvent.VK_DOWN;

                if (keyCode == p1Left) {
                    p1LeftPressed = false;
                } else if (keyCode == p1Right) {
                    p1RightPressed = false;
                } else if (keyCode == p1Down) {
                    p1DownPressed = false;
                } else if (keyCode == p2Left) {
                    p2LeftPressed = false;
                } else if (keyCode == p2Right) {
                    p2RightPressed = false;
                } else if (keyCode == p2Down) {
                    p2DownPressed = false;
                }
            } else {
                int left = p1Customized ? p1KeyLeft : KeyEvent.VK_LEFT;
                int right = p1Customized ? p1KeyRight : KeyEvent.VK_RIGHT;
                int down = p1Customized ? p1KeyDown : KeyEvent.VK_DOWN;

                if (keyCode == left) {
                    leftPressed = false;
                } else if (keyCode == right) {
                    rightPressed = false;
                } else if (keyCode == down) {
                    downPressed = false;
                }
            }
        }
    }
}
