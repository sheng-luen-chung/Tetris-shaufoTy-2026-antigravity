package com.tetris.view;

import javax.swing.JPanel;

import com.tetris.model.Board;
import com.tetris.model.LeaderboardEntry;
import com.tetris.model.Piece;
import com.tetris.model.GameMode;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
// import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.Timer;

// Game panel
public class GamePanel extends JPanel {
    public static final int TILE_SIZE = 30;
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int SIDEBAR_WIDTH = 200;

    private Board board;
    private Piece currentPiece;
    private com.tetris.controller.GameEngine gameEngine;
    
    // PVP Player 2 properties
    private com.tetris.controller.GameEngine gameEngine2 = null;
    private final List<ScorePopup> scorePopups2 = new ArrayList<>();
    private final List<Particle> particles2 = new ArrayList<>();
    private int shakeIntensity2 = 0;
    private int shakeDuration2 = 100;
    private long shakeEndTime2 = 0;
    private long perfectClearStartTime2 = 0;
    
    // Active score popups & particles
    private final List<ScorePopup> scorePopups = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> particlePool = new ArrayList<>();
    private final Timer animationTimer;

    private Particle getOrCreateParticle(double x, double y, Color color, int size, double angle, double speed, float decay) {
        synchronized (particlePool) {
            if (!particlePool.isEmpty()) {
                Particle p = particlePool.remove(particlePool.size() - 1);
                p.reset(x, y, color, size, angle, speed, decay, 0);
                return p;
            }
        }
        return new Particle(x, y, color, size, angle, speed, decay);
    }

    private Particle getOrCreateParticle(double x, double y, Color color, int size, double angle, double speed, float decay, int shapeType) {
        synchronized (particlePool) {
            if (!particlePool.isEmpty()) {
                Particle p = particlePool.remove(particlePool.size() - 1);
                p.reset(x, y, color, size, angle, speed, decay, shapeType);
                return p;
            }
        }
        return new Particle(x, y, color, size, angle, speed, decay, shapeType);
    }

    private static final java.util.Map<String, Font> fontCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static Font getCachedFont(String name, int style, int size) {
        String key = name + "_" + style + "_" + size;
        Font font = fontCache.get(key);
        if (font == null) {
            font = new Font(name, style, size);
            fontCache.put(key, font);
        }
        return font;
    }

    // Menu properties
    private int selectedMenuIndex = 0;
    private final Rectangle[] menuOptionBounds = new Rectangle[8];
    private Rectangle backButtonBounds = new Rectangle();
    private final List<FloatingPiece> floatingPieces = new ArrayList<>();
    private Timer menuAnimationTimer;

    // Tutorial Select Screen properties
    private int selectedTutorialIndex = 0;
    private final Rectangle[] tutorialOptionBounds = new Rectangle[8];

    // Achievements Screen properties
    private Rectangle achievementsBackButtonBounds = new Rectangle();

    // Toast Notification properties
    private String activeToastTitle = null;
    private String activeToastDesc = null;
    private long toastStartTime = 0;
    private static final int TOAST_DURATION = 3000;

    // Pause menu properties
    private int selectedPauseIndex = 0;
    private final Rectangle[] pauseOptionBounds = new Rectangle[7];
    private boolean showSettingsInPause = false;
    private boolean showGhostPiece = true;
    private boolean showGridLines = true;
    private long lastSaveTime = 0;

    // Screenshake properties
    private int shakeIntensity = 0;
    private int shakeDuration = 100;
    private long shakeEndTime = 0;

    // Perfect Clear properties
    private long perfectClearStartTime = 0;
    private static final int PERFECT_CLEAR_DURATION = 2500;

    // Volume Settings controls properties
    private boolean showSettingsInMenu = false;
    private Rectangle bgmTrackBounds = new Rectangle();
    private Rectangle sfxTrackBounds = new Rectangle();
    private Rectangle bgmMuteBounds = new Rectangle();
    private Rectangle sfxMuteBounds = new Rectangle();
    private Rectangle menuSettingsBackButtonBounds = new Rectangle();
    private Rectangle menuSettingsThemeBounds = new Rectangle();
    private Rectangle menuSettingsColorBlindBounds = new Rectangle();
    private Rectangle menuSettingsSoundPackBounds = new Rectangle();
    private Rectangle menuSettingsPreviewCountBounds = new Rectangle();
    private Rectangle menuSettingsControlBounds = new Rectangle();
    private Rectangle controlBackButtonBounds = new Rectangle();
    private Rectangle controlResetButtonBounds = new Rectangle();
    private Rectangle controlSingleTabBounds = new Rectangle();
    private Rectangle controlP1TabBounds = new Rectangle();
    private Rectangle controlP2TabBounds = new Rectangle();
    private Rectangle controlDasBounds = new Rectangle();
    private Rectangle controlArrBounds = new Rectangle();
    private Rectangle controlSdrBounds = new Rectangle();
    private final Rectangle[] controlKeyBounds = new Rectangle[6];
    private boolean showControlSettings = false;
    private int rebindingPlayer = 1;
    private String rebindingAction = null;
    private int nextPiecesCount = 3;
    private boolean isDraggingBGM = false;
    private boolean isDraggingSFX = false;

    // Game mode selection properties
    private boolean showModeSelectInMenu = false;
    private int selectedModeIndex = 0; // 0: ENDLESS, 1: SPRINT, 2: ULTRA, 3: SURVIVAL, 4: PVP, 5: BACK
    private final Rectangle[] modeOptionBounds = new Rectangle[6];

    // Difficulty selection properties
    private boolean showDifficultySelectInMenu = false;
    private int selectedDifficultyIndex = 0; // 0: EASY, 1: MEDIUM, 2: HARD, 3: BACK
    private final Rectangle[] difficultyOptionBounds = new Rectangle[4];

    // Leaderboard screen properties
    public enum LeaderboardScope { LOCAL, GLOBAL }
    private LeaderboardScope selectedLeaderboardScope = LeaderboardScope.LOCAL;
    private final Rectangle[] leaderboardScopeTabBounds = new Rectangle[2];
    private boolean isLeaderboardLoading = false;
    private com.tetris.controller.GameEngine.Difficulty selectedLeaderboardDifficulty = com.tetris.controller.GameEngine.Difficulty.NORMAL;
    private final Rectangle[] leaderboardTabBounds = new Rectangle[3];
    private GameMode selectedLeaderboardMode = GameMode.ENDLESS;
    private final Rectangle[] leaderboardModeTabBounds = new Rectangle[4];

    private static class FloatingPiece {
        int typeIndex;
        double x, y;
        double speed;
        double angle;
        double rotationSpeed;
        double scale;
        Color color;
    }

    // Add a score popup at a grid cell (col, row)
    public void addScorePopup(int gridCol, int gridRow, int score, int lines, com.tetris.controller.GameEngine.TSpinType tSpinType, int comboCount) {
        addScorePopup(gridCol, gridRow, score, lines, tSpinType, comboCount, 1);
    }

    public void addScorePopup(int gridCol, int gridRow, int score, int lines, com.tetris.controller.GameEngine.TSpinType tSpinType, int comboCount, int playerNum) {
        String scoreText = "+" + score;
        String lineText = "";
        String comboText = (comboCount >= 1) ? ((comboCount + 1) + " COMBO!") : "";
        Color popupColor = new Color(255, 235, 120); // Default gold
        Font font = getCachedFont("Arial", Font.BOLD, 24); // Font size

        if (tSpinType == com.tetris.controller.GameEngine.TSpinType.REGULAR) {
            switch (lines) {
                case 0:
                    lineText = "T-SPIN";
                    popupColor = new Color(255, 0, 255); // Neon Purple
                    font = getCachedFont("Impact", Font.BOLD, 28);
                    break;
                case 1:
                    lineText = "T-SPIN SINGLE!";
                    popupColor = new Color(255, 100, 255); // Neon Magenta
                    font = getCachedFont("Impact", Font.BOLD, 28);
                    break;
                case 2:
                    lineText = "T-SPIN DOUBLE!!";
                    popupColor = new Color(255, 215, 0); // Neon Gold
                    font = getCachedFont("Impact", Font.BOLD, 32);
                    break;
                case 3:
                    lineText = "T-SPIN TRIPLE!!!";
                    popupColor = new Color(255, 50, 50); // Neon Red
                    font = getCachedFont("Impact", Font.BOLD, 36);
                    break;
                default:
                    lineText = "T-SPIN CLEAR";
                    popupColor = new Color(255, 0, 255);
                    font = getCachedFont("Impact", Font.BOLD, 28);
                    break;
            }
        } else if (tSpinType == com.tetris.controller.GameEngine.TSpinType.MINI) {
            switch (lines) {
                case 0:
                    lineText = "T-SPIN MINI";
                    popupColor = new Color(255, 160, 122); // Light Salmon / Coral
                    font = getCachedFont("Impact", Font.BOLD, 24);
                    break;
                case 1:
                    lineText = "T-SPIN MINI SINGLE!";
                    popupColor = new Color(255, 160, 122);
                    font = getCachedFont("Impact", Font.BOLD, 24);
                    break;
                case 2:
                    lineText = "T-SPIN MINI DOUBLE!!";
                    popupColor = new Color(255, 160, 122);
                    font = getCachedFont("Impact", Font.BOLD, 26);
                    break;
                default:
                    lineText = "T-SPIN MINI CLEAR";
                    popupColor = new Color(255, 160, 122);
                    font = getCachedFont("Impact", Font.BOLD, 24);
                    break;
            }
        } else {
            switch (lines) {
                case 1:
                    lineText = "SINGLE";
                    popupColor = new Color(0, 255, 100); // Neon green
                    break;
                case 2:
                    lineText = "DOUBLE";
                    popupColor = new Color(0, 255, 255); // Neon cyan
                    break;
                case 3:
                    lineText = "TRIPLE";
                    popupColor = new Color(255, 100, 255); // Neon magenta
                    break;
                case 4:
                    lineText = "TETRIS!";
                    popupColor = new Color(255, 215, 0); // Gold
                    font = getCachedFont("Impact", Font.BOLD, 44); // Size 44 instead of 30!
                    break;
                default:
                    lineText = "";
                    break;
            }
        }

        // If no lines cleared and not a T-spin, we don't display anything unless it's a raw T-Spin
        if (lineText.isEmpty() && score <= 0) {
            return;
        }

        // Center popup horizontally in the grid
        int px = (COLS * TILE_SIZE) / 2;
        int py = gridRow * TILE_SIZE;
        if (py < 120) {
            py = 120; // Keep it low enough to not be cut off by top border
        }

        synchronized (playerNum == 2 ? scorePopups2 : scorePopups) {
            List<ScorePopup> targetList = (playerNum == 2) ? scorePopups2 : scorePopups;
            targetList.add(new ScorePopup(lineText, scoreText, comboText, px, py, popupColor, font));
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    // Draw and update active score popups
    private void drawScorePopups(Graphics g) {
        drawScorePopups(g, scorePopups);
    }

    private void drawScorePopups(Graphics g, List<ScorePopup> popupsList) {
        if (popupsList.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        // Enable anti-aliasing for smooth rendering of text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long now = System.currentTimeMillis();

        synchronized (popupsList) {
            Iterator<ScorePopup> it = popupsList.iterator();
            while (it.hasNext()) {
                ScorePopup sp = it.next();
                float elapsed = now - sp.startTime;
                float progress = Math.min(1.0f, elapsed / (float) sp.duration);

                int y = (int) (sp.startY - sp.rise * progress);
                float alpha = 1.0f - progress;

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                // 1. Draw Line clear text (e.g. TETRIS!)
                g2.setFont(sp.font);
                FontMetrics fmLine = g2.getFontMetrics();
                int lineW = fmLine.stringWidth(sp.lineText);
                int lineH = fmLine.getAscent();

                // Shadow
                g2.setColor(new Color(0, 0, 0, (int) (200 * alpha)));
                g2.drawString(sp.lineText, sp.startX - lineW / 2 + 2, y + 2);
                // Main
                g2.setColor(sp.color);
                g2.drawString(sp.lineText, sp.startX - lineW / 2, y);

                // 2. Draw Score text (e.g. +800) right below it
                g2.setFont(getCachedFont("Arial", Font.BOLD, 22)); // Score font size
                FontMetrics fmScore = g2.getFontMetrics();
                int scoreW = fmScore.stringWidth(sp.scoreText);
                int scoreY = y + lineH + 6;

                // Shadow
                g2.setColor(new Color(0, 0, 0, (int) (180 * alpha)));
                g2.drawString(sp.scoreText, sp.startX - scoreW / 2 + 1, scoreY + 1);
                // Main
                g2.setColor(Color.WHITE);
                g2.drawString(sp.scoreText, sp.startX - scoreW / 2, scoreY);

                // 3. Draw Combo text if present
                if (sp.comboText != null && !sp.comboText.isEmpty()) {
                    g2.setFont(getCachedFont("Impact", Font.ITALIC, 22));
                    FontMetrics fmCombo = g2.getFontMetrics();
                    int comboW = fmCombo.stringWidth(sp.comboText);
                    int comboY = scoreY + fmScore.getAscent() + 10;

                    // Shadow
                    g2.setColor(new Color(0, 0, 0, (int) (180 * alpha)));
                    g2.drawString(sp.comboText, sp.startX - comboW / 2 + 1, comboY + 1);
                    // Main - Neon Orange/Gold
                    g2.setColor(new Color(255, 140, 0));
                    g2.drawString(sp.comboText, sp.startX - comboW / 2, comboY);
                }

                if (progress >= 1.0f) {
                    it.remove();
                }
            }
        }

        g2.dispose();
    }

    // Game panel constructor
    public GamePanel(Board board) {
        this.board = board;

        animationTimer = new Timer(16, e -> {
            boolean active = false;
            synchronized (scorePopups) {
                if (!scorePopups.isEmpty()) {
                    active = true;
                }
            }
            synchronized (scorePopups2) {
                if (!scorePopups2.isEmpty()) {
                    active = true;
                }
            }
            synchronized (particles) {
                if (!particles.isEmpty()) {
                    active = true;
                    Iterator<Particle> it = particles.iterator();
                    while (it.hasNext()) {
                        Particle p = it.next();
                        p.update();
                        if (p.life <= 0) {
                            it.remove();
                            synchronized (particlePool) {
                                if (particlePool.size() < 1000) {
                                    particlePool.add(p);
                                }
                            }
                        }
                    }
                }
            }
            synchronized (particles2) {
                if (!particles2.isEmpty()) {
                    active = true;
                    Iterator<Particle> it = particles2.iterator();
                    while (it.hasNext()) {
                        Particle p = it.next();
                        p.update();
                        if (p.life <= 0) {
                            it.remove();
                            synchronized (particlePool) {
                                if (particlePool.size() < 1000) {
                                    particlePool.add(p);
                                }
                            }
                        }
                    }
                }
            }
            if (System.currentTimeMillis() < shakeEndTime || System.currentTimeMillis() < shakeEndTime2) {
                active = true;
            }
            if (System.currentTimeMillis() - perfectClearStartTime < PERFECT_CLEAR_DURATION ||
                System.currentTimeMillis() - perfectClearStartTime2 < PERFECT_CLEAR_DURATION) {
                active = true;
            }
            if (gameEngine != null && gameEngine.isEnteringName()) {
                active = true;
            }
            if (!active) {
                ((Timer) e.getSource()).stop();
            } else {
                repaint();
            }
        });

        // Initialize floating pieces for the menu background
        Random rand = new Random();
        com.tetris.model.Tetromino[] types = com.tetris.model.Tetromino.values();
        for (int i = 0; i < 8; i++) {
            FloatingPiece fp = new FloatingPiece();
            fp.typeIndex = rand.nextInt(types.length);
            fp.x = rand.nextInt(COLS * TILE_SIZE + SIDEBAR_WIDTH);
            fp.y = rand.nextInt(ROWS * TILE_SIZE);
            fp.speed = 0.4 + rand.nextDouble() * 0.8;
            fp.angle = rand.nextDouble() * Math.PI * 2;
            fp.rotationSpeed = -0.02 + rand.nextDouble() * 0.04;
            fp.scale = 0.5 + rand.nextDouble() * 0.7;
            fp.color = types[fp.typeIndex].getColor();
            floatingPieces.add(fp);
        }

        // Start menu animation timer (30 FPS)
        menuAnimationTimer = new Timer(33, e -> {
            if (gameEngine != null) {
                com.tetris.controller.GameEngine.GameState state = gameEngine.getGameState();
                if (state == com.tetris.controller.GameEngine.GameState.MENU || 
                    state == com.tetris.controller.GameEngine.GameState.LEADERBOARD) {
                    updateFloatingPieces();
                    repaint();
                }
            }
        });
        menuAnimationTimer.start();

        // Unified mouse inputs for hover, click, and volume dragging
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (gameEngine == null) return;
                com.tetris.controller.GameEngine.GameState state = gameEngine.getGameState();
                if (state == com.tetris.controller.GameEngine.GameState.MENU) {
                    if (showSettingsInMenu) {
                        if (showControlSettings) {
                            // Clicks inside control settings
                            if (controlBackButtonBounds != null && controlBackButtonBounds.contains(e.getPoint())) {
                                showControlSettings = false;
                                rebindingAction = null;
                                repaint();
                                return;
                            }
                            if (controlResetButtonBounds != null && controlResetButtonBounds.contains(e.getPoint())) {
                                com.tetris.controller.InputHandler.resetToDefault();
                                rebindingAction = null;
                                repaint();
                                return;
                            }
                            if (controlSingleTabBounds != null && controlSingleTabBounds.contains(e.getPoint())) {
                                rebindingPlayer = 1;
                                rebindingAction = null;
                                repaint();
                                return;
                            }
                            if (controlP1TabBounds != null && controlP1TabBounds.contains(e.getPoint())) {
                                rebindingPlayer = 2;
                                rebindingAction = null;
                                repaint();
                                return;
                            }
                            if (controlP2TabBounds != null && controlP2TabBounds.contains(e.getPoint())) {
                                rebindingPlayer = 3;
                                rebindingAction = null;
                                repaint();
                                return;
                            }
                            // DAS Click
                            if (controlDasBounds != null && controlDasBounds.contains(e.getPoint())) {
                                int current = com.tetris.controller.InputHandler.getDasDelayMs();
                                int next = (current >= 300) ? 100 : current + 20;
                                com.tetris.controller.InputHandler.setDasDelayMs(next);
                                repaint();
                                return;
                            }
                            // ARR Click
                            if (controlArrBounds != null && controlArrBounds.contains(e.getPoint())) {
                                int current = com.tetris.controller.InputHandler.getArrRateMs();
                                int next = (current >= 100) ? 10 : current + 10;
                                com.tetris.controller.InputHandler.setArrRateMs(next);
                                repaint();
                                return;
                            }
                            // SDR Click
                            if (controlSdrBounds != null && controlSdrBounds.contains(e.getPoint())) {
                                int current = com.tetris.controller.InputHandler.getSoftDropIntervalMs();
                                int next = (current >= 100) ? 10 : current + 10;
                                com.tetris.controller.InputHandler.setSoftDropIntervalMs(next);
                                repaint();
                                return;
                            }
                            // Key Binding Clicks
                            String[] actions = { "LEFT", "RIGHT", "ROTATE", "DOWN", "DROP", "HOLD" };
                            for (int i = 0; i < 6; i++) {
                                if (controlKeyBounds[i] != null && controlKeyBounds[i].contains(e.getPoint())) {
                                    rebindingAction = actions[i];
                                    repaint();
                                    return;
                                }
                            }
                            return; // swallow other clicks while in control settings
                        }

                        // BACK button click
                        if (menuSettingsBackButtonBounds != null && menuSettingsBackButtonBounds.contains(e.getPoint())) {
                            showSettingsInMenu = false;
                            selectedMenuIndex = com.tetris.util.SaveManager.hasSave() ? 4 : 3; // Return Indicator to SETTINGS
                            repaint();
                            return;
                        }

                        // THEME button click
                        if (menuSettingsThemeBounds != null && menuSettingsThemeBounds.contains(e.getPoint())) {
                            com.tetris.util.ThemeManager.nextTheme();
                            com.tetris.controller.InputHandler.saveConfig();
                            repaint();
                            return;
                        }

                        // COLORBLIND button click
                        if (menuSettingsColorBlindBounds != null && menuSettingsColorBlindBounds.contains(e.getPoint())) {
                            com.tetris.util.ThemeManager.nextColorBlindMode();
                            com.tetris.controller.InputHandler.saveConfig();
                            repaint();
                            return;
                        }

                        // SOUND PACK button click
                        if (menuSettingsSoundPackBounds != null && menuSettingsSoundPackBounds.contains(e.getPoint())) {
                            com.tetris.util.SoundManager.nextSoundPack();
                            if (!com.tetris.util.SoundManager.isSFXMuted()) {
                                com.tetris.util.SoundManager.playSynthSound(com.tetris.util.SoundManager.SoundType.MOVE);
                            }
                            com.tetris.controller.InputHandler.saveConfig();
                            repaint();
                            return;
                        }

                        // PREVIEW COUNT button click
                        if (menuSettingsPreviewCountBounds != null && menuSettingsPreviewCountBounds.contains(e.getPoint())) {
                            nextPiecesCount = nextPiecesCount % 5 + 1;
                            com.tetris.controller.InputHandler.saveConfig();
                            repaint();
                            return;
                        }

                        // CONTROL SETTINGS button click
                        if (menuSettingsControlBounds != null && menuSettingsControlBounds.contains(e.getPoint())) {
                            showControlSettings = true;
                            rebindingPlayer = 1;
                            rebindingAction = null;
                            repaint();
                            return;
                        }

                        // Volume and mute controls
                        if (bgmTrackBounds.contains(e.getPoint())) {
                            isDraggingBGM = true;
                            updateBGMVolumeFromMouse(e.getX());
                        } else if (sfxTrackBounds.contains(e.getPoint())) {
                            isDraggingSFX = true;
                            updateSFXVolumeFromMouse(e.getX());
                        } else if (bgmMuteBounds.contains(e.getPoint())) {
                            com.tetris.util.SoundManager.setBGMMuted(!com.tetris.util.SoundManager.isBGMMuted());
                            com.tetris.controller.InputHandler.saveConfig();
                            repaint();
                        } else if (sfxMuteBounds.contains(e.getPoint())) {
                            com.tetris.util.SoundManager.setSFXMuted(!com.tetris.util.SoundManager.isSFXMuted());
                            com.tetris.controller.InputHandler.saveConfig();
                            repaint();
                        }
                    } else if (showModeSelectInMenu) {
                        for (int i = 0; i < 6; i++) {
                            if (modeOptionBounds[i] != null && modeOptionBounds[i].contains(e.getPoint())) {
                                selectedModeIndex = i;
                                triggerModeSelection();
                                break;
                            }
                        }
                    } else if (showDifficultySelectInMenu) {
                        for (int i = 0; i < 4; i++) {
                            if (difficultyOptionBounds[i] != null && difficultyOptionBounds[i].contains(e.getPoint())) {
                                selectedDifficultyIndex = i;
                                triggerDifficultySelection();
                                break;
                            }
                        }
                    } else {
                        int numOptions = com.tetris.util.SaveManager.hasSave() ? 8 : 7;
                        for (int i = 0; i < numOptions; i++) {
                            if (menuOptionBounds[i] != null && menuOptionBounds[i].contains(e.getPoint())) {
                                selectedMenuIndex = i;
                                selectCurrentOption();
                                break;
                            }
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.TUTORIAL_SELECT) {
                    for (int i = 0; i < 8; i++) {
                        if (tutorialOptionBounds[i] != null && tutorialOptionBounds[i].contains(e.getPoint())) {
                            selectedTutorialIndex = i;
                            selectTutorialOption();
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.ACHIEVEMENTS) {
                    if (achievementsBackButtonBounds != null && achievementsBackButtonBounds.contains(e.getPoint())) {
                        gameEngine.returnToMenu();
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.LEADERBOARD) {
                    if (backButtonBounds != null && backButtonBounds.contains(e.getPoint())) {
                        gameEngine.returnToMenu();
                    }
                    // Scope tabs (Local vs Global)
                    for (int i = 0; i < 2; i++) {
                        if (leaderboardScopeTabBounds[i] != null && leaderboardScopeTabBounds[i].contains(e.getPoint())) {
                            LeaderboardScope nextScope = LeaderboardScope.values()[i];
                            if (nextScope != selectedLeaderboardScope) {
                                selectedLeaderboardScope = nextScope;
                                if (selectedLeaderboardScope == LeaderboardScope.GLOBAL) {
                                    triggerGlobalLeaderboardLoad();
                                } else {
                                    repaint();
                                }
                            }
                            break;
                        }
                    }
                    // Mode tabs (Endless vs Sprint vs Ultra vs Survival)
                    for (int i = 0; i < 4; i++) {
                        if (leaderboardModeTabBounds[i] != null && leaderboardModeTabBounds[i].contains(e.getPoint())) {
                            selectedLeaderboardMode = GameMode.values()[i];
                            if (selectedLeaderboardScope == LeaderboardScope.GLOBAL) {
                                triggerGlobalLeaderboardLoad();
                            } else {
                                repaint();
                            }
                            break;
                        }
                    }
                    // Difficulty tabs
                    for (int i = 0; i < 3; i++) {
                        if (leaderboardTabBounds[i] != null && leaderboardTabBounds[i].contains(e.getPoint())) {
                            selectedLeaderboardDifficulty = com.tetris.controller.GameEngine.Difficulty.values()[i];
                            if (selectedLeaderboardScope == LeaderboardScope.GLOBAL) {
                                triggerGlobalLeaderboardLoad();
                            } else {
                                repaint();
                            }
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.PLAYING || state == com.tetris.controller.GameEngine.GameState.TUTORIAL) {
                    if (gameEngine.isGameOver()) {
                        gameEngine.returnToMenu();
                    } else if (gameEngine.isPaused()) {
                        if (showSettingsInPause) {
                            // Volume and Mute clicks inside pause settings
                            if (bgmTrackBounds.contains(e.getPoint())) {
                                isDraggingBGM = true;
                                updateBGMVolumeFromMouse(e.getX());
                                return;
                            } else if (sfxTrackBounds.contains(e.getPoint())) {
                                isDraggingSFX = true;
                                updateSFXVolumeFromMouse(e.getX());
                                return;
                            } else if (bgmMuteBounds.contains(e.getPoint())) {
                                com.tetris.util.SoundManager.setBGMMuted(!com.tetris.util.SoundManager.isBGMMuted());
                                repaint();
                                return;
                            } else if (sfxMuteBounds.contains(e.getPoint())) {
                                com.tetris.util.SoundManager.setSFXMuted(!com.tetris.util.SoundManager.isSFXMuted());
                                // Play a test SFX so the toggle is immediately audible
                                if (!com.tetris.util.SoundManager.isSFXMuted()) {
                                    com.tetris.util.SoundManager.playSFX("/resources/clear.wav");
                                }
                                repaint();
                                return;
                            }
                        }
                        int numPauseOptions = showSettingsInPause ? 5 : 4;
                        for (int i = 0; i < numPauseOptions; i++) {
                            if (pauseOptionBounds[i] != null && pauseOptionBounds[i].contains(e.getPoint())) {
                                selectedPauseIndex = i;
                                selectPauseMenuItem();
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDraggingBGM || isDraggingSFX) {
                    com.tetris.controller.InputHandler.saveConfig();
                }
                isDraggingBGM = false;
                isDraggingSFX = false;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameEngine == null) return;
                com.tetris.controller.GameEngine.GameState state = gameEngine.getGameState();
                if (state == com.tetris.controller.GameEngine.GameState.MENU) {
                    if (showSettingsInMenu) {
                        repaint();
                    } else if (showModeSelectInMenu) {
                        for (int i = 0; i < 6; i++) {
                            if (modeOptionBounds[i] != null && modeOptionBounds[i].contains(e.getPoint())) {
                                if (selectedModeIndex != i) {
                                    selectedModeIndex = i;
                                    repaint();
                                }
                                break;
                            }
                        }
                    } else if (showDifficultySelectInMenu) {
                        for (int i = 0; i < 4; i++) {
                            if (difficultyOptionBounds[i] != null && difficultyOptionBounds[i].contains(e.getPoint())) {
                                if (selectedDifficultyIndex != i) {
                                    selectedDifficultyIndex = i;
                                    repaint();
                                }
                                break;
                            }
                        }
                    } else {
                        int numOptions = com.tetris.util.SaveManager.hasSave() ? 8 : 7;
                        for (int i = 0; i < numOptions; i++) {
                            if (menuOptionBounds[i] != null && menuOptionBounds[i].contains(e.getPoint())) {
                                if (selectedMenuIndex != i) {
                                    selectedMenuIndex = i;
                                    repaint();
                                }
                                break;
                            }
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.TUTORIAL_SELECT) {
                    for (int i = 0; i < 8; i++) {
                        if (tutorialOptionBounds[i] != null && tutorialOptionBounds[i].contains(e.getPoint())) {
                            if (selectedTutorialIndex != i) {
                                selectedTutorialIndex = i;
                                repaint();
                            }
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.ACHIEVEMENTS) {
                    repaint(); // Repaint to update achievements back button hover state
                } else if ((state == com.tetris.controller.GameEngine.GameState.PLAYING || state == com.tetris.controller.GameEngine.GameState.TUTORIAL) && gameEngine.isPaused()) {
                    int numPauseOptions = showSettingsInPause ? 5 : 4;
                    for (int i = 0; i < numPauseOptions; i++) {
                        if (pauseOptionBounds[i] != null && pauseOptionBounds[i].contains(e.getPoint())) {
                            if (selectedPauseIndex != i) {
                                selectedPauseIndex = i;
                                repaint();
                            }
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.LEADERBOARD) {
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDraggingBGM) {
                    updateBGMVolumeFromMouse(e.getX());
                } else if (isDraggingSFX) {
                    updateSFXVolumeFromMouse(e.getX());
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        // Set the size of the game panel (grid + sidebar)
        setPreferredSize(new Dimension(COLS * TILE_SIZE + SIDEBAR_WIDTH, ROWS * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
    }

    // Set the game engine reference
    public void setGameEngine(com.tetris.controller.GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public void setGameEngine2(com.tetris.controller.GameEngine gameEngine2) {
        this.gameEngine2 = gameEngine2;
        synchronized (particles2) {
            particles2.clear();
        }
        synchronized (scorePopups2) {
            scorePopups2.clear();
        }
        shakeIntensity2 = 0;
        shakeEndTime2 = 0;
    }

    public void updateWindowSize() {
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (window instanceof javax.swing.JFrame) {
            javax.swing.JFrame frame = (javax.swing.JFrame) window;
            if (gameEngine != null && gameEngine.getGameMode() == GameMode.PVP && gameEngine.getGameState() == com.tetris.controller.GameEngine.GameState.PLAYING) {
                setPreferredSize(new Dimension(1000, ROWS * TILE_SIZE));
            } else {
                setPreferredSize(new Dimension(500, ROWS * TILE_SIZE));
            }
            revalidate();
            frame.pack();
            frame.setLocationRelativeTo(null);
        }
    }

    public boolean isShowSettingsInMenu() {
        return showSettingsInMenu;
    }

    public void setShowSettingsInMenu(boolean show) {
        this.showSettingsInMenu = show;
        repaint();
    }

    public boolean isShowDifficultySelectInMenu() {
        return showDifficultySelectInMenu;
    }

    public void setShowDifficultySelectInMenu(boolean show) {
        this.showDifficultySelectInMenu = show;
        repaint();
    }

    public boolean isShowModeSelectInMenu() {
        return showModeSelectInMenu;
    }

    public void setShowModeSelectInMenu(boolean show) {
        this.showModeSelectInMenu = show;
        repaint();
    }

    public void setSelectedModeIndex(int index) {
        this.selectedModeIndex = index;
        repaint();
    }

    public boolean isShowSettingsInPause() {
        return showSettingsInPause;
    }

    public void setShowSettingsInPause(boolean show) {
        this.showSettingsInPause = show;
        repaint();
    }

    public void resetUIState() {
        showSettingsInMenu = false;
        showSettingsInPause = false;
        showDifficultySelectInMenu = false;
        showModeSelectInMenu = false;
        showControlSettings = false;
        rebindingAction = null;
        selectedPauseIndex = 0;
        repaint();
    }

    public boolean isShowControlSettings() { return showControlSettings; }
    public void setShowControlSettings(boolean val) { this.showControlSettings = val; }
    public String getRebindingAction() { return rebindingAction; }
    public void setRebindingAction(String val) { this.rebindingAction = val; }
    public int getRebindingPlayer() { return rebindingPlayer; }
    public void setRebindingPlayer(int val) { this.rebindingPlayer = val; }

    // Set the current piece
    public void setCurrentPiece(Piece piece) {
        this.currentPiece = piece;
    }

    // Deprecated: Score is now retrieved from gameEngine
    public void setScore(int score) {
        // Keeping it for compatibility with existing calls, but will use engine's score
    }

    // Double buffering fields
    private java.awt.image.BufferedImage visibleBuffer;
    private java.awt.image.BufferedImage drawingBuffer;
    private final Object bufferLock = new Object();

    @Override
    protected void paintComponent(Graphics g) {
        synchronized (bufferLock) {
            if (visibleBuffer != null) {
                g.drawImage(visibleBuffer, 0, 0, getWidth(), getHeight(), null);
            } else {
                super.paintComponent(g);
                drawGameScene(g);
            }
        }
    }

    private void drawGameScene(Graphics g) {
        if (gameEngine == null) return;
        synchronized (gameEngine) {
            if (gameEngine.getGameMode() == GameMode.PVP && gameEngine2 != null) {
                synchronized (gameEngine2) {
                    drawGameSceneInternal(g);
                }
            } else {
                drawGameSceneInternal(g);
            }
        }
    }

    private void drawGameSceneInternal(Graphics g) {
        switch (gameEngine.getGameState()) {
            case MENU:
                drawMenuScreen(g);
                break;
            case LEADERBOARD:
                drawLeaderboardScreen(g);
                break;
            case TUTORIAL_SELECT:
                drawTutorialSelectScreen(g);
                break;
            case ACHIEVEMENTS:
                drawAchievementsScreen(g);
                break;
            case PLAYING:
            case TUTORIAL:
            default:
                Graphics2D g2d = (Graphics2D) g;
                java.awt.geom.AffineTransform oldTransform = g2d.getTransform();

                if (gameEngine.getGameMode() == GameMode.PVP && gameEngine2 != null) {
                    // --- PLAYER 1 (Left: x=0) ---
                    java.awt.geom.AffineTransform p1Transform = g2d.getTransform();
                    int currentShakeX = 0;
                    int currentShakeY = 0;
                    if (System.currentTimeMillis() < shakeEndTime) {
                        double progress = (shakeEndTime - System.currentTimeMillis()) / (double) shakeDuration;
                        if (progress > 0) {
                            int currentMaxOffset = (int) (shakeIntensity * progress);
                            if (currentMaxOffset > 0) {
                                currentShakeX = (int) (Math.random() * currentMaxOffset * 2 - currentMaxOffset);
                                currentShakeY = (int) (Math.random() * currentMaxOffset * 2 - currentMaxOffset);
                            }
                        }
                    }
                    if (currentShakeX != 0 || currentShakeY != 0) {
                        g2d.translate(currentShakeX, currentShakeY);
                    }

                    drawGrid(g2d);
                    drawFixedBlocks(g2d, gameEngine.getBoard());
                    drawCurrentPiece(g2d, gameEngine.getCurrentPiece(), gameEngine.getBoard(), gameEngine);
                    drawParticles(g2d, particles);
                    drawScorePopups(g2d, scorePopups);
                    drawSidebar(g2d, gameEngine);
                    drawPerfectClearOverlay(g2d, perfectClearStartTime);
                    if (gameEngine.isGameOver() && !gameEngine2.isGameOver()) {
                        drawLocalPvpGameOver(g2d);
                    }
                    g2d.setTransform(p1Transform);

                    // --- PLAYER 2 (Right: x=500) ---
                    java.awt.geom.AffineTransform p2Transform = g2d.getTransform();
                    g2d.translate(500, 0);
                    int currentShakeX2 = 0;
                    int currentShakeY2 = 0;
                    if (System.currentTimeMillis() < shakeEndTime2) {
                        double progress = (shakeEndTime2 - System.currentTimeMillis()) / (double) shakeDuration2;
                        if (progress > 0) {
                            int currentMaxOffset = (int) (shakeIntensity2 * progress);
                            if (currentMaxOffset > 0) {
                                currentShakeX2 = (int) (Math.random() * currentMaxOffset * 2 - currentMaxOffset);
                                currentShakeY2 = (int) (Math.random() * currentMaxOffset * 2 - currentMaxOffset);
                            }
                        }
                    }
                    if (currentShakeX2 != 0 || currentShakeY2 != 0) {
                        g2d.translate(currentShakeX2, currentShakeY2);
                    }

                    drawGrid(g2d);
                    drawFixedBlocks(g2d, gameEngine2.getBoard());
                    drawCurrentPiece(g2d, gameEngine2.getCurrentPiece(), gameEngine2.getBoard(), gameEngine2);
                    drawParticles(g2d, particles2);
                    drawScorePopups(g2d, scorePopups2);
                    drawSidebar(g2d, gameEngine2);
                    drawPerfectClearOverlay(g2d, perfectClearStartTime2);
                    if (gameEngine2.isGameOver() && !gameEngine.isGameOver()) {
                        drawLocalPvpGameOver(g2d);
                    }
                    g2d.setTransform(p2Transform);

                    // --- OVERLAYS (Centered over both windows) ---
                    // Draw Pause Overlay
                    if (gameEngine.isPaused()) {
                        drawPauseOverlay(g2d);
                    }

                    // Draw Game Over Overlay
                    if (gameEngine.isGameOver() && gameEngine2.isGameOver()) {
                        drawPvpGameOverOverlay(g2d);
                    }

                } else {
                    // Single Player Mode
                    int currentShakeX = 0;
                    int currentShakeY = 0;
                    if (System.currentTimeMillis() < shakeEndTime) {
                        double progress = (shakeEndTime - System.currentTimeMillis()) / (double) shakeDuration;
                        if (progress > 0) {
                            int currentMaxOffset = (int) (shakeIntensity * progress);
                            if (currentMaxOffset > 0) {
                                currentShakeX = (int) (Math.random() * currentMaxOffset * 2 - currentMaxOffset);
                                currentShakeY = (int) (Math.random() * currentMaxOffset * 2 - currentMaxOffset);
                            }
                        }
                    }

                    if (currentShakeX != 0 || currentShakeY != 0) {
                        g2d.translate(currentShakeX, currentShakeY);
                    }

                    // Draw Game Area (Left)
                    drawGrid(g2d);
                    if (gameEngine.getGameState() == com.tetris.controller.GameEngine.GameState.TUTORIAL) {
                        drawTutorialTargetZone(g2d);
                    }
                    drawFixedBlocks(g2d);
                    drawCurrentPiece(g2d);

                    // Draw particles inside the grid area
                    drawParticles(g2d);

                    // Draw active score popups on top of the grid
                    drawScorePopups(g2d);

                    // Draw Sidebar (Right)
                    drawSidebar(g2d);

                    // Draw Pause Overlay
                    if (gameEngine.isPaused()) {
                        drawPauseOverlay(g2d);
                    }

                    // Draw Game Over Overlay
                    if (gameEngine.isGameOver()) {
                        if (gameEngine.getGameState() == com.tetris.controller.GameEngine.GameState.TUTORIAL) {
                            drawTutorialVictoryOverlay(g2d);
                        } else if (gameEngine.isEnteringName()) {
                            drawNameEntryOverlay(g2d);
                        } else {
                            drawGameOverOverlay(g2d);
                        }
                    }

                    // Draw Tutorial Overlays
                    if (gameEngine.getGameState() == com.tetris.controller.GameEngine.GameState.TUTORIAL) {
                        drawTutorialOverlays(g2d);
                    }

                    // Draw Perfect Clear Overlay
                    drawPerfectClearOverlay(g2d);
                }

                g2d.setTransform(oldTransform);
                break;
        }
        
        if (activeToastTitle != null && System.currentTimeMillis() - toastStartTime < TOAST_DURATION) {
            drawAchievementToast((Graphics2D) g);
        }
    }

    public void renderOffscreen() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            w = COLS * TILE_SIZE + SIDEBAR_WIDTH;
            if (gameEngine != null && gameEngine.getGameMode() == GameMode.PVP && gameEngine.getGameState() == com.tetris.controller.GameEngine.GameState.PLAYING) {
                w = 1000;
            } else {
                w = 500;
            }
            h = ROWS * TILE_SIZE;
        }

        // Get scaling factor (HiDPI support)
        double scaleX = 1.0;
        double scaleY = 1.0;
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc != null) {
            java.awt.geom.AffineTransform tx = gc.getDefaultTransform();
            scaleX = tx.getScaleX();
            scaleY = tx.getScaleY();
        } else {
            try {
                java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                java.awt.GraphicsConfiguration defaultGc = ge.getDefaultScreenDevice().getDefaultConfiguration();
                if (defaultGc != null) {
                    java.awt.geom.AffineTransform tx = defaultGc.getDefaultTransform();
                    scaleX = tx.getScaleX();
                    scaleY = tx.getScaleY();
                }
            } catch (Exception ignored) {}
        }

        int imgW = (int) (w * scaleX);
        int imgH = (int) (h * scaleY);

        synchronized (bufferLock) {
            if (drawingBuffer == null || drawingBuffer.getWidth() != imgW || drawingBuffer.getHeight() != imgH) {
                drawingBuffer = new java.awt.image.BufferedImage(imgW, imgH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                visibleBuffer = new java.awt.image.BufferedImage(imgW, imgH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            }
        }

        Graphics2D g2d = drawingBuffer.createGraphics();
        g2d.scale(scaleX, scaleY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Clear buffer
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, w, h);

        drawGameScene(g2d);

        g2d.dispose();

        synchronized (bufferLock) {
            java.awt.image.BufferedImage temp = visibleBuffer;
            visibleBuffer = drawingBuffer;
            drawingBuffer = temp;
        }

        repaint();
    }

    // Menu navigation helper
    public void navigateMenu(int dir) {
        if (showDifficultySelectInMenu) {
            selectedDifficultyIndex = (selectedDifficultyIndex + dir + 4) % 4;
        } else if (showModeSelectInMenu) {
            selectedModeIndex = (selectedModeIndex + dir + 5) % 5;
        } else {
            int numOptions = com.tetris.util.SaveManager.hasSave() ? 8 : 7;
            selectedMenuIndex = (selectedMenuIndex + dir + numOptions) % numOptions;
        }
        repaint();
    }

    // Menu select action helper
    public void selectCurrentOption() {
        if (showDifficultySelectInMenu) {
            triggerDifficultySelection();
        } else if (showModeSelectInMenu) {
            triggerModeSelection();
        } else {
            boolean hasSave = com.tetris.util.SaveManager.hasSave();
            if (hasSave) {
                switch (selectedMenuIndex) {
                    case 0:
                        gameEngine.loadGame();
                        break;
                    case 1:
                        showModeSelectInMenu = true;
                        selectedModeIndex = 0; // Default to ENDLESS
                        break;
                    case 2:
                        gameEngine.startTutorial();
                        break;
                    case 3:
                        // AI DEMO Option
                        gameEngine.setDifficulty(com.tetris.controller.GameEngine.Difficulty.NORMAL);
                        showDifficultySelectInMenu = false;
                        gameEngine.startGame();
                        gameEngine.setAiPlay(true);
                        break;
                    case 4:
                        showSettingsInMenu = true;
                        break;
                    case 5:
                        gameEngine.setGameState(com.tetris.controller.GameEngine.GameState.ACHIEVEMENTS);
                        break;
                    case 6:
                        gameEngine.showLeaderboard();
                        break;
                    case 7:
                        System.exit(0);
                        break;
                }
            } else {
                switch (selectedMenuIndex) {
                    case 0:
                        showModeSelectInMenu = true;
                        selectedModeIndex = 0; // Default to ENDLESS
                        break;
                    case 1:
                        gameEngine.startTutorial();
                        break;
                    case 2:
                        // AI DEMO Option
                        gameEngine.setDifficulty(com.tetris.controller.GameEngine.Difficulty.NORMAL);
                        showDifficultySelectInMenu = false;
                        gameEngine.startGame();
                        gameEngine.setAiPlay(true);
                        break;
                    case 3:
                        showSettingsInMenu = true;
                        break;
                    case 4:
                        gameEngine.setGameState(com.tetris.controller.GameEngine.GameState.ACHIEVEMENTS);
                        break;
                    case 5:
                        gameEngine.showLeaderboard();
                        break;
                    case 6:
                        System.exit(0);
                        break;
                }
            }
        }
        repaint();
    }

    private void triggerModeSelection() {
        switch (selectedModeIndex) {
            case 0: // ENDLESS
                gameEngine.setGameMode(GameMode.ENDLESS);
                showModeSelectInMenu = false;
                showDifficultySelectInMenu = true;
                selectedDifficultyIndex = 0; // Default to EASY
                break;
            case 1: // SPRINT
                gameEngine.setGameMode(GameMode.SPRINT);
                showModeSelectInMenu = false;
                showDifficultySelectInMenu = true;
                selectedDifficultyIndex = 0; // Default to EASY
                break;
            case 2: // ULTRA
                gameEngine.setGameMode(GameMode.ULTRA);
                showModeSelectInMenu = false;
                showDifficultySelectInMenu = true;
                selectedDifficultyIndex = 0; // Default to EASY
                break;
            case 3: // SURVIVAL
                gameEngine.setGameMode(GameMode.SURVIVAL);
                showModeSelectInMenu = false;
                showDifficultySelectInMenu = true;
                selectedDifficultyIndex = 0; // Default to EASY
                break;
            case 4: // PVP BATTLE
                gameEngine.setGameMode(GameMode.PVP);
                showModeSelectInMenu = false;
                gameEngine.startGame();
                break;
            case 5: // BACK
                showModeSelectInMenu = false;
                selectedMenuIndex = com.tetris.util.SaveManager.hasSave() ? 1 : 0; // Return to Play Game
                break;
        }
        repaint();
    }

    private void triggerDifficultySelection() {
        switch (selectedDifficultyIndex) {
            case 0:
                gameEngine.setDifficulty(com.tetris.controller.GameEngine.Difficulty.EASY);
                showDifficultySelectInMenu = false;
                gameEngine.startGame();
                break;
            case 1:
                gameEngine.setDifficulty(com.tetris.controller.GameEngine.Difficulty.NORMAL);
                showDifficultySelectInMenu = false;
                gameEngine.startGame();
                break;
            case 2:
                gameEngine.setDifficulty(com.tetris.controller.GameEngine.Difficulty.HARD);
                showDifficultySelectInMenu = false;
                gameEngine.startGame();
                break;
            case 3:
                showDifficultySelectInMenu = false;
                showModeSelectInMenu = true; // Return to Mode Selection
                if (gameEngine.getGameMode() == GameMode.SPRINT) {
                    selectedModeIndex = 1;
                } else if (gameEngine.getGameMode() == GameMode.ULTRA) {
                    selectedModeIndex = 2;
                } else if (gameEngine.getGameMode() == GameMode.SURVIVAL) {
                    selectedModeIndex = 3;
                } else {
                    selectedModeIndex = 0;
                }
                break;
        }
        repaint();
    }

    // private com.tetris.controller.GameEngine.Difficulty getNextDifficulty(com.tetris.controller.GameEngine.Difficulty current) {
    //     switch (current) {
    //         case EASY:
    //             return com.tetris.controller.GameEngine.Difficulty.NORMAL;
    //         case NORMAL:
    //             return com.tetris.controller.GameEngine.Difficulty.HARD;
    //         case HARD:
    //             return com.tetris.controller.GameEngine.Difficulty.EASY;
    //         default:
    //             return com.tetris.controller.GameEngine.Difficulty.NORMAL;
    //     }
    // }

    // Update floating particle positions
    private void updateFloatingPieces() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) width = COLS * TILE_SIZE + SIDEBAR_WIDTH;
        if (height <= 0) height = ROWS * TILE_SIZE;

        for (FloatingPiece fp : floatingPieces) {
            fp.y += fp.speed;
            fp.angle += fp.rotationSpeed;
            if (fp.y > height + 60) {
                Random rand = new Random();
                fp.y = -60;
                fp.x = rand.nextInt(width);
                fp.speed = 0.4 + rand.nextDouble() * 0.8;
            }
        }
    }

    // Draw floating particle in menu/leaderboard background
    private void drawFloatingPiece(Graphics2D g2, FloatingPiece fp) {
        com.tetris.model.Tetromino type = com.tetris.model.Tetromino.values()[fp.typeIndex];
        int[][] coords = type.getCoords();

        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        g2.translate(fp.x, fp.y);
        g2.rotate(fp.angle);
        g2.scale(fp.scale, fp.scale);

        int blockSize = 18;
        Color themeColor = com.tetris.util.ThemeManager.getMappedColor(fp.color);
        Color blockColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 30);
        Color borderColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 70);

        for (int[] cell : coords) {
            int bx = cell[0] * blockSize - blockSize;
            int by = cell[1] * blockSize - blockSize;
            g2.setColor(blockColor);
            g2.fillRect(bx, by, blockSize, blockSize);
            g2.setColor(borderColor);
            g2.drawRect(bx, by, blockSize, blockSize);
        }

        g2.setTransform(oldTransform);
    }

    // Draw start menu screen
    private void drawMenuScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Dark indigo space gradient background
        java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, new Color(10, 10, 25), 0, getHeight(), new Color(30, 15, 50));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw floating background blocks
        for (FloatingPiece fp : floatingPieces) {
            drawFloatingPiece(g2d, fp);
        }

        if (showSettingsInMenu) {
            if (showControlSettings) {
                drawControlSettings(g2d);
            } else {
                drawMenuSettings(g2d);
            }
            return;
        }

        if (showModeSelectInMenu) {
            drawModeSelectScreen(g2d);
            return;
        }

        if (showDifficultySelectInMenu) {
            drawDifficultySelectScreen(g2d);
            return;
        }

        // Animated neon "TETRIS" title
        long now = System.currentTimeMillis();
        double scale = 1.0 + 0.03 * Math.sin(now / 350.0);
        int titleY = 120;

        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(getWidth() / 2.0, titleY);
        g2d.scale(scale, scale);
        g2d.translate(-getWidth() / 2.0, -titleY);

        String titleText = "TETRIS";
        g2d.setFont(getCachedFont("Impact", Font.BOLD | Font.ITALIC, 68));
        FontMetrics fmTitle = g2d.getFontMetrics();
        int titleX = (getWidth() - fmTitle.stringWidth(titleText)) / 2;

        Color[] neonColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, new Color(160, 32, 240)};
        int charX = titleX;
        for (int i = 0; i < titleText.length(); i++) {
            String ch = String.valueOf(titleText.charAt(i));
            Color letterColor = neonColors[i % neonColors.length];
            
            // Draw neon glow layers
            g2d.setColor(new Color(letterColor.getRed(), letterColor.getGreen(), letterColor.getBlue(), 60));
            g2d.drawString(ch, charX - 3, titleY - 3);
            g2d.drawString(ch, charX + 3, titleY + 3);
            g2d.drawString(ch, charX - 1, titleY + 1);
            g2d.drawString(ch, charX + 1, titleY - 1);

            // Draw white core
            g2d.setColor(Color.WHITE);
            g2d.drawString(ch, charX, titleY);
            charX += fmTitle.stringWidth(ch);
        }

        g2d.setTransform(oldTransform);

        // Neon subtitle
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 13));
        g2d.setColor(new Color(0, 255, 255));
        String subtitle = "霓虹街機版 (NEON ARCADE EDITION)";
        int subX = (getWidth() - g2d.getFontMetrics().stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subX, 155);

        // Options
        boolean hasSave = com.tetris.util.SaveManager.hasSave();
        int numOptions = hasSave ? 8 : 7;
        int startY = hasSave ? 180 : 205;
        int gap = 38;
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 20));
        FontMetrics fmOption = g2d.getFontMetrics();

        for (int i = 0; i < numOptions; i++) {
            String label = "";
            if (hasSave) {
                switch (i) {
                    case 0: label = "繼續上一局"; break;
                    case 1: label = "開始新遊戲"; break;
                    case 2: label = "教學與新手引導"; break;
                    case 3: label = "AI 自動遊玩演示"; break;
                    case 4: label = "遊戲設定"; break;
                    case 5: label = "成就與說明"; break;
                    case 6: label = "高分排行榜"; break;
                    case 7: label = "結束遊戲"; break;
                }
            } else {
                switch (i) {
                    case 0: label = "開始新遊戲"; break;
                    case 1: label = "教學與新手引導"; break;
                    case 2: label = "AI 自動遊玩演示"; break;
                    case 3: label = "遊戲設定"; break;
                    case 4: label = "成就與說明"; break;
                    case 5: label = "高分排行榜"; break;
                    case 6: label = "結束遊戲"; break;
                }
            }

            int textWidth = fmOption.stringWidth(label);
            int textHeight = fmOption.getHeight();
            int x = (getWidth() - textWidth) / 2;
            int y = startY + i * gap;

            if (menuOptionBounds[i] == null) menuOptionBounds[i] = new Rectangle(x - 20, y - textHeight + 5, textWidth + 40, textHeight + 10); else menuOptionBounds[i].setBounds(x - 20, y - textHeight + 5, textWidth + 40, textHeight + 10);

            boolean isSelected = (i == selectedMenuIndex);
            if (isSelected) {
                // Background highlight box
                g2d.setColor(new Color(0, 255, 255, 35));
                g2d.fillRoundRect(menuOptionBounds[i].x, menuOptionBounds[i].y, menuOptionBounds[i].width, menuOptionBounds[i].height, 10, 10);
                g2d.setColor(new Color(0, 255, 255));
                g2d.drawRoundRect(menuOptionBounds[i].x, menuOptionBounds[i].y, menuOptionBounds[i].width, menuOptionBounds[i].height, 10, 10);

                // Highlighted text
                g2d.setColor(Color.WHITE);
                g2d.drawString(label, x, y);

                // Small neon block indicators on both sides
                int sqSize = 8;
                g2d.fillRect(menuOptionBounds[i].x + 6, y - textHeight/2 - sqSize/2 + 2, sqSize, sqSize);
                g2d.fillRect(menuOptionBounds[i].x + menuOptionBounds[i].width - 14, y - textHeight/2 - sqSize/2 + 2, sqSize, sqSize);
            } else {
                // Dim/inactive text
                g2d.setColor(new Color(150, 150, 180));
                g2d.drawString(label, x, y);
            }
        }

        // Control Hints
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(120, 120, 150));
        String hint1 = "使用 ⬆ / ⬇ 方向鍵或滑鼠移動游標";
        String hint2 = "按下 ENTER 鍵或點擊滑鼠確認選擇";
        g2d.drawString(hint1, (getWidth() - g2d.getFontMetrics().stringWidth(hint1)) / 2, 530);
        g2d.drawString(hint2, (getWidth() - g2d.getFontMetrics().stringWidth(hint2)) / 2, 550);
    }

    // Draw leaderboard screen
    private void drawLeaderboardScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Background
        java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, new Color(10, 10, 25), 0, getHeight(), new Color(30, 15, 50));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw floating background blocks
        for (FloatingPiece fp : floatingPieces) {
            drawFloatingPiece(g2d, fp);
        }

        // Title
        g2d.setFont(getCachedFont("Impact", Font.BOLD, 42));
        g2d.setColor(new Color(255, 215, 0)); // Gold
        String title = "TOP HIGH SCORES";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 75);

        // Underline
        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.fillRect(60, 80, getWidth() - 120, 3);

        java.awt.Point mousePos = getMousePosition();

        // Draw Scope Tabs (Local vs Global)
        int scopeTabW = 120;
        int scopeTabH = 24;
        int scopeTabGap = 16;
        int totalScopeW = 2 * scopeTabW + scopeTabGap;
        int startScopeX = (getWidth() - totalScopeW) / 2;
        int scopeTabY = 90;
        
        for (int i = 0; i < 2; i++) {
            int x = startScopeX + i * (scopeTabW + scopeTabGap);
            if (leaderboardScopeTabBounds[i] == null) leaderboardScopeTabBounds[i] = new Rectangle(x, scopeTabY, scopeTabW, scopeTabH); else leaderboardScopeTabBounds[i].setBounds(x, scopeTabY, scopeTabW, scopeTabH);
            
            LeaderboardScope tabScope = LeaderboardScope.values()[i];
            boolean isSelected = (selectedLeaderboardScope == tabScope);
            boolean isHovered = (mousePos != null && leaderboardScopeTabBounds[i].contains(mousePos));
            
            if (isSelected) {
                g2d.setColor(new Color(0, 255, 100, 55)); // Neon Green glow
                g2d.fillRoundRect(x, scopeTabY, scopeTabW, scopeTabH, 8, 8);
                g2d.setColor(new Color(0, 255, 100));
                g2d.drawRoundRect(x, scopeTabY, scopeTabW, scopeTabH, 8, 8);
            } else if (isHovered) {
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(x, scopeTabY, scopeTabW, scopeTabH, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.drawRoundRect(x, scopeTabY, scopeTabW, scopeTabH, 8, 8);
            } else {
                g2d.setColor(new Color(255, 255, 255, 10));
                g2d.fillRoundRect(x, scopeTabY, scopeTabW, scopeTabH, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 45));
                g2d.drawRoundRect(x, scopeTabY, scopeTabW, scopeTabH, 8, 8);
            }
            
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 10));
            g2d.setColor(isSelected ? new Color(0, 255, 100) : new Color(200, 200, 200));
            String scopeLabel = (tabScope == LeaderboardScope.LOCAL) ? "本地榜單 (LOCAL)" : "全球榜單 (GLOBAL)";
            int labelW = g2d.getFontMetrics().stringWidth(scopeLabel);
            int labelH = g2d.getFontMetrics().getAscent();
            g2d.drawString(scopeLabel, x + (scopeTabW - labelW) / 2, scopeTabY + (scopeTabH + labelH) / 2 - 2);
        }

        // Draw Mode Tabs (Endless vs Sprint vs Ultra vs Survival)
        int modeTabW = 75;
        int modeTabH = 26;
        int modeTabGap = 8;
        int totalModeW = 324;
        int startModeX = (getWidth() - totalModeW) / 2;
        int modeTabY = 122;
        
        for (int i = 0; i < 4; i++) {
            int x = startModeX + i * (modeTabW + modeTabGap);
            if (leaderboardModeTabBounds[i] == null) leaderboardModeTabBounds[i] = new Rectangle(x, modeTabY, modeTabW, modeTabH); else leaderboardModeTabBounds[i].setBounds(x, modeTabY, modeTabW, modeTabH);
            
            GameMode tabMode = GameMode.values()[i];
            boolean isSelected = (selectedLeaderboardMode == tabMode);
            boolean isHovered = (mousePos != null && leaderboardModeTabBounds[i].contains(mousePos));
            
            if (isSelected) {
                g2d.setColor(new Color(255, 100, 255, 55)); // Neon purple/pink glow
                g2d.fillRoundRect(x, modeTabY, modeTabW, modeTabH, 8, 8);
                g2d.setColor(new Color(255, 100, 255));
                g2d.drawRoundRect(x, modeTabY, modeTabW, modeTabH, 8, 8);
            } else if (isHovered) {
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(x, modeTabY, modeTabW, modeTabH, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.drawRoundRect(x, modeTabY, modeTabW, modeTabH, 8, 8);
            } else {
                g2d.setColor(new Color(255, 255, 255, 10));
                g2d.fillRoundRect(x, modeTabY, modeTabW, modeTabH, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 45));
                g2d.drawRoundRect(x, modeTabY, modeTabW, modeTabH, 8, 8);
            }
            
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 10));
            g2d.setColor(isSelected ? new Color(255, 100, 255) : new Color(200, 200, 200));
            String modeLabel;
            if (tabMode == GameMode.ENDLESS) {
                modeLabel = "ENDLESS";
            } else if (tabMode == GameMode.SPRINT) {
                modeLabel = "SPRINT (40L)";
            } else if (tabMode == GameMode.ULTRA) {
                modeLabel = "ULTRA (2M)";
            } else {
                modeLabel = "SURVIVAL";
            }
            int labelW = g2d.getFontMetrics().stringWidth(modeLabel);
            int labelH = g2d.getFontMetrics().getAscent();
            g2d.drawString(modeLabel, x + (modeTabW - labelW) / 2, modeTabY + (modeTabH + labelH) / 2 - 2);
        }

        // Draw Difficulty Tabs
        int tabW = 85;
        int tabH = 26;
        int tabGap = 12;
        int totalW = 3 * tabW + 2 * tabGap;
        int startX = (getWidth() - totalW) / 2;
        int tabY = 154;
        
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (tabW + tabGap);
            if (leaderboardTabBounds[i] == null) leaderboardTabBounds[i] = new Rectangle(x, tabY, tabW, tabH); else leaderboardTabBounds[i].setBounds(x, tabY, tabW, tabH);
            
            com.tetris.controller.GameEngine.Difficulty tabDiff = com.tetris.controller.GameEngine.Difficulty.values()[i];
            boolean isSelected = (selectedLeaderboardDifficulty == tabDiff);
            boolean isHovered = (mousePos != null && leaderboardTabBounds[i].contains(mousePos));
            
            if (isSelected) {
                g2d.setColor(new Color(0, 255, 255, 55)); // Cyan glow
                g2d.fillRoundRect(x, tabY, tabW, tabH, 8, 8);
                g2d.setColor(new Color(0, 255, 255));
                g2d.drawRoundRect(x, tabY, tabW, tabH, 8, 8);
            } else if (isHovered) {
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(x, tabY, tabW, tabH, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.drawRoundRect(x, tabY, tabW, tabH, 8, 8);
            } else {
                g2d.setColor(new Color(255, 255, 255, 10));
                g2d.fillRoundRect(x, tabY, tabW, tabH, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 45));
                g2d.drawRoundRect(x, tabY, tabW, tabH, 8, 8);
            }
            
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 11));
            g2d.setColor(isSelected ? new Color(0, 255, 255) : new Color(200, 200, 200));
            String tabLabel = tabDiff.getLabel();
            int labelW = g2d.getFontMetrics().stringWidth(tabLabel);
            int labelH = g2d.getFontMetrics().getAscent();
            g2d.drawString(tabLabel, x + (tabW - labelW) / 2, tabY + (tabH + labelH) / 2 - 2);
        }

        // Columns headers
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 13));
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString("RANK", 45, 190);
        g2d.drawString("NAME", 95, 190);
        if (selectedLeaderboardMode == GameMode.SPRINT) {
            g2d.drawString("TIME", 205, 190);
            g2d.drawString("LINES", 295, 190);
        } else if (selectedLeaderboardMode == GameMode.SURVIVAL) {
            g2d.drawString("TIME", 205, 190);
            g2d.drawString("SCORE", 295, 190);
        } else {
            g2d.drawString("SCORE", 205, 190);
            g2d.drawString("DIFF", 295, 190);
        }
        g2d.drawString("DATE", 385, 190);

        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.drawLine(40, 200, getWidth() - 40, 200);

        // List
        if (isLeaderboardLoading) {
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 15));
            g2d.setColor(new Color(0, 255, 255));
            String loadingText = "連線全球伺服器中...";
            int loadingW = g2d.getFontMetrics().stringWidth(loadingText);
            g2d.drawString(loadingText, (getWidth() - loadingW) / 2, 280);
            
            g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
            g2d.setColor(new Color(150, 150, 180));
            String subText = "正在讀取全球排行榜數據...";
            int subW = g2d.getFontMetrics().stringWidth(subText);
            g2d.drawString(subText, (getWidth() - subW) / 2, 310);
            
            long time = System.currentTimeMillis();
            double angle = (time % 2000) * (2 * Math.PI / 2000.0);
            int circleX = getWidth() / 2 - 20;
            int circleY = 340;
            g2d.setStroke(new java.awt.BasicStroke(3f));
            g2d.setColor(new Color(0, 255, 255, 60));
            g2d.drawOval(circleX, circleY, 40, 40);
            g2d.setColor(new Color(0, 255, 255));
            g2d.drawArc(circleX, circleY, 40, 40, (int)Math.toDegrees(angle), 120);
            g2d.setStroke(new java.awt.BasicStroke(1f));
        } else {
            List<LeaderboardEntry> entries;
            if (selectedLeaderboardScope == LeaderboardScope.GLOBAL) {
                entries = gameEngine.getGlobalLeaderboardEntriesForDifficultyAndMode(selectedLeaderboardDifficulty, selectedLeaderboardMode);
            } else {
                entries = gameEngine.getLeaderboardEntriesForDifficultyAndMode(selectedLeaderboardDifficulty, selectedLeaderboardMode);
            }
            
            g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 13));
            int y = 222;
            int rowGap = 28;

            if (entries == null || entries.isEmpty()) {
                g2d.setColor(new Color(150, 150, 180));
                g2d.drawString("尚無紀錄 (No records yet)", (getWidth() - g2d.getFontMetrics().stringWidth("尚無紀錄 (No records yet)")) / 2, 280);
                g2d.drawString("開始遊戲來締造新紀錄吧！", (getWidth() - g2d.getFontMetrics().stringWidth("開始遊戲來締造新紀錄吧！")) / 2, 310);
            } else {
                int rank = 1;
                for (LeaderboardEntry entry : entries) {
                    if (rank > 8) break;

                    if (rank % 2 == 1) {
                        g2d.setColor(new Color(255, 255, 255, 10));
                        g2d.fillRect(40, y - 18, getWidth() - 80, 24);
                    }

                    if (rank == 1) g2d.setColor(new Color(255, 223, 0));
                    else if (rank == 2) g2d.setColor(new Color(192, 192, 192));
                    else if (rank == 3) g2d.setColor(new Color(205, 127, 50));
                    else g2d.setColor(Color.WHITE);

                    g2d.drawString("#" + rank, 45, y);
                    g2d.drawString(entry.getPlayerName(), 95, y);
                    
                    if (selectedLeaderboardMode == GameMode.SPRINT) {
                        int sec = entry.getSecondsElapsed();
                        String timeVal = String.format("%02d:%02d", sec / 60, sec % 60);
                        g2d.drawString(timeVal, 205, y);
                        g2d.drawString(entry.getLinesCleared() + " / 40", 295, y);
                    } else if (selectedLeaderboardMode == GameMode.SURVIVAL) {
                        int sec = entry.getSecondsElapsed();
                        String timeVal = String.format("%02d:%02d", sec / 60, sec % 60);
                        g2d.drawString(timeVal, 205, y);
                        g2d.drawString(String.format("%,d", entry.getScore()), 295, y);
                    } else {
                        g2d.drawString(String.format("%,d", entry.getScore()), 205, y);
                        String diffDisp = "NORMAL".equalsIgnoreCase(entry.getDifficulty()) ? "MEDIUM" : entry.getDifficulty();
                        g2d.drawString(diffDisp, 295, y);
                    }
                    g2d.drawString(entry.getPlayedAtDisplay(), 385, y);

                    y += rowGap;
                    rank++;
                }
            }
        }

        // Back Button
        String backBtnText = "返回主選單 (BACK)";
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 18));
        FontMetrics fmBack = g2d.getFontMetrics();
        int btnW = fmBack.stringWidth(backBtnText) + 40;
        int btnH = 40;
        int btnX = (getWidth() - btnW) / 2;
        int btnY = 485;

        if (backButtonBounds == null) backButtonBounds = new Rectangle(btnX, btnY - 30, btnW, btnH); else backButtonBounds.setBounds(btnX, btnY - 30, btnW, btnH);

        boolean hoverBack = (mousePos != null && backButtonBounds.contains(mousePos));

        if (hoverBack) {
            g2d.setColor(new Color(0, 255, 255, 45));
            g2d.fillRoundRect(btnX, btnY - 30, btnW, btnH, 10, 10);
            g2d.setColor(new Color(0, 255, 255));
            g2d.drawRoundRect(btnX, btnY - 30, btnW, btnH, 10, 10);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 25));
            g2d.fillRoundRect(btnX, btnY - 30, btnW, btnH, 10, 10);
            g2d.setColor(new Color(255, 255, 255, 90));
            g2d.drawRoundRect(btnX, btnY - 30, btnW, btnH, 10, 10);
            g2d.setColor(new Color(200, 200, 200));
        }

        g2d.drawString(backBtnText, btnX + 20, btnY - 11);

        // Help hints at the bottom
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(120, 120, 140));
        String tabHint1 = "提示：點擊或按 ⬅ / ➡ 鍵切換難度，點擊或按 ⬆ / ⬇ 鍵切換模式";
        String tabHint2 = "提示：點擊最上方按鈕可在 本地 與 全球 排行榜之間切換";
        g2d.drawString(tabHint1, (getWidth() - g2d.getFontMetrics().stringWidth(tabHint1)) / 2, 535);
        g2d.drawString(tabHint2, (getWidth() - g2d.getFontMetrics().stringWidth(tabHint2)) / 2, 555);
    }

    public void navigateLeaderboardTabs(int dir) {
        int currentIndex = selectedLeaderboardDifficulty.ordinal();
        int nextIndex = (currentIndex + dir + 3) % 3;
        selectedLeaderboardDifficulty = com.tetris.controller.GameEngine.Difficulty.values()[nextIndex];
        repaint();
    }

    public void navigateLeaderboardModes(int dir) {
        int currentIndex = selectedLeaderboardMode.ordinal();
        int nextIndex = (currentIndex + dir + 4) % 4;
        selectedLeaderboardMode = GameMode.values()[nextIndex];
        repaint();
    }

    private static class Badge {
        final String title;
        final String description;
        final Color color;

        Badge(String title, String description, Color color) {
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    private void drawLocalPvpGameOver(Graphics2D g2d) {
        int w = COLS * TILE_SIZE;
        int h = ROWS * TILE_SIZE;

        g2d.setColor(new Color(15, 5, 25, 200));
        g2d.fillRect(0, 0, w, h);

        g2d.setColor(new Color(255, 40, 80, 150));
        g2d.setStroke(new java.awt.BasicStroke(2f));
        g2d.drawRect(2, 2, w - 4, h - 4);
        g2d.setStroke(new java.awt.BasicStroke(1f));

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 36));
        FontMetrics fm = g2d.getFontMetrics();
        String text1 = "遊戲結束";
        int x1 = (w - fm.stringWidth(text1)) / 2;
        int y1 = h / 2 - 20;

        g2d.setColor(new Color(255, 40, 80, 120));
        g2d.drawString(text1, x1 + 2, y1 + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text1, x1, y1);

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        FontMetrics fmSub = g2d.getFontMetrics();
        String text2 = "等待對手完成...";
        int x2 = (w - fmSub.stringWidth(text2)) / 2;
        int y2 = h / 2 + 20;

        g2d.setColor(new Color(200, 200, 220));
        g2d.drawString(text2, x2, y2);
    }

    private void drawPvpGameOverOverlay(Graphics2D g2d) {
        int width = 1000;
        int height = ROWS * TILE_SIZE;

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Semi-transparent background
        g2d.setColor(new Color(15, 5, 25, 240));
        g2d.fillRect(0, 0, width, height);

        // Neon glowing border
        int winner = gameEngine.getPvpWinner();
        Color themeColor = Color.WHITE;
        if (winner == 1) {
            themeColor = new Color(0, 255, 255); // Cyan
        } else if (winner == 2) {
            themeColor = new Color(255, 100, 255); // Purple
        } else if (winner == 3) {
            themeColor = new Color(255, 215, 0); // Gold
        }
        g2d.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 100));
        g2d.setStroke(new java.awt.BasicStroke(3f));
        g2d.drawRect(5, 5, width - 10, height - 10);
        g2d.setStroke(new java.awt.BasicStroke(1f));

        // 2. Header Title
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD | Font.ITALIC, 48));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = (winner == 1) ? "玩家 1 獲得勝利！" : 
                           ((winner == 2) ? "玩家 2 獲得勝利！" : 
                           ((winner == 3) ? "雙方平手！" : "遊戲結束"));
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 80;

        // Glow
        g2d.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 120));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.drawString(titleText, titleX - 2, titleY - 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(titleText, titleX, titleY);

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        g2d.setColor(new Color(180, 100, 255));
        String subTitleText = "雙人對戰戰績結算 (LOCAL PVP BATTLE SUMMARY)";
        int subTitleX = (width - g2d.getFontMetrics().stringWidth(subTitleText)) / 2;
        g2d.drawString(subTitleText, subTitleX, 110);

        // 3. Side-by-side Player stats
        int p1StartX = 50;
        int p2StartX = 550;
        int statsY = 150;
        int cardW = 400;
        int cardH = 320;

        // Player 1 Card Background
        g2d.setColor(new Color(0, 255, 255, 15));
        g2d.fillRoundRect(p1StartX, statsY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(0, 255, 255, 50));
        g2d.drawRoundRect(p1StartX, statsY, cardW, cardH, 15, 15);

        // Player 2 Card Background
        g2d.setColor(new Color(255, 100, 255, 15));
        g2d.fillRoundRect(p2StartX, statsY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 100, 255, 50));
        g2d.drawRoundRect(p2StartX, statsY, cardW, cardH, 15, 15);

        // Render Stats for P1
        drawPlayerStatsCard(g2d, p1StartX, statsY, cardW, "玩家 1 (PLAYER 1)", gameEngine, new Color(0, 255, 255));


        // Render Stats for P2
        drawPlayerStatsCard(g2d, p2StartX, statsY, cardW, "玩家 2 (PLAYER 2)", gameEngine2, new Color(255, 100, 255));

        // 4. Return Hint at bottom
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 16));
        g2d.setColor(new Color(120, 120, 150));
        String hintText = "按下 ENTER 或 空白鍵 返回主選單";
        int hintX = (width - g2d.getFontMetrics().stringWidth(hintText)) / 2;
        g2d.drawString(hintText, hintX, 520);
    }

    private void drawPlayerStatsCard(Graphics2D g2d, int startX, int startY, int cardW, String playerTitle, com.tetris.controller.GameEngine engine, Color themeColor) {
        if (engine == null) return;
        
        g2d.setColor(themeColor);
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 22));
        FontMetrics fmTitle = g2d.getFontMetrics();
        g2d.drawString(playerTitle, startX + (cardW - fmTitle.stringWidth(playerTitle)) / 2, startY + 35);

        // Stats details
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 16));
        g2d.setColor(Color.WHITE);
        int itemY = startY + 80;
        int gap = 40;

        g2d.drawString("最終分數:", startX + 40, itemY);
        g2d.drawString(String.format("%,d", engine.getScore()), startX + 260, itemY);
        itemY += gap;

        g2d.drawString("消除行數:", startX + 40, itemY);
        g2d.drawString(String.valueOf(engine.getTotalLinesCleared()), startX + 260, itemY);
        itemY += gap;

        g2d.drawString("達成 T-Spin:", startX + 40, itemY);
        g2d.drawString(String.valueOf(engine.getTSpins()), startX + 260, itemY);
        itemY += gap;

        g2d.drawString("最高連消:", startX + 40, itemY);
        g2d.drawString(String.valueOf(Math.max(0, engine.getMaxCombo())), startX + 260, itemY);
        itemY += gap;

        g2d.drawString("放置方塊:", startX + 40, itemY);
        g2d.drawString(String.valueOf(engine.getPiecesSpawned()), startX + 260, itemY);
    }

    public void startAnimationTimer() {
        if (animationTimer != null && !animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    private void triggerGlobalLeaderboardLoad() {
        isLeaderboardLoading = true;
        repaint();
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                // Ignore
            }
            isLeaderboardLoading = false;
            repaint();
        }).start();
    }

    private void drawNameEntryOverlay(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) width = COLS * TILE_SIZE + SIDEBAR_WIDTH;
        if (height <= 0) height = ROWS * TILE_SIZE;

        // 1. Semi-transparent background
        g2d.setColor(new Color(15, 5, 25, 235));
        g2d.fillRect(0, 0, width, height);

        // Neon Golden breathing frame in the center
        long now = System.currentTimeMillis();
        float pulse = (float) (0.8 + 0.2 * Math.sin(now / 300.0));
        g2d.setColor(new Color(255, 215, 0, (int)(255 * pulse * 0.4)));
        g2d.setStroke(new java.awt.BasicStroke(4f));
        g2d.drawRoundRect(40, 80, width - 80, 420, 16, 16);
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new java.awt.BasicStroke(1.5f));
        g2d.drawRoundRect(40, 80, width - 80, 420, 16, 16);
        g2d.setStroke(new java.awt.BasicStroke(1f));

        // Pulsing Golden Title
        g2d.setFont(getCachedFont("Impact", Font.BOLD | Font.ITALIC, 38));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "NEW HIGH SCORE!";
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        g2d.setColor(new Color(255, 215, 0, (int)(255 * pulse)));
        g2d.drawString(titleText, titleX, 140);

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 18));
        g2d.setColor(new Color(0, 255, 255));
        String subTitleText = "達成新紀錄！";
        int subTitleX = (width - g2d.getFontMetrics().stringWidth(subTitleText)) / 2;
        g2d.drawString(subTitleText, subTitleX, 175);

        // Stats Box
        int statsY = 215;
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("遊戲模式 (MODE): " + gameEngine.getGameMode().name(), 80, statsY);
        g2d.drawString("難易度 (DIFF): " + gameEngine.getDifficulty().getLabel(), 80, statsY + 30);
        
        String scoreVal;
        if (gameEngine.getGameMode() == GameMode.SPRINT) {
            int sec = gameEngine.getSecondsElapsed();
            scoreVal = String.format("%02d:%02d", sec / 60, sec % 60) + " (時間)";
        } else if (gameEngine.getGameMode() == GameMode.SURVIVAL) {
            int sec = gameEngine.getSecondsElapsed();
            scoreVal = String.format("%02d:%02d", sec / 60, sec % 60) + " (生存) / " + String.format("%,d", gameEngine.getScore()) + " 分";
        } else {
            scoreVal = String.format("%,d", gameEngine.getScore()) + " 分";
        }
        g2d.drawString("最終成績 (SCORE): " + scoreVal, 80, statsY + 60);

        // Name input prompt
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 16));
        g2d.setColor(new Color(255, 100, 255));
        String prompt = "請輸入您的名字 (ENTER INITIALS):";
        int promptX = (width - g2d.getFontMetrics().stringWidth(prompt)) / 2;
        g2d.drawString(prompt, promptX, 330);

        // Name text field
        int fieldW = 280;
        int fieldH = 45;
        int fieldX = (width - fieldW) / 2;
        int fieldY = 350;

        // Render input field box
        g2d.setColor(new Color(255, 255, 255, 10));
        g2d.fillRoundRect(fieldX, fieldY, fieldW, fieldH, 8, 8);
        g2d.setColor(new Color(255, 100, 255, 100));
        g2d.drawRoundRect(fieldX, fieldY, fieldW, fieldH, 8, 8);

        // Render current name in buffer
        String name = gameEngine.getNameInputString();
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        int nameW = g2d.getFontMetrics().stringWidth(name);
        int nameX = fieldX + (fieldW - nameW) / 2;
        int nameY = fieldY + 31;
        g2d.drawString(name, nameX, nameY);

        // Render cursor
        boolean cursorBlink = (now / 500) % 2 == 0;
        if (cursorBlink) {
            int cursorX = nameX + nameW + 2;
            if (name.isEmpty()) {
                cursorX = fieldX + fieldW / 2;
            }
            g2d.setColor(new Color(255, 100, 255));
            g2d.fillRect(cursorX, fieldY + 12, 12, 22);
        }

        // Instructions
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(150, 150, 160));
        String inst1 = "鍵盤直接輸入字母 / 按 Backspace 刪除";
        String inst2 = "輸入完成後按下 ENTER 鍵確認，按 ESC 跳過";
        g2d.drawString(inst1, (width - g2d.getFontMetrics().stringWidth(inst1)) / 2, 435);
        g2d.drawString(inst2, (width - g2d.getFontMetrics().stringWidth(inst2)) / 2, 455);
    }

    // Draw game over screen overlay covering the full screen
    private void drawGameOverOverlay(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) width = COLS * TILE_SIZE + SIDEBAR_WIDTH;
        if (height <= 0) height = ROWS * TILE_SIZE;

        // Enable anti-aliasing for text and shapes
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Semi-transparent dark violet space background
        g2d.setColor(new Color(15, 5, 25, 235));
        g2d.fillRect(0, 0, width, height);

        // Draw a subtle neon red glowing border around the whole panel
        // Draw a subtle neon glowing border around the whole panel
        boolean isVictory = gameEngine.isVictory();
        g2d.setColor(isVictory ? new Color(0, 255, 100, 100) : new Color(255, 40, 80, 100));
        g2d.setStroke(new java.awt.BasicStroke(3f));
        g2d.drawRect(5, 5, width - 10, height - 10);
        g2d.setStroke(new java.awt.BasicStroke(1f));

        // 2. Header: Centered neon "VICTORY!" or "GAME OVER" or "TIME UP!"
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD | Font.ITALIC, 42));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText;
        if (isVictory) {
            titleText = (gameEngine.getGameMode() == GameMode.ULTRA) ? "時間到！" : "挑戰成功！";
        } else {
            titleText = "遊戲結束";
        }
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 65;

        // Glow effect
        g2d.setColor(isVictory ? new Color(0, 255, 100, 120) : new Color(255, 0, 50, 120));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.drawString(titleText, titleX - 2, titleY - 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(titleText, titleX, titleY);

        // Neon violet subtitle "MISSION SUMMARY"
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 13));
        g2d.setColor(isVictory ? new Color(0, 255, 255) : new Color(180, 100, 255));
        String subTitleText = isVictory ? "挑戰完成" : "戰績結算";
        int subTitleX = (width - g2d.getFontMetrics().stringWidth(subTitleText)) / 2;
        g2d.drawString(subTitleText, subTitleX, 92);

        // 3. Grid of Statistics (y = 120 to 310)
        int cardW = 180;
        int cardH = 58;
        int col1X = 60;
        int col2X = 260;
        int row1Y = 120;
        int row2Y = 190;
        int row3Y = 260;

        // Fetch stats from gameEngine
        int score = gameEngine.getScore();
        int seconds = gameEngine.getSecondsElapsed();
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
        int lines = gameEngine.getTotalLinesCleared();
        int pieces = gameEngine.getPiecesSpawned();
        double ppm = gameEngine.getPPM();
        int actions = gameEngine.getTotalActions();
        double apm = gameEngine.getAPM();
        double tetrisRate = gameEngine.getTetrisRate();

        drawStatCard(g2d, col1X, row1Y, cardW, cardH, "最終分數", String.format("%,d", score), new Color(255, 215, 0));
        String timeLabel;
        if (gameEngine.getGameMode() == GameMode.SURVIVAL) {
            timeLabel = "生存時間";
        } else if (gameEngine.getGameMode() == GameMode.ULTRA) {
            timeLabel = "消耗時間";
        } else {
            timeLabel = isVictory ? "通關時間" : "消耗時間";
        }
        drawStatCard(g2d, col2X, row1Y, cardW, cardH, timeLabel, timeStr, new Color(0, 255, 255));
        
        String linesStr = (gameEngine.getGameMode() == GameMode.SPRINT) ? (lines + " / 40") : String.valueOf(lines);
        drawStatCard(g2d, col1X, row2Y, cardW, cardH, "消行總數", linesStr, new Color(0, 255, 100));
        drawStatCard(g2d, col2X, row2Y, cardW, cardH, "方塊總數", String.format("%d (%.1f PPM)", pieces, ppm), new Color(255, 140, 0));
        drawStatCard(g2d, col1X, row3Y, cardW, cardH, "操作次數", String.format("%d (%.1f APM)", actions, apm), new Color(180, 100, 255));
        drawStatCard(g2d, col2X, row3Y, cardW, cardH, "四行消率", String.format("%.1f%%", tetrisRate), new Color(255, 50, 150));

        // 4. Earned Badges Section (y = 335 to 500)
        int badgeContainerX = 60;
        int badgeContainerY = 335;
        int badgeContainerW = 380;
        int badgeContainerH = 160;

        // Draw Badge Container Box
        g2d.setColor(new Color(255, 255, 255, 10));
        g2d.fillRoundRect(badgeContainerX, badgeContainerY, badgeContainerW, badgeContainerH, 12, 12);
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawRoundRect(badgeContainerX, badgeContainerY, badgeContainerW, badgeContainerH, 12, 12);

        // Container title
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 13));
        g2d.setColor(new Color(255, 215, 120)); // Gold
        g2d.drawString("獲得徽章", badgeContainerX + 15, badgeContainerY + 22);

        // Identify Earned Badges
        List<Badge> earnedBadges = new ArrayList<>();
        if (seconds >= 300) {
            earnedBadges.add(new Badge("🏆 生存者", "生存時間超過 5 分鐘 (" + timeStr + ")", new Color(0, 255, 100)));
        }
        if (gameEngine.getTetrisClears() >= 5) {
            earnedBadges.add(new Badge("👑 消行大師", "達成 4 行消除 5 次以上 (" + gameEngine.getTetrisClears() + " 次)", new Color(255, 215, 0)));
        }
        if (ppm >= 40.0 && seconds >= 60) {
            earnedBadges.add(new Badge("⚡ 極速狂魔", String.format("平均速度維持在 %.1f PPM (>40 PPM)", ppm), new Color(0, 255, 255)));
        }
        if (gameEngine.getTSpins() >= 3) {
            earnedBadges.add(new Badge("🔮 T-Spin 戰術家", "達成 T-Spin 3 次以上 (" + gameEngine.getTSpins() + " 次)", new Color(180, 100, 255)));
        }
        if (gameEngine.getMaxCombo() >= 3) { // 4+ consecutive clears
            earnedBadges.add(new Badge("🔥 連消專家", "最高連消次數達 " + (gameEngine.getMaxCombo() + 1) + " 次", new Color(255, 50, 150)));
        }

        // Draw Badges
        if (earnedBadges.isEmpty()) {
            g2d.setFont(getCachedFont("SansSerif", Font.ITALIC, 13));
            g2d.setColor(new Color(150, 150, 160));
            String emptyMsg = "本局未獲得徽章。";
            String emptyTip = "提示：嘗試生存更久（5分鐘）或獲得 3 次以上 T-Spin！";
            g2d.drawString(emptyMsg, badgeContainerX + 25, badgeContainerY + 65);
            g2d.drawString(emptyTip, badgeContainerX + 25, badgeContainerY + 95);
        } else {
            int badgeY = badgeContainerY + 45;
            int gap = 38;
            for (int i = 0; i < Math.min(3, earnedBadges.size()); i++) {
                Badge badge = earnedBadges.get(i);
                
                // Draw badge title
                g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 12));
                g2d.setColor(badge.color);
                g2d.drawString(badge.title, badgeContainerX + 20, badgeY);

                // Draw badge description
                g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 11));
                g2d.setColor(new Color(210, 210, 220));
                g2d.drawString(badge.description, badgeContainerX + 160, badgeY);

                // Draw a divider line
                if (i < Math.min(3, earnedBadges.size()) - 1) {
                    g2d.setColor(new Color(255, 255, 255, 15));
                    g2d.drawLine(badgeContainerX + 15, badgeY + 12, badgeContainerX + badgeContainerW - 15, badgeY + 12);
                }

                badgeY += gap;
            }
            if (earnedBadges.size() > 3) {
                g2d.setFont(getCachedFont("SansSerif", Font.ITALIC, 11));
                g2d.setColor(new Color(150, 150, 160));
                String moreMsg = "+ 還有 " + (earnedBadges.size() - 3) + " 個獲得的徽章";
                g2d.drawString(moreMsg, badgeContainerX + badgeContainerW - 130, badgeContainerY + badgeContainerH - 12);
            }
        }

        // 5. Pulsing/Breathing Footer Instruction
        long now = System.currentTimeMillis();
        float pulse = (float) (0.6 + 0.4 * Math.sin(now / 250.0));
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        String footerText = "按下 ENTER 鍵或點擊返回主選單";
        int footerX = (width - g2d.getFontMetrics().stringWidth(footerText)) / 2;
        int footerY = 540;

        g2d.setColor(new Color(0, 255, 255, (int) (255 * pulse)));
        g2d.drawString(footerText, footerX, footerY);
    }

    private void drawStatCard(Graphics2D g2d, int x, int y, int w, int h, String label, String value, Color accentColor) {
        // Draw card background
        g2d.setColor(new Color(255, 255, 255, 10));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        
        // Draw card border
        g2d.setColor(new Color(255, 255, 255, 25));
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // Draw left accent bar
        g2d.setColor(accentColor);
        g2d.fillRoundRect(x, y, 4, h, 4, 4);

        // Draw label text
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 10));
        g2d.setColor(new Color(170, 170, 185));
        g2d.drawString(label, x + 12, y + 18);

        // Draw value text
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 15));
        g2d.setColor(Color.WHITE);
        g2d.drawString(value, x + 12, y + 42);
    }

    // Pause menu navigation helper
    public void navigatePauseMenu(int dir) {
        int numOptions = showSettingsInPause ? 7 : 4;
        selectedPauseIndex = (selectedPauseIndex + dir + numOptions) % numOptions;
        repaint();
    }

    // Pause menu select action helper
    public void selectPauseMenuItem() {
        if (!showSettingsInPause) {
            switch (selectedPauseIndex) {
                case 0: // RESUME
                    gameEngine.togglePause();
                    break;
                case 1: // SAVE GAME
                    gameEngine.saveGame();
                    lastSaveTime = System.currentTimeMillis();
                    break;
                case 2: // SETTINGS
                    showSettingsInPause = true;
                    selectedPauseIndex = 0;
                    break;
                case 3: // BACK TO MAIN
                    showSettingsInPause = false;
                    gameEngine.returnToMenu();
                    break;
            }
        } else {
            switch (selectedPauseIndex) {
                case 0: // GHOST PIECE
                    showGhostPiece = !showGhostPiece;
                    break;
                case 1: // GRID LINES
                    showGridLines = !showGridLines;
                    break;
                case 2: // PREVIEW COUNT
                    nextPiecesCount = nextPiecesCount % 5 + 1;
                    break;
                case 3: // THEME
                    com.tetris.util.ThemeManager.nextTheme();
                    break;
                case 4: // COLOR BLIND
                    com.tetris.util.ThemeManager.nextColorBlindMode();
                    break;
                case 5: // SOUND PACK
                    com.tetris.util.SoundManager.nextSoundPack();
                    break;
                case 6: // BACK
                    showSettingsInPause = false;
                    selectedPauseIndex = 2; // return cursor to settings option (index 2 now, because SETTINGS is index 2!)
                    break;
            }
        }
        repaint();
    }

    // Draw Pause Overlay
    private void drawPauseOverlay(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) width = COLS * TILE_SIZE + SIDEBAR_WIDTH;
        if (height <= 0) height = ROWS * TILE_SIZE;

        // Semi-transparent dark background
        g2d.setColor(new Color(10, 10, 20, 210));
        g2d.fillRect(0, 0, width, height);

        // Title: GAME PAUSED
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 42));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "遊戲暫停";
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 120;

        // Draw shadow/glow
        g2d.setColor(new Color(255, 200, 0, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(255, 215, 0)); // Gold
        g2d.drawString(titleText, titleX, titleY);

        // Selection container card
        int cardW = 340;
        int cardH = showSettingsInPause ? 430 : 310;
        int cardX = (width - cardW) / 2;
        int cardY = 160;

        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        // Draw Options
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 18));
        FontMetrics fmOption = g2d.getFontMetrics();
        int numPauseOptions = showSettingsInPause ? 7 : 4;

        for (int i = 0; i < numPauseOptions; i++) {
            String label = "";
            int x = 0, y = 0;
            int textHeight = fmOption.getHeight();
            int textWidth = 0;

            if (!showSettingsInPause) {
                switch (i) {
                    case 0: label = "繼續遊戲"; break;
                    case 1: label = "儲存進度"; break;
                    case 2: label = "遊戲設定"; break;
                    case 3: label = "返回主選單"; break;
                }
                textWidth = fmOption.stringWidth(label);
                x = cardX + (cardW - textWidth) / 2;
                y = cardY + 50 + i * 65;
                if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, y - textHeight + 5, cardW - 30, textHeight + 15); else pauseOptionBounds[i].setBounds(cardX + 15, y - textHeight + 5, cardW - 30, textHeight + 15);
            } else {
                switch (i) {
                    case 0: label = "幻影方塊: " + (showGhostPiece ? "開啟" : "關閉"); break;
                    case 1: label = "網格線: " + (showGridLines ? "顯示" : "隱藏"); break;
                    case 2: label = "預覽數量: " + nextPiecesCount + " 個"; break;
                    case 3: label = "視覺主題: " + getThemeChineseLabel(com.tetris.util.ThemeManager.getCurrentTheme()); break;
                    case 4: label = "色盲模式: " + getColorBlindModeChineseLabel(com.tetris.util.ThemeManager.getCurrentColorBlindMode()); break;
                    case 5: label = "音效套件: " + getSoundPackChineseLabel(com.tetris.util.SoundManager.getCurrentSoundPack()); break;
                    case 6: label = "返回"; break;
                }
                textWidth = fmOption.stringWidth(label);
                x = cardX + (cardW - textWidth) / 2;
                if (i == 0) {
                    y = cardY + 35;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 12, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 12, cardW - 30, 32);
                } else if (i == 1) {
                    y = cardY + 75;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 52, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 52, cardW - 30, 32);
                } else if (i == 2) {
                    y = cardY + 115;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 92, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 92, cardW - 30, 32);
                } else if (i == 3) {
                    y = cardY + 275;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 252, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 252, cardW - 30, 32);
                } else if (i == 4) {
                    y = cardY + 315;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 292, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 292, cardW - 30, 32);
                } else if (i == 5) {
                    y = cardY + 355;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 332, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 332, cardW - 30, 32);
                } else {
                    y = cardY + 395;
                    if (pauseOptionBounds[i] == null) pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 372, cardW - 30, 32); else pauseOptionBounds[i].setBounds(cardX + 15, cardY + 372, cardW - 30, 32);
                }
            }

            boolean isSelected = (i == selectedPauseIndex);
            if (isSelected) {
                // Background highlight box
                g2d.setColor(new Color(0, 255, 255, 35));
                g2d.fillRoundRect(pauseOptionBounds[i].x, pauseOptionBounds[i].y, pauseOptionBounds[i].width, pauseOptionBounds[i].height, 8, 8);
                g2d.setColor(new Color(0, 255, 255));
                g2d.drawRoundRect(pauseOptionBounds[i].x, pauseOptionBounds[i].y, pauseOptionBounds[i].width, pauseOptionBounds[i].height, 8, 8);

                // Highlighted text
                g2d.setColor(Color.WHITE);
                g2d.drawString(label, x, y + 4);

                // Indicators
                int sqSize = 6;
                g2d.fillRect(pauseOptionBounds[i].x + 8, y - textHeight/2 - sqSize/2 + 6, sqSize, sqSize);
                g2d.fillRect(pauseOptionBounds[i].x + pauseOptionBounds[i].width - 14, y - textHeight/2 - sqSize/2 + 6, sqSize, sqSize);
            } else {
                g2d.setColor(new Color(180, 180, 200));
                g2d.drawString(label, x, y + 4);
            }
        }

        if (showSettingsInPause) {
            drawVolumeSettings(g2d, cardX, cardY, cardW, cardY + 155, 45);
        } else {
            // Draw temporary save status text if it was saved within 3 seconds
            if (System.currentTimeMillis() - lastSaveTime < 3000) {
                g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
                boolean canSave = (gameEngine.getGameMode() == com.tetris.model.GameMode.ENDLESS);
                String savedText = canSave ? "進度已儲存！" : "此模式不支援存檔！";
                g2d.setColor(canSave ? new Color(0, 255, 100) : new Color(255, 60, 60)); // Green for success, Red for warning
                int savedW = g2d.getFontMetrics().stringWidth(savedText);
                g2d.drawString(savedText, cardX + (cardW - savedW) / 2, cardY + cardH - 15);
            }
        }

        // Control Hints
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(120, 120, 150));
        String hint = "使用 ⬆ / ⬇ 方向鍵或滑鼠選擇 | Enter 鍵確認";
        g2d.drawString(hint, (width - g2d.getFontMetrics().stringWidth(hint)) / 2, 515);
    }

    // Draw the grid
    private void drawGrid(Graphics g) {
        if (!showGridLines) return;
        g.setColor(com.tetris.util.ThemeManager.getGridLineColor());
        for (int i = 0; i <= COLS; i++) {
            g.drawLine(i * TILE_SIZE, 0, i * TILE_SIZE, ROWS * TILE_SIZE);
        }
        for (int i = 0; i <= ROWS; i++) {
            g.drawLine(0, i * TILE_SIZE, COLS * TILE_SIZE, i * TILE_SIZE);
        }
    }

    // Draw fixed blocks
    private void drawFixedBlocks(Graphics g) {
        drawFixedBlocks(g, board);
    }

    private void drawFixedBlocks(Graphics g, Board targetBoard) {
        if (targetBoard == null) return;
        Color[][] grid = targetBoard.getGrid();

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    drawSquare(g, c, r, grid[r][c]);
                }
            }
        }
    }

    // Draw current piece
    private void drawCurrentPiece(Graphics g) {
        drawCurrentPiece(g, currentPiece, board, gameEngine);
    }

    private void drawCurrentPiece(Graphics g, Piece targetPiece, Board targetBoard, com.tetris.controller.GameEngine targetEngine) {
        if (targetPiece != null && targetBoard != null && targetEngine != null) {
            // Draw ghost first
            if (showGhostPiece) {
                drawGhostPiece(g, targetPiece, targetBoard);
            }

            Color color = targetPiece.getType().getColor();

            // Flash effect for Lock Delay
            if (targetEngine.isLocking()) {
                // Flash every 100ms for more urgency
                if ((System.currentTimeMillis() / 100) % 2 == 0) {
                    // Switch to a semi-transparent version or white tint
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 120);
                } else {
                    // Strong white tint to make it pop
                    color = new Color(
                        Math.min(255, color.getRed() + 100),
                        Math.min(255, color.getGreen() + 100),
                        Math.min(255, color.getBlue() + 100),
                        255
                    );
                }
            }

            // Get the absolute coordinates of the current piece after rotation
            for (int[] coord : targetPiece.getAbsoluteCoords()) {
                drawSquare(g, coord[0], coord[1], color);
            }
        }
    }

    // Draw the ghost piece
    private void drawGhostPiece(Graphics g) {
        drawGhostPiece(g, currentPiece, board);
    }

    private void drawGhostPiece(Graphics g, Piece targetPiece, Board targetBoard) {
        if (targetPiece == null || targetBoard == null)
            return;

        // Create a copy of the current piece at same pos/rotation
        com.tetris.model.Piece ghost = new com.tetris.model.Piece(
            targetPiece.getType(), targetPiece.getRow(),
            targetPiece.getCol(), targetPiece.getRotationIndex()
        );

        // Move ghost down
        while (targetBoard.isValidMove(ghost)) {
            ghost.move(1, 0);
        }
        ghost.move(-1, 0);

        // Draw ghost squares
        Color baseColor = targetPiece.getType().getColor();
        Color ghostColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 75); // Mapped translucent color
        for (int[] coord : ghost.getAbsoluteCoords()) {
            drawSquare(g, coord[0], coord[1], ghostColor);
        }
    }

    // Draw Sidebar
    private void drawSidebar(Graphics g) {
        drawSidebar(g, gameEngine);
    }

    private void drawSidebar(Graphics g, com.tetris.controller.GameEngine targetEngine) {
        if (targetEngine == null)
            return;

        Graphics2D g2d = (Graphics2D) g;
        int startX = COLS * TILE_SIZE;

        // Draw Sidebar Background
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(startX, 0, SIDEBAR_WIDTH, getHeight());

        // Draw Sidebar Border
        g.setColor(Color.GRAY);
        g.drawLine(startX, 0, startX, getHeight());

        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));

        // 1. Draw Score or Sprint Lines progress
        if (targetEngine.getGameMode() == GameMode.SPRINT) {
            g.drawString("消行進度", startX + 20, 45);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 26));
            g.drawString(targetEngine.getTotalLinesCleared() + " / 40", startX + 20, 72);
        } else {
            g.drawString("目前分數", startX + 20, 45);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 26));
            g.drawString(String.valueOf(targetEngine.getScore()), startX + 20, 72);
        }

        // 2. Draw Timer
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
        g.drawString("遊玩時間", startX + 20, 108);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 24));
        int seconds = targetEngine.getSecondsElapsed();
        String timeStr;
        if (targetEngine.getGameMode() == GameMode.ULTRA) {
            int timeLeft = Math.max(0, 120 - seconds);
            timeStr = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60);
            if (timeLeft <= 10) {
                g.setColor(new Color(255, 60, 60)); // Red alert
            }
        } else {
            timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
        }
        g.drawString(timeStr, startX + 20, 133);
        g.setColor(Color.WHITE); // reset color

        // Draw Garbage Warning Countdown for Survival Mode
        if (targetEngine.getGameMode() == GameMode.SURVIVAL) {
            int interval = 15;
            if (targetEngine.getDifficulty() == com.tetris.controller.GameEngine.Difficulty.EASY) {
                interval = 20;
            } else if (targetEngine.getDifficulty() == com.tetris.controller.GameEngine.Difficulty.HARD) {
                interval = 10;
            }
            int timeLeft = interval - (seconds % interval);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
            if (timeLeft <= 3) {
                g.setColor(new Color(255, 60, 60)); // Red warning
            } else {
                g.setColor(new Color(255, 140, 0)); // Neon Orange/Gold
            }
            g.drawString("垃圾行倒數: " + timeLeft + " 秒", startX + 20, 150);
            g.setColor(Color.WHITE);
        }

        // 3. Draw Level
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
        g.drawString("遊戲難度", startX + 20, 170);
        if (targetEngine.getGameState() == com.tetris.controller.GameEngine.GameState.TUTORIAL) {
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 22));
            g.setColor(new Color(0, 255, 255));
            g.drawString("教學 " + targetEngine.getTutorialLevel() + " / 7", startX + 20, 195);
        } else {
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
            String diffStr = "中等";
            switch (targetEngine.getDifficulty()) {
                case EASY: diffStr = "簡單"; break;
                case NORMAL: diffStr = "中等"; break;
                case HARD: diffStr = "困難"; break;
            }
            g.drawString(diffStr, startX + 20, 195);

            // Draw Sprint score in small font on the side
            if (targetEngine.getGameMode() == GameMode.SPRINT) {
                g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
                g.setColor(new Color(180, 180, 180));
                g.drawString("分數: " + targetEngine.getScore(), startX + 110, 193);
            }
        }
        g.setColor(Color.WHITE);

        // 4. Previews (NEXT & HOLD)
        int boxY = 215;
        int boxW = 80;
        int boxH = 85;

        // Next Box (first piece)
        int nextBoxX = startX + 15;
        g.setColor(new Color(255, 255, 255, 15));
        g.fillRoundRect(nextBoxX, boxY, boxW, boxH, 8, 8);
        g.setColor(new Color(255, 255, 255, 45));
        g.drawRoundRect(nextBoxX, boxY, boxW, boxH, 8, 8);

        g.setColor(new Color(0, 255, 255)); // Cyan title for NEXT
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        g.drawString("下一個", nextBoxX + 8, boxY + 18);

        java.util.List<Piece> nextPieces = targetEngine.getNextPieces();
        if (nextPieces != null && !nextPieces.isEmpty()) {
            drawCenteredPiece(g, nextPieces.get(0).getType(), nextBoxX, boxY + 20, boxW, boxH - 22);
        }

        // Draw subsequent preview boxes vertically stacked
        int currentY = boxY + boxH; // 215 + 85 = 300
        for (int i = 1; i < nextPiecesCount; i++) {
            currentY += 4; // spacing
            int subBoxY = currentY;
            int subBoxH = 30;

            g.setColor(new Color(255, 255, 255, 10));
            g.fillRoundRect(nextBoxX, subBoxY, boxW, subBoxH, 6, 6);
            g.setColor(new Color(255, 255, 255, 30));
            g.drawRoundRect(nextBoxX, subBoxY, boxW, subBoxH, 6, 6);

            if (nextPieces != null && i < nextPieces.size()) {
                drawCenteredPiece(g, nextPieces.get(i).getType(), nextBoxX, subBoxY, boxW, subBoxH);
            }
            currentY += subBoxH;
        }

        // Hold Box
        int holdBoxX = startX + 105;
        g.setColor(new Color(255, 255, 255, 15));
        g.fillRoundRect(holdBoxX, boxY, boxW, boxH, 8, 8);
        g.setColor(new Color(255, 255, 255, 45));
        g.drawRoundRect(holdBoxX, boxY, boxW, boxH, 8, 8);

        g.setColor(new Color(255, 100, 255)); // Neon magenta title for HOLD
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        g.drawString("暫存區", holdBoxX + 8, boxY + 18);

        Piece heldPiece = targetEngine.getHeldPiece();
        if (heldPiece != null) {
            drawCenteredPiece(g, heldPiece.getType(), holdBoxX, boxY + 20, boxW, boxH - 22);
        } else {
            g.setColor(new Color(150, 150, 180));
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 11));
            int emptyW = g.getFontMetrics().stringWidth("[無]");
            g.drawString("[無]", holdBoxX + (boxW - emptyW) / 2, boxY + 20 + (boxH - 22)/2 + 4);
        }

        // 5. Draw Leaderboard or PVP info
        int dynamicYStart = currentY + 15;
        if (targetEngine.getGameState() == com.tetris.controller.GameEngine.GameState.TUTORIAL) {
            drawTutorialHints(g, startX + 15, dynamicYStart);
        } else if (targetEngine.getGameMode() == GameMode.PVP) {
            // Draw PvP stats!
            int statsX = startX + 15;
            int statsY = dynamicYStart;
            int statsW = 170;
            int statsH = 130;

            // Draw card background
            g2d.setColor(new Color(255, 255, 255, 10));
            g2d.fillRoundRect(statsX, statsY, statsW, statsH, 8, 8);
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.drawRoundRect(statsX, statsY, statsW, statsH, 8, 8);

            g2d.setColor(new Color(0, 255, 255));
            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
            String playerTitle = (targetEngine.getPlayerNum() == 1) ? "玩家 1 (WASD)" : "玩家 2 (方向鍵)";
            g2d.drawString(playerTitle, statsX + 10, statsY + 20);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
            g2d.drawString("消除行數: " + targetEngine.getTotalLinesCleared(), statsX + 10, statsY + 45);
            g2d.drawString("T-Spins 次數: " + targetEngine.getTSpins(), statsX + 10, statsY + 68);
            g2d.drawString("最高連消: " + targetEngine.getMaxCombo(), statsX + 10, statsY + 91);
            g2d.drawString("已放方塊: " + targetEngine.getPiecesSpawned(), statsX + 10, statsY + 114);
        } else {
            drawLeaderboard(g, startX + 15, dynamicYStart);

            // 6. AI Autoplay Status Overlay / Score Disqualification Warning
            int leaderboardCount = targetEngine.getLeaderboardEntries().size();
            int leaderboardH = leaderboardCount == 0 ? 25 : 16 + leaderboardCount * 13;
            int aiY = dynamicYStart + leaderboardH + 15;

            if (targetEngine.isAiPlay()) {
                int aiX = startX + 15;
                int aiW = 170;
                int aiH = 75;

                long now = System.currentTimeMillis();
                int alpha = 130 + (int)(60 * Math.sin(now / 150.0));

                // Semi-transparent container
                g2d.setColor(new Color(160, 32, 240, 25));
                g2d.fillRoundRect(aiX, aiY, aiW, aiH, 8, 8);

                // Glowing border
                g2d.setColor(new Color(180, 50, 255, alpha));
                g2d.drawRoundRect(aiX, aiY, aiW, aiH, 8, 8);

                // Pulsing dot
                g2d.setColor(new Color(180, 50, 255, alpha));
                g2d.fillOval(aiX + 15, aiY + 16, 8, 8);

                g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
                g2d.setColor(new Color(230, 200, 255));
                g2d.drawString("自動遊玩中", aiX + 30, aiY + 24);

                g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
                g2d.setColor(new Color(185, 175, 200));
                g2d.drawString("AI 正在替您遊玩遊戲中", aiX + 15, aiY + 44);
                g2d.drawString("按 [A] 鍵切換回手動操作", aiX + 15, aiY + 59);
            } else if (targetEngine.hasUsedAiThisSession()) {
                int warnX = startX + 15;
                int warnW = 170;
                int warnH = 65;

                // Semi-transparent warm warning background
                g2d.setColor(new Color(255, 69, 0, 25));
                g2d.fillRoundRect(warnX, aiY, warnW, warnH, 8, 8);

                // Orange-red border
                g2d.setColor(new Color(255, 100, 0, 160));
                g2d.drawRoundRect(warnX, aiY, warnW, warnH, 8, 8);

                g2d.setFont(new java.awt.Font("Microsoft JhengHei", java.awt.Font.BOLD, 11));
                g2d.setColor(new Color(255, 160, 122));
                g2d.drawString("不納入排行榜紀錄", warnX + 10, aiY + 22);

                g2d.setFont(new java.awt.Font("Microsoft JhengHei", java.awt.Font.PLAIN, 10));
                g2d.setColor(new Color(230, 200, 185));
                g2d.drawString("本局曾使用自動遊玩", warnX + 10, aiY + 40);
                g2d.drawString("分數將不寫入排行榜", warnX + 10, aiY + 53);
            }
            if (targetEngine.getGameState() == com.tetris.controller.GameEngine.GameState.PLAYING && targetEngine.getGameMode() != GameMode.PVP && !targetEngine.isAiPlay() && !targetEngine.hasUsedAiThisSession()) {
                drawSidebarTipBox(g2d, startX + 15, 470, 170, 110);
            }
        }
    }

    // Helper: Draw centered piece in preview boxes
    private void drawCenteredPiece(Graphics g, com.tetris.model.Tetromino type, int boxX, int boxY, int boxW, int boxH) {
        if (type == null) return;
        int[][] coords = type.getCoords();

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int[] cell : coords) {
            int cx = cell[1]; // Swap 0 and 1: 0 is row, 1 is col
            int cy = cell[0];
            if (cx < minX) minX = cx;
            if (cx > maxX) maxX = cx;
            if (cy < minY) minY = cy;
            if (cy > maxY) maxY = cy;
        }

        int spanX = maxX - minX + 1;
        int spanY = maxY - minY + 1;

        // Calculate tileSize to fit inside boxW and boxH with padding
        int maxTileW = (boxW - 10) / spanX;
        int maxTileH = (boxH - 6) / spanY;
        int tileSize = Math.min(maxTileW, maxTileH);
        if (tileSize > 18) tileSize = 18;
        if (tileSize < 8) tileSize = 8;

        int pieceW = spanX * tileSize;
        int pieceH = spanY * tileSize;

        int offsetX = boxX + (boxW - pieceW) / 2 - minX * tileSize;
        int offsetY = boxY + (boxH - pieceH) / 2 - minY * tileSize;

        Color color = type.getColor();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int[] cell : coords) {
            // cell[0] is row, cell[1] is col
            int px = offsetX + cell[1] * tileSize; 
            int py = offsetY + cell[0] * tileSize;
            drawThemeSquare(g2, px, py, tileSize, color);
        }
        g2.dispose();
    }

    private void drawLeaderboard(Graphics g, int x, int startY) {
        if (gameEngine == null) {
            return;
        }

        List<LeaderboardEntry> entries = gameEngine.getLeaderboardEntries();

        g.setColor(new Color(255, 215, 120));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        g.drawString("高分排行榜", x, startY);

        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        int lineY = startY + 16;

        if (entries.isEmpty()) {
            g.setColor(new Color(210, 210, 210));
            g.drawString("尚無紀錄", x, lineY);
            return;
        }

        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (rank > 3) {
                break;
            }

            g.setColor(new Color(240, 240, 240));
            String diffDisp = "中等";
            if ("EASY".equalsIgnoreCase(entry.getDifficulty())) {
                diffDisp = "簡單";
            } else if ("HARD".equalsIgnoreCase(entry.getDifficulty())) {
                diffDisp = "困難";
            }
            String text = String.format("%d. %d 分 | %s", rank, entry.getScore(), diffDisp);
            g.drawString(text, x, lineY);
            lineY += 13;
            rank++;
        }
    }

    // Helper method: To draw a square with a border
    private void drawSquare(Graphics g, int x, int y, Color color) {
        if (y < 0)
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawThemeSquare(g2, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, color);
        g2.dispose();
    }

    private void drawThemeSquare(Graphics2D g2, int px, int py, int size, Color originalColor) {
        com.tetris.util.ThemeManager.Theme theme = com.tetris.util.ThemeManager.getCurrentTheme();
        Color color = com.tetris.util.ThemeManager.getMappedColor(originalColor);
        
        switch (theme) {
            case RETRO_GAMEBOY:
                g2.setColor(color);
                g2.fillRect(px, py, size, size);
                g2.setColor(com.tetris.util.ThemeManager.getGameBoyDarkColor());
                g2.drawRect(px, py, size, size);
                
                int gbInset = Math.max(2, size / 4);
                g2.drawRect(px + gbInset, py + gbInset, size - gbInset * 2, size - gbInset * 2);
                break;
                
            case CLASSIC_ARCADE:
                g2.setColor(color);
                g2.fillRect(px, py, size, size);
                
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawLine(px, py, px + size, py);
                g2.drawLine(px, py, px, py + size);
                
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawLine(px, py + size - 1, px + size - 1, py + size - 1);
                g2.drawLine(px + size - 1, py, px + size - 1, py + size - 1);
                
                int dotSize = Math.max(1, size / 8);
                int arcInset = size <= 16 ? 2 : 4;
                g2.setColor(Color.WHITE);
                g2.fillRect(px + arcInset, py + arcInset, dotSize, dotSize);
                break;
                
            case MIDNIGHT_STEALTH:
                g2.setColor(new Color(15, 10, 20, 210));
                g2.fillRect(px, py, size, size);
                g2.setColor(color);
                g2.drawRect(px, py, size, size);
                break;
                
            case CYBERPUNK_NEON:
            default:
                g2.setColor(new Color(10, 10, 15, 220));
                g2.fillRect(px, py, size, size);
                
                g2.setColor(color);
                g2.setStroke(new java.awt.BasicStroke(Math.max(1f, size / 15f)));
                g2.drawRect(px + 1, py + 1, size - 2, size - 2);
                g2.setStroke(new java.awt.BasicStroke(1f));
                
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                int dotS = Math.max(2, size / 8);
                g2.fillOval(px + size / 2 - dotS / 2, py + size / 2 - dotS / 2, dotS, dotS);
                break;
        }

        // Draw Colorblind Symbol Assist
        if (originalColor != null) {
            int rgb = originalColor.getRGB() & 0xFFFFFF;
            int cyanRGB = Color.CYAN.getRGB() & 0xFFFFFF;
            int blueRGB = Color.BLUE.getRGB() & 0xFFFFFF;
            int orangeRGB = Color.ORANGE.getRGB() & 0xFFFFFF;
            int yellowRGB = Color.YELLOW.getRGB() & 0xFFFFFF;
            int greenRGB = Color.GREEN.getRGB() & 0xFFFFFF;
            int magentaRGB = Color.MAGENTA.getRGB() & 0xFFFFFF;
            int redRGB = Color.RED.getRGB() & 0xFFFFFF;
            
            int shapeIndex = -1;
            if (rgb == cyanRGB) shapeIndex = 0;
            else if (rgb == blueRGB) shapeIndex = 1;
            else if (rgb == orangeRGB) shapeIndex = 2;
            else if (rgb == yellowRGB) shapeIndex = 3;
            else if (rgb == greenRGB) shapeIndex = 4;
            else if (rgb == magentaRGB) shapeIndex = 5;
            else if (rgb == redRGB) shapeIndex = 6;
            
            com.tetris.util.ThemeManager.ColorBlindMode cbMode = com.tetris.util.ThemeManager.getCurrentColorBlindMode();
            if (shapeIndex != -1 && (cbMode == com.tetris.util.ThemeManager.ColorBlindMode.SYMBOL_ASSIST || cbMode == com.tetris.util.ThemeManager.ColorBlindMode.BOTH)) {
                g2.setStroke(new java.awt.BasicStroke(Math.max(1.5f, size / 14f)));
                if (theme == com.tetris.util.ThemeManager.Theme.RETRO_GAMEBOY) {
                    g2.setColor(new Color(15, 56, 15, originalColor.getAlpha()));
                } else {
                    g2.setColor(new Color(255, 255, 255, Math.min(originalColor.getAlpha(), 180)));
                }
                
                switch (shapeIndex) {
                    case 0: // I: Circle
                        g2.drawOval(px + size / 4, py + size / 4, size / 2, size / 2);
                        break;
                    case 1: // J: Triangle Left
                        int[] xP1 = { px + size / 4, px + 3 * size / 4, px + 3 * size / 4 };
                        int[] yP1 = { py + size / 2, py + size / 4, py + 3 * size / 4 };
                        g2.drawPolygon(xP1, yP1, 3);
                        break;
                    case 2: // L: Triangle Right
                        int[] xP2 = { px + 3 * size / 4, px + size / 4, px + size / 4 };
                        int[] yP2 = { py + size / 2, py + size / 4, py + 3 * size / 4 };
                        g2.drawPolygon(xP2, yP2, 3);
                        break;
                    case 3: // O: Square
                        g2.drawRect(px + size / 4, py + size / 4, size / 2, size / 2);
                        break;
                    case 4: // S: Slash "/"
                        g2.drawLine(px + size / 4, py + 3 * size / 4, px + 3 * size / 4, py + size / 4);
                        break;
                    case 5: // T: Plus "+"
                        g2.drawLine(px + size / 2, py + size / 4, px + size / 2, py + 3 * size / 4);
                        g2.drawLine(px + size / 4, py + size / 2, px + 3 * size / 4, py + size / 2);
                        break;
                    case 6: // Z: Backslash "\"
                        g2.drawLine(px + size / 4, py + size / 4, px + 3 * size / 4, py + 3 * size / 4);
                        break;
                }
                g2.setStroke(new java.awt.BasicStroke(1f));
            }
        }
    }

    // Simple score popup structure
    private static class ScorePopup {
        final String lineText;
        final String scoreText;
        final String comboText;
        final int startX;
        final int startY;
        final long startTime;
        final int duration = 1200; // ms (slightly longer for readability)
        final int rise = 50; // pixels to move up
        final Color color;
        final Font font;

        ScorePopup(String lineText, String scoreText, String comboText, int startX, int startY, Color color, Font font) {
            this.lineText = lineText;
            this.scoreText = scoreText;
            this.comboText = comboText;
            this.startX = startX;
            this.startY = startY;
            this.startTime = System.currentTimeMillis();
            this.color = color;
            this.font = font;
        }
    }

    // Particle structure for visual effects
    private static class Particle {
        double x, y;
        double vx, vy;
        Color color;
        float life = 1.0f;
        float decay;
        int size;
        int shapeType = 0; // 0: circle, 1: star

        Particle(double x, double y, Color color, int size, double angle, double speed, float decay) {
            reset(x, y, color, size, angle, speed, decay, 0);
        }

        Particle(double x, double y, Color color, int size, double angle, double speed, float decay, int shapeType) {
            reset(x, y, color, size, angle, speed, decay, shapeType);
        }

        void reset(double x, double y, Color color, int size, double angle, double speed, float decay, int shapeType) {
            this.x = x;
            this.y = y;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.color = color;
            this.size = size;
            this.decay = decay;
            this.life = 1.0f;
            this.shapeType = shapeType;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.08; // subtle gravity downwards
            vx *= 0.98; // slight drag
            life -= decay;
        }
    }

    // Spawns particles for row clears
    public void spawnRowClearParticles(int gridRow, Color[] rowColors) {
        spawnRowClearParticles(gridRow, rowColors, 1);
    }

    public void spawnRowClearParticles(int gridRow, Color[] rowColors, int playerNum) {
        int py = gridRow * TILE_SIZE + TILE_SIZE / 2;
        Random rand = new Random();
        synchronized (playerNum == 2 ? particles2 : particles) {
            List<Particle> targetList = (playerNum == 2) ? particles2 : particles;
            for (int col = 0; col < COLS; col++) {
                int px = col * TILE_SIZE + TILE_SIZE / 2;
                Color colColor = rowColors[col];
                if (colColor == null) {
                    colColor = Color.CYAN;
                }
                
                // Spawn 6 particles per column cell
                for (int i = 0; i < 6; i++) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double speed = 0.8 + rand.nextDouble() * 3.8;
                    float decay = 0.015f + rand.nextFloat() * 0.02f;
                    int size = 3 + rand.nextInt(5);
                    
                    targetList.add(getOrCreateParticle(px, py, colColor, size, angle, speed, decay));
                }
            }
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    // Spawns particles when a piece lands/hard drops
    public void spawnDropParticles(Piece piece) {
        spawnDropParticles(piece, 1);
    }

    public void spawnDropParticles(Piece piece, int playerNum) {
        if (piece == null) return;
        Color color = piece.getType().getColor();
        Random rand = new Random();
        synchronized (playerNum == 2 ? particles2 : particles) {
            List<Particle> targetList = (playerNum == 2) ? particles2 : particles;
            for (int[] coord : piece.getAbsoluteCoords()) {
                int col = coord[0];
                int row = coord[1];
                
                int px = col * TILE_SIZE + TILE_SIZE / 2;
                int py = (row + 1) * TILE_SIZE; // bottom edge of the block
                
                // Spawn 4 dust/spark particles per block
                for (int i = 0; i < 4; i++) {
                    double angle = -Math.PI / 4.0 - rand.nextDouble() * (Math.PI / 2.0);
                    double speed = 0.5 + rand.nextDouble() * 2.2;
                    float decay = 0.035f + rand.nextFloat() * 0.025f;
                    int size = 2 + rand.nextInt(3);
                    
                    targetList.add(getOrCreateParticle(px, py, color, size, angle, speed, decay));
                }
            }
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }


    // Triggers a screenshake with intensity and duration
    public void triggerScreenshake(int intensity, int durationMs) {
        triggerScreenshake(intensity, durationMs, 1);
    }

    public void triggerScreenshake(int intensity, int durationMs, int playerNum) {
        if (playerNum == 2) {
            this.shakeIntensity2 = intensity;
            this.shakeDuration2 = durationMs;
            this.shakeEndTime2 = System.currentTimeMillis() + durationMs;
        } else {
            this.shakeIntensity = intensity;
            this.shakeDuration = durationMs;
            this.shakeEndTime = System.currentTimeMillis() + durationMs;
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    // Trigger Perfect Clear visual explosion and text display
    public void triggerPerfectClear() {
        triggerPerfectClear(1);
    }

    public void triggerPerfectClear(int playerNum) {
        if (playerNum == 2) {
            this.perfectClearStartTime2 = System.currentTimeMillis();
        } else {
            this.perfectClearStartTime = System.currentTimeMillis();
        }

        int boardW = COLS * TILE_SIZE;
        int boardH = ROWS * TILE_SIZE;
        int cx = boardW / 2;
        int cy = boardH / 2;

        Random rand = new Random();
        synchronized (playerNum == 2 ? particles2 : particles) {
            List<Particle> targetList = (playerNum == 2) ? particles2 : particles;
            for (int i = 0; i < 80; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double speed = 1.0 + rand.nextDouble() * 5.0;
                float decay = 0.008f + rand.nextFloat() * 0.012f; // slower decay for star particles
                int size = 5 + rand.nextInt(8);
                Color goldColor = new Color(255, 200 + rand.nextInt(56), 0); // golden colors

                targetList.add(getOrCreateParticle(cx, cy, goldColor, size, angle, speed, decay, 1));
            }
        }

        // Heavy screenshake shockwave!
        triggerScreenshake(12, 400, playerNum);

        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    // Draws a beautiful 5-pointed star
    private void drawStar(Graphics2D g2d, int cx, int cy, int size) {
        int r1 = size / 2;
        int r2 = size / 4;
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI / 5.0 - Math.PI / 2.0;
            int r = (i % 2 == 0) ? r1 : r2;
            xPoints[i] = cx + (int) (r * Math.cos(angle));
            yPoints[i] = cy + (int) (r * Math.sin(angle));
        }
        g2d.fillPolygon(xPoints, yPoints, 10);
    }

    // Render expanding gold ring and pulsing golden text for Perfect Clear
    private void drawPerfectClearOverlay(Graphics2D g2d) {
        drawPerfectClearOverlay(g2d, perfectClearStartTime);
    }

    private void drawPerfectClearOverlay(Graphics2D g2d, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= PERFECT_CLEAR_DURATION) {
            return;
        }

        double progress = elapsed / (double) PERFECT_CLEAR_DURATION;
        Graphics2D g2 = (Graphics2D) g2d.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int boardW = COLS * TILE_SIZE;
        int boardH = ROWS * TILE_SIZE;
        int cx = boardW / 2;
        int cy = boardH / 2;

        // 1. Expanding Golden Ring
        double ringProgress = progress * 2.0; // expand fast in the first half
        if (ringProgress <= 1.0) {
            int maxRadius = Math.max(boardW, boardH);
            int currentRadius = (int) (maxRadius * ringProgress);
            float ringAlpha = (float) (1.0 - ringProgress);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ringAlpha));
            g2.setColor(new Color(255, 215, 0)); // Gold
            g2.setStroke(new java.awt.BasicStroke(4.0f));
            g2.drawOval(cx - currentRadius, cy - currentRadius, currentRadius * 2, currentRadius * 2);
        }

        // 2. Pulsing/Floating Text
        float textAlpha = 1.0f;
        if (progress > 0.8) {
            textAlpha = (float) ((1.0 - progress) / 0.2); // fade out
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, textAlpha))));

        // Main Text: PERFECT CLEAR!
        g2.setFont(getCachedFont("Impact", Font.BOLD, 36));
        FontMetrics fmMain = g2.getFontMetrics();
        String mainText = "PERFECT CLEAR!";

        // Scale pulse based on time
        double pulse = 1.0 + 0.15 * Math.sin(progress * Math.PI * 4.0);
        g2.translate(cx, cy - 30);
        g2.scale(pulse, pulse);

        // Draw shadow
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(mainText, -fmMain.stringWidth(mainText) / 2 + 2, 2);
        // Draw fill
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(mainText, -fmMain.stringWidth(mainText) / 2, 0);

        // Sub Text: +2000 PTS
        g2.scale(1.0 / pulse, 1.0 / pulse); // reset scale
        g2.setFont(getCachedFont("Arial", Font.BOLD, 18));
        FontMetrics fmSub = g2.getFontMetrics();
        String subText = "+2000 BONUS";
        g2.setColor(Color.WHITE);
        g2.drawString(subText, -fmSub.stringWidth(subText) / 2, 40);

        g2.dispose();
    }

    // Draw active particles in the grid area
    private void drawParticles(Graphics g) {
        drawParticles(g, particles);
    }

    private void drawParticles(Graphics g, List<Particle> particlesList) {
        synchronized (particlesList) {
            if (particlesList.isEmpty()) return;
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (Particle p : particlesList) {
                if (p.life <= 0) continue;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.life));
                g2d.setColor(p.color);
                if (p.shapeType == 1) {
                    drawStar(g2d, (int) p.x, (int) p.y, p.size);
                } else {
                    g2d.fillOval((int) (p.x - p.size / 2.0), (int) (p.y - p.size / 2.0), p.size, p.size);
                }
            }
            g2d.dispose();
        }
    }

    /**
     * 繪製音量調整滑桿與一鍵靜音按鈕
     */
    private void drawVolumeSettings(Graphics2D g2d, int cardX, int cardY, int cardW, int startY, int gap) {
        g2d.setFont(getCachedFont("Arial", Font.BOLD, 16));
        
        // 1. BGM 音量調整項
        int bgmY = startY;
        g2d.setColor(Color.WHITE);
        g2d.drawString("BGM", cardX + 20, bgmY + 16);
        
        int trackX = cardX + 75;
        int trackW = 120;
        int trackH = 6;
        int trackY = bgmY + 10;
        bgmTrackBounds.setBounds(trackX, trackY - 10, trackW, trackH + 20); // 擴大滑動感應區
        
        g2d.setColor(new Color(50, 50, 80));
        g2d.fillRoundRect(trackX, trackY, trackW, trackH, 3, 3);
        
        float bgmVol = com.tetris.util.SoundManager.getBGMVolume();
        boolean bgmMute = com.tetris.util.SoundManager.isBGMMuted();
        
        int fillW = (int) (bgmVol * trackW);
        g2d.setColor(bgmMute ? new Color(100, 100, 100) : new Color(0, 255, 255));
        g2d.fillRoundRect(trackX, trackY, fillW, trackH, 3, 3);
        
        int thumbR = 8;
        int thumbX = trackX + fillW;
        int thumbY = trackY + trackH / 2;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(thumbX - thumbR, thumbY - thumbR, thumbR * 2, thumbR * 2);
        if (!bgmMute) {
            g2d.setColor(new Color(0, 255, 255));
            g2d.drawOval(thumbX - thumbR, thumbY - thumbR, thumbR * 2, thumbR * 2);
        }
        
        int pct = bgmMute ? 0 : Math.round(bgmVol * 100);
        String pctStr = pct + "%";
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString(pctStr, cardX + 205, bgmY + 16);
        
        int muteX = cardX + 255;
        int muteY = bgmY + 2;
        int muteW = 65;
        int muteH = 22;
        bgmMuteBounds.setBounds(muteX, muteY, muteW, muteH);
        
        java.awt.Point mousePos = getMousePosition();
        boolean hoverBgmMute = (mousePos != null && bgmMuteBounds.contains(mousePos));
        
        if (bgmMute) {
            g2d.setColor(new Color(255, 50, 50, 180));
            g2d.fillRoundRect(muteX, muteY, muteW, muteH, 6, 6);
            g2d.setColor(Color.WHITE);
        } else if (hoverBgmMute) {
            g2d.setColor(new Color(255, 255, 255, 45));
            g2d.fillRoundRect(muteX, muteY, muteW, muteH, 6, 6);
            g2d.setColor(new Color(200, 200, 200));
        } else {
            g2d.setColor(new Color(255, 255, 255, 20));
            g2d.fillRoundRect(muteX, muteY, muteW, muteH, 6, 6);
            g2d.setColor(new Color(160, 160, 180));
        }
        g2d.drawRoundRect(muteX, muteY, muteW, muteH, 6, 6);
        
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 11));
        String muteText = bgmMute ? "已靜音" : "靜音";
        int muteTextW = g2d.getFontMetrics().stringWidth(muteText);
        g2d.drawString(muteText, muteX + (muteW - muteTextW) / 2, muteY + 15);
        
        // 2. SFX 音量調整項
        g2d.setFont(getCachedFont("Arial", Font.BOLD, 16));
        int sfxY = bgmY + gap;
        g2d.setColor(Color.WHITE);
        g2d.drawString("SFX", cardX + 20, sfxY + 16);
        
        int sfxTrackY = sfxY + 10;
        sfxTrackBounds.setBounds(trackX, sfxTrackY - 10, trackW, trackH + 20); // 擴大滑動感應區
        
        g2d.setColor(new Color(50, 50, 80));
        g2d.fillRoundRect(trackX, sfxTrackY, trackW, trackH, 3, 3);
        
        float sfxVol = com.tetris.util.SoundManager.getSFXVolume();
        boolean sfxMute = com.tetris.util.SoundManager.isSFXMuted();
        
        int sfxFillW = (int) (sfxVol * trackW);
        g2d.setColor(sfxMute ? new Color(100, 100, 100) : new Color(0, 255, 255));
        g2d.fillRoundRect(trackX, sfxTrackY, sfxFillW, trackH, 3, 3);
        
        int sfxThumbX = trackX + sfxFillW;
        int sfxThumbY = sfxTrackY + trackH / 2;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(sfxThumbX - thumbR, sfxThumbY - thumbR, thumbR * 2, thumbR * 2);
        if (!sfxMute) {
            g2d.setColor(new Color(0, 255, 255));
            g2d.drawOval(sfxThumbX - thumbR, sfxThumbY - thumbR, thumbR * 2, thumbR * 2);
        }
        
        int sfxPct = sfxMute ? 0 : Math.round(sfxVol * 100);
        String sfxPctStr = sfxPct + "%";
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString(sfxPctStr, cardX + 205, sfxY + 16);
        
        int sfxMuteX = cardX + 255;
        int sfxMuteY = sfxY + 2;
        sfxMuteBounds.setBounds(sfxMuteX, sfxMuteY, muteW, muteH);
        boolean hoverSfxMute = (mousePos != null && sfxMuteBounds.contains(mousePos));
        
        if (sfxMute) {
            g2d.setColor(new Color(255, 50, 50, 180));
            g2d.fillRoundRect(sfxMuteX, sfxMuteY, muteW, muteH, 6, 6);
            g2d.setColor(Color.WHITE);
        } else if (hoverSfxMute) {
            g2d.setColor(new Color(255, 255, 255, 45));
            g2d.fillRoundRect(sfxMuteX, sfxMuteY, muteW, muteH, 6, 6);
            g2d.setColor(new Color(200, 200, 200));
        } else {
            g2d.setColor(new Color(255, 255, 255, 20));
            g2d.fillRoundRect(sfxMuteX, sfxMuteY, muteW, muteH, 6, 6);
            g2d.setColor(new Color(160, 160, 180));
        }
        g2d.drawRoundRect(sfxMuteX, sfxMuteY, muteW, muteH, 6, 6);
        
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 11));
        String sfxMuteText = sfxMute ? "已靜音" : "靜音";
        int sfxMuteTextW = g2d.getFontMetrics().stringWidth(sfxMuteText);
        g2d.drawString(sfxMuteText, sfxMuteX + (muteW - sfxMuteTextW) / 2, sfxMuteY + 15);
    }

    /**
     * 繪製主畫面設定卡片
     */
    private void drawMenuSettings(Graphics2D g2d) {
        java.awt.Point mousePos = getMousePosition();
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 42));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "遊戲設定";
        int titleX = (getWidth() - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 120;

        g2d.setColor(new Color(0, 255, 255, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString(titleText, titleX, titleY);

        int cardW = 340;
        int cardH = 470;
        int cardX = (getWidth() - cardW) / 2;
        int cardY = 150;

        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        drawVolumeSettings(g2d, cardX, cardY, cardW, cardY + 25, 45);

        // THEME button
        String themeText = "視覺主題: " + getThemeChineseLabel(com.tetris.util.ThemeManager.getCurrentTheme());
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 15));
        FontMetrics fmTheme = g2d.getFontMetrics();
        int btnW = 240;
        int btnH = 34;
        int btnX = (getWidth() - btnW) / 2;
        int themeBtnY = cardY + 130;

        if (menuSettingsThemeBounds == null) menuSettingsThemeBounds = new Rectangle(btnX, themeBtnY, btnW, btnH); else menuSettingsThemeBounds.setBounds(btnX, themeBtnY, btnW, btnH);
        boolean hoverTheme = (mousePos != null && menuSettingsThemeBounds.contains(mousePos));

        if (hoverTheme) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(btnX, themeBtnY, btnW, btnH, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(btnX, themeBtnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(btnX, themeBtnY, btnW, btnH, 8, 8);
        g2d.drawString(themeText, btnX + (btnW - fmTheme.stringWidth(themeText)) / 2, themeBtnY + 22);

        // COLORBLIND button
        String cbText = "色盲模式: " + getColorBlindModeChineseLabel(com.tetris.util.ThemeManager.getCurrentColorBlindMode());
        int cbBtnY = cardY + 172;
        if (menuSettingsColorBlindBounds == null) menuSettingsColorBlindBounds = new Rectangle(btnX, cbBtnY, btnW, btnH); else menuSettingsColorBlindBounds.setBounds(btnX, cbBtnY, btnW, btnH);
        boolean hoverCb = (mousePos != null && menuSettingsColorBlindBounds.contains(mousePos));

        if (hoverCb) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(btnX, cbBtnY, btnW, btnH, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(btnX, cbBtnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(btnX, cbBtnY, btnW, btnH, 8, 8);
        g2d.drawString(cbText, btnX + (btnW - fmTheme.stringWidth(cbText)) / 2, cbBtnY + 22);

        // SOUND PACK button
        String spText = "音效套件: " + getSoundPackChineseLabel(com.tetris.util.SoundManager.getCurrentSoundPack());
        int spBtnY = cardY + 214;
        if (menuSettingsSoundPackBounds == null) menuSettingsSoundPackBounds = new Rectangle(btnX, spBtnY, btnW, btnH); else menuSettingsSoundPackBounds.setBounds(btnX, spBtnY, btnW, btnH);
        boolean hoverSp = (mousePos != null && menuSettingsSoundPackBounds.contains(mousePos));

        if (hoverSp) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(btnX, spBtnY, btnW, btnH, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(btnX, spBtnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(btnX, spBtnY, btnW, btnH, 8, 8);
        g2d.drawString(spText, btnX + (btnW - fmTheme.stringWidth(spText)) / 2, spBtnY + 22);

        // PREVIEW COUNT button
        String previewText = "預覽數量: " + nextPiecesCount + " 個";
        int previewBtnY = cardY + 256;

        if (menuSettingsPreviewCountBounds == null) menuSettingsPreviewCountBounds = new Rectangle(btnX, previewBtnY, btnW, btnH); else menuSettingsPreviewCountBounds.setBounds(btnX, previewBtnY, btnW, btnH);
        boolean hoverPreview = (mousePos != null && menuSettingsPreviewCountBounds.contains(mousePos));

        if (hoverPreview) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(btnX, previewBtnY, btnW, btnH, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(btnX, previewBtnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(btnX, previewBtnY, btnW, btnH, 8, 8);
        g2d.drawString(previewText, btnX + (btnW - fmTheme.stringWidth(previewText)) / 2, previewBtnY + 22);

        // CONTROL SETTINGS button
        String controlBtnText = "控制設定";
        int ctrlBtnY = cardY + 298;

        if (menuSettingsControlBounds == null) menuSettingsControlBounds = new Rectangle(btnX, ctrlBtnY, btnW, btnH); else menuSettingsControlBounds.setBounds(btnX, ctrlBtnY, btnW, btnH);
        boolean hoverControl = (mousePos != null && menuSettingsControlBounds.contains(mousePos));

        if (hoverControl) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(btnX, ctrlBtnY, btnW, btnH, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(btnX, ctrlBtnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(btnX, ctrlBtnY, btnW, btnH, 8, 8);
        g2d.drawString(controlBtnText, btnX + (btnW - fmTheme.stringWidth(controlBtnText)) / 2, ctrlBtnY + 22);

        // BACK button
        String backText = "返回";
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 16));
        FontMetrics fmBack = g2d.getFontMetrics();
        int backBtnW = 120;
        int backBtnH = 34;
        int backBtnX = (getWidth() - backBtnW) / 2;
        int backBtnY = cardY + 390;

        if (menuSettingsBackButtonBounds == null) menuSettingsBackButtonBounds = new Rectangle(backBtnX, backBtnY, backBtnW, backBtnH); else menuSettingsBackButtonBounds.setBounds(backBtnX, backBtnY, backBtnW, backBtnH);
        boolean hoverBack = (mousePos != null && menuSettingsBackButtonBounds.contains(mousePos));

        if (hoverBack) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 8, 8);
        g2d.drawString(backText, backBtnX + (backBtnW - fmBack.stringWidth(backText)) / 2, backBtnY + 22);
    }

    private void updateBGMVolumeFromMouse(int mouseX) {
        float pct = (mouseX - bgmTrackBounds.x) / (float) bgmTrackBounds.width;
        if (pct < 0f) pct = 0f;
        if (pct > 1f) pct = 1f;
        com.tetris.util.SoundManager.setBGMVolume(pct);
        repaint();
    }

    private void updateSFXVolumeFromMouse(int mouseX) {
        float pct = (mouseX - sfxTrackBounds.x) / (float) sfxTrackBounds.width;
        if (pct < 0f) pct = 0f;
        if (pct > 1f) pct = 1f;
        com.tetris.util.SoundManager.setSFXVolume(pct);
        repaint();
    }

    /**
     * 繪製主畫面 GameMode 選擇卡片
     */
    private void drawModeSelectScreen(Graphics2D g2d) {
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 42));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "選擇模式";
        int titleX = (getWidth() - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 120;

        g2d.setColor(new Color(0, 255, 255, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString(titleText, titleX, titleY);

        int cardW = 340;
        int cardH = 405;
        int cardX = (getWidth() - cardW) / 2;
        int cardY = 170;

        // Draw card background
        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        // Subtitle inside card
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 15));
        g2d.setColor(new Color(200, 200, 220));
        String subText = "請選擇遊戲模式";
        int subW = g2d.getFontMetrics().stringWidth(subText);
        g2d.drawString(subText, cardX + (cardW - subW) / 2, cardY + 25);

        int btnW = 240;
        int btnH = 34;
        int btnX = cardX + (cardW - btnW) / 2;
        int startY = cardY + 50;
        int gap = 42;

        java.awt.Point mousePos = getMousePosition();

        for (int i = 0; i < 6; i++) {
            int y = startY + i * gap;
            if (i == 5) {
                // BACK button offset
                y = cardY + 345;
            }

            if (modeOptionBounds[i] == null) modeOptionBounds[i] = new Rectangle(btnX, y, btnW, btnH); else modeOptionBounds[i].setBounds(btnX, y, btnW, btnH);

            boolean isSelected = (i == selectedModeIndex);
            String label = "";
            Color baseColor;
            Color selectColor;
            Color fillCol;

            switch (i) {
                case 0:
                    label = "無盡模式 (ENDLESS)";
                    baseColor = new Color(0, 255, 255);
                    selectColor = Color.WHITE;
                    fillCol = new Color(0, 255, 255, isSelected ? 45 : 20);
                    break;
                case 1:
                    label = "40行衝刺賽 (SPRINT)";
                    baseColor = new Color(255, 100, 255);
                    selectColor = Color.WHITE;
                    fillCol = new Color(255, 100, 255, isSelected ? 45 : 20);
                    break;
                case 2:
                    label = "2分鐘限時賽 (ULTRA)";
                    baseColor = new Color(255, 215, 0);
                    selectColor = Color.WHITE;
                    fillCol = new Color(255, 215, 0, isSelected ? 45 : 20);
                    break;
                case 3:
                    label = "生存挑戰賽 (SURVIVAL)";
                    baseColor = new Color(255, 140, 0);
                    selectColor = Color.WHITE;
                    fillCol = new Color(255, 140, 0, isSelected ? 45 : 20);
                    break;
                case 4:
                    label = "雙人對戰模式 (PVP)";
                    baseColor = new Color(255, 60, 60);
                    selectColor = Color.WHITE;
                    fillCol = new Color(255, 60, 60, isSelected ? 45 : 20);
                    break;
                case 5:
                default:
                    label = "返回";
                    baseColor = new Color(160, 160, 180);
                    selectColor = new Color(0, 255, 255);
                    fillCol = new Color(255, 255, 255, isSelected ? 30 : 10);
                    break;
            }

            // Draw button background
            g2d.setColor(fillCol);
            g2d.fillRoundRect(btnX, y, btnW, btnH, 8, 8);

            // Draw border
            if (isSelected) {
                g2d.setColor(selectColor);
                g2d.setStroke(new java.awt.BasicStroke(2.0f));
            } else {
                g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80));
                g2d.setStroke(new java.awt.BasicStroke(1.0f));
            }
            g2d.drawRoundRect(btnX, y, btnW, btnH, 8, 8);
            g2d.setStroke(new java.awt.BasicStroke(1.0f));

            // Draw text
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 15));
            FontMetrics fmOpt = g2d.getFontMetrics();
            int labelW = fmOpt.stringWidth(label);
            int labelH = fmOpt.getAscent();

            if (isSelected) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.drawString(label, btnX + (btnW - labelW) / 2 + 1, y + (btnH + labelH) / 2 - 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(label, btnX + (btnW - labelW) / 2, y + (btnH + labelH) / 2 - 2);

                // Glow block indicators
                int sqSize = 6;
                g2d.setColor(selectColor);
                g2d.fillRect(btnX + 12, y + (btnH - sqSize) / 2, sqSize, sqSize);
                g2d.fillRect(btnX + btnW - 18, y + (btnH - sqSize) / 2, sqSize, sqSize);
            } else {
                g2d.setColor(baseColor);
                g2d.drawString(label, btnX + (btnW - labelW) / 2, y + (btnH + labelH) / 2 - 2);
            }
        }
    }

    /**
     * 繪製主畫面 Difficulty 選擇卡片
     */
    private void drawDifficultySelectScreen(Graphics2D g2d) {
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 42));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "開始遊戲";
        int titleX = (getWidth() - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 120;

        g2d.setColor(new Color(0, 255, 255, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString(titleText, titleX, titleY);

        int cardW = 340;
        int cardH = 300;
        int cardX = (getWidth() - cardW) / 2;
        int cardY = 180;

        // 畫卡片背景
        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        // 卡片內子標題
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 15));
        g2d.setColor(new Color(200, 200, 220));
        String subText = "請選擇難易度";
        int subW = g2d.getFontMetrics().stringWidth(subText);
        g2d.drawString(subText, cardX + (cardW - subW) / 2, cardY + 25);

        int btnW = 240;
        int btnH = 40;
        int btnX = cardX + (cardW - btnW) / 2;
        int startY = cardY + 45;
        int gap = 55;

        java.awt.Point mousePos = getMousePosition();

        for (int i = 0; i < 4; i++) {
            int y = startY + i * gap;
            if (i == 3) {
                // BACK 鍵留大一點點的間距
                y = cardY + 235;
            }

            if (difficultyOptionBounds[i] == null) difficultyOptionBounds[i] = new Rectangle(btnX, y, btnW, btnH); else difficultyOptionBounds[i].setBounds(btnX, y, btnW, btnH);

            boolean isSelected = (i == selectedDifficultyIndex);
            String label = "";
            Color baseColor;
            Color selectColor;
            Color fillCol;

            switch (i) {
                case 0:
                    label = "簡單 (EASY)";
                    baseColor = new Color(0, 200, 80);
                    selectColor = new Color(0, 255, 100);
                    fillCol = new Color(0, 200, 80, isSelected ? 45 : 20);
                    break;
                case 1:
                    label = "中等 (NORMAL)";
                    baseColor = new Color(220, 180, 0);
                    selectColor = new Color(255, 220, 0);
                    fillCol = new Color(220, 180, 0, isSelected ? 45 : 20);
                    break;
                case 2:
                    label = "困難 (HARD)";
                    baseColor = new Color(220, 40, 40);
                    selectColor = new Color(255, 60, 60);
                    fillCol = new Color(220, 40, 40, isSelected ? 45 : 20);
                    break;
                case 3:
                default:
                    label = "返回";
                    baseColor = new Color(160, 160, 180);
                    selectColor = new Color(0, 255, 255);
                    fillCol = new Color(255, 255, 255, isSelected ? 30 : 10);
                    break;
            }

            // 畫按鈕背景
            g2d.setColor(fillCol);
            g2d.fillRoundRect(btnX, y, btnW, btnH, 8, 8);

            // 畫邊框
            if (isSelected) {
                g2d.setColor(selectColor);
                g2d.setStroke(new java.awt.BasicStroke(2.0f));
            } else {
                g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80));
                g2d.setStroke(new java.awt.BasicStroke(1.0f));
            }
            g2d.drawRoundRect(btnX, y, btnW, btnH, 8, 8);
            g2d.setStroke(new java.awt.BasicStroke(1.0f));

            // 畫文字
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 18));
            FontMetrics fmOpt = g2d.getFontMetrics();
            int labelW = fmOpt.stringWidth(label);
            int labelH = fmOpt.getAscent();

            if (isSelected) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.drawString(label, btnX + (btnW - labelW) / 2 + 1, y + (btnH + labelH) / 2 - 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(label, btnX + (btnW - labelW) / 2, y + (btnH + labelH) / 2 - 2);

                // 發光小方塊標記
                int sqSize = 6;
                g2d.setColor(selectColor);
                g2d.fillRect(btnX + 12, y + (btnH - sqSize) / 2, sqSize, sqSize);
                g2d.fillRect(btnX + btnW - 18, y + (btnH - sqSize) / 2, sqSize, sqSize);
            } else {
                g2d.setColor(baseColor);
                g2d.drawString(label, btnX + (btnW - labelW) / 2, y + (btnH + labelH) / 2 - 2);
            }
        }
    }

    private void drawTutorialHints(Graphics g, int x, int startY) {
        g.setColor(new Color(255, 215, 120)); // Gold
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        g.drawString("訓練提示 (TRAINING HINTS)", x, startY);
        
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        
        int level = gameEngine.getTutorialLevel();
        String[] lines;
        if (level == 1) {
            lines = new String[] {
                "關卡 1：移動與軟降",
                "目標：填補最左側的空缺以消行。",
                "",
                "1. 按 ⬅ 方向鍵將 I 方塊",
                "   向左移動到最左側第 1 欄。",
                "2. 按住 ⬇ 方向鍵（軟降）",
                "   讓方塊加速落入窄道中。",
                "3. 填滿空缺，成功消行！"
            };
        } else if (level == 2) {
            lines = new String[] {
                "關卡 2：旋轉方塊",
                "目標：旋轉方塊並消行。",
                "",
                "1. I 方塊初始為橫向，",
                "   無法穿過第 5 欄的窄道。",
                "2. 按下 ⬆ 方向鍵（旋轉）或",
                "   Z 鍵，將 I 方塊轉為直向。",
                "3. 方塊落入洞中，完成消行！"
            };
        } else if (level == 3) {
            lines = new String[] {
                "關卡 3：暫存區 Hold 的使用",
                "目標：利用暫存功能完成消行。",
                "",
                "1. 當前為 Z 方塊，不適合",
                "   填補第 6 欄直向深槽。",
                "2. 按下 [C] 鍵將 Z 方塊暫存",
                "   至暫存區，並取出 I 方塊。",
                "3. 將直向 I 方塊對準第 6 欄",
                "   落下，填滿並消行通關！"
            };
        } else if (level == 4) {
            lines = new String[] {
                "關卡 4：硬降與消行",
                "目標：使用硬降瞬間落底消行。",
                "",
                "1. 將方形 O 方塊向右移動",
                "   對齊第 5 與第 6 欄的空缺。",
                "2. 確認對齊後，按下 [空白鍵]",
                "   進行硬降 (Hard Drop)。",
                "3. 方塊將瞬間鎖定並消除",
                "   底部兩行！"
            };
        } else if (level == 5) {
            lines = new String[] {
                "關卡 5：T-Spin Single 基礎",
                "目標：以 T-Spin 消除 1 行。",
                "",
                "1. 將 T 方塊右朝下 (指向右)",
                "   放入右側入口通道中。",
                "2. 當它到達底部時，",
                "   按下 ⬆ 方向鍵進行",
                "   順時針旋轉 (指向下)。",
                "3. 成功完成 T-spin Single！"
            };
        } else if (level == 6) {
            lines = new String[] {
                "關卡 6：T-Spin Double 右旋",
                "目標：右旋 T-Spin 消除 2 行。",
                "",
                "1. 本關結構與關卡 5 相似，",
                "   但是凹槽深度更深。",
                "2. 將 T 方塊右朝下 (指向右)",
                "   放入入口並塞入最底部。",
                "3. 按下 ⬆ 方向鍵順時針",
                "   旋轉，一次消除 2 行！"
            };
        } else { // Level 7
            lines = new String[] {
                "關卡 7：T-Spin Double 左旋",
                "目標：左旋 T-Spin 消除 2 行。",
                "",
                "1. 這次的凹槽位於左側。",
                "2. 將 T 方塊左朝下 (指向左)",
                "   放入左側入口通道中。",
                "3. 按下 ⬆ 方向鍵兩次",
                "   (或按 Z 鍵逆時針旋轉)",
                "   將方塊轉入凹槽中！"
            };
        }
        
        int lineY = startY + 20;
        int lineSpacing = (startY > 380) ? 12 : 16;
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, (startY > 380) ? 10 : 12));
        for (String line : lines) {
            g.drawString(line, x, lineY);
            lineY += lineSpacing;
        }
    }

    private void drawTutorialOverlays(Graphics2D g2d) {
        if (gameEngine.isTransitioning()) {
            int boardW = COLS * TILE_SIZE;
            int boardH = ROWS * TILE_SIZE;
            
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(0, 0, boardW, boardH);
            
            boolean success = gameEngine.isLastTutorialSuccess();
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 32));
            FontMetrics fm = g2d.getFontMetrics();
            String text = success ? "挑戰成功！" : "挑戰失敗！請再試一次";
            Color color = success ? new Color(0, 255, 100) : new Color(255, 50, 50);
            
            int textX = (boardW - fm.stringWidth(text)) / 2;
            int textY = boardH / 2 - 10;
            
            // Draw neon glow
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g2d.drawString(text, textX - 2, textY - 2);
            g2d.drawString(text, textX + 2, textY + 2);
            
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, textX, textY);
            
            // Draw a small subtext
            g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 14));
            FontMetrics fmSub = g2d.getFontMetrics();
            String subText = success ? "正在載入下一關..." : "正在重置版面...";
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawString(subText, (boardW - fmSub.stringWidth(subText)) / 2, textY + 40);
        }
    }

    private void drawTutorialVictoryOverlay(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) width = COLS * TILE_SIZE + SIDEBAR_WIDTH;
        if (height <= 0) height = ROWS * TILE_SIZE;
        
        // Semi-transparent dark background
        g2d.setColor(new Color(10, 10, 25, 235));
        g2d.fillRect(0, 0, width, height);
        
        // Title: TUTORIAL COMPLETE
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 36));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "教學關卡全部完成！";
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        g2d.setColor(new Color(255, 215, 0)); // Gold
        g2d.drawString(titleText, titleX, 150);
        
        // Subtitle: T-SPIN MASTER!
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 32));
        FontMetrics fmSub = g2d.getFontMetrics();
        String subText = "T-SPIN 大師！";
        int subX = (width - fmSub.stringWidth(subText)) / 2;
        g2d.setColor(new Color(0, 255, 255)); // Neon Cyan
        g2d.drawString(subText, subX, 220);
        
        // A nice message card
        int cardW = 320;
        int cardH = 150;
        int cardX = (width - cardW) / 2;
        int cardY = 270;
        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 12, 12);
        
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 15));
        g2d.setColor(Color.WHITE);
        String msg1 = "您已成功通過所有";
        String msg2 = "T-Spin 旋轉訓練關卡！";
        String msg3 = "現在您已準備好在實戰中";
        String msg4 = "運用 T-Spin 主宰整場戰局！";
        
        int textY = cardY + 30;
        g2d.drawString(msg1, cardX + (cardW - g2d.getFontMetrics().stringWidth(msg1)) / 2, textY);
        g2d.drawString(msg2, cardX + (cardW - g2d.getFontMetrics().stringWidth(msg2)) / 2, textY + 25);
        g2d.drawString(msg3, cardX + (cardW - g2d.getFontMetrics().stringWidth(msg3)) / 2, textY + 60);
        g2d.drawString(msg4, cardX + (cardW - g2d.getFontMetrics().stringWidth(msg4)) / 2, textY + 85);
        
        // Prompt
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 16));
        g2d.setColor(new Color(150, 150, 180));
        String prompt = "按下 ENTER 鍵或 ESC 返回主選單";
        g2d.drawString(prompt, (width - g2d.getFontMetrics().stringWidth(prompt)) / 2, 480);
    }

    private String getThemeChineseLabel(com.tetris.util.ThemeManager.Theme theme) {
        if (theme == null) return "未知";
        switch (theme) {
            case CYBERPUNK_NEON: return "賽博霓虹";
            case RETRO_GAMEBOY: return "復古 GameBoy";
            case CLASSIC_ARCADE: return "經典街機";
            case MIDNIGHT_STEALTH: return "暗夜幽影";
            default: return theme.getLabel();
        }
    }

    private String getColorBlindModeChineseLabel(com.tetris.util.ThemeManager.ColorBlindMode mode) {
        if (mode == null) return "未知";
        return mode.getLabel();
    }

    private String getSoundPackChineseLabel(com.tetris.util.SoundManager.SoundPack pack) {
        if (pack == null) return "未知";
        return pack.getLabel();
    }
    /**
     * 繪製雙欄控制設定卡片
     */
    private void drawControlSettings(Graphics2D g2d) {
        java.awt.Point mousePos = getMousePosition();
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 42));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "控制設定";
        int titleX = (getWidth() - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 100;

        g2d.setColor(new Color(0, 255, 255, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString(titleText, titleX, titleY);

        int cardW = 440;
        int cardH = 400;
        int cardX = (getWidth() - cardW) / 2;
        int cardY = 135;

        // Draw card background
        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        // Column 1: Sensitivity & Gamepad (X starts around cardX + 20)
        int col1X = cardX + 20;
        int col1W = 185;

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 16));
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString("靈敏度設定", col1X, cardY + 35);

        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);

        // DAS Button
        int dasVal = com.tetris.controller.InputHandler.getDasDelayMs();
        String dasText = "橫移延遲 (DAS): " + dasVal + " ms";
        int btnH = 26;
        if (controlDasBounds == null) controlDasBounds = new Rectangle(col1X, cardY + 50, col1W, btnH); else controlDasBounds.setBounds(col1X, cardY + 50, col1W, btnH);
        boolean hoverDas = (mousePos != null && controlDasBounds.contains(mousePos));
        drawControlSubButton(g2d, dasText, controlDasBounds, hoverDas);

        // ARR Button
        int arrVal = com.tetris.controller.InputHandler.getArrRateMs();
        String arrText = "橫移重複 (ARR): " + arrVal + " ms";
        if (controlArrBounds == null) controlArrBounds = new Rectangle(col1X, cardY + 90, col1W, btnH); else controlArrBounds.setBounds(col1X, cardY + 90, col1W, btnH);
        boolean hoverArr = (mousePos != null && controlArrBounds.contains(mousePos));
        drawControlSubButton(g2d, arrText, controlArrBounds, hoverArr);

        // SDR Button
        int sdrVal = com.tetris.controller.InputHandler.getSoftDropIntervalMs();
        String sdrText = "軟降速度 (SDR): " + sdrVal + " ms";
        if (controlSdrBounds == null) controlSdrBounds = new Rectangle(col1X, cardY + 130, col1W, btnH); else controlSdrBounds.setBounds(col1X, cardY + 130, col1W, btnH);
        boolean hoverSdr = (mousePos != null && controlSdrBounds.contains(mousePos));
        drawControlSubButton(g2d, sdrText, controlSdrBounds, hoverSdr);

        // Keyboard Default Config Guide Box
        int guideY = cardY + 185;
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        g2d.setColor(new Color(255, 160, 122)); // Coral Title
        g2d.drawString("預設鍵盤配置", col1X, guideY + 15);

        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(180, 180, 195));
        g2d.drawString("單人預設：", col1X, guideY + 36);
        g2d.drawString("  方向鍵控制、Space 硬降、C 暫存", col1X, guideY + 52);
        g2d.drawString("雙人左手 (P1)：", col1X, guideY + 72);
        g2d.drawString("  WASD 控制、SPACE 硬降、C 暫存", col1X, guideY + 88);
        g2d.drawString("雙人右手 (P2)：", col1X, guideY + 108);
        g2d.drawString("  方向鍵控制、ENTER 硬降、SHIFT 暫存", col1X, guideY + 124);

        // Column 2: Key Rebinding (X starts around cardX + 235)
        int col2X = cardX + 235;
        int col2W = 185;

        // Player Tab Buttons (Single / PvP P1 / PvP P2)
        int tabW = (col2W - 4) / 3;
        if (controlSingleTabBounds == null) controlSingleTabBounds = new Rectangle(col2X, cardY + 15, tabW, 28);
        else controlSingleTabBounds.setBounds(col2X, cardY + 15, tabW, 28);

        if (controlP1TabBounds == null) controlP1TabBounds = new Rectangle(col2X + tabW + 2, cardY + 15, tabW, 28);
        else controlP1TabBounds.setBounds(col2X + tabW + 2, cardY + 15, tabW, 28);

        int tab3W = col2W - 2 * tabW - 4;
        if (controlP2TabBounds == null) controlP2TabBounds = new Rectangle(col2X + 2 * tabW + 4, cardY + 15, tab3W, 28);
        else controlP2TabBounds.setBounds(col2X + 2 * tabW + 4, cardY + 15, tab3W, 28);

        boolean hoverSingle = (mousePos != null && controlSingleTabBounds.contains(mousePos));
        boolean hoverP1 = (mousePos != null && controlP1TabBounds.contains(mousePos));
        boolean hoverP2 = (mousePos != null && controlP2TabBounds.contains(mousePos));

        // Draw Tabs
        drawTabButton(g2d, "單人", controlSingleTabBounds, rebindingPlayer == 1, hoverSingle);
        drawTabButton(g2d, "雙人左", controlP1TabBounds, rebindingPlayer == 2, hoverP1);
        drawTabButton(g2d, "雙人右", controlP2TabBounds, rebindingPlayer == 3, hoverP2);

        // Action Label and Buttons List
        String[] actions = { "LEFT", "RIGHT", "ROTATE", "DOWN", "DROP", "HOLD" };
        String[] labels = { "向左移動", "向右移動", "順時針轉", "軟降加速", "硬降直接", "暫存方塊" };

        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i < 6; i++) {
            int y = cardY + 55 + i * 36;
            if (controlKeyBounds[i] == null) controlKeyBounds[i] = new Rectangle(col2X + 75, y, col2W - 75, btnH); else controlKeyBounds[i].setBounds(col2X + 75, y, col2W - 75, btnH);
            
            // Draw action description label
            g2d.setColor(Color.WHITE);
            g2d.drawString(labels[i], col2X, y + 17);

            // Get Key Name
            int code = getRegisteredKeyCode(rebindingPlayer, actions[i]);
            String keyText = KeyEvent.getKeyText(code);

            boolean isThisRebinding = (rebindingPlayer == getRebindingPlayer() && actions[i].equals(getRebindingAction()));
            if (isThisRebinding) {
                keyText = (System.currentTimeMillis() % 1000 < 500) ? "請按按鍵..." : "";
            }

            boolean hoverKey = (mousePos != null && controlKeyBounds[i].contains(mousePos));
            drawKeyRebindButton(g2d, keyText, controlKeyBounds[i], hoverKey, isThisRebinding);
        }

        // Bottom Row: Reset and Back Buttons
        int bottomY = cardY + 345;
        if (controlResetButtonBounds == null) controlResetButtonBounds = new Rectangle(cardX + 45, bottomY, 150, 34); else controlResetButtonBounds.setBounds(cardX + 45, bottomY, 150, 34);
        if (controlBackButtonBounds == null) controlBackButtonBounds = new Rectangle(cardX + 245, bottomY, 150, 34); else controlBackButtonBounds.setBounds(cardX + 245, bottomY, 150, 34);

        boolean hoverReset = (mousePos != null && controlResetButtonBounds.contains(mousePos));
        boolean hoverBack = (mousePos != null && controlBackButtonBounds.contains(mousePos));

        // Draw Reset Button
        if (hoverReset) {
            g2d.setColor(new Color(255, 60, 60, 45));
            g2d.fillRoundRect(controlResetButtonBounds.x, controlResetButtonBounds.y, controlResetButtonBounds.width, controlResetButtonBounds.height, 8, 8);
            g2d.setColor(new Color(255, 100, 100));
        } else {
            g2d.setColor(new Color(255, 255, 255, 10));
            g2d.fillRoundRect(controlResetButtonBounds.x, controlResetButtonBounds.y, controlResetButtonBounds.width, controlResetButtonBounds.height, 8, 8);
            g2d.setColor(new Color(255, 80, 80));
        }
        g2d.drawRoundRect(controlResetButtonBounds.x, controlResetButtonBounds.y, controlResetButtonBounds.width, controlResetButtonBounds.height, 8, 8);
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        int rTextW = g2d.getFontMetrics().stringWidth("重設預設值");
        g2d.drawString("重設預設值", controlResetButtonBounds.x + (controlResetButtonBounds.width - rTextW) / 2, bottomY + 22);

        // Draw Back Button
        if (hoverBack) {
            g2d.setColor(new Color(255, 255, 255, 45));
            g2d.fillRoundRect(controlBackButtonBounds.x, controlBackButtonBounds.y, controlBackButtonBounds.width, controlBackButtonBounds.height, 8, 8);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 10));
            g2d.fillRoundRect(controlBackButtonBounds.x, controlBackButtonBounds.y, controlBackButtonBounds.width, controlBackButtonBounds.height, 8, 8);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(controlBackButtonBounds.x, controlBackButtonBounds.y, controlBackButtonBounds.width, controlBackButtonBounds.height, 8, 8);
        int bTextW = g2d.getFontMetrics().stringWidth("返回");
        g2d.drawString("返回", controlBackButtonBounds.x + (controlBackButtonBounds.width - bTextW) / 2, bottomY + 22);
    }

    private void drawControlSubButton(Graphics2D g2d, String text, Rectangle bounds, boolean hover) {
        if (hover) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 12));
        int textW = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, bounds.x + (bounds.width - textW) / 2, bounds.y + 17);
    }

    private void drawTabButton(Graphics2D g2d, String text, Rectangle bounds, boolean active, boolean hover) {
        if (active) {
            g2d.setColor(new Color(0, 255, 255, 60));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(new Color(0, 255, 255));
        } else if (hover) {
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 10));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(new Color(180, 180, 180));
        }
        g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 12));
        int textW = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, bounds.x + (bounds.width - textW) / 2, bounds.y + 18);
    }

    private void drawKeyRebindButton(Graphics2D g2d, String text, Rectangle bounds, boolean hover, boolean active) {
        if (active) {
            g2d.setColor(new Color(255, 255, 0, 45)); // flashing yellow highlight
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(Color.YELLOW);
        } else if (hover) {
            g2d.setColor(new Color(255, 255, 255, 45));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2d.setColor(new Color(220, 220, 220));
        }
        g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 11));
        int textW = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, bounds.x + (bounds.width - textW) / 2, bounds.y + 17);
    }

    private int getRegisteredKeyCode(int player, String action) {
        if (player == 1) {
            if ("LEFT".equals(action)) return com.tetris.controller.InputHandler.getSingleKeyLeft();
            if ("RIGHT".equals(action)) return com.tetris.controller.InputHandler.getSingleKeyRight();
            if ("ROTATE".equals(action)) return com.tetris.controller.InputHandler.getSingleKeyRotate();
            if ("DOWN".equals(action)) return com.tetris.controller.InputHandler.getSingleKeyDown();
            if ("DROP".equals(action)) return com.tetris.controller.InputHandler.getSingleKeyDrop();
            if ("HOLD".equals(action)) return com.tetris.controller.InputHandler.getSingleKeyHold();
        } else if (player == 2) {
            if ("LEFT".equals(action)) return com.tetris.controller.InputHandler.getPvpP1KeyLeft();
            if ("RIGHT".equals(action)) return com.tetris.controller.InputHandler.getPvpP1KeyRight();
            if ("ROTATE".equals(action)) return com.tetris.controller.InputHandler.getPvpP1KeyRotate();
            if ("DOWN".equals(action)) return com.tetris.controller.InputHandler.getPvpP1KeyDown();
            if ("DROP".equals(action)) return com.tetris.controller.InputHandler.getPvpP1KeyDrop();
            if ("HOLD".equals(action)) return com.tetris.controller.InputHandler.getPvpP1KeyHold();
        } else if (player == 3) {
            if ("LEFT".equals(action)) return com.tetris.controller.InputHandler.getPvpP2KeyLeft();
            if ("RIGHT".equals(action)) return com.tetris.controller.InputHandler.getPvpP2KeyRight();
            if ("ROTATE".equals(action)) return com.tetris.controller.InputHandler.getPvpP2KeyRotate();
            if ("DOWN".equals(action)) return com.tetris.controller.InputHandler.getPvpP2KeyDown();
            if ("DROP".equals(action)) return com.tetris.controller.InputHandler.getPvpP2KeyDrop();
            if ("HOLD".equals(action)) return com.tetris.controller.InputHandler.getPvpP2KeyHold();
        }
        return 0;
    }

    // Novice guide tips array (Pre-split for width formatting)
    private static final String[][] SIDEBAR_TIPS = {
        { "按 [C] 鍵可將當前方塊", "存入暫存區，需要時", "再按 C 取出。" },
        { "按 [空白鍵] 可以執行硬降", "讓方塊瞬間落底並鎖定。" },
        { "在方塊落地的瞬間，您有", "短暫的鎖定延遲時間可以", "進行移動或旋轉。" },
        { "進行 T-Spin 旋轉消行", "在實戰中可以獲得極高的", "分數與攻擊力！" },
        { "達成 Combo 連擊或一次", "消除四行 (Tetris) 可以", "獲得顯著的分數加成。" },
        { "完美的版面全消 (PC)", "會為您帶來額外的", "2000 分大獎！" },
        { "靈敏度（DAS 與 ARR）", "可以在設定選單中調整，", "幫助您移動得更快。" }
    };

    private void drawSidebarTipBox(Graphics2D g2d, int x, int y, int w, int h) {
        g2d.setColor(new Color(255, 255, 255, 12));
        g2d.fillRoundRect(x, y, w, h, 10, 10);
        g2d.setColor(new Color(255, 215, 120, 65));
        g2d.drawRoundRect(x, y, w, h, 10, 10);

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 12));
        g2d.setColor(new Color(255, 215, 120));
        g2d.drawString("💡 新手小撇步", x + 10, y + 20);

        int tipIndex = (gameEngine.getSecondsElapsed() / 12) % SIDEBAR_TIPS.length;
        String[] lines = SIDEBAR_TIPS[tipIndex];

        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(220, 220, 240));

        int textY = y + 42;
        for (String line : lines) {
            g2d.drawString(line, x + 10, textY);
            textY += 17;
        }
    }

    // Tutorial level target areas
    private List<java.awt.Point> getTutorialTargetCells() {
        List<java.awt.Point> cells = new ArrayList<>();
        if (gameEngine == null || gameEngine.getGameState() != com.tetris.controller.GameEngine.GameState.TUTORIAL) {
            return cells;
        }
        int level = gameEngine.getTutorialLevel();
        if (level == 1) {
            cells.add(new java.awt.Point(0, 16));
            cells.add(new java.awt.Point(0, 17));
            cells.add(new java.awt.Point(0, 18));
            cells.add(new java.awt.Point(0, 19));
        } else if (level == 2) {
            cells.add(new java.awt.Point(4, 16));
            cells.add(new java.awt.Point(4, 17));
            cells.add(new java.awt.Point(4, 18));
            cells.add(new java.awt.Point(4, 19));
        } else if (level == 3) {
            cells.add(new java.awt.Point(5, 16));
            cells.add(new java.awt.Point(5, 17));
            cells.add(new java.awt.Point(5, 18));
            cells.add(new java.awt.Point(5, 19));
        } else if (level == 4) {
            cells.add(new java.awt.Point(4, 18));
            cells.add(new java.awt.Point(5, 18));
            cells.add(new java.awt.Point(4, 19));
            cells.add(new java.awt.Point(5, 19));
        } else if (level == 5) {
            cells.add(new java.awt.Point(2, 18));
            cells.add(new java.awt.Point(3, 18));
            cells.add(new java.awt.Point(4, 18));
            cells.add(new java.awt.Point(3, 19));
        } else if (level == 6) {
            cells.add(new java.awt.Point(3, 17));
            cells.add(new java.awt.Point(3, 18));
            cells.add(new java.awt.Point(4, 18));
            cells.add(new java.awt.Point(3, 19));
        } else if (level == 7) {
            cells.add(new java.awt.Point(6, 17));
            cells.add(new java.awt.Point(5, 18));
            cells.add(new java.awt.Point(6, 18));
            cells.add(new java.awt.Point(6, 19));
        }
        return cells;
    }

    private void drawTutorialTargetZone(Graphics2D g2d) {
        List<java.awt.Point> targets = getTutorialTargetCells();
        if (targets.isEmpty()) return;

        g2d.setColor(new Color(0, 255, 255, 80));
        java.awt.Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new java.awt.BasicStroke(2.0f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        for (java.awt.Point p : targets) {
            g2d.drawRect(p.x * TILE_SIZE + 2, p.y * TILE_SIZE + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            g2d.fillRect(p.x * TILE_SIZE + 4, p.y * TILE_SIZE + 4, TILE_SIZE - 8, TILE_SIZE - 8);
        }
        g2d.setStroke(oldStroke);
    }

    // Draw Tutorial Select Screen
    private void drawTutorialSelectScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, new Color(10, 10, 25), 0, getHeight(), new Color(30, 15, 50));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        for (FloatingPiece fp : floatingPieces) {
            drawFloatingPiece(g2d, fp);
        }

        g2d.setFont(getCachedFont("Impact", Font.BOLD, 42));
        g2d.setColor(new Color(0, 255, 255));
        String title = "互動教學與新手引導";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 75);

        g2d.setColor(new Color(0, 255, 255, 150));
        g2d.fillRect(40, 85, getWidth() - 80, 3);

        int startY = 120;
        int gap = 48;
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 18));
        FontMetrics fmOpt = g2d.getFontMetrics();

        String[] levels = {
            "1. 基礎移動與軟降練習",
            "2. 方塊旋轉與卡位技巧",
            "3. 暫存區 (Hold) 靈活使用",
            "4. 硬降 (Hard Drop) 與快速消行",
            "5. T-Spin 基礎：單次消行 (Single)",
            "6. T-Spin 進階：雙重消行 (Double 右旋)",
            "7. T-Spin 進階：雙重消行 (Double 左旋)",
            "返回主選單"
        };

        for (int i = 0; i < 8; i++) {
            int textWidth = fmOpt.stringWidth(levels[i]);
            int textHeight = fmOpt.getHeight();
            int x = (getWidth() - textWidth) / 2;
            int y = startY + i * gap;

            if (tutorialOptionBounds[i] == null) tutorialOptionBounds[i] = new Rectangle(x - 20, y - textHeight + 5, textWidth + 40, textHeight + 10); else tutorialOptionBounds[i].setBounds(x - 20, y - textHeight + 5, textWidth + 40, textHeight + 10);

            boolean isSelected = (i == selectedTutorialIndex);
            if (isSelected) {
                g2d.setColor(new Color(0, 255, 255, 35));
                g2d.fillRoundRect(tutorialOptionBounds[i].x, tutorialOptionBounds[i].y, tutorialOptionBounds[i].width, tutorialOptionBounds[i].height, 10, 10);
                g2d.setColor(new Color(0, 255, 255));
                g2d.drawRoundRect(tutorialOptionBounds[i].x, tutorialOptionBounds[i].y, tutorialOptionBounds[i].width, tutorialOptionBounds[i].height, 10, 10);

                g2d.setColor(Color.WHITE);
                g2d.drawString(levels[i], x, y);

                int sqSize = 8;
                g2d.fillRect(tutorialOptionBounds[i].x + 6, y - textHeight/2 - sqSize/2 + 2, sqSize, sqSize);
                g2d.fillRect(tutorialOptionBounds[i].x + tutorialOptionBounds[i].width - 14, y - textHeight/2 - sqSize/2 + 2, sqSize, sqSize);
            } else {
                g2d.setColor(new Color(150, 150, 180));
                g2d.drawString(levels[i], x, y);
            }
        }
    }

    public void navigateTutorialSelect(int dir) {
        selectedTutorialIndex = (selectedTutorialIndex + dir + 8) % 8;
        repaint();
    }

    public void selectTutorialOption() {
        if (selectedTutorialIndex == 7) {
            gameEngine.returnToMenu();
        } else {
            gameEngine.startTutorialLevel(selectedTutorialIndex + 1);
        }
    }

    // Draw Achievements Screen
    private void drawAchievementsScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, new Color(10, 10, 25), 0, getHeight(), new Color(30, 15, 50));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        for (FloatingPiece fp : floatingPieces) {
            drawFloatingPiece(g2d, fp);
        }

        g2d.setFont(getCachedFont("Impact", Font.BOLD, 42));
        g2d.setColor(new Color(255, 215, 0));
        String title = "成就與說明";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 75);

        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.fillRect(40, 85, getWidth() - 80, 3);

        int cardW = 460;
        int cardH = 390;
        int cardX = (getWidth() - cardW) / 2;
        int cardY = 105;

        g2d.setColor(new Color(255, 255, 255, 12));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        List<com.tetris.util.AchievementManager.AchievementInfo> list = com.tetris.util.AchievementManager.getAchievements();
        int startX1 = cardX + 15;
        int startX2 = cardX + 235;
        int itemH = 68;
        int startItemY = cardY + 20;

        for (int i = 0; i < list.size(); i++) {
            com.tetris.util.AchievementManager.AchievementInfo info = list.get(i);
            int col = i % 2;
            int row = i / 2;

            int x = (col == 0) ? startX1 : startX2;
            int y = startItemY + row * itemH;

            // Draw item container
            if (info.unlocked) {
                g2d.setColor(new Color(0, 255, 100, 15));
                g2d.fillRoundRect(x, y, 210, 60, 8, 8);
                g2d.setColor(new Color(0, 255, 100, 50));
            } else {
                g2d.setColor(new Color(255, 255, 255, 5));
                g2d.fillRoundRect(x, y, 210, 60, 8, 8);
                g2d.setColor(new Color(255, 255, 255, 15));
            }
            g2d.drawRoundRect(x, y, 210, 60, 8, 8);

            // Draw status icon
            if (info.unlocked) {
                g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 18));
                g2d.drawString("🏆", x + 10, y + 36);
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 18));
                g2d.drawString("🔒", x + 10, y + 36);
                g2d.setColor(new Color(120, 120, 130));
            }

            // Draw title
            g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 13));
            g2d.drawString(info.title, x + 40, y + 20);

            // Draw desc and requirement
            g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 10));
            if (info.unlocked) {
                g2d.setColor(new Color(200, 200, 200));
                g2d.drawString(info.desc, x + 40, y + 36);
                g2d.setColor(new Color(0, 255, 100));
                g2d.drawString("解鎖條件: " + info.requirement, x + 40, y + 50);
            } else {
                g2d.setColor(new Color(110, 110, 120));
                g2d.drawString("???", x + 40, y + 36);
                g2d.drawString("解鎖條件: " + info.requirement, x + 40, y + 50);
            }
        }

        // Back Button
        int btnW = 120;
        int btnH = 34;
        int btnX = (getWidth() - btnW) / 2;
        int btnY = cardY + cardH + 15;
        if (achievementsBackButtonBounds == null) achievementsBackButtonBounds = new Rectangle(btnX, btnY, btnW, btnH); else achievementsBackButtonBounds.setBounds(btnX, btnY, btnW, btnH);

        java.awt.Point mousePos = getMousePosition();
        boolean hover = (mousePos != null && achievementsBackButtonBounds.contains(mousePos));

        if (hover) {
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(0, 255, 255));
        } else {
            g2d.setColor(new Color(255, 255, 255, 12));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
            g2d.setColor(new Color(200, 200, 220));
        }
        g2d.drawRoundRect(btnX, btnY, btnW, btnH, 8, 8);

        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 14));
        int textW = g2d.getFontMetrics().stringWidth("返回主選單");
        g2d.drawString("返回主選單", btnX + (btnW - textW) / 2, btnY + 22);
    }

    // Draw Toast notification for achievements
    private void drawAchievementToast(Graphics2D g2d) {
        long elapsed = System.currentTimeMillis() - toastStartTime;
        float progress;
        float alpha;
        int yOffset;

        if (elapsed < 400) { // fade in & slide down
            progress = elapsed / 400.0f;
            alpha = progress;
            yOffset = (int) (-40 + 60 * progress);
        } else if (elapsed > TOAST_DURATION - 400) { // fade out & slide up
            progress = (TOAST_DURATION - elapsed) / 400.0f;
            alpha = progress;
            yOffset = (int) (-40 + 60 * progress);
        } else {
            alpha = 1.0f;
            yOffset = 20;
        }

        int toastW = 300;
        int toastH = 50;
        int toastX = (getWidth() - toastW) / 2;
        int toastY = yOffset;

        java.awt.Composite oldComp = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Background card
        g2d.setColor(new Color(15, 15, 30, 230));
        g2d.fillRoundRect(toastX, toastY, toastW, toastH, 12, 12);

        // Gold border
        g2d.setColor(new Color(255, 215, 0, 220));
        g2d.setStroke(new java.awt.BasicStroke(1.5f));
        g2d.drawRoundRect(toastX, toastY, toastW, toastH, 12, 12);
        g2d.setStroke(new java.awt.BasicStroke(1.0f));

        // Icon
        g2d.setFont(getCachedFont("SansSerif", Font.PLAIN, 20));
        g2d.drawString("🏆", toastX + 15, toastY + 33);

        // Title text
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 12));
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawString("成就解鎖！", toastX + 50, toastY + 20);

        // Achievement Title
        g2d.setFont(getCachedFont("SansSerif", Font.BOLD, 13));
        g2d.setColor(Color.WHITE);
        g2d.drawString(activeToastTitle, toastX + 50, toastY + 38);

        g2d.setComposite(oldComp);
    }

    public void showAchievementToast(String title, String desc) {
        this.activeToastTitle = title;
        this.activeToastDesc = desc;
        this.toastStartTime = System.currentTimeMillis();
        
        // Play clear sound as achievement chime
        com.tetris.util.SoundManager.playSFX("/resources/clear.wav");
        
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }
}