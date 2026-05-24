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
import com.tetris.model.GameMode;
import java.awt.Color;

public class GameEngine {
    public enum GameState {
        MENU,
        PLAYING,
        LEADERBOARD,
        TUTORIAL
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

    public enum TSpinType {
        NONE,
        MINI,
        REGULAR
    }

    private Board board;
    private GamePanel panel;
    private Piece currentPiece;
    private Piece nextPiece;
    private final java.util.List<Piece> nextPieces = new java.util.ArrayList<>();
    private Piece heldPiece = null;
    private boolean canHoldThisTurn = true;
    
    // PVP Mode fields
    private GameEngine opponent = null;
    private int playerNum = 1;
    private int pvpWinner = 0; // 0: none, 1: P1, 2: P2
    private InputHandler inputHandler = null;
    private Timer gameLoop;
    private Timer secondTimer;
    private boolean isGameOver = false;
    private boolean isPaused = false;
    private boolean leaderboardRecorded = false;
    private int score = 0;
    private int secondsElapsed = 0;
    private int comboCount = -1; // -1 means no combo, 0+ means consecutive clears
    private boolean lastMoveWasRotation = false;
    private int[] lastRotationKickOffset = {0, 0};

    // Tutorial Mode fields
    private int tutorialLevel = 1;
    private boolean isTransitioning = false;
    private boolean lastTutorialSuccess = false;

    // Game Mode fields
    private GameMode gameMode = GameMode.ENDLESS;
    private boolean isVictory = false;

    // Stats tracking
    private int piecesSpawned = 0;
    private int totalActions = 0;
    private int totalLinesCleared = 0;
    private int tetrisClears = 0;
    private int tSpins = 0;
    private int maxCombo = 0;
    
    // Lock Delay Mechanism
    private boolean isLocking = false;
    private long lockStartTime = 0;      // resets each time the player moves
    private long lockTotalStartTime = 0; // set once when piece FIRST touches ground; never reset
    private int lockMoveResets = 0;
    private static final int MAX_LOCK_RESETS = 15;
    // Hard cap: piece locks no later than lockDelayMs * LOCK_HARD_CAP_MULTIPLIER
    // after it first touched the ground, regardless of move resets.
    private static final int LOCK_HARD_CAP_MULTIPLIER = 3;

    // AI Autoplay Mechanism
    private boolean aiPlay = false;
    private boolean usedAiThisSession = false;
    private Timer aiTimer;
    private int targetRotation = 0;
    private int targetCol = 0;
    private boolean needsAiCalculation = true;

    private Difficulty difficulty = Difficulty.NORMAL;
    private final LeaderboardManager leaderboardManager;
    private GameState gameState = GameState.MENU;
    private boolean isEnteringName = false;
    private final StringBuilder nameInputBuffer = new StringBuilder();

    public GameEngine(Board board, GamePanel panel) {
        this.board = board;
        this.panel = panel;
        this.leaderboardManager = new LeaderboardManager();

        // Initialize pieces
        nextPieces.clear();
        for (int i = 0; i < 5; i++) {
            nextPieces.add(generateRandomPiece());
        }
        nextPiece = nextPieces.get(0);
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
                if (gameMode == GameMode.ULTRA && secondsElapsed >= 120) {
                    isVictory = true;
                    isGameOver = true;
                    setAiPlay(false);
                    gameLoop.stop();
                    secondTimer.stop();
                    SoundManager.stopBGM();
                    SoundManager.playSFX("/resources/clear.wav");
                    recordFinalScore();
                } else if (gameMode == GameMode.SURVIVAL) {
                    int interval = 15;
                    if (difficulty == Difficulty.EASY) {
                        interval = 20;
                    } else if (difficulty == Difficulty.HARD) {
                        interval = 10;
                    }
                    if (secondsElapsed > 0 && secondsElapsed % interval == 0) {
                        int holeCol = new Random().nextInt(com.tetris.model.Board.COLS);
                        board.addGarbageLine(holeCol);
                        // Check if current piece collides and push it up if possible
                        if (currentPiece != null && !board.isValidMove(currentPiece)) {
                            currentPiece.move(-1, 0); // push up
                            if (!board.isValidMove(currentPiece)) {
                                currentPiece.move(1, 0); // push back if failed
                            }
                        }
                        SoundManager.playSFX("/resources/clear.wav");
                    }
                }
                panel.repaint(); // Refresh UI to show timer
            }
        });

        // Set AI autoplay timer (ticks every 80ms)
        aiTimer = new Timer(80, e -> {
            if (gameState == GameState.PLAYING && !isPaused && !isGameOver && aiPlay) {
                runAIStep();
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
        isVictory = false;
        isPaused = false;
        leaderboardRecorded = false;
        heldPiece = null;
        canHoldThisTurn = true;
        piecesSpawned = 0;
        totalActions = 0;
        totalLinesCleared = 0;
        tetrisClears = 0;
        tSpins = 0;
        maxCombo = 0;
        pvpWinner = 0;
        nextPieces.clear();
        for (int i = 0; i < 5; i++) {
            nextPieces.add(generateRandomPiece());
        }
        nextPiece = nextPieces.get(0);
        spawnNewPiece();
        usedAiThisSession = false;
        gameState = GameState.PLAYING;

        // Restart loops
        gameLoop.stop();
        gameLoop.setDelay(difficulty.getFallDelayMs());
        gameLoop.setInitialDelay(difficulty.getFallDelayMs());
        gameLoop.start();

        secondTimer.stop();
        secondTimer.start();

        if (gameMode == GameMode.PVP) {
            playerNum = 1;
            if (opponent == null) {
                Board board2 = new Board();
                opponent = new GameEngine(board2, panel);
                opponent.setOpponent(this);
                opponent.setPlayerNum(2);
            }
            panel.setGameEngine2(opponent);
            if (inputHandler != null) {
                inputHandler.setEngine2(opponent);
            }

            // Sync state and mode for opponent
            opponent.setGameMode(GameMode.PVP);
            opponent.gameState = GameState.PLAYING;
            opponent.pvpWinner = 0;
            opponent.board.clear();
            opponent.score = 0;
            opponent.secondsElapsed = 0;
            opponent.comboCount = -1;
            opponent.isGameOver = false;
            opponent.isVictory = false;
            opponent.isPaused = false;
            opponent.heldPiece = null;
            opponent.canHoldThisTurn = true;
            opponent.piecesSpawned = 0;
            opponent.totalActions = 0;
            opponent.totalLinesCleared = 0;
            opponent.tetrisClears = 0;
            opponent.tSpins = 0;
            opponent.maxCombo = 0;
            opponent.nextPieces.clear();
            for (int i = 0; i < 5; i++) {
                opponent.nextPieces.add(opponent.generateRandomPiece());
            }
            opponent.nextPiece = opponent.nextPieces.get(0);
            opponent.spawnNewPiece();

            opponent.gameLoop.stop();
            opponent.gameLoop.setDelay(opponent.difficulty.getFallDelayMs());
            opponent.gameLoop.setInitialDelay(opponent.difficulty.getFallDelayMs());
            opponent.gameLoop.start();

            opponent.secondTimer.stop();
            opponent.secondTimer.start();
        }

        panel.updateWindowSize();

        // Play BGM
        SoundManager.playBGM("/resources/bgm.wav");

        panel.repaint();
    }

    // Return to main menu
    public void returnToMenu() {
        gameLoop.stop();
        secondTimer.stop();
        setAiPlay(false); // Stop AI Autoplay
        gameState = GameState.MENU;
        SoundManager.stopBGM();

        if (gameMode == GameMode.PVP && opponent != null && opponent.getGameState() != GameState.MENU) {
            opponent.returnToMenu();
        }

        panel.resetUIState();
        panel.setGameEngine2(null);
        panel.updateWindowSize();
        panel.repaint();
    }

    // Go to leaderboard screen
    public void showLeaderboard() {
        gameState = GameState.LEADERBOARD;
        panel.repaint();
    }

    // Toggle pause
    public void togglePause() {
        if (gameState != GameState.PLAYING && gameState != GameState.TUTORIAL)
            return;
        if (isGameOver)
            return;
        isPaused = !isPaused;
        if (isPaused) {
            SoundManager.pauseBGM();
            if (aiTimer != null) aiTimer.stop();
        } else {
            SoundManager.resumeBGM();
            panel.resetUIState();
            if (aiPlay && aiTimer != null && !aiTimer.isRunning()) {
                aiTimer.start();
            }
        }
        if (gameMode == GameMode.PVP && opponent != null && opponent.isPaused() != isPaused) {
            opponent.togglePause();
        }
        panel.repaint();
    }

    // Lock Delay Logic
    private void startLockDelay() {
        if (!isLocking) {
            isLocking = true;
            long now = System.currentTimeMillis();

            // Record the very first moment this piece touched the ground.
            // This timestamp is NEVER reset — it is the hard-cap anchor.
            if (lockTotalStartTime == 0) {
                lockTotalStartTime = now;
            }

            // Only give a fresh per-move window when within the reset budget.
            // Once the budget is exhausted we keep the stale lockStartTime so
            // the remaining portion of the last window ticks down naturally.
            if (lockMoveResets < MAX_LOCK_RESETS) {
                lockStartTime = now;
            }
        }
    }

    private void stopLockDelay() {
        isLocking = false;
        // NOTE: do NOT reset lockMoveResets here.
        // The counter must persist across mid-air intervals so players cannot
        // bypass MAX_LOCK_RESETS by briefly lifting the piece and re-landing it.
    }

    private void resetLockDelay() {
        // Check if the piece is actually on the ground
        currentPiece.move(1, 0);
        boolean onGround = !board.isValidMove(currentPiece);
        currentPiece.move(-1, 0);

        if (onGround) {
            if (!isLocking) {
                // Piece just landed – start the timer (respects the cap inside startLockDelay)
                startLockDelay();
            } else if (lockMoveResets < MAX_LOCK_RESETS) {
                // Still within the allowed reset budget: refresh the timer
                lockStartTime = System.currentTimeMillis();
                lockMoveResets++;
            }
            // If the cap is already reached we simply do nothing, letting the
            // existing timer tick down to zero and lock the piece naturally.
        } else {
            // Piece moved/rotated to a floating position – pause the timer
            stopLockDelay();
        }
    }

    public void tickLockDelay() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused)
            return;

        if (isLocking) {
            long now = System.currentTimeMillis();
            long hardCap = difficulty.getLockDelayMs() * LOCK_HARD_CAP_MULTIPLIER;

            // Two independent expiry conditions:
            // 1. Per-move window: time since last reset exceeded lockDelayMs
            // 2. Hard cap: total time since first ground contact exceeded hardCap
            boolean perMoveExpired = (now - lockStartTime >= difficulty.getLockDelayMs());
            boolean hardCapExpired = (lockTotalStartTime > 0 && now - lockTotalStartTime >= hardCap);

            if (perMoveExpired || hardCapExpired) {
                // Verify the piece is still on the ground before locking
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

    // Soft drop piece (down)
    public void softDrop() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        update();
        // Restart gravity timer to prevent "gravity stutter" during soft drop
        if (gameLoop != null) {
            gameLoop.restart();
        }
    }

    // Update the piece (down)
    public void update() {
        if (gameState != GameState.PLAYING && gameState != GameState.TUTORIAL)
            return;
        if (isGameOver || isPaused || isTransitioning)
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

        if (gameState == GameState.TUTORIAL) {
            TSpinType tSpinType = checkTSpinType();
            // A regular T-spin is required to pass
            if (tSpinType == TSpinType.REGULAR) {
                // Clear the lines, which clears the row blocks
                int lines = board.clearLines();
                tSpins++;
                SoundManager.playSFX("/resources/clear.wav");
                if (lines > 0) {
                    java.awt.Color[] rowColors = new java.awt.Color[Board.COLS];
                    java.util.Arrays.fill(rowColors, Tetromino.T.getColor());
                    panel.spawnRowClearParticles(popupRow, rowColors, playerNum);
                    panel.triggerScreenshake(lines * 3, 200, playerNum);
                }
                tutorialSuccess();
            } else {
                tutorialFail();
            }
            return;
        }

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
                panel.spawnRowClearParticles(r, rowColors, playerNum);
            }
        }

        // Detect T-Spin before clearing lines but after piece is in final position
        TSpinType tSpinType = checkTSpinType();
        boolean isTSpin = (tSpinType != TSpinType.NONE);

        int lines = board.clearLines(); // Clear lines
        
        // Update Combo
        if (lines > 0) {
            comboCount++;
            totalLinesCleared += lines;
            if (lines == 4) {
                tetrisClears++;
            }
            maxCombo = Math.max(maxCombo, comboCount);
            SoundManager.playSFX("/resources/clear.wav");
        } else {
            comboCount = -1;
        }

        // PVP Garbage Generation
        if (gameMode == GameMode.PVP && opponent != null && (lines > 0 || isTSpin)) {
            int garbageToSend = 0;
            if (isTSpin) {
                if (tSpinType == TSpinType.REGULAR) {
                    switch (lines) {
                        case 0: garbageToSend = 1; break;
                        case 1: garbageToSend = 2; break;
                        case 2: garbageToSend = 4; break;
                        case 3: garbageToSend = 6; break;
                    }
                } else if (tSpinType == TSpinType.MINI) {
                    garbageToSend = (lines >= 1) ? lines : 1;
                }
            } else {
                switch (lines) {
                    case 2: garbageToSend = 1; break;
                    case 3: garbageToSend = 2; break;
                    case 4: garbageToSend = 4; break;
                }
            }
            if (comboCount >= 1) {
                garbageToSend += comboCount;
            }
            if (garbageToSend > 0) {
                opponent.receiveGarbage(garbageToSend);
            }
        }

        // Check for Perfect Clear
        boolean isPerfectClear = false;
        if (lines > 0) {
            isPerfectClear = true;
            Color[][] boardGrid = board.getGrid();
            for (int r = 0; r < Board.ROWS; r++) {
                for (int c = 0; c < Board.COLS; c++) {
                    if (boardGrid[r][c] != null) {
                        isPerfectClear = false;
                        break;
                    }
                }
                if (!isPerfectClear) break;
            }
        }

        // Trigger screenshake based on clears, T-spins, or high combos
        if (lines > 0 || isTSpin) {
            int intensity = 0;
            int duration = 0;
            if (lines > 0) {
                switch (lines) {
                    case 1: intensity = 2; duration = 100; break;
                    case 2: intensity = 4; duration = 150; break;
                    case 3: intensity = 6; duration = 200; break;
                    case 4: intensity = 10; duration = 300; break;
                }
            }
            if (isTSpin) {
                intensity += 4;
                duration = Math.max(duration, 180);
            }
            if (comboCount >= 2) {
                intensity += 2;
                duration += 50;
            }
            if (intensity > 0) {
                panel.triggerScreenshake(intensity, duration, playerNum);
            }
        }

        if (isTSpin) {
            tSpins++;
        }

        if (lines > 0 || isTSpin) {
            int points = getLineClearPoints(lines, tSpinType);
            
            // Add Combo Bonus
            if (comboCount > 0) {
                points += 50 * comboCount;
            }
            
            // Add Perfect Clear Bonus
            if (isPerfectClear) {
                points += 2000;
            }
            
            updateScore(points);
            panel.addScorePopup(popupCol, popupRow, points, lines, tSpinType, comboCount, playerNum);

            if (isPerfectClear) {
                panel.triggerPerfectClear(playerNum);
            }
        }

        // Check for 40-line Sprint victory
        if (gameMode == GameMode.SPRINT && totalLinesCleared >= 40) {
            isVictory = true;
            isGameOver = true;
            setAiPlay(false);
            if (gameLoop != null) {
                gameLoop.stop();
            }
            if (secondTimer != null) {
                secondTimer.stop();
            }
            SoundManager.stopBGM();
            SoundManager.playSFX("/resources/clear.wav");
            recordFinalScore();
            panel.repaint();
            return;
        }

        spawnNewPiece(); // Spawn
        canHoldThisTurn = true; // Reset hold status for the next turn
    }

    public void receiveGarbage(int garbageLinesCount) {
        if (isGameOver || isPaused) return;

        // Generate garbage lines on this board
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < garbageLinesCount; i++) {
            int holeCol = rand.nextInt(Board.COLS);
            board.addGarbageLine(holeCol);
        }

        // If current piece collides and push it up if possible
        if (currentPiece != null && !board.isValidMove(currentPiece)) {
            currentPiece.move(-1, 0); // push up
            if (!board.isValidMove(currentPiece)) {
                currentPiece.move(1, 0); // push back if failed
            }
        }

        // Trigger screenshake on this player's screen!
        if (panel != null) {
            panel.triggerScreenshake(garbageLinesCount * 2, 150, playerNum);
            panel.repaint();
        }
    }

    private TSpinType checkTSpinType() {
        if (currentPiece.getType() != Tetromino.T || !lastMoveWasRotation) {
            return TSpinType.NONE;
        }

        int r = currentPiece.getRow();
        int c = currentPiece.getCol();
        int rotation = currentPiece.getRotationIndex();

        // corners relative to center (r+1, c+1)
        int[][] frontCorners;
        int[][] backCorners;

        if (rotation == 0) { // Pointing UP
            frontCorners = new int[][] { {r, c}, {r, c + 2} }; // TL, TR
            backCorners = new int[][] { {r + 2, c}, {r + 2, c + 2} }; // BL, BR
        } else if (rotation == 1) { // Pointing RIGHT
            frontCorners = new int[][] { {r, c + 2}, {r + 2, c + 2} }; // TR, BR
            backCorners = new int[][] { {r, c}, {r + 2, c} }; // TL, BL
        } else if (rotation == 2) { // Pointing DOWN
            frontCorners = new int[][] { {r + 2, c}, {r + 2, c + 2} }; // BL, BR
            backCorners = new int[][] { {r, c}, {r, c + 2} }; // TL, TR
        } else { // Pointing LEFT (rotation == 3)
            frontCorners = new int[][] { {r, c}, {r + 2, c} }; // TL, BL
            backCorners = new int[][] { {r, c + 2}, {r + 2, c + 2} }; // TR, BR
        }

        int occupiedFront = 0;
        for (int[] corner : frontCorners) {
            if (board.isOccupied(corner[0], corner[1])) {
                occupiedFront++;
            }
        }

        int occupiedBack = 0;
        for (int[] corner : backCorners) {
            if (board.isOccupied(corner[0], corner[1])) {
                occupiedBack++;
            }
        }

        int totalOccupied = occupiedFront + occupiedBack;

        if (totalOccupied < 3) {
            return TSpinType.NONE;
        }

        // 3-Corner Rule satisfied. Classify Regular vs Mini:
        if (occupiedFront == 2) {
            return TSpinType.REGULAR;
        }

        // If only 1 front corner is occupied, check if upgraded by a complex wall kick
        if (lastRotationKickOffset != null) {
            int dr = lastRotationKickOffset[0];
            int dc = lastRotationKickOffset[1];
            // Simple kicks are: (0,0), (0, -1), (0, 1), (-1, 0)
            boolean isSimpleKick = (dr == 0 && Math.abs(dc) <= 1) || (dr == -1 && dc == 0);
            if (!isSimpleKick) {
                return TSpinType.REGULAR;
            }
        }

        return TSpinType.MINI;
    }

    private int getLineClearPoints(int lines, TSpinType tSpinType) {
        if (tSpinType == TSpinType.REGULAR) {
            switch (lines) {
                case 0: return 400;
                case 1: return 800;
                case 2: return 1200;
                case 3: return 1600;
                default: return 0;
            }
        } else if (tSpinType == TSpinType.MINI) {
            switch (lines) {
                case 0: return 100;
                case 1: return 100;
                case 2: return 400;
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
        if (gameState == GameState.TUTORIAL) {
            return new Piece(Tetromino.T);
        }
        Tetromino[] types = Tetromino.values();
        Tetromino randomType = types[new Random().nextInt(types.length)];
        return new Piece(randomType);
    }

    // Spawn new piece
    private void spawnNewPiece() {
        // Fully reset all lock-delay state for the incoming piece
        stopLockDelay();
        lockMoveResets = 0;
        lockTotalStartTime = 0;

        lastMoveWasRotation = false;
        lastRotationKickOffset = new int[] {0, 0};

        piecesSpawned++;
        needsAiCalculation = true; // Request AI path recalculation

        // Use nextPiece and generate new nextPiece
        currentPiece = nextPieces.remove(0);
        nextPieces.add(generateRandomPiece());
        nextPiece = nextPieces.get(0);

        // Update view
        panel.setCurrentPiece(currentPiece);

        // Restart gravity timer to give player a full tick window
        if (gameLoop != null) {
            gameLoop.restart();
        }

        // If new piece collision, game over
        if (!board.isValidMove(currentPiece)) {
            isGameOver = true;
            setAiPlay(false); // Stop AI Autoplay
            SaveManager.deleteSave();
            if (gameLoop != null) {
                gameLoop.stop();
            }
            if (secondTimer != null) {
                secondTimer.stop();
            }

            if (gameMode == GameMode.PVP && opponent != null) {
                if (opponent.isGameOver()) {
                    // Both players are now game over, determine the winner
                    int winner = 0; // 0: none, 1: P1, 2: P2, 3: Tie
                    int p1Score = (playerNum == 1) ? this.score : opponent.getScore();
                    int p2Score = (playerNum == 1) ? opponent.getScore() : this.score;
                    int p1Time = (playerNum == 1) ? this.secondsElapsed : opponent.getSecondsElapsed();
                    int p2Time = (playerNum == 1) ? opponent.getSecondsElapsed() : this.secondsElapsed;

                    if (p1Score > p2Score) {
                        winner = 1;
                    } else if (p2Score > p1Score) {
                        winner = 2;
                    } else {
                        // Tie breaker: longer survival time wins
                        if (p1Time > p2Time) {
                            winner = 1;
                        } else if (p2Time > p1Time) {
                            winner = 2;
                        } else {
                            winner = 3; // True tie
                        }
                    }

                    this.pvpWinner = winner;
                    opponent.setPvpWinner(winner);
                    SoundManager.stopBGM();
                } else {
                    // Opponent is still playing, do not stop BGM or determine winner yet
                }
            } else {
                SoundManager.stopBGM();
            }

            // Check if score qualifies for leaderboard
            if (gameMode != GameMode.PVP && !usedAiThisSession && leaderboardManager.qualifiesForLeaderboard(score, secondsElapsed, totalLinesCleared, difficulty, gameMode)) {
                isEnteringName = true;
                nameInputBuffer.setLength(0);
                panel.startAnimationTimer();
            } else {
                recordFinalScore();
            }
            System.out.println("Game Over!");
        }
    }

    private void recordFinalScore() {
        if (leaderboardRecorded) {
            return;
        }

        leaderboardRecorded = true;
        if (!usedAiThisSession && gameMode != GameMode.PVP) {
            leaderboardManager.recordScore(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, "PLAYER");
        }
    }

    // Save current game state
    public void saveGame() {
        if (gameState != GameState.PLAYING || isGameOver || gameMode == GameMode.SPRINT || gameMode == GameMode.ULTRA || gameMode == GameMode.SURVIVAL) {
            return;
        }
        SaveManager.save(score, secondsElapsed, difficulty, canHoldThisTurn, currentPiece, nextPiece, heldPiece, board,
                         piecesSpawned, totalActions, totalLinesCleared, tetrisClears, tSpins, maxCombo);
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
        this.gameMode = GameMode.ENDLESS; // Loaded games are always Endless
        this.canHoldThisTurn = state.canHoldThisTurn;
        this.currentPiece = state.currentPiece;
        this.nextPiece = state.nextPiece;
        this.nextPieces.clear();
        this.nextPieces.add(this.nextPiece);
        for (int i = 0; i < 4; i++) {
            this.nextPieces.add(generateRandomPiece());
        }
        this.heldPiece = state.heldPiece;
        this.piecesSpawned = state.piecesSpawned;
        this.totalActions = state.totalActions;
        this.totalLinesCleared = state.totalLinesCleared;
        this.tetrisClears = state.tetrisClears;
        this.tSpins = state.tSpins;
        this.maxCombo = state.maxCombo;
        this.comboCount = -1;
        this.isGameOver = false;
        this.isVictory = false;
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

    // Getters and Setters for PVP and Board state
    public Board getBoard() {
        return board;
    }

    public Piece getCurrentPiece() {
        return currentPiece;
    }

    public void setOpponent(GameEngine opponent) {
        this.opponent = opponent;
    }

    public GameEngine getOpponent() {
        return opponent;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    public int getPvpWinner() {
        return pvpWinner;
    }

    public void setPvpWinner(int winner) {
        this.pvpWinner = winner;
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

    public java.util.List<Piece> getNextPieces() {
        return nextPieces;
    }

    public Piece getHeldPiece() {
        return heldPiece;
    }

    public List<LeaderboardEntry> getLeaderboardEntries() {
        return leaderboardManager.getTopEntries(difficulty, gameMode);
    }

    public List<LeaderboardEntry> getLeaderboardEntriesForDifficulty(Difficulty diff) {
        return leaderboardManager.getTopEntries(diff, gameMode);
    }

    public List<LeaderboardEntry> getLeaderboardEntriesForDifficultyAndMode(Difficulty diff, GameMode mode) {
        return leaderboardManager.getTopEntries(diff, mode);
    }

    public List<LeaderboardEntry> getGlobalLeaderboardEntriesForDifficultyAndMode(Difficulty diff, GameMode mode) {
        return leaderboardManager.getGlobalTopEntries(diff, mode);
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean isVictory() {
        return isVictory;
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
        if ((gameState == GameState.PLAYING || gameState == GameState.TUTORIAL) && isPaused) {
            panel.navigatePauseMenu(dir);
        }
    }

    public void selectPauseMenuItem() {
        if ((gameState == GameState.PLAYING || gameState == GameState.TUTORIAL) && isPaused) {
            panel.selectPauseMenuItem();
        }
    }

    public void handleBackAction() {
        if ((gameState == GameState.PLAYING || gameState == GameState.TUTORIAL) && isPaused) {
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
                panel.setShowModeSelectInMenu(true); // Go back to Mode Select
                panel.setSelectedModeIndex(0);
            } else if (panel.isShowModeSelectInMenu()) {
                panel.setShowModeSelectInMenu(false); // Go back to Main Menu
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

    public void navigateLeaderboardModes(int dir) {
        if (gameState == GameState.LEADERBOARD) {
            panel.navigateLeaderboardModes(dir);
        }
    }



    // Move piece left
    public void movePieceLeft() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        if (handleMove(0, -1)) {
            lastMoveWasRotation = false;
            resetLockDelay();
        }
    }

    // Move piece right
    public void movePieceRight() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        if (handleMove(0, 1)) {
            lastMoveWasRotation = false;
            resetLockDelay();
        }
    }

    // Rotate piece
    public void rotatePiece() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        
        int fromState = currentPiece.getRotationIndex();
        currentPiece.rotate();
        int toState = currentPiece.getRotationIndex();
        
        // 1. Check if the rotated position is immediately valid
        if (board.isValidMove(currentPiece)) {
            lastMoveWasRotation = true;
            lastRotationKickOffset = new int[] {0, 0};
            resetLockDelay();
            panel.repaint();
            return;
        }
        
        // 2. Wall Kick / Floor Kick Mechanism (SRS)
        int[][] kickOffsets = getSrsKickOffsets(currentPiece.getType(), fromState, toState);
        
        boolean kickSuccessful = false;
        int[] successfulOffset = null;
        for (int[] offset : kickOffsets) {
            int dr = offset[0];
            int dc = offset[1];
            
            currentPiece.move(dr, dc);
            if (board.isValidMove(currentPiece)) {
                kickSuccessful = true;
                successfulOffset = offset;
                break;
            }
            // Undo this kick attempt before trying next
            currentPiece.move(-dr, -dc);
        }
        
        if (kickSuccessful) {
            lastMoveWasRotation = true;
            lastRotationKickOffset = successfulOffset;
            resetLockDelay();
        } else {
            // All kicks failed, undo rotation
            currentPiece.undoRotate();
        }
        panel.repaint();
    }

    private int[][] getSrsKickOffsets(Tetromino type, int fromState, int toState) {
        if (type == Tetromino.O) {
            return new int[0][2];
        }
        
        if (type == Tetromino.I) {
            if (fromState == 0 && toState == 1) {
                return new int[][] { {0, -2}, {0, 1}, {1, -2}, {-2, 1} };
            } else if (fromState == 1 && toState == 2) {
                return new int[][] { {0, -1}, {0, 2}, {-2, -1}, {1, 2} };
            } else if (fromState == 2 && toState == 3) {
                return new int[][] { {0, 2}, {0, -1}, {-1, 2}, {2, -1} };
            } else if (fromState == 3 && toState == 0) {
                return new int[][] { {0, 1}, {0, -2}, {2, 1}, {-1, -2} };
            }
        } else { // J, L, S, T, Z
            if (fromState == 0 && toState == 1) {
                return new int[][] { {0, -1}, {-1, -1}, {2, 0}, {2, -1} };
            } else if (fromState == 1 && toState == 2) {
                return new int[][] { {0, 1}, {1, 1}, {-2, 0}, {-2, 1} };
            } else if (fromState == 2 && toState == 3) {
                return new int[][] { {0, 1}, {-1, 1}, {2, 0}, {2, 1} };
            } else if (fromState == 3 && toState == 0) {
                return new int[][] { {0, -1}, {1, -1}, {-2, 0}, {-2, -1} };
            }
        }
        return new int[0][2];
    }

    // Drop piece (Hard Drop)
    public void dropPiece() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        int startRow = currentPiece.getRow();
        while (board.isValidMove(currentPiece)) {
            currentPiece.move(1, 0);
        }
        // Move back to last valid position
        currentPiece.move(-1, 0);

        int endRow = currentPiece.getRow();
        if (endRow > startRow) {
            panel.spawnDropParticles(currentPiece);
            // Do NOT set lastMoveWasRotation = false here, so T-Spins can be triggered on Hard Drop
        }

        // Hard drop = immediate lock, no delay window.
        // Lock delay (with time to rotate/shift) only applies to gravity & soft drop.
        stopLockDelay();
        freezeAndSpawn();
        panel.repaint();
    }

    // Hold the current piece
    public void holdPiece() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        if (!canHoldThisTurn)
            return;
        totalActions++;

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
        needsAiCalculation = true; // Request AI path recalculation
        panel.repaint();
    }

    public int getPiecesSpawned() {
        return piecesSpawned;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public int getTotalLinesCleared() {
        return totalLinesCleared;
    }

    public int getTetrisClears() {
        return tetrisClears;
    }

    public int getTSpins() {
        return tSpins;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public double getPPM() {
        return secondsElapsed > 0 ? (piecesSpawned / (secondsElapsed / 60.0)) : 0.0;
    }

    public double getAPM() {
        return secondsElapsed > 0 ? (totalActions / (secondsElapsed / 60.0)) : 0.0;
    }

    public double getTetrisRate() {
        return totalLinesCleared > 0 ? (((double)(tetrisClears * 4) / totalLinesCleared) * 100.0) : 0.0;
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

    public boolean isAiPlay() {
        return aiPlay;
    }

    public boolean hasUsedAiThisSession() {
        return usedAiThisSession;
    }

    public void setAiPlay(boolean active) {
        this.aiPlay = active;
        if (active) {
            this.usedAiThisSession = true;
            needsAiCalculation = true;
            if (aiTimer != null && !aiTimer.isRunning()) {
                aiTimer.start();
            }
        } else {
            if (aiTimer != null) {
                aiTimer.stop();
            }
        }
        panel.repaint();
    }

    // AI step execution
    private void runAIStep() {
        if (gameState != GameState.PLAYING || isPaused || isGameOver) {
            return;
        }

        if (needsAiCalculation) {
            com.tetris.util.TetrisAI.Move bestMove = com.tetris.util.TetrisAI.findBestMove(board, currentPiece, heldPiece, canHoldThisTurn);
            if (bestMove != null) {
                if (bestMove.useHold) {
                    holdPiece();
                    needsAiCalculation = true;
                    return;
                }
                targetRotation = bestMove.rotationIndex;
                targetCol = bestMove.targetCol;
                needsAiCalculation = false;
            } else {
                dropPiece();
                return;
            }
        }

        // Align rotation first
        if (currentPiece.getRotationIndex() != targetRotation) {
            rotatePiece();
        }
        // Align column position next
        else if (currentPiece.getCol() < targetCol) {
            movePieceRight();
        }
        else if (currentPiece.getCol() > targetCol) {
            movePieceLeft();
        }
        // If aligned, perform Hard Drop!
        else {
            dropPiece();
        }
    }

    public int getTutorialLevel() {
        return tutorialLevel;
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    public void startTutorial() {
        tutorialLevel = 1;
        startTutorialLevel(tutorialLevel);
    }

    public void startTutorialLevel(int level) {
        this.tutorialLevel = level;
        this.isTransitioning = false;
        
        board.clear();
        score = 0;
        secondsElapsed = 0;
        comboCount = -1;
        isGameOver = false;
        isPaused = false;
        heldPiece = null;
        canHoldThisTurn = true;
        piecesSpawned = 0;
        totalActions = 0;
        totalLinesCleared = 0;
        tetrisClears = 0;
        tSpins = 0;
        maxCombo = 0;
        
        nextPieces.clear();
        for (int i = 0; i < 5; i++) {
            nextPieces.add(new Piece(Tetromino.T));
        }
        nextPiece = nextPieces.get(0);
        spawnNewPiece();
        
        board.setupTutorialLevel(level);
        
        gameState = GameState.TUTORIAL;
        
        SoundManager.playBGM("/resources/bgm.wav");
        
        gameLoop.stop();
        gameLoop.setDelay(1000);
        gameLoop.setInitialDelay(1000);
        gameLoop.start();
        
        secondTimer.stop();
        secondTimer.start();
        
        panel.repaint();
    }

    public boolean isLastTutorialSuccess() {
        return lastTutorialSuccess;
    }

    private void tutorialSuccess() {
        lastTutorialSuccess = true;
        isTransitioning = true;
        gameLoop.stop();
        panel.repaint();
        
        javax.swing.Timer delayTimer = new javax.swing.Timer(1200, e -> {
            if (tutorialLevel < 3) {
                startTutorialLevel(tutorialLevel + 1);
            } else {
                isGameOver = true;
                isTransitioning = false;
                SoundManager.stopBGM();
                panel.repaint();
            }
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void tutorialFail() {
        lastTutorialSuccess = false;
        isTransitioning = true;
        gameLoop.stop();
        panel.repaint();
        
        javax.swing.Timer delayTimer = new javax.swing.Timer(1200, e -> {
            startTutorialLevel(tutorialLevel);
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    public boolean isEnteringName() {
        return isEnteringName;
    }

    public StringBuilder getNameInputBuffer() {
        return nameInputBuffer;
    }

    public void appendNameInput(char c) {
        if (nameInputBuffer.length() < 10) {
            nameInputBuffer.append(c);
            panel.repaint();
        }
    }

    public void backspaceNameInput() {
        if (nameInputBuffer.length() > 0) {
            nameInputBuffer.setLength(nameInputBuffer.length() - 1);
            panel.repaint();
        }
    }

    public void submitLeaderboardName() {
        String name = nameInputBuffer.toString().trim();
        if (name.isEmpty()) {
            name = "Guest";
        }
        leaderboardManager.recordScore(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, name);
        leaderboardManager.submitGlobalScoreAsync(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, name);
        isEnteringName = false;
        leaderboardRecorded = true;
        panel.repaint();
    }

    public void submitDefaultName() {
        leaderboardManager.recordScore(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, "PLAYER");
        leaderboardManager.submitGlobalScoreAsync(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, "PLAYER");
        isEnteringName = false;
        leaderboardRecorded = true;
        panel.repaint();
    }

}