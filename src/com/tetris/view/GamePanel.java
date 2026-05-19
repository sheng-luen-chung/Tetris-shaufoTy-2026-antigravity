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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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

        // Draw Game Area (Left)
        drawGrid(g);
        drawFixedBlocks(g);
        drawCurrentPiece(g);

        // Draw active score popups on top of the grid
        drawScorePopups(g);

        // Draw Sidebar (Right)
        drawSidebar(g);

        // Draw Pause Overlay
        if (gameEngine != null && gameEngine.isPaused()) {
            drawPauseOverlay(g);
        }
    }

    // Draw Pause Overlay
    private void drawPauseOverlay(Graphics g) {
        int width = COLS * TILE_SIZE;
        int height = ROWS * TILE_SIZE;

        // Semi-transparent background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);

        // "PAUSED" Text
        g.setColor(Color.YELLOW);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 36));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "PAUSED";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);

        // Instructions
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
        fm = g.getFontMetrics();
        String subText = "Press 'P' to Resume";
        x = (width - fm.stringWidth(subText)) / 2;
        y += 40;
        g.drawString(subText, x, y);
    }


    // Draw the grid
    private void drawGrid(Graphics g) {
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
            drawGhostPiece(g);

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
        g.drawString("P / Esc Pause | 1 / 2 / 3 Level", x, startY + 34);
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