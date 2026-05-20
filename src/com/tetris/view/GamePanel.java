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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
    private static final int SIDEBAR_WIDTH = 170;

    private Board board;
    private Piece currentPiece;
    private com.tetris.controller.GameEngine gameEngine;
    
    // Active score popups
    private final List<ScorePopup> scorePopups = new ArrayList<>();
    private final Timer scorePopupTimer;

    // Menu properties
    private int selectedMenuIndex = 0;
    private final Rectangle[] menuOptionBounds = new Rectangle[4];
    private Rectangle backButtonBounds = new Rectangle();
    private final List<FloatingPiece> floatingPieces = new ArrayList<>();
    private Timer menuAnimationTimer;

    // Pause menu properties
    private int selectedPauseIndex = 0;
    private final Rectangle[] pauseOptionBounds = new Rectangle[3];
    private boolean showSettingsInPause = false;
    private boolean showGhostPiece = true;
    private boolean showGridLines = true;

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
    public void addScorePopup(int gridCol, int gridRow, int score) {
        String text = (score >= 0 ? "+" + score : String.valueOf(score));
        int px = gridCol * TILE_SIZE + TILE_SIZE / 2;
        int py = gridRow * TILE_SIZE + TILE_SIZE / 2;
        synchronized (scorePopups) {
            scorePopups.add(new ScorePopup(text, px, py));
        }
        if (!scorePopupTimer.isRunning()) {
            scorePopupTimer.start();
        }
        // Request a repaint so animation starts immediately
        repaint();
    }

    // Draw and update active score popups
    private void drawScorePopups(Graphics g) {
        if (scorePopups.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        long now = System.currentTimeMillis();

        synchronized (scorePopups) {
            Iterator<ScorePopup> it = scorePopups.iterator();
            while (it.hasNext()) {
                ScorePopup sp = it.next();
                float elapsed = now - sp.startTime;
                float progress = Math.min(1.0f, elapsed / (float) sp.duration);

                int y = (int) (sp.startY - sp.rise * progress);
                float alpha = 1.0f - progress;

                g2.setFont(sp.font);
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(sp.text);
                int textHeight = fm.getAscent();

                // Shadow
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(0, 0, 0, (int) (160 * alpha)));
                g2.drawString(sp.text, sp.startX - textWidth / 2 + 1, y + textHeight / 2 + 1);

                // Main text
                g2.setColor(sp.color);
                g2.drawString(sp.text, sp.startX - textWidth / 2, y + textHeight / 2);

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

        scorePopupTimer = new Timer(16, e -> {
            synchronized (scorePopups) {
                if (scorePopups.isEmpty()) {
                    ((Timer) e.getSource()).stop();
                    return;
                }
            }
            repaint();
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

        // Mouse inputs
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameEngine == null) return;
                com.tetris.controller.GameEngine.GameState state = gameEngine.getGameState();
                if (state == com.tetris.controller.GameEngine.GameState.MENU) {
                    for (int i = 0; i < 4; i++) {
                        if (menuOptionBounds[i] != null && menuOptionBounds[i].contains(e.getPoint())) {
                            selectedMenuIndex = i;
                            selectCurrentOption();
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.LEADERBOARD) {
                    if (backButtonBounds != null && backButtonBounds.contains(e.getPoint())) {
                        gameEngine.returnToMenu();
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.PLAYING) {
                    if (gameEngine.isGameOver()) {
                        gameEngine.returnToMenu();
                    } else if (gameEngine.isPaused()) {
                        for (int i = 0; i < 3; i++) {
                            if (pauseOptionBounds[i] != null && pauseOptionBounds[i].contains(e.getPoint())) {
                                selectedPauseIndex = i;
                                selectPauseMenuItem();
                                break;
                            }
                        }
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameEngine == null) return;
                com.tetris.controller.GameEngine.GameState state = gameEngine.getGameState();
                if (state == com.tetris.controller.GameEngine.GameState.MENU) {
                    for (int i = 0; i < 4; i++) {
                        if (menuOptionBounds[i] != null && menuOptionBounds[i].contains(e.getPoint())) {
                            if (selectedMenuIndex != i) {
                                selectedMenuIndex = i;
                                repaint();
                            }
                            break;
                        }
                    }
                } else if (state == com.tetris.controller.GameEngine.GameState.PLAYING && gameEngine.isPaused()) {
                    for (int i = 0; i < 3; i++) {
                        if (pauseOptionBounds[i] != null && pauseOptionBounds[i].contains(e.getPoint())) {
                            if (selectedPauseIndex != i) {
                                selectedPauseIndex = i;
                                repaint();
                            }
                            break;
                        }
                    }
                }
            }
        });

        // Set the size of the game panel (grid + sidebar)
        setPreferredSize(new Dimension(COLS * TILE_SIZE + SIDEBAR_WIDTH, ROWS * TILE_SIZE));
        setBackground(Color.BLACK);
    }

    // Set the game engine reference
    public void setGameEngine(com.tetris.controller.GameEngine gameEngine) {
        this.gameEngine = gameEngine;
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
                // Draw Game Area (Left)
                drawGrid(g);
                drawFixedBlocks(g);
                drawCurrentPiece(g);

                // Draw active score popups on top of the grid
                drawScorePopups(g);

                // Draw Sidebar (Right)
                drawSidebar(g);

                // Draw Pause Overlay
                if (gameEngine.isPaused()) {
                    drawPauseOverlay(g);
                }

                // Draw Game Over Overlay
                if (gameEngine.isGameOver()) {
                    drawGameOverOverlay(g);
                }
                break;
        }
    }

    // Menu navigation helper
    public void navigateMenu(int dir) {
        selectedMenuIndex = (selectedMenuIndex + dir + 4) % 4;
        repaint();
    }

    // Menu select action helper
    public void selectCurrentOption() {
        switch (selectedMenuIndex) {
            case 0:
                gameEngine.startGame();
                break;
            case 1:
                com.tetris.controller.GameEngine.Difficulty next = getNextDifficulty(gameEngine.getDifficulty());
                gameEngine.setDifficulty(next);
                break;
            case 2:
                gameEngine.showLeaderboard();
                break;
            case 3:
                System.exit(0);
                break;
        }
        repaint();
    }

    private com.tetris.controller.GameEngine.Difficulty getNextDifficulty(com.tetris.controller.GameEngine.Difficulty current) {
        switch (current) {
            case EASY:
                return com.tetris.controller.GameEngine.Difficulty.NORMAL;
            case NORMAL:
                return com.tetris.controller.GameEngine.Difficulty.HARD;
            case HARD:
                return com.tetris.controller.GameEngine.Difficulty.EASY;
            default:
                return com.tetris.controller.GameEngine.Difficulty.NORMAL;
        }
    }

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
        Color blockColor = new Color(fp.color.getRed(), fp.color.getGreen(), fp.color.getBlue(), 30);
        Color borderColor = new Color(fp.color.getRed(), fp.color.getGreen(), fp.color.getBlue(), 70);

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
        int startY = 280;
        int gap = 55;
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fmOption = g2d.getFontMetrics();

        for (int i = 0; i < 4; i++) {
            String label = "";
            switch (i) {
                case 0: label = "PLAY GAME"; break;
                case 1: label = "DIFFICULTY: " + gameEngine.getDifficulty().getLabel(); break;
                case 2: label = "LEADERBOARD"; break;
                case 3: label = "EXIT"; break;
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

        // Columns headers
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString("RANK", 50, 140);
        g2d.drawString("SCORE", 110, 140);
        g2d.drawString("DIFF", 210, 140);
        g2d.drawString("DATE", 310, 140);

        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.drawLine(40, 150, getWidth() - 40, 150);

        // List
        List<LeaderboardEntry> entries = gameEngine.getLeaderboardEntries();
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        int y = 185;
        int rowGap = 35;

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
                g2d.drawString(entry.getDifficulty(), 210, y);
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
        int btnY = 500;

        backButtonBounds = new Rectangle(btnX, btnY - 30, btnW, btnH);

        java.awt.Point mousePos = getMousePosition();
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
    }

    // Draw game over screen overlay on grid area
    private void drawGameOverOverlay(Graphics g) {
        int width = COLS * TILE_SIZE;
        int height = ROWS * TILE_SIZE;

        g.setColor(new Color(50, 0, 15, 185)); // Deep red-purple translucent overlay
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        String text = "GAME OVER";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = height / 2 - 40;
        
        // Red glowing drop-shadow
        g.setColor(new Color(255, 0, 50, 160));
        g.drawString(text, x + 2, y + 2);
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);

        // Score
        g.setFont(new Font("Arial", Font.BOLD, 20));
        fm = g.getFontMetrics();
        String scoreText = "Score: " + gameEngine.getScore();
        x = (width - fm.stringWidth(scoreText)) / 2;
        y += 45;
        g.setColor(new Color(255, 215, 0));
        g.drawString(scoreText, x, y);

        // Instruction to return
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        fm = g.getFontMetrics();
        String subText = "Press ENTER / Click to Menu";
        x = (width - fm.stringWidth(subText)) / 2;
        y += 45;
        g.setColor(new Color(200, 200, 220));
        g.drawString(subText, x, y);
    }

    // Pause menu navigation helper
    public void navigatePauseMenu(int dir) {
        selectedPauseIndex = (selectedPauseIndex + dir + 3) % 3;
        repaint();
    }

    // Pause menu select action helper
    public void selectPauseMenuItem() {
        if (!showSettingsInPause) {
            switch (selectedPauseIndex) {
                case 0: // RESUME
                    gameEngine.togglePause();
                    break;
                case 1: // SETTINGS
                    showSettingsInPause = true;
                    selectedPauseIndex = 0;
                    break;
                case 2: // BACK TO MAIN
                    showSettingsInPause = false;
                    gameEngine.togglePause(); // Unpause first!
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
                case 2: // BACK
                    showSettingsInPause = false;
                    selectedPauseIndex = 1; // return cursor to settings option
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
        int cardW = 310;
        int cardH = 250;
        int cardX = (width - cardW) / 2;
        int cardY = 180;

        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

        // Draw Options
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fmOption = g2d.getFontMetrics();
        int startY = cardY + 60;
        int gap = 65;

        for (int i = 0; i < 3; i++) {
            String label = "";
            if (!showSettingsInPause) {
                switch (i) {
                    case 0: label = "RESUME GAME"; break;
                    case 1: label = "SETTINGS"; break;
                    case 2: label = "BACK TO MAIN"; break;
                }
            } else {
                switch (i) {
                    case 0: label = "GHOST PIECE: " + (showGhostPiece ? "ON" : "OFF"); break;
                    case 1: label = "GRID LINES: " + (showGridLines ? "ON" : "OFF"); break;
                    case 2: label = "BACK"; break;
                }
            }

            int textWidth = fmOption.stringWidth(label);
            int textHeight = fmOption.getHeight();
            int x = cardX + (cardW - textWidth) / 2;
            int y = startY + i * gap;

            pauseOptionBounds[i] = new Rectangle(cardX + 15, y - textHeight + 5, cardW - 30, textHeight + 15);

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

        // Control Hints
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(120, 120, 150));
        String hint = "Use \u2191 / \u2193 Arrows or Mouse | Enter to Select";
        g2d.drawString(hint, (width - g2d.getFontMetrics().stringWidth(hint)) / 2, 500);
    }

    // Draw the grid
    private void drawGrid(Graphics g) {
        if (!showGridLines) return;
        g.setColor(new Color(40, 40, 40));
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
        Color ghostColor = new Color(255, 255, 255, 80);    // Translucent white
        for (int[] coord : ghost.getAbsoluteCoords()) {
            drawSquare(g, coord[0], coord[1], ghostColor);
        }
    }

    // Draw Sidebar
    private void drawSidebar(Graphics g) {
        int startX = COLS * TILE_SIZE;

        // Draw Sidebar Background
        g.setColor(new Color(30, 30, 30));
        g.fillRect(startX, 0, SIDEBAR_WIDTH, getHeight());

        // Draw Sidebar Border
        g.setColor(Color.GRAY);
        g.drawLine(startX, 0, startX, getHeight());

        if (gameEngine == null)
            return;

        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));

        // 1. Draw Score
        g.drawString("SCORE", startX + 20, 50);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 24));
        g.drawString(String.valueOf(gameEngine.getScore()), startX + 20, 80);

        // 2. Draw Timer
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        g.drawString("TIME", startX + 20, 140);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 24));
        int seconds = gameEngine.getSecondsElapsed();
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
        g.drawString(timeStr, startX + 20, 170);

        // 3. Draw Next Piece Preview
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        g.drawString("LEVEL", startX + 20, 220);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 20));
        g.drawString(gameEngine.getDifficulty().getLabel(), startX + 20, 245);

        // 4. Draw Next Piece Preview
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        g.drawString("NEXT", startX + 20, 300);
        drawNextPiecePreview(g, startX + 20, 320);

        // 5. Draw Controls Hints
        drawControlHints(g, startX + 10, 430);

        // 6. Draw Leaderboard
        drawLeaderboard(g, startX + 10, 480);

        // 7. Game Over Message
        if (gameEngine.isGameOver()) {
            g.setColor(Color.RED);
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22));
            g.drawString("GAME OVER", startX + 10, ROWS * TILE_SIZE - 170);
        }
    }

    // Draw Controls Hints
    private void drawControlHints(Graphics g, int x, int startY) {
        g.setColor(new Color(210, 210, 210));
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        g.drawString("CONTROLS", x, startY);

        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        g.drawString("Arrows Move | Up Rotate | Space Drop", x, startY + 18);
        g.drawString("P / Esc Pause", x, startY + 34);
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
        int lineY = startY + 18;

        if (entries.isEmpty()) {
            g.setColor(new Color(210, 210, 210));
            g.drawString("No scores yet", x, lineY);
            return;
        }

        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (rank > 5) {
                break;
            }

            g.setColor(new Color(240, 240, 240));
            String text = String.format("%d. %d pts | %s | %s", rank, entry.getScore(), entry.getDifficulty(),
                    entry.getPlayedAtDisplay());
            g.drawString(text, x, lineY);
            lineY += 15;
            rank++;
        }
    }

    // Draw Next Piece Preview
    private void drawNextPiecePreview(Graphics g, int x, int y) {
        Piece nextPiece = gameEngine.getNextPiece();
        if (nextPiece != null) {
            Color color = nextPiece.getType().getColor();
            int[][] coords = nextPiece.getType().getCoords();

            // Adjust scale for preview
            int previewTileSize = 25;
            for (int[] coord : coords) {
                g.setColor(color);
                g.fillRect(x + coord[0] * previewTileSize, y + coord[1] * previewTileSize, previewTileSize,
                        previewTileSize);
                g.setColor(color.darker());
                g.drawRect(x + coord[0] * previewTileSize, y + coord[1] * previewTileSize, previewTileSize,
                        previewTileSize);
            }
        }
    }

    // Helper method: To draw a square with a border
    private void drawSquare(Graphics g, int x, int y, Color color) {
        if (y < 0)
            return;

        g.setColor(color);
        g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE); // Draw square

        // To make the blocks easier to distinguish,
        // draw a dark border around each block.
        g.setColor(color.darker());
        g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE); // Draw border
    }

    // Simple score popup structure
    private static class ScorePopup {
        final String text;
        final int startX;
        final int startY;
        final long startTime;
        final int duration = 900; // ms
        final int rise = 36; // pixels to move up
        final Color color = new Color(255, 235, 120);
        final Font font = new Font("Arial", Font.BOLD, 18);

        ScorePopup(String text, int startX, int startY) {
            this.text = text;
            this.startX = startX;
            this.startY = startY;
            this.startTime = System.currentTimeMillis();
        }
    }

}