package com.tetris.view;

import javax.swing.JPanel;

import com.tetris.model.Board;
import com.tetris.model.LeaderboardEntry;
import com.tetris.model.Piece;

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
    
    // Active score popups & particles
    private final List<ScorePopup> scorePopups = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Timer animationTimer;

    // Menu properties
    private int selectedMenuIndex = 0;
    private final Rectangle[] menuOptionBounds = new Rectangle[6];
    private Rectangle backButtonBounds = new Rectangle();
    private final List<FloatingPiece> floatingPieces = new ArrayList<>();
    private Timer menuAnimationTimer;

    // Pause menu properties
    private int selectedPauseIndex = 0;
    private final Rectangle[] pauseOptionBounds = new Rectangle[4];
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
    private boolean isDraggingBGM = false;
    private boolean isDraggingSFX = false;

    // Difficulty selection properties
    private boolean showDifficultySelectInMenu = false;
    private int selectedDifficultyIndex = 0; // 0: EASY, 1: MEDIUM, 2: HARD, 3: BACK
    private final Rectangle[] difficultyOptionBounds = new Rectangle[4];

    // Leaderboard screen properties
    private com.tetris.controller.GameEngine.Difficulty selectedLeaderboardDifficulty = com.tetris.controller.GameEngine.Difficulty.NORMAL;
    private final Rectangle[] leaderboardTabBounds = new Rectangle[3];

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
    public void addScorePopup(int gridCol, int gridRow, int score, int lines, boolean isTSpin, int comboCount) {
        String scoreText = "+" + score;
        String lineText = "";
        String comboText = (comboCount >= 1) ? ((comboCount + 1) + " COMBO!") : "";
        Color popupColor = new Color(255, 235, 120); // Default gold
        Font font = new Font("Arial", Font.BOLD, 24); // Font size

        if (isTSpin) {
            switch (lines) {
                case 0:
                    lineText = "T-SPIN";
                    popupColor = new Color(255, 0, 255); // Neon Purple
                    font = new Font("Impact", Font.BOLD, 28);
                    break;
                case 1:
                    lineText = "T-SPIN SINGLE!";
                    popupColor = new Color(255, 100, 255); // Neon Magenta
                    font = new Font("Impact", Font.BOLD, 28);
                    break;
                case 2:
                    lineText = "T-SPIN DOUBLE!!";
                    popupColor = new Color(255, 215, 0); // Neon Gold
                    font = new Font("Impact", Font.BOLD, 32);
                    break;
                case 3:
                    lineText = "T-SPIN TRIPLE!!!";
                    popupColor = new Color(255, 50, 50); // Neon Red
                    font = new Font("Impact", Font.BOLD, 36);
                    break;
                default:
                    lineText = "T-SPIN CLEAR";
                    popupColor = new Color(255, 0, 255);
                    font = new Font("Impact", Font.BOLD, 28);
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
                    font = new Font("Impact", Font.BOLD, 44); // Size 44 instead of 30!
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

        synchronized (scorePopups) {
            scorePopups.add(new ScorePopup(lineText, scoreText, comboText, px, py, popupColor, font));
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    // Draw and update active score popups
    private void drawScorePopups(Graphics g) {
        if (scorePopups.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        // Enable anti-aliasing for smooth rendering of text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long now = System.currentTimeMillis();

        synchronized (scorePopups) {
            Iterator<ScorePopup> it = scorePopups.iterator();
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
                g2.setFont(new Font("Arial", Font.BOLD, 22)); // Score font size
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
                    g2.setFont(new Font("Impact", Font.ITALIC, 22));
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
            synchronized (particles) {
                if (!particles.isEmpty()) {
                    active = true;
                    Iterator<Particle> it = particles.iterator();
                    while (it.hasNext()) {
                        Particle p = it.next();
                        p.update();
                        if (p.life <= 0) {
                            it.remove();
                        }
                    }
                }
            }
            if (System.currentTimeMillis() < shakeEndTime) {
                active = true;
            }
            if (System.currentTimeMillis() - perfectClearStartTime < PERFECT_CLEAR_DURATION) {
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
                if (gameEngine == null) return;
                com.tetris.controller.GameEngine.GameState state = gameEngine.getGameState();
                if (state == com.tetris.controller.GameEngine.GameState.MENU) {
                    if (showSettingsInMenu) {
                        // BACK button click
                        if (menuSettingsBackButtonBounds != null && menuSettingsBackButtonBounds.contains(e.getPoint())) {
                            showSettingsInMenu = false;
                            selectedMenuIndex = com.tetris.util.SaveManager.hasSave() ? 3 : 2; // Return Indicator to SETTINGS
                            repaint();
                            return;
                        }

                        // THEME button click
                        if (menuSettingsThemeBounds != null && menuSettingsThemeBounds.contains(e.getPoint())) {
                            com.tetris.util.ThemeManager.nextTheme();
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
                            repaint();
                        } else if (sfxMuteBounds.contains(e.getPoint())) {
                            com.tetris.util.SoundManager.setSFXMuted(!com.tetris.util.SoundManager.isSFXMuted());
                            repaint();
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
                        int numOptions = com.tetris.util.SaveManager.hasSave() ? 6 : 5;
                        for (int i = 0; i < numOptions; i++) {
                            if (menuOptionBounds[i] != null && menuOptionBounds[i].contains(e.getPoint())) {
                                selectedMenuIndex = i;
                                selectCurrentOption();
                                break;
                            }
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.LEADERBOARD) {
                    if (backButtonBounds != null && backButtonBounds.contains(e.getPoint())) {
                        gameEngine.returnToMenu();
                    }
                    for (int i = 0; i < 3; i++) {
                        if (leaderboardTabBounds[i] != null && leaderboardTabBounds[i].contains(e.getPoint())) {
                            selectedLeaderboardDifficulty = com.tetris.controller.GameEngine.Difficulty.values()[i];
                            repaint();
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.PLAYING) {
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
                        int numPauseOptions = 4;
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
                        int numOptions = com.tetris.util.SaveManager.hasSave() ? 6 : 5;
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
                } else if (state == com.tetris.controller.GameEngine.GameState.PLAYING && gameEngine.isPaused()) {
                    int numPauseOptions = 4;
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
    }

    // Set the game engine reference
    public void setGameEngine(com.tetris.controller.GameEngine gameEngine) {
        this.gameEngine = gameEngine;
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
        selectedPauseIndex = 0;
        repaint();
    }

    // Set the current piece
    public void setCurrentPiece(Piece piece) {
        this.currentPiece = piece;
    }

    // Deprecated: Score is now retrieved from gameEngine
    public void setScore(int score) {
        // Keeping it for compatibility with existing calls, but will use engine's score
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameEngine == null) return;

        switch (gameEngine.getGameState()) {
            case MENU:
                drawMenuScreen(g);
                break;
            case LEADERBOARD:
                drawLeaderboardScreen(g);
                break;
            case PLAYING:
            default:
                Graphics2D g2d = (Graphics2D) g;
                java.awt.geom.AffineTransform oldTransform = g2d.getTransform();

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
                    drawGameOverOverlay(g2d);
                }

                // Draw Perfect Clear Overlay
                drawPerfectClearOverlay(g2d);

                g2d.setTransform(oldTransform);
                break;
        }
    }

    // Menu navigation helper
    public void navigateMenu(int dir) {
        if (showDifficultySelectInMenu) {
            selectedDifficultyIndex = (selectedDifficultyIndex + dir + 4) % 4;
        } else {
            int numOptions = com.tetris.util.SaveManager.hasSave() ? 6 : 5;
            selectedMenuIndex = (selectedMenuIndex + dir + numOptions) % numOptions;
        }
        repaint();
    }

    // Menu select action helper
    public void selectCurrentOption() {
        if (showDifficultySelectInMenu) {
            triggerDifficultySelection();
        } else {
            boolean hasSave = com.tetris.util.SaveManager.hasSave();
            if (hasSave) {
                switch (selectedMenuIndex) {
                    case 0:
                        gameEngine.loadGame();
                        break;
                    case 1:
                        showDifficultySelectInMenu = true;
                        selectedDifficultyIndex = 0; // Default to EASY
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
                        gameEngine.showLeaderboard();
                        break;
                    case 5:
                        System.exit(0);
                        break;
                }
            } else {
                switch (selectedMenuIndex) {
                    case 0:
                        showDifficultySelectInMenu = true;
                        selectedDifficultyIndex = 0; // Default to EASY
                        break;
                    case 1:
                        // AI DEMO Option
                        gameEngine.setDifficulty(com.tetris.controller.GameEngine.Difficulty.NORMAL);
                        showDifficultySelectInMenu = false;
                        gameEngine.startGame();
                        gameEngine.setAiPlay(true);
                        break;
                    case 2:
                        showSettingsInMenu = true;
                        break;
                    case 3:
                        gameEngine.showLeaderboard();
                        break;
                    case 4:
                        System.exit(0);
                        break;
                }
            }
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
                selectedMenuIndex = com.tetris.util.SaveManager.hasSave() ? 1 : 0; // Return to Play Game
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
            drawMenuSettings(g2d);
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
        g2d.setFont(new Font("Impact", Font.BOLD | Font.ITALIC, 68));
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
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.setColor(new Color(0, 255, 255));
        String subtitle = "NEON ARCADE EDITION";
        int subX = (getWidth() - g2d.getFontMetrics().stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subX, 155);

        // Options
        boolean hasSave = com.tetris.util.SaveManager.hasSave();
        int numOptions = hasSave ? 6 : 5;
        int startY = hasSave ? 230 : 255;
        int gap = 45;
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fmOption = g2d.getFontMetrics();

        for (int i = 0; i < numOptions; i++) {
            String label = "";
            if (hasSave) {
                switch (i) {
                    case 0: label = "CONTINUE"; break;
                    case 1: label = "PLAY GAME"; break;
                    case 2: label = "AI DEMO"; break;
                    case 3: label = "SETTINGS"; break;
                    case 4: label = "LEADERBOARD"; break;
                    case 5: label = "EXIT"; break;
                }
            } else {
                switch (i) {
                    case 0: label = "PLAY GAME"; break;
                    case 1: label = "AI DEMO"; break;
                    case 2: label = "SETTINGS"; break;
                    case 3: label = "LEADERBOARD"; break;
                    case 4: label = "EXIT"; break;
                }
            }

            int textWidth = fmOption.stringWidth(label);
            int textHeight = fmOption.getHeight();
            int x = (getWidth() - textWidth) / 2;
            int y = startY + i * gap;

            menuOptionBounds[i] = new Rectangle(x - 20, y - textHeight + 5, textWidth + 40, textHeight + 10);

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
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(120, 120, 150));
        String hint1 = "Use \u2191 / \u2193 Arrows or Mouse to Navigate";
        String hint2 = "Press ENTER or Click to Select";
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
        g2d.setFont(new Font("Impact", Font.BOLD, 42));
        g2d.setColor(new Color(255, 215, 0)); // Gold
        String title = "TOP HIGH SCORES";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 80);

        // Underline
        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.fillRect(60, 95, getWidth() - 120, 3);

        // Draw Tabs
        int tabW = 90;
        int tabH = 30;
        int tabGap = 15;
        int totalW = 3 * tabW + 2 * tabGap;
        int startX = (getWidth() - totalW) / 2;
        int tabY = 112;
        
        java.awt.Point mousePos = getMousePosition();
        
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (tabW + tabGap);
            leaderboardTabBounds[i] = new Rectangle(x, tabY, tabW, tabH);
            
            com.tetris.controller.GameEngine.Difficulty tabDiff = com.tetris.controller.GameEngine.Difficulty.values()[i];
            boolean isSelected = (selectedLeaderboardDifficulty == tabDiff);
            boolean isHovered = (mousePos != null && leaderboardTabBounds[i].contains(mousePos));
            
            // Draw button background
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
            
            // Draw label
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            if (isSelected) {
                g2d.setColor(new Color(0, 255, 255));
            } else {
                g2d.setColor(new Color(200, 200, 200));
            }
            String tabLabel = tabDiff.getLabel();
            int labelW = g2d.getFontMetrics().stringWidth(tabLabel);
            int labelH = g2d.getFontMetrics().getAscent();
            g2d.drawString(tabLabel, x + (tabW - labelW) / 2, tabY + (tabH + labelH) / 2 - 2);
        }

        // Columns headers
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString("RANK", 50, 172);
        g2d.drawString("SCORE", 110, 172);
        g2d.drawString("DIFF", 210, 172);
        g2d.drawString("DATE", 310, 172);

        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.drawLine(40, 182, getWidth() - 40, 182);

        // List
        List<LeaderboardEntry> entries = gameEngine.getLeaderboardEntriesForDifficulty(selectedLeaderboardDifficulty);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        int y = 217;
        int rowGap = 32;

        if (entries == null || entries.isEmpty()) {
            g2d.setColor(new Color(150, 150, 180));
            g2d.drawString("No high scores recorded yet.", (getWidth() - g2d.getFontMetrics().stringWidth("No high scores recorded yet.")) / 2, 280);
            g2d.drawString("Start playing and set a record!", (getWidth() - g2d.getFontMetrics().stringWidth("Start playing and set a record!")) / 2, 310);
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : entries) {
                if (rank > 8) break;

                if (rank % 2 == 1) {
                    g2d.setColor(new Color(255, 255, 255, 10));
                    g2d.fillRect(40, y - 20, getWidth() - 80, 28);
                }

                if (rank == 1) g2d.setColor(new Color(255, 223, 0));
                else if (rank == 2) g2d.setColor(new Color(192, 192, 192));
                else if (rank == 3) g2d.setColor(new Color(205, 127, 50));
                else g2d.setColor(Color.WHITE);

                g2d.drawString("#" + rank, 50, y);
                g2d.drawString(String.valueOf(entry.getScore()), 110, y);
                String diffDisp = "NORMAL".equalsIgnoreCase(entry.getDifficulty()) ? "MEDIUM" : entry.getDifficulty();
                g2d.drawString(diffDisp, 210, y);
                g2d.drawString(entry.getPlayedAtDisplay(), 310, y);

                y += rowGap;
                rank++;
            }
        }

        // Back Button
        String backBtnText = "BACK TO MENU";
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fmBack = g2d.getFontMetrics();
        int btnW = fmBack.stringWidth(backBtnText) + 40;
        int btnH = 40;
        int btnX = (getWidth() - btnW) / 2;
        int btnY = 505;

        backButtonBounds = new Rectangle(btnX, btnY - 30, btnW, btnH);

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
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(new Color(120, 120, 140));
        String tabHint = "Press \u2190 / \u2192 Arrows or Click Tabs to Switch Difficulty";
        g2d.drawString(tabHint, (getWidth() - g2d.getFontMetrics().stringWidth(tabHint)) / 2, 560);
    }

    public void navigateLeaderboardTabs(int dir) {
        int currentIndex = selectedLeaderboardDifficulty.ordinal();
        int nextIndex = (currentIndex + dir + 3) % 3;
        selectedLeaderboardDifficulty = com.tetris.controller.GameEngine.Difficulty.values()[nextIndex];
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
        g2d.setColor(new Color(255, 40, 80, 100));
        g2d.setStroke(new java.awt.BasicStroke(3f));
        g2d.drawRect(5, 5, width - 10, height - 10);
        g2d.setStroke(new java.awt.BasicStroke(1f));

        // 2. Header: Centered red neon "GAME OVER"
        g2d.setFont(new Font("Impact", Font.BOLD | Font.ITALIC, 46));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "GAME OVER";
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 65;

        // Glow effect
        g2d.setColor(new Color(255, 0, 50, 120));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.drawString(titleText, titleX - 2, titleY - 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(titleText, titleX, titleY);

        // Neon violet subtitle "MISSION SUMMARY"
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.setColor(new Color(180, 100, 255));
        String subTitleText = "MISSION SUMMARY";
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

        drawStatCard(g2d, col1X, row1Y, cardW, cardH, "FINAL SCORE", String.format("%,d", score), new Color(255, 215, 0));
        drawStatCard(g2d, col2X, row1Y, cardW, cardH, "TIME ELAPSED", timeStr, new Color(0, 255, 255));
        drawStatCard(g2d, col1X, row2Y, cardW, cardH, "LINES CLEARED", String.valueOf(lines), new Color(0, 255, 100));
        drawStatCard(g2d, col2X, row2Y, cardW, cardH, "PIECES SPAWNED", String.format("%d (%.1f PPM)", pieces, ppm), new Color(255, 140, 0));
        drawStatCard(g2d, col1X, row3Y, cardW, cardH, "TOTAL ACTIONS", String.format("%d (%.1f APM)", actions, apm), new Color(180, 100, 255));
        drawStatCard(g2d, col2X, row3Y, cardW, cardH, "TETRIS RATE", String.format("%.1f%%", tetrisRate), new Color(255, 50, 150));

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
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.setColor(new Color(255, 215, 120)); // Gold
        g2d.drawString("EARNED BADGES", badgeContainerX + 15, badgeContainerY + 22);

        // Identify Earned Badges
        List<Badge> earnedBadges = new ArrayList<>();
        if (seconds >= 300) {
            earnedBadges.add(new Badge("🏆 Survivor", "Survived over 5 minutes (" + timeStr + ")", new Color(0, 255, 100)));
        }
        if (gameEngine.getTetrisClears() >= 5) {
            earnedBadges.add(new Badge("👑 Tetris Master", "Cleared 4 lines 5+ times (" + gameEngine.getTetrisClears() + " times)", new Color(255, 215, 0)));
        }
        if (ppm >= 40.0 && seconds >= 60) {
            earnedBadges.add(new Badge("⚡ Speed Demon", String.format("Maintained %.1f PPM (>40 PPM)", ppm), new Color(0, 255, 255)));
        }
        if (gameEngine.getTSpins() >= 3) {
            earnedBadges.add(new Badge("🔮 T-Spin Tactician", "Cleared T-Spin 3+ times (" + gameEngine.getTSpins() + " times)", new Color(180, 100, 255)));
        }
        if (gameEngine.getMaxCombo() >= 3) { // 4+ consecutive clears
            earnedBadges.add(new Badge("🔥 Combo Specialist", "Reached combo multiplier of " + (gameEngine.getMaxCombo() + 1), new Color(255, 50, 150)));
        }

        // Draw Badges
        if (earnedBadges.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.ITALIC, 13));
            g2d.setColor(new Color(150, 150, 160));
            String emptyMsg = "No badges earned this game.";
            String emptyTip = "Tip: Try surviving longer (5m) or getting 3+ T-Spins!";
            g2d.drawString(emptyMsg, badgeContainerX + 25, badgeContainerY + 65);
            g2d.drawString(emptyTip, badgeContainerX + 25, badgeContainerY + 95);
        } else {
            int badgeY = badgeContainerY + 45;
            int gap = 38;
            for (int i = 0; i < Math.min(3, earnedBadges.size()); i++) {
                Badge badge = earnedBadges.get(i);
                
                // Draw badge title
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.setColor(badge.color);
                g2d.drawString(badge.title, badgeContainerX + 20, badgeY);

                // Draw badge description
                g2d.setFont(new Font("Arial", Font.PLAIN, 11));
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
                g2d.setFont(new Font("Arial", Font.ITALIC, 11));
                g2d.setColor(new Color(150, 150, 160));
                String moreMsg = "+ " + (earnedBadges.size() - 3) + " more badges earned";
                g2d.drawString(moreMsg, badgeContainerX + badgeContainerW - 130, badgeContainerY + badgeContainerH - 12);
            }
        }

        // 5. Pulsing/Breathing Footer Instruction
        long now = System.currentTimeMillis();
        float pulse = (float) (0.6 + 0.4 * Math.sin(now / 250.0));
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String footerText = "Press ENTER or Click to return to Menu";
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
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(new Color(170, 170, 185));
        g2d.drawString(label, x + 12, y + 18);

        // Draw value text
        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        g2d.setColor(Color.WHITE);
        g2d.drawString(value, x + 12, y + 42);
    }

    // Pause menu navigation helper
    public void navigatePauseMenu(int dir) {
        int numOptions = 4;
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
                case 2: // THEME
                    com.tetris.util.ThemeManager.nextTheme();
                    break;
                case 3: // BACK
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
        g2d.setFont(new Font("Impact", Font.BOLD, 46));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "GAME PAUSED";
        int titleX = (width - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 120;

        // Draw shadow/glow
        g2d.setColor(new Color(255, 200, 0, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(255, 215, 0)); // Gold
        g2d.drawString(titleText, titleX, titleY);

        // Selection container card
        int cardW = 340;
        int cardH = showSettingsInPause ? 360 : 310;
        int cardX = (width - cardW) / 2;
        int cardY = 160;

        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        // Draw Options
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fmOption = g2d.getFontMetrics();
        int numPauseOptions = 4;

        for (int i = 0; i < numPauseOptions; i++) {
            String label = "";
            int x = 0, y = 0;
            int textHeight = fmOption.getHeight();
            int textWidth = 0;

            if (!showSettingsInPause) {
                switch (i) {
                    case 0: label = "RESUME GAME"; break;
                    case 1: label = "SAVE GAME"; break;
                    case 2: label = "SETTINGS"; break;
                    case 3: label = "BACK TO MAIN"; break;
                }
                textWidth = fmOption.stringWidth(label);
                x = cardX + (cardW - textWidth) / 2;
                y = cardY + 50 + i * 65;
                pauseOptionBounds[i] = new Rectangle(cardX + 15, y - textHeight + 5, cardW - 30, textHeight + 15);
            } else {
                switch (i) {
                    case 0: label = "GHOST PIECE: " + (showGhostPiece ? "ON" : "OFF"); break;
                    case 1: label = "GRID LINES: " + (showGridLines ? "ON" : "OFF"); break;
                    case 2: label = "THEME: " + com.tetris.util.ThemeManager.getCurrentTheme().getLabel(); break;
                    case 3: label = "BACK"; break;
                }
                textWidth = fmOption.stringWidth(label);
                x = cardX + (cardW - textWidth) / 2;
                if (i == 0) {
                    y = cardY + 45;
                    pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 22, cardW - 30, 32);
                } else if (i == 1) {
                    y = cardY + 95;
                    pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 72, cardW - 30, 32);
                } else if (i == 2) {
                    y = cardY + 260;
                    pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 237, cardW - 30, 32);
                } else {
                    y = cardY + 312;
                    pauseOptionBounds[i] = new Rectangle(cardX + 15, cardY + 289, cardW - 30, 32);
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
            drawVolumeSettings(g2d, cardX, cardY, cardW, cardY + 140, 55);
        } else {
            // Draw temporary "Progress Saved!" text if it was saved within 3 seconds
            if (System.currentTimeMillis() - lastSaveTime < 3000) {
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.setColor(new Color(0, 255, 100)); // Green confirmation
                String savedText = "Progress Saved!";
                int savedW = g2d.getFontMetrics().stringWidth(savedText);
                g2d.drawString(savedText, cardX + (cardW - savedW) / 2, cardY + cardH - 15);
            }
        }

        // Control Hints
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(120, 120, 150));
        String hint = "Use \u2191 / \u2193 Arrows or Mouse | Enter to Select";
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
        Color[][] grid = board.getGrid();

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
        if (currentPiece != null) {
            // Draw ghost first
            if (showGhostPiece) {
                drawGhostPiece(g);
            }

            Color color = currentPiece.getType().getColor();

            // Flash effect for Lock Delay
            if (gameEngine.isLocking()) {
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
            for (int[] coord : currentPiece.getAbsoluteCoords()) {
                drawSquare(g, coord[0], coord[1], color);
            }
        }
    }

    // Draw the ghost piece
    private void drawGhostPiece(Graphics g) {
        if (currentPiece == null)
            return;

        // Create a copy of the current piece at same pos/rotation
        com.tetris.model.Piece ghost = new com.tetris.model.Piece(
            currentPiece.getType(), currentPiece.getRow(),
            currentPiece.getCol(), currentPiece.getRotationIndex()
        );

        // Move ghost down
        while (board.isValidMove(ghost)) {
            ghost.move(1, 0);
        }
        ghost.move(-1, 0);

        // Draw ghost squares
        Color baseColor = currentPiece.getType().getColor();
        Color ghostColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 75); // Mapped translucent color
        for (int[] coord : ghost.getAbsoluteCoords()) {
            drawSquare(g, coord[0], coord[1], ghostColor);
        }
    }

    // Draw Sidebar
    private void drawSidebar(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int startX = COLS * TILE_SIZE;

        // Draw Sidebar Background
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(startX, 0, SIDEBAR_WIDTH, getHeight());

        // Draw Sidebar Border
        g.setColor(Color.GRAY);
        g.drawLine(startX, 0, startX, getHeight());

        if (gameEngine == null)
            return;

        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));

        // 1. Draw Score
        g.drawString("SCORE", startX + 20, 45);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));
        g.drawString(String.valueOf(gameEngine.getScore()), startX + 20, 72);

        // 2. Draw Timer
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        g.drawString("TIME", startX + 20, 108);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 24));
        int seconds = gameEngine.getSecondsElapsed();
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
        g.drawString(timeStr, startX + 20, 133);

        // 3. Draw Level
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        g.drawString("LEVEL", startX + 20, 170);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 20));
        g.drawString(gameEngine.getDifficulty().getLabel(), startX + 20, 195);

        // 4. Previews Side-by-Side (NEXT & HOLD)
        int boxY = 215;
        int boxW = 80;
        int boxH = 85;

        // Next Box
        int nextBoxX = startX + 15;
        g.setColor(new Color(255, 255, 255, 15));
        g.fillRoundRect(nextBoxX, boxY, boxW, boxH, 8, 8);
        g.setColor(new Color(255, 255, 255, 45));
        g.drawRoundRect(nextBoxX, boxY, boxW, boxH, 8, 8);

        g.setColor(new Color(0, 255, 255)); // Cyan title for NEXT
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        g.drawString("NEXT", nextBoxX + 8, boxY + 18);

        Piece nextPiece = gameEngine.getNextPiece();
        if (nextPiece != null) {
            drawCenteredPiece(g, nextPiece.getType(), nextBoxX, boxY + 20, boxW, boxH - 22);
        }

        // Hold Box
        int holdBoxX = startX + 105;
        g.setColor(new Color(255, 255, 255, 15));
        g.fillRoundRect(holdBoxX, boxY, boxW, boxH, 8, 8);
        g.setColor(new Color(255, 255, 255, 45));
        g.drawRoundRect(holdBoxX, boxY, boxW, boxH, 8, 8);

        g.setColor(new Color(255, 100, 255)); // Neon magenta title for HOLD
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        g.drawString("HOLD", holdBoxX + 8, boxY + 18);

        Piece heldPiece = gameEngine.getHeldPiece();
        if (heldPiece != null) {
            drawCenteredPiece(g, heldPiece.getType(), holdBoxX, boxY + 20, boxW, boxH - 22);
        } else {
            g.setColor(new Color(150, 150, 180));
            g.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 11));
            int emptyW = g.getFontMetrics().stringWidth("[Empty]");
            g.drawString("[Empty]", holdBoxX + (boxW - emptyW) / 2, boxY + 20 + (boxH - 22)/2 + 4);
        }

        // 5. Draw Leaderboard (Controls hints deleted to free up space)
        drawLeaderboard(g, startX + 15, 330);

        // 6. AI Autoplay Status Overlay / Score Disqualification Warning
        if (gameEngine.isAiPlay()) {
            int aiX = startX + 15;
            int aiY = 405;
            int aiW = 170;
            int aiH = 80;

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

            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g2d.setColor(new Color(230, 200, 255));
            g2d.drawString("AUTO PLAY", aiX + 30, aiY + 24);

            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
            g2d.setColor(new Color(185, 175, 200));
            g2d.drawString("AI is playing the game.", aiX + 15, aiY + 44);
            g2d.drawString("Press [A] to manual control", aiX + 15, aiY + 59);
        } else if (gameEngine.hasUsedAiThisSession()) {
            int warnX = startX + 15;
            int warnY = 405;
            int warnW = 170;
            int warnH = 65;

            // Semi-transparent warm warning background
            g2d.setColor(new Color(255, 69, 0, 25));
            g2d.fillRoundRect(warnX, warnY, warnW, warnH, 8, 8);

            // Orange-red border
            g2d.setColor(new Color(255, 100, 0, 160));
            g2d.drawRoundRect(warnX, warnY, warnW, warnH, 8, 8);

            g2d.setFont(new java.awt.Font("Microsoft JhengHei", java.awt.Font.BOLD, 11));
            g2d.setColor(new Color(255, 160, 122));
            g2d.drawString("不納入排行榜紀錄", warnX + 10, warnY + 22);

            g2d.setFont(new java.awt.Font("Microsoft JhengHei", java.awt.Font.PLAIN, 10));
            g2d.setColor(new Color(230, 200, 185));
            g2d.drawString("本局曾使用自動遊玩", warnX + 10, warnY + 40);
            g2d.drawString("分數將不寫入排行榜", warnX + 10, warnY + 53);
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

        int tileSize = 16;
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
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        g.drawString("LEADERBOARD", x, startY);

        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        int lineY = startY + 16;

        if (entries.isEmpty()) {
            g.setColor(new Color(210, 210, 210));
            g.drawString("No scores yet", x, lineY);
            return;
        }

        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (rank > 3) {
                break;
            }

            g.setColor(new Color(240, 240, 240));
            String diffDisp = "NORMAL".equalsIgnoreCase(entry.getDifficulty()) ? "MEDIUM" : entry.getDifficulty();
            String text = String.format("%d. %d pts | %s", rank, entry.getScore(), diffDisp);
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
            this.x = x;
            this.y = y;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.color = color;
            this.size = size;
            this.decay = decay;
        }

        Particle(double x, double y, Color color, int size, double angle, double speed, float decay, int shapeType) {
            this(x, y, color, size, angle, speed, decay);
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
        int py = gridRow * TILE_SIZE + TILE_SIZE / 2;
        Random rand = new Random();
        synchronized (particles) {
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
                    
                    particles.add(new Particle(px, py, colColor, size, angle, speed, decay));
                }
            }
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    // Spawns particles when a piece lands/hard drops
    public void spawnDropParticles(Piece piece) {
        if (piece == null) return;
        Color color = piece.getType().getColor();
        Random rand = new Random();
        synchronized (particles) {
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
                    
                    particles.add(new Particle(px, py, color, size, angle, speed, decay));
                }
            }
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }


    // Triggers a screenshake with intensity and duration
    public void triggerScreenshake(int intensity, int durationMs) {
        this.shakeIntensity = intensity;
        this.shakeDuration = durationMs;
        this.shakeEndTime = System.currentTimeMillis() + durationMs;
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    // Trigger Perfect Clear visual explosion and text display
    public void triggerPerfectClear() {
        this.perfectClearStartTime = System.currentTimeMillis();

        int boardW = COLS * TILE_SIZE;
        int boardH = ROWS * TILE_SIZE;
        int cx = boardW / 2;
        int cy = boardH / 2;

        Random rand = new Random();
        synchronized (particles) {
            for (int i = 0; i < 80; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double speed = 1.0 + rand.nextDouble() * 5.0;
                float decay = 0.008f + rand.nextFloat() * 0.012f; // slower decay for star particles
                int size = 5 + rand.nextInt(8);
                Color goldColor = new Color(255, 200 + rand.nextInt(56), 0); // golden colors

                particles.add(new Particle(cx, cy, goldColor, size, angle, speed, decay, 1));
            }
        }

        // Heavy screenshake shockwave!
        triggerScreenshake(12, 400);

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
        long elapsed = System.currentTimeMillis() - perfectClearStartTime;
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
        g2.setFont(new Font("Impact", Font.BOLD, 36));
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
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fmSub = g2.getFontMetrics();
        String subText = "+2000 BONUS";
        g2.setColor(Color.WHITE);
        g2.drawString(subText, -fmSub.stringWidth(subText) / 2, 40);

        g2.dispose();
    }

    // Draw active particles in the grid area
    private void drawParticles(Graphics g) {
        synchronized (particles) {
            if (particles.isEmpty()) return;
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (Particle p : particles) {
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
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
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
        
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        String muteText = bgmMute ? "MUTED" : "MUTE";
        int muteTextW = g2d.getFontMetrics().stringWidth(muteText);
        g2d.drawString(muteText, muteX + (muteW - muteTextW) / 2, muteY + 15);
        
        // 2. SFX 音量調整項
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
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
        
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        String sfxMuteText = sfxMute ? "MUTED" : "MUTE";
        int sfxMuteTextW = g2d.getFontMetrics().stringWidth(sfxMuteText);
        g2d.drawString(sfxMuteText, sfxMuteX + (muteW - sfxMuteTextW) / 2, sfxMuteY + 15);
    }

    /**
     * 繪製主畫面設定卡片
     */
    private void drawMenuSettings(Graphics2D g2d) {
        java.awt.Point mousePos = getMousePosition();
        g2d.setFont(new Font("Impact", Font.BOLD, 46));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "SETTINGS";
        int titleX = (getWidth() - fmTitle.stringWidth(titleText)) / 2;
        int titleY = 120;

        g2d.setColor(new Color(0, 255, 255, 70));
        g2d.drawString(titleText, titleX + 2, titleY + 2);
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString(titleText, titleX, titleY);

        int cardW = 340;
        int cardH = 285;
        int cardX = (getWidth() - cardW) / 2;
        int cardY = 180;

        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        drawVolumeSettings(g2d, cardX, cardY, cardW, cardY + 30, 50);

        // THEME button
        String themeText = "THEME: " + com.tetris.util.ThemeManager.getCurrentTheme().getLabel();
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fmTheme = g2d.getFontMetrics();
        int themeBtnW = 240;
        int themeBtnH = 36;
        int themeBtnX = (getWidth() - themeBtnW) / 2;
        int themeBtnY = cardY + 145;

        menuSettingsThemeBounds = new Rectangle(themeBtnX, themeBtnY, themeBtnW, themeBtnH);
        boolean hoverTheme = (mousePos != null && menuSettingsThemeBounds.contains(mousePos));

        if (hoverTheme) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(themeBtnX, themeBtnY, themeBtnW, themeBtnH, 10, 10);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(themeBtnX, themeBtnY, themeBtnW, themeBtnH, 10, 10);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(themeBtnX, themeBtnY, themeBtnW, themeBtnH, 10, 10);
        g2d.drawString(themeText, themeBtnX + (themeBtnW - fmTheme.stringWidth(themeText)) / 2, themeBtnY + 24);

        // BACK button
        String backText = "BACK";
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fmBack = g2d.getFontMetrics();
        int btnW = 120;
        int btnH = 36;
        int btnX = (getWidth() - btnW) / 2;
        int btnY = cardY + 215;

        menuSettingsBackButtonBounds = new Rectangle(btnX, btnY, btnW, btnH);
        boolean hoverBack = (mousePos != null && menuSettingsBackButtonBounds.contains(mousePos));

        if (hoverBack) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.setColor(new Color(200, 200, 200));
        }
        g2d.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
        g2d.drawString(backText, btnX + (btnW - fmBack.stringWidth(backText)) / 2, btnY + 24);
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
     * 繪製主畫面 Difficulty 選擇卡片
     */
    private void drawDifficultySelectScreen(Graphics2D g2d) {
        g2d.setFont(new Font("Impact", Font.BOLD, 46));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String titleText = "START GAME";
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
        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        g2d.setColor(new Color(200, 200, 220));
        String subText = "SELECT DIFFICULTY";
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

            difficultyOptionBounds[i] = new Rectangle(btnX, y, btnW, btnH);

            boolean isSelected = (i == selectedDifficultyIndex);
            String label = "";
            Color baseColor;
            Color selectColor;
            Color fillCol;

            switch (i) {
                case 0:
                    label = "EASY";
                    baseColor = new Color(0, 200, 80);
                    selectColor = new Color(0, 255, 100);
                    fillCol = new Color(0, 200, 80, isSelected ? 45 : 20);
                    break;
                case 1:
                    label = "MEDIUM";
                    baseColor = new Color(220, 180, 0);
                    selectColor = new Color(255, 220, 0);
                    fillCol = new Color(220, 180, 0, isSelected ? 45 : 20);
                    break;
                case 2:
                    label = "HARD";
                    baseColor = new Color(220, 40, 40);
                    selectColor = new Color(255, 60, 60);
                    fillCol = new Color(220, 40, 40, isSelected ? 45 : 20);
                    break;
                case 3:
                default:
                    label = "BACK";
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
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
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

}