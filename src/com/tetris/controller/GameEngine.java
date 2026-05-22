package com.tetris.controller;

import com.tetris.model.Board;
import com.tetris.model.LeaderboardEntry;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import com.tetris.view.GamePanel;
import javax.swing.Timer;
import java.util.Random;
import java.util.List;
import com.tetris.util.SoundManager;
import com.tetris.util.SaveManager;
import java.awt.Color;

public class GameEngine {
    public enum GameState {
        MENU,
        PLAYING,
        LEADERBOARD
    }

    public enum Difficulty {
        EASY("EASY", 700, 500),
        NORMAL("MEDIUM", 500, 350),
        HARD("HARD", 300, 250);

        private final String label;
        private final int fallDelayMs;
        private final int lockDelayMs;

        Difficulty(String label, int fallDelayMs, int lockDelayMs) {
            this.label = label;
            this.fallDelayMs = fallDelayMs;
            this.lockDelayMs = lockDelayMs;
        }

        public String getLabel() {
            return label;
        }

        public int getFallDelayMs() {
            return fallDelayMs;
        }

        public int getLockDelayMs() {
            return lockDelayMs;
        }
    }

    private Board board;
    private GamePanel panel;
    private Piece currentPiece;
    private Piece nextPiece;
    private Piece heldPiece = null;
    private boolean canHoldThisTurn = true;
    private Timer gameLoop;
    private Timer secondTimer;
    private boolean isGameOver = false;
    private boolean isPaused = false;
    private boolean leaderboardRecorded = false;
    private int score = 0;
    private int secondsElapsed = 0;
    private int comboCount = -1; // -1 means no combo, 0+ means consecutive clears
    private boolean lastMoveWasRotation = false;
    
    // Lock Delay Mechanism
    private boolean isLocking = false;
    private long lockStartTime = 0;
    private int lockMoveResets = 0;
    private static final int MAX_LOCK_RESETS = 15;

    private Difficulty difficulty = Difficulty.NORMAL;
    private final LeaderboardManager leaderboardManager;
    private GameState gameState = GameState.MENU;

    public GameEngine(Board board, GamePanel panel) {
        this.board = board;
        this.panel = panel;
        this.leaderboardManager = new LeaderboardManager();

        // Initialize pieces
        nextPiece = generateRandomPiece();
        spawnNewPiece();

        // Set game loop (falling piece)
        gameLoop = new Timer(difficulty.getFallDelayMs(), e -> {
            if (!isPaused) {
                update();
            }
        });

        // Set second timer (game time)
        secondTimer = new Timer(1000, e -> {
            if (!isGameOver && !isPaused) {
                secondsElapsed++;
                panel.repaint(); // Refresh UI to show timer
            }
        });
    }

    // Start the game
    public void start() {
        if (gameState == GameState.PLAYING) {
            gameLoop.start();
            secondTimer.start();
        }
    }

    // Start a new game session
    public void startGame() {
        board.clear();
        SaveManager.deleteSave();
        score = 0;
        secondsElapsed = 0;
        comboCount = -1;
        isGameOver = false;
        isPaused = false;
        leaderboardRecorded = false;
        heldPiece = null;
        canHoldThisTurn = true;
        nextPiece = generateRandomPiece();
        spawnNewPiece();
        gameState = GameState.PLAYING;

        // Restart loops
        gameLoop.stop();
        gameLoop.setDelay(difficulty.getFallDelayMs());
        gameLoop.setInitialDelay(difficulty.getFallDelayMs());
        gameLoop.start();

        secondTimer.stop();
        secondTimer.start();

        // Play BGM
        SoundManager.playBGM("/resources/bgm.wav");

        panel.repaint();
    }

    // Return to main menu
    public void returnToMenu() {
        gameLoop.stop();
        secondTimer.stop();
        gameState = GameState.MENU;
        SoundManager.stopBGM();
        panel.resetUIState();
        panel.repaint();
    }

    // Go to leaderboard screen
    public void showLeaderboard() {
        gameState = GameState.LEADERBOARD;
        panel.repaint();
    }

    // Toggle pause
    public void togglePause() {
        if (gameState != GameState.PLAYING)
            return;
        if (isGameOver)
            return;
        isPaused = !isPaused;
        if (isPaused) {
            SoundManager.pauseBGM();
        } else {
            SoundManager.resumeBGM();
            panel.resetUIState();
        }
        panel.repaint();
    }

    // Lock Delay Logic
    private void startLockDelay() {
        if (!isLocking) {
            isLocking = true;
            lockStartTime = System.currentTimeMillis();
            lockMoveResets = 0;
        }
    }

    private void stopLockDelay() {
        isLocking = false;
        lockMoveResets = 0;
    }

    private void resetLockDelay() {
        if (lockMoveResets < MAX_LOCK_RESETS) {
            // Check if the piece is actually on the ground
            currentPiece.move(1, 0);
            boolean onGround = !board.isValidMove(currentPiece);
            currentPiece.move(-1, 0);

            if (onGround) {
                if (!isLocking) {
                    startLockDelay();
                } else {
                    lockStartTime = System.currentTimeMillis();
                    lockMoveResets++;
                }
            } else {
                // If it moved/rotated to a floating position, stop the lock timer
                stopLockDelay();
            }
        }
    }

    public void tickLockDelay() {
        if (gameState != GameState.PLAYING || isGameOver || isPaused)
            return;

        if (isLocking) {
            if (System.currentTimeMillis() - lockStartTime >= difficulty.getLockDelayMs()) {
                // Time's up, check if still on ground
                currentPiece.move(1, 0);
                if (!board.isValidMove(currentPiece)) {
                    currentPiece.move(-1, 0);
                    freezeAndSpawn();
                    stopLockDelay();
                    panel.repaint();
                } else {
                    currentPiece.move(-1, 0);
                    stopLockDelay();
                }
            }
        }
    }

    // Update the piece (down)
    public void update() {
        if (gameState != GameState.PLAYING)
            return;
        if (isGameOver || isPaused)
            return;

        // 1. Try to move down
        currentPiece.move(1, 0);

        // 2. Check collision
        if (!board.isValidMove(currentPiece)) { // Collision
            currentPiece.move(-1, 0); // Reposition (move back up)
            panel.spawnDropParticles(currentPiece); // Landing particles
            
            // Instead of immediate freeze, start lock delay
            if (!isLocking) {
                startLockDelay();
            }
        } else {
            // Successfully moved down
            stopLockDelay();
            lastMoveWasRotation = false; 
        }

        panel.repaint(); // Repaint
    }

    // Freeze and spawn piece
    private void freezeAndSpawn() {
        int popupCol = getCurrentPieceCenterCol();
        int popupRow = getCurrentPieceCenterRow();
        board.freezePiece(currentPiece); // Freeze

        // Check full rows before clearing to spawn block-colored particles
        java.util.List<Integer> fullRows = new java.util.ArrayList<>();
        java.awt.Color[][] grid = board.getGrid();
        for (int r = 0; r < Board.ROWS; r++) {
            boolean isFull = true;
            for (int c = 0; c < Board.COLS; c++) {
                if (grid[r][c] == null) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                fullRows.add(r);
            }
        }

        // Spawn particles for each full row!
        if (!fullRows.isEmpty()) {
            for (int r : fullRows) {
                java.awt.Color[] rowColors = new java.awt.Color[Board.COLS];
                for (int c = 0; c < Board.COLS; c++) {
                    rowColors[c] = grid[r][c];
                }
                panel.spawnRowClearParticles(r, rowColors);
            }
        }

        // Detect T-Spin before clearing lines but after piece is in final position
        boolean isTSpin = checkTSpin();

        int lines = board.clearLines(); // Clear lines
        
        // Update Combo
        if (lines > 0) {
            comboCount++;
            SoundManager.playSFX("/resources/clear.wav");
        } else {
            comboCount = -1;
        }

        if (lines > 0 || isTSpin) {
            int points = getLineClearPoints(lines, isTSpin);
            
            // Add Combo Bonus
            if (comboCount > 0) {
                points += 50 * comboCount;
            }
            
            updateScore(points);
            panel.addScorePopup(popupCol, popupRow, points, lines, isTSpin, comboCount);
        }
        spawnNewPiece(); // Spawn
        canHoldThisTurn = true; // Reset hold status for the next turn
    }

    private boolean checkTSpin() {
        if (currentPiece.getType() != Tetromino.T || !lastMoveWasRotation) {
            return false;
        }

        int r = currentPiece.getRow();
        int c = currentPiece.getCol();

        // 4 corners relative to (1,1) center: (0,0), (0,2), (2,0), (2,2)
        int[][] corners = {
            {r + 0, c + 0}, {r + 0, c + 2},
            {r + 2, c + 0}, {r + 2, c + 2}
        };

        int count = 0;
        for (int[] corner : corners) {
            if (board.isOccupied(corner[0], corner[1])) {
                count++;
            }
        }

        return count >= 3;
    }

    private int getLineClearPoints(int lines, boolean isTSpin) {
        if (isTSpin) {
            switch (lines) {
                case 0: return 400;
                case 1: return 800;
                case 2: return 1200;
                case 3: return 1600;
                default: return 0;
            }
        } else {
            switch (lines) {
                case 1: return 100;
                case 2: return 300;
                case 3: return 500;
                case 4: return 800;
                default: return 0;
            }
        }
    }

    private void updateScore(int points) {
        score += points;
        panel.setScore(score);
    }

    private int getCurrentPieceCenterCol() {
        int sum = 0;
        int count = 0;
        for (int[] coord : currentPiece.getAbsoluteCoords()) {
            sum += coord[0];
            count++;
        }
        return count == 0 ? 0 : Math.round(sum / (float) count);
    }

    private int getCurrentPieceCenterRow() {
        int sum = 0;
        int count = 0;
        for (int[] coord : currentPiece.getAbsoluteCoords()) {
            sum += coord[1];
            count++;
        }
        return count == 0 ? 0 : Math.round(sum / (float) count);
    }

    // Generate a random piece
    private Piece generateRandomPiece() {
        Tetromino[] types = Tetromino.values();
        Tetromino randomType = types[new Random().nextInt(types.length)];
        return new Piece(randomType);
    }

    // Spawn new piece
    private void spawnNewPiece() {
        // Reset lock delay state for the new piece
        stopLockDelay();

        // Use nextPiece and generate new nextPiece
        currentPiece = nextPiece;
        nextPiece = generateRandomPiece();

        // Update view
        panel.setCurrentPiece(currentPiece);

        // Restart gravity timer to give player a full tick window
        if (gameLoop != null) {
            gameLoop.restart();
        }

        // If new piece collision, game over
        if (!board.isValidMove(currentPiece)) {
            isGameOver = true;
            SaveManager.deleteSave();
            if (gameLoop != null) {
                gameLoop.stop();
            }
            if (secondTimer != null) {
                secondTimer.stop();
            }
            SoundManager.stopBGM();
            recordFinalScore();
            System.out.println("Game Over!");
        }
    }

    private void recordFinalScore() {
        if (leaderboardRecorded) {
            return;
        }

        leaderboardRecorded = true;
        leaderboardManager.recordScore(score, secondsElapsed, difficulty);
    }

    // Save current game state
    public void saveGame() {
        if (gameState != GameState.PLAYING || isGameOver) {
            return;
        }
        SaveManager.save(score, secondsElapsed, difficulty, canHoldThisTurn, currentPiece, nextPiece, heldPiece, board);
    }

    // Load game state from save file
    public void loadGame() {
        if (!SaveManager.hasSave()) {
            return;
        }
        SaveManager.SaveState state = SaveManager.load();
        if (state == null) {
            return;
        }

        // Restore fields
        this.score = state.score;
        this.secondsElapsed = state.secondsElapsed;
        this.difficulty = state.difficulty;
        this.canHoldThisTurn = state.canHoldThisTurn;
        this.currentPiece = state.currentPiece;
        this.nextPiece = state.nextPiece;
        this.heldPiece = state.heldPiece;
        this.comboCount = -1;
        this.isGameOver = false;
        this.isPaused = true; // Start paused for safety
        this.leaderboardRecorded = false;
        this.isLocking = false;
        this.lastMoveWasRotation = false;

        // Sync view with restored piece
        panel.setCurrentPiece(currentPiece);

        // Restore board grid
        Color[][] currentGrid = board.getGrid();
        for (int r = 0; r < Board.ROWS; r++) {
            System.arraycopy(state.grid[r], 0, currentGrid[r], 0, Board.COLS);
        }

        this.gameState = GameState.PLAYING;

        // Restart loops
        gameLoop.stop();
        gameLoop.setDelay(difficulty.getFallDelayMs());
        gameLoop.setInitialDelay(difficulty.getFallDelayMs());
        gameLoop.start();

        secondTimer.stop();
        secondTimer.start();

        // Play BGM but immediately pause it
        SoundManager.playBGM("/resources/bgm.wav");
        SoundManager.pauseBGM();

        panel.repaint();
    }

    // Getters for UI
    public int getScore() {
        return score;
    }

    public int getSecondsElapsed() {
        return secondsElapsed;
    }

    public Piece getNextPiece() {
        return nextPiece;
    }

    public Piece getHeldPiece() {
        return heldPiece;
    }

    public List<LeaderboardEntry> getLeaderboardEntries() {
        return leaderboardManager.getTopEntries(difficulty);
    }

    public List<LeaderboardEntry> getLeaderboardEntriesForDifficulty(Difficulty diff) {
        return leaderboardManager.getTopEntries(diff);
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty == null || this.difficulty == difficulty) {
            return;
        }
        this.difficulty = difficulty;
        gameLoop.setDelay(difficulty.getFallDelayMs());
        gameLoop.setInitialDelay(difficulty.getFallDelayMs());
        panel.repaint();
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isLocking() {
        return isLocking;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        panel.repaint();
    }

    public void navigateMenuUp() {
        if (gameState == GameState.MENU) {
            panel.navigateMenu(-1);
        }
    }

    public void navigateMenuDown() {
        if (gameState == GameState.MENU) {
            panel.navigateMenu(1);
        }
    }

    public void selectMenuItem() {
        if (gameState == GameState.MENU) {
            panel.selectCurrentOption();
        } else if (gameState == GameState.LEADERBOARD) {
            returnToMenu();
        }
    }

    public void navigatePauseMenu(int dir) {
        if (gameState == GameState.PLAYING && isPaused) {
            panel.navigatePauseMenu(dir);
        }
    }

    public void selectPauseMenuItem() {
        if (gameState == GameState.PLAYING && isPaused) {
            panel.selectPauseMenuItem();
        }
    }

    public void handleBackAction() {
        if (gameState == GameState.PLAYING && isPaused) {
            if (panel.isShowSettingsInPause()) {
                panel.setShowSettingsInPause(false);
            } else {
                togglePause();
            }
        } else if (gameState == GameState.MENU) {
            if (panel.isShowSettingsInMenu()) {
                panel.setShowSettingsInMenu(false);
            } else if (panel.isShowDifficultySelectInMenu()) {
                panel.setShowDifficultySelectInMenu(false);
            } else {
                System.exit(0);
            }
        } else if (gameState == GameState.LEADERBOARD) {
            returnToMenu();
        }
    }

    public void navigateLeaderboardTabs(int dir) {
        if (gameState == GameState.LEADERBOARD) {
            panel.navigateLeaderboardTabs(dir);
        }
    }

    // Move piece left
    public void movePieceLeft() {
        if (gameState != GameState.PLAYING || isGameOver || isPaused)
            return;
        if (handleMove(0, -1)) {
            lastMoveWasRotation = false;
            resetLockDelay();
        }
    }

    // Move piece right
    public void movePieceRight() {
        if (gameState != GameState.PLAYING || isGameOver || isPaused)
            return;
        if (handleMove(0, 1)) {
            lastMoveWasRotation = false;
            resetLockDelay();
        }
    }

    // Rotate piece
    public void rotatePiece() {
        if (gameState != GameState.PLAYING || isGameOver || isPaused)
            return;
        currentPiece.rotate();
        if (!board.isValidMove(currentPiece)) {
            // If collision, cancel rotation
            currentPiece.undoRotate();
        } else {
            lastMoveWasRotation = true;
            resetLockDelay();
        }
        panel.repaint();
    }

    // Drop piece
    public void dropPiece() {
        if (gameState != GameState.PLAYING || isGameOver || isPaused)
            return;
        int startRow = currentPiece.getRow();
        while (board.isValidMove(currentPiece)) {
            currentPiece.move(1, 0);
        }
        // Move back to last valid position
        currentPiece.move(-1, 0);

        int endRow = currentPiece.getRow();
        if (endRow > startRow) {
            panel.spawnDropParticles(currentPiece);
            lastMoveWasRotation = false;
        }

        // Instead of immediate freeze, trigger lock delay for space bar too
        startLockDelay();
        panel.repaint();
    }

    // Hold the current piece
    public void holdPiece() {
        if (gameState != GameState.PLAYING || isGameOver || isPaused)
            return;
        if (!canHoldThisTurn)
            return;

        stopLockDelay(); // Stop lock delay when holding

        if (heldPiece == null) {
            // Store current piece type as held
            heldPiece = new Piece(currentPiece.getType());
            // Spawn next piece as current piece
            spawnNewPiece();
        } else {
            // Swap current piece and held piece
            Piece temp = heldPiece;
            heldPiece = new Piece(currentPiece.getType());
            
            // Reset current piece properties to spawn at the top
            currentPiece = temp;
            panel.setCurrentPiece(currentPiece);
            
            // Restart the gravity timer
            if (gameLoop != null) {
                gameLoop.restart();
            }
        }
        
        canHoldThisTurn = false;
        panel.repaint();
    }

    // Handle move
    private boolean handleMove(int dx, int dy) {
        currentPiece.move(dx, dy);
        if (!board.isValidMove(currentPiece)) {
            currentPiece.move(-dx, -dy); // Collision
            return false;
        }
        panel.repaint();
        return true;
    }

}