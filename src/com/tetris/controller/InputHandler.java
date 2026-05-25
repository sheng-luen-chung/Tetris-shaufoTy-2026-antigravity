package com.tetris.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputHandler extends KeyAdapter {
    private final GameEngine engine;

    private GameEngine engine2 = null;

    // Movement state flags for Player 1
    private volatile boolean p1LeftPressed = false;
    private volatile boolean p1RightPressed = false;
    private volatile boolean p1DownPressed = false;
    private int p1LeftHoldTime = 0;
    private int p1RightHoldTime = 0;
    private int p1DownHoldTime = 0;
    private int p1LeftRepeatCounter = 0;
    private int p1RightRepeatCounter = 0;

    // Movement state flags for Player 2
    private volatile boolean p2LeftPressed = false;
    private volatile boolean p2RightPressed = false;
    private volatile boolean p2DownPressed = false;
    private int p2LeftHoldTime = 0;
    private int p2RightHoldTime = 0;
    private int p2DownHoldTime = 0;
    private int p2LeftRepeatCounter = 0;
    private int p2RightRepeatCounter = 0;

    // Backward compatibility for single player
    private volatile boolean leftPressed = false;
    private volatile boolean rightPressed = false;
    private volatile boolean downPressed = false;
    private int leftHoldTime = 0;
    private int rightHoldTime = 0;
    private int downHoldTime = 0;
    private int leftRepeatCounter = 0;
    private int rightRepeatCounter = 0;

    // Professional Tetris Tuning Constants (Configurable)
    private static int dasDelayMs = 170;
    private static int arrRateMs = 45;
    private static int softDropIntervalMs = 30;

    // Single Player Keys
    private static boolean singleCustomized = false;
    private static int singleKeyLeft = KeyEvent.VK_LEFT;
    private static int singleKeyRight = KeyEvent.VK_RIGHT;
    private static int singleKeyDown = KeyEvent.VK_DOWN;
    private static int singleKeyRotate = KeyEvent.VK_UP;
    private static int singleKeyDrop = KeyEvent.VK_SPACE;
    private static int singleKeyHold = KeyEvent.VK_C;

    // PvP Player 1 (Left) Keys
    private static boolean pvpP1Customized = false;
    private static int pvpP1KeyLeft = KeyEvent.VK_A;
    private static int pvpP1KeyRight = KeyEvent.VK_D;
    private static int pvpP1KeyDown = KeyEvent.VK_S;
    private static int pvpP1KeyRotate = KeyEvent.VK_W;
    private static int pvpP1KeyDrop = KeyEvent.VK_SPACE;
    private static int pvpP1KeyHold = KeyEvent.VK_C;

    // PvP Player 2 (Right) Keys
    private static boolean pvpP2Customized = false;
    private static int pvpP2KeyLeft = KeyEvent.VK_LEFT;
    private static int pvpP2KeyRight = KeyEvent.VK_RIGHT;
    private static int pvpP2KeyDown = KeyEvent.VK_DOWN;
    private static int pvpP2KeyRotate = KeyEvent.VK_UP;
    private static int pvpP2KeyDrop = KeyEvent.VK_ENTER;
    private static int pvpP2KeyHold = KeyEvent.VK_SHIFT;

    public static int getDasDelayMs() { return dasDelayMs; }
    public static void setDasDelayMs(int val) { dasDelayMs = val; saveConfig(); }
    public static int getArrRateMs() { return arrRateMs; }
    public static void setArrRateMs(int val) { arrRateMs = val; saveConfig(); }
    public static int getSoftDropIntervalMs() { return softDropIntervalMs; }
    public static void setSoftDropIntervalMs(int val) { softDropIntervalMs = val; saveConfig(); }

    public static boolean isSingleCustomized() { return singleCustomized; }
    public static void setSingleCustomized(boolean val) { singleCustomized = val; saveConfig(); }
    public static int getSingleKeyLeft() { return singleKeyLeft; }
    public static void setSingleKeyLeft(int val) { singleKeyLeft = val; }
    public static int getSingleKeyRight() { return singleKeyRight; }
    public static void setSingleKeyRight(int val) { singleKeyRight = val; }
    public static int getSingleKeyDown() { return singleKeyDown; }
    public static void setSingleKeyDown(int val) { singleKeyDown = val; }
    public static int getSingleKeyRotate() { return singleKeyRotate; }
    public static void setSingleKeyRotate(int val) { singleKeyRotate = val; }
    public static int getSingleKeyDrop() { return singleKeyDrop; }
    public static void setSingleKeyDrop(int val) { singleKeyDrop = val; }
    public static int getSingleKeyHold() { return singleKeyHold; }
    public static void setSingleKeyHold(int val) { singleKeyHold = val; }

    public static boolean isPvpP1Customized() { return pvpP1Customized; }
    public static void setPvpP1Customized(boolean val) { pvpP1Customized = val; saveConfig(); }
    public static int getPvpP1KeyLeft() { return pvpP1KeyLeft; }
    public static void setPvpP1KeyLeft(int val) { pvpP1KeyLeft = val; }
    public static int getPvpP1KeyRight() { return pvpP1KeyRight; }
    public static void setPvpP1KeyRight(int val) { pvpP1KeyRight = val; }
    public static int getPvpP1KeyDown() { return pvpP1KeyDown; }
    public static void setPvpP1KeyDown(int val) { pvpP1KeyDown = val; }
    public static int getPvpP1KeyRotate() { return pvpP1KeyRotate; }
    public static void setPvpP1KeyRotate(int val) { pvpP1KeyRotate = val; }
    public static int getPvpP1KeyDrop() { return pvpP1KeyDrop; }
    public static void setPvpP1KeyDrop(int val) { pvpP1KeyDrop = val; }
    public static int getPvpP1KeyHold() { return pvpP1KeyHold; }
    public static void setPvpP1KeyHold(int val) { pvpP1KeyHold = val; }

    public static boolean isPvpP2Customized() { return pvpP2Customized; }
    public static void setPvpP2Customized(boolean val) { pvpP2Customized = val; saveConfig(); }
    public static int getPvpP2KeyLeft() { return pvpP2KeyLeft; }
    public static void setPvpP2KeyLeft(int val) { pvpP2KeyLeft = val; }
    public static int getPvpP2KeyRight() { return pvpP2KeyRight; }
    public static void setPvpP2KeyRight(int val) { pvpP2KeyRight = val; }
    public static int getPvpP2KeyDown() { return pvpP2KeyDown; }
    public static void setPvpP2KeyDown(int val) { pvpP2KeyDown = val; }
    public static int getPvpP2KeyRotate() { return pvpP2KeyRotate; }
    public static void setPvpP2KeyRotate(int val) { pvpP2KeyRotate = val; }
    public static int getPvpP2KeyDrop() { return pvpP2KeyDrop; }
    public static void setPvpP2KeyDrop(int val) { pvpP2KeyDrop = val; }
    public static int getPvpP2KeyHold() { return pvpP2KeyHold; }
    public static void setPvpP2KeyHold(int val) { pvpP2KeyHold = val; }

    public static void saveConfig() {
        int[] singleKeys = { singleKeyLeft, singleKeyRight, singleKeyDown, singleKeyRotate, singleKeyDrop, singleKeyHold };
        int[] pvpP1Keys = { pvpP1KeyLeft, pvpP1KeyRight, pvpP1KeyDown, pvpP1KeyRotate, pvpP1KeyDrop, pvpP1KeyHold };
        int[] pvpP2Keys = { pvpP2KeyLeft, pvpP2KeyRight, pvpP2KeyDown, pvpP2KeyRotate, pvpP2KeyDrop, pvpP2KeyHold };
        com.tetris.util.SaveManager.saveSettings(dasDelayMs, arrRateMs, softDropIntervalMs, 
                                                singleCustomized, singleKeys, 
                                                pvpP1Customized, pvpP1Keys,
                                                pvpP2Customized, pvpP2Keys,
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
        
        singleCustomized = false;
        singleKeyLeft = KeyEvent.VK_LEFT;
        singleKeyRight = KeyEvent.VK_RIGHT;
        singleKeyDown = KeyEvent.VK_DOWN;
        singleKeyRotate = KeyEvent.VK_UP;
        singleKeyDrop = KeyEvent.VK_SPACE;
        singleKeyHold = KeyEvent.VK_C;

        pvpP1Customized = false;
        pvpP1KeyLeft = KeyEvent.VK_A;
        pvpP1KeyRight = KeyEvent.VK_D;
        pvpP1KeyDown = KeyEvent.VK_S;
        pvpP1KeyRotate = KeyEvent.VK_W;
        pvpP1KeyDrop = KeyEvent.VK_SPACE;
        pvpP1KeyHold = KeyEvent.VK_C;

        pvpP2Customized = false;
        pvpP2KeyLeft = KeyEvent.VK_LEFT;
        pvpP2KeyRight = KeyEvent.VK_RIGHT;
        pvpP2KeyDown = KeyEvent.VK_DOWN;
        pvpP2KeyRotate = KeyEvent.VK_UP;
        pvpP2KeyDrop = KeyEvent.VK_ENTER;
        pvpP2KeyHold = KeyEvent.VK_SHIFT;
        saveConfig();
    }

    public InputHandler(GameEngine engine) {
        this.engine = engine;

        // Load custom settings
        java.util.Map<String, String> config = com.tetris.util.SaveManager.loadSettings();
        if (config != null) {
            try {
                if (config.containsKey("das")) dasDelayMs = Integer.parseInt(config.get("das"));
                if (config.containsKey("arr")) arrRateMs = Integer.parseInt(config.get("arr"));
                if (config.containsKey("sdr")) softDropIntervalMs = Integer.parseInt(config.get("sdr"));
                if (config.containsKey("singleCustom")) singleCustomized = Boolean.parseBoolean(config.get("singleCustom"));
                if (config.containsKey("singleKeys")) {
                    String[] parts = config.get("singleKeys").split(",");
                    if (parts.length >= 6) {
                        singleKeyLeft = Integer.parseInt(parts[0]);
                        singleKeyRight = Integer.parseInt(parts[1]);
                        singleKeyDown = Integer.parseInt(parts[2]);
                        singleKeyRotate = Integer.parseInt(parts[3]);
                        singleKeyDrop = Integer.parseInt(parts[4]);
                        singleKeyHold = Integer.parseInt(parts[5]);
                    }
                }
                if (config.containsKey("pvpP1Custom")) pvpP1Customized = Boolean.parseBoolean(config.get("pvpP1Custom"));
                if (config.containsKey("pvpP1Keys")) {
                    String[] parts = config.get("pvpP1Keys").split(",");
                    if (parts.length >= 6) {
                        pvpP1KeyLeft = Integer.parseInt(parts[0]);
                        pvpP1KeyRight = Integer.parseInt(parts[1]);
                        pvpP1KeyDown = Integer.parseInt(parts[2]);
                        pvpP1KeyRotate = Integer.parseInt(parts[3]);
                        pvpP1KeyDrop = Integer.parseInt(parts[4]);
                        pvpP1KeyHold = Integer.parseInt(parts[5]);
                    }
                }
                if (config.containsKey("pvpP2Custom")) pvpP2Customized = Boolean.parseBoolean(config.get("pvpP2Custom"));
                if (config.containsKey("pvpP2Keys")) {
                    String[] parts = config.get("pvpP2Keys").split(",");
                    if (parts.length >= 6) {
                        pvpP2KeyLeft = Integer.parseInt(parts[0]);
                        pvpP2KeyRight = Integer.parseInt(parts[1]);
                        pvpP2KeyDown = Integer.parseInt(parts[2]);
                        pvpP2KeyRotate = Integer.parseInt(parts[3]);
                        pvpP2KeyDrop = Integer.parseInt(parts[4]);
                        pvpP2KeyHold = Integer.parseInt(parts[5]);
                    }
                }
                
                // Fallback support for old settings config
                if (!config.containsKey("singleKeys") && config.containsKey("p1Keys")) {
                    String[] parts = config.get("p1Keys").split(",");
                    if (parts.length >= 6) {
                        int kl = Integer.parseInt(parts[0]);
                        int kr = Integer.parseInt(parts[1]);
                        int kd = Integer.parseInt(parts[2]);
                        int kro = Integer.parseInt(parts[3]);
                        int kdr = Integer.parseInt(parts[4]);
                        int kh = Integer.parseInt(parts[5]);
                        singleKeyLeft = pvpP2KeyLeft = kl;
                        singleKeyRight = pvpP2KeyRight = kr;
                        singleKeyDown = pvpP2KeyDown = kd;
                        singleKeyRotate = pvpP2KeyRotate = kro;
                        singleKeyDrop = pvpP2KeyDrop = kdr;
                        singleKeyHold = pvpP2KeyHold = kh;
                    }
                }
                if (!config.containsKey("pvpP1Keys") && config.containsKey("p2Keys")) {
                    String[] parts = config.get("p2Keys").split(",");
                    if (parts.length >= 6) {
                        int kl = Integer.parseInt(parts[0]);
                        int kr = Integer.parseInt(parts[1]);
                        int kd = Integer.parseInt(parts[2]);
                        int kro = Integer.parseInt(parts[3]);
                        int kdr = Integer.parseInt(parts[4]);
                        int kh = Integer.parseInt(parts[5]);
                        pvpP1KeyLeft = kl;
                        pvpP1KeyRight = kr;
                        pvpP1KeyDown = kd;
                        pvpP1KeyRotate = kro;
                        pvpP1KeyDrop = kdr;
                        pvpP1KeyHold = kh;
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
    }

    public void tickInputs(int deltaTime) {
        GameEngine.GameState state = engine.getGameState();
        boolean isActiveState = (state == GameEngine.GameState.PLAYING || state == GameEngine.GameState.TUTORIAL);
        if (isActiveState) {
            if (engine.getGameMode() == com.tetris.model.GameMode.PVP) {
                if (engine2 != null && !engine.isPaused()) {
                    if (!engine.isGameOver()) {
                        updateP1Inputs(deltaTime);
                    } else {
                        resetP1KeyStates();
                    }

                    if (!engine2.isGameOver()) {
                        updateP2Inputs(deltaTime);
                    } else {
                        resetP2KeyStates();
                    }
                }
            } else {
                if (!engine.isGameOver() && !engine.isPaused() && !engine.isTransitioning()) {
                    updateInputs(deltaTime);
                } else {
                    resetKeyStates();
                }
            }
        } else {
            resetKeyStates();
        }
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
                singleCustomized = true;
                if ("LEFT".equals(action)) singleKeyLeft = keyCode;
                else if ("RIGHT".equals(action)) singleKeyRight = keyCode;
                else if ("ROTATE".equals(action)) singleKeyRotate = keyCode;
                else if ("DOWN".equals(action)) singleKeyDown = keyCode;
                else if ("DROP".equals(action)) singleKeyDrop = keyCode;
                else if ("HOLD".equals(action)) singleKeyHold = keyCode;
            } else if (player == 2) {
                pvpP1Customized = true;
                if ("LEFT".equals(action)) pvpP1KeyLeft = keyCode;
                else if ("RIGHT".equals(action)) pvpP1KeyRight = keyCode;
                else if ("ROTATE".equals(action)) pvpP1KeyRotate = keyCode;
                else if ("DOWN".equals(action)) pvpP1KeyDown = keyCode;
                else if ("DROP".equals(action)) pvpP1KeyDrop = keyCode;
                else if ("HOLD".equals(action)) pvpP1KeyHold = keyCode;
            } else if (player == 3) {
                pvpP2Customized = true;
                if ("LEFT".equals(action)) pvpP2KeyLeft = keyCode;
                else if ("RIGHT".equals(action)) pvpP2KeyRight = keyCode;
                else if ("ROTATE".equals(action)) pvpP2KeyRotate = keyCode;
                else if ("DOWN".equals(action)) pvpP2KeyDown = keyCode;
                else if ("DROP".equals(action)) pvpP2KeyDrop = keyCode;
                else if ("HOLD".equals(action)) pvpP2KeyHold = keyCode;
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
        } else if (state == GameEngine.GameState.TUTORIAL_SELECT) {
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    engine.getPanel().navigateTutorialSelect(-1);
                    break;
                case KeyEvent.VK_DOWN:
                    engine.getPanel().navigateTutorialSelect(1);
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    engine.getPanel().selectTutorialOption();
                    break;
                case KeyEvent.VK_ESCAPE:
                    engine.returnToMenu();
                    break;
            }
        } else if (state == GameEngine.GameState.ACHIEVEMENTS) {
            if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
                engine.returnToMenu();
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
                int p1Left = pvpP1KeyLeft;
                int p1Right = pvpP1KeyRight;
                int p1Down = pvpP1KeyDown;
                int p1Rotate = pvpP1KeyRotate;
                int p1Drop = pvpP1KeyDrop;
                int p1Hold = pvpP1KeyHold;

                int p2Left = pvpP2KeyLeft;
                int p2Right = pvpP2KeyRight;
                int p2Down = pvpP2KeyDown;
                int p2Rotate = pvpP2KeyRotate;
                int p2Drop = pvpP2KeyDrop;
                int p2Hold = pvpP2KeyHold;

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
            int left = singleKeyLeft;
            int right = singleKeyRight;
            int down = singleKeyDown;
            int rotate = singleKeyRotate;
            int drop = singleKeyDrop;
            int hold = singleKeyHold;

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
            } else if (keyCode == KeyEvent.VK_Z) {
                engine.rotatePieceCounterClockwise();
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
                int p1Left = pvpP1KeyLeft;
                int p1Right = pvpP1KeyRight;
                int p1Down = pvpP1KeyDown;
                int p2Left = pvpP2KeyLeft;
                int p2Right = pvpP2KeyRight;
                int p2Down = pvpP2KeyDown;

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
                int left = singleKeyLeft;
                int right = singleKeyRight;
                int down = singleKeyDown;

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
