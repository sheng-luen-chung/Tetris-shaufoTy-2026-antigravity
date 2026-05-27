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
        TUTORIAL,
        TUTORIAL_SELECT,
        ACHIEVEMENTS,
        NET_LOBBY
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

    // Multi-threaded Game Loop fields (Fixed Timestep)
    private Thread gameLoopThread;
    private volatile boolean running = false;
    private int gravityAccumulator = 0;
    private int secondAccumulator = 0;
    private int aiAccumulator = 0;
    private int menuAccumulator = 0;
    private volatile boolean isGameOver = false;
    private volatile boolean isPaused = false;
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
    private volatile boolean aiPlay = false;
    private volatile boolean usedAiThisSession = false;
    private static final double DEFAULT_AI_DEMO_DELAY_MULTIPLIER = 0.5;
    private volatile boolean aiDemoMode = false;
    private volatile double aiDemoDelayMultiplier = DEFAULT_AI_DEMO_DELAY_MULTIPLIER;
    private int targetRotation = 0;
    private int targetCol = 0;
    private volatile boolean needsAiCalculation = true;

    // Reuse Random to avoid frequent allocations
    private final Random rng = new Random();

    private volatile Difficulty difficulty = Difficulty.NORMAL;
    private final LeaderboardManager leaderboardManager;
    private volatile GameState gameState = GameState.MENU;
    private volatile boolean isEnteringName = false;
    private final StringBuilder nameInputBuffer = new StringBuilder();

    // Thread-safe Action Queues to prevent Race Conditions & Deadlocks
    public enum GameAction {
        MOVE_LEFT,
        MOVE_RIGHT,
        ROTATE_CW,
        ROTATE_CCW,
        SOFT_DROP,
        HARD_DROP,
        HOLD
    }

    private final java.util.concurrent.ConcurrentLinkedQueue<GameAction> actionQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<Integer> garbageQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public void queueAction(GameAction action) {
        actionQueue.add(action);
    }

    public void queueGarbage(int garbageLinesCount) {
        garbageQueue.add(garbageLinesCount);
    }

    private void processInputActions() {
        GameAction action;
        while ((action = actionQueue.poll()) != null) {
            switch (action) {
                case MOVE_LEFT:
                    movePieceLeftInternal();
                    break;
                case MOVE_RIGHT:
                    movePieceRightInternal();
                    break;
                case ROTATE_CW:
                    rotatePieceInternal();
                    break;
                case ROTATE_CCW:
                    rotatePieceCounterClockwiseInternal();
                    break;
                case SOFT_DROP:
                    softDropInternal();
                    break;
                case HARD_DROP:
                    dropPieceInternal();
                    break;
                case HOLD:
                    holdPieceInternal();
                    break;
            }
        }
    }

    private void processGarbageQueue() {
        Integer lines;
        while ((lines = garbageQueue.poll()) != null) {
            receiveGarbageInternal(lines);
        }
    }

    public GameEngine(Board board, GamePanel panel) {
        this.board = board;
        this.panel = panel;
        this.leaderboardManager = new LeaderboardManager();
        com.tetris.util.AchievementManager.init(this);

        // Initialize pieces
        nextPieces.clear();
        for (int i = 0; i < 5; i++) {
            nextPieces.add(generateRandomPiece());
        }
        nextPiece = nextPieces.get(0);
        spawnNewPiece();
    }

    // Start the game loop thread
    public synchronized void start() {
        if (running) return;
        running = true;
        gameLoopThread = new Thread(this::runGameLoop, "Tetris-GameLoop-" + playerNum);
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();

        // Play Main Menu BGM when starting up!
        if (playerNum == 1 && gameState == GameState.MENU) {
            SoundManager.playBGM("/resources/menu_bgm.wav");
        }
    }

    public synchronized void stop() {
        running = false;
        if (gameLoopThread != null) {
            try {
                gameLoopThread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            gameLoopThread = null;
        }
    }

    private void runGameLoop() {
        long lastTime = System.nanoTime();
        double tickTimeNs = 1000000000.0 / 100.0; // 100Hz logic tick
        double unprocessed = 0;

        long lastFrameTime = System.nanoTime();
        double frameTimeNs = 1000000000.0 / 60.0; // 60 FPS target

        while (running) {
            long now = System.nanoTime();
            unprocessed += (now - lastTime) / tickTimeNs;
            lastTime = now;

            boolean ticked = false;
            while (unprocessed >= 1) {
                synchronized (this) {
                    tickLogic(10);
                }
                unprocessed--;
                ticked = true;
            }

            if (ticked) {
                long frameNow = System.nanoTime();
                if (frameNow - lastFrameTime >= frameTimeNs) {
                    if (playerNum == 1) {
                        panel.renderOffscreen();
                    }
                    lastFrameTime = frameNow;
                }
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void tickLogic(int dt) {
        if (gameMode == GameMode.NET_PVP && playerNum == 2) {
            return;
        }
        // 0. Process queued garbage and inputs from EDT first
        processGarbageQueue();
        processInputActions();

        // 1. Tick inputs
        if (inputHandler != null) {
            inputHandler.tickInputs(dt);
        }

        // Process any movements queued by tickInputs (continuous DAS/Soft Drop) immediately
        processInputActions();

        // 2. Tick game state
        if (gameState == GameState.PLAYING || gameState == GameState.TUTORIAL) {
            if (!isGameOver && !isPaused && !isTransitioning) {
                // Gravity
                gravityAccumulator += dt;
                if (gravityAccumulator >= getFallDelayMs()) {
                    gravityAccumulator = 0;
                    update();
                }

                // Lock delay
                tickLockDelay();

                // Game time (second timer)
                secondAccumulator += dt;
                if (secondAccumulator >= 1000) {
                    secondAccumulator -= 1000;
                    secondsElapsed++;
                    handleSecondsTick();
                }

                // AI step timer
                if (aiPlay) {
                    aiAccumulator += dt;
                    int aiDelay = getAiStepDelayMs();
                    if (aiAccumulator >= aiDelay) {
                        aiAccumulator -= aiDelay;
                        runAIStep();
                    }
                }
            }
        } else if (gameState == GameState.MENU || gameState == GameState.LEADERBOARD) {
            // Menu floating pieces animation
            menuAccumulator += dt;
            if (menuAccumulator >= 33) {
                menuAccumulator -= 33;
                panel.repaint();
            }
        }
    }

    private void handleSecondsTick() {
        if (gameMode == GameMode.ULTRA && secondsElapsed >= 120) {
            isGameOver = true;
            isVictory = true;
            setAiPlay(false);
            SoundManager.stopBGM();
            SoundManager.playSFX("/resources/clear.wav");
            recordFinalScore();
        } else if (gameMode == GameMode.SURVIVAL) {
            if (!usedAiThisSession && secondsElapsed >= 180) {
                com.tetris.util.AchievementManager.unlockAchievement("survival_master");
            }
            int interval = 15;
            if (difficulty == Difficulty.EASY) {
                interval = 20;
            } else if (difficulty == Difficulty.HARD) {
                interval = 10;
            }
                if (secondsElapsed > 0 && secondsElapsed % interval == 0) {
                    int holeCol = rng.nextInt(Board.COLS);
                board.addGarbageLine(holeCol);
                if (currentPiece != null && !board.isValidMove(currentPiece)) {
                    currentPiece.move(-1, 0); // push up
                    if (!board.isValidMove(currentPiece)) {
                        currentPiece.move(1, 0); // push back if failed
                    }
                }
                SoundManager.playSFX("/resources/clear.wav");
            }
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
        aiDemoMode = false;
        aiDemoDelayMultiplier = DEFAULT_AI_DEMO_DELAY_MULTIPLIER;

        // Reset logic accumulators
        gravityAccumulator = 0;
        secondAccumulator = 0;
        aiAccumulator = 0;

        if (gameMode == GameMode.PVP || gameMode == GameMode.VS_AI || gameMode == GameMode.NET_PVP) {
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
            opponent.setGameMode(gameMode);
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

            // Set difficulty for opponent (AI speed will depend on it)
            opponent.setDifficulty(this.difficulty);

            // In VS_AI mode: Player 1 (left) is AI, Player 2 (right) is human
            this.setDifficulty(this.difficulty);
            this.setAiPlay(gameMode == GameMode.VS_AI);
            opponent.setAiPlay(false);

            if (gameMode != GameMode.NET_PVP) {
                opponent.start(); // Ensure opponent thread is running
            }
            opponent.gravityAccumulator = 0;
            opponent.secondAccumulator = 0;
            opponent.aiAccumulator = 0;

            if (gameMode == GameMode.VS_AI) {
                this.isPaused = true;
                opponent.isPaused = true;
                if (panel != null) {
                    panel.setShowVsAiControlsPrompt(true);
                }
            }
        }

        panel.updateWindowSize();

        // Play BGM
        SoundManager.playBGM("/resources/bgm.wav");
        if (isPaused) {
            SoundManager.pauseBGM();
        }

        panel.repaint();
    }

    // Return to main menu
    public void returnToMenu() {
        setAiPlay(false); // Stop AI Autoplay
        gameState = GameState.MENU;
        if (playerNum == 1) {
            SoundManager.playBGM("/resources/menu_bgm.wav");
        } else {
            SoundManager.stopBGM();
        }

        if ((gameMode == GameMode.PVP || gameMode == GameMode.VS_AI || gameMode == GameMode.NET_PVP) && opponent != null && opponent.getGameState() != GameState.MENU) {
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
        if (gameMode == GameMode.NET_PVP)
            return;
        if (gameState != GameState.PLAYING && gameState != GameState.TUTORIAL)
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
        if ((gameMode == GameMode.PVP || gameMode == GameMode.VS_AI || gameMode == GameMode.NET_PVP) && opponent != null && opponent.isPaused() != isPaused) {
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
        queueAction(GameAction.SOFT_DROP);
    }

    private void softDropInternal() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        update();
        // Reset gravity accumulator to prevent gravity stutter
        gravityAccumulator = 0;
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
            panel.spawnDropParticles(currentPiece, playerNum); // Landing particles
            
            // Instead of immediate freeze, start lock delay
            if (!isLocking) {
                startLockDelay();
            }
        } else {
            // Successfully moved down
            stopLockDelay();
            lastMoveWasRotation = false; 
        }

        if (gameMode == GameMode.NET_PVP && playerNum == 1) {
            sendNetworkGameState();
        }
        panel.repaint(); // Repaint
    }

    // Freeze and spawn piece
    private void freezeAndSpawn() {
        int popupCol = getCurrentPieceCenterCol();
        int popupRow = getCurrentPieceCenterRow();
        board.freezePiece(currentPiece); // Freeze

        if (gameState == GameState.TUTORIAL) {
            boolean success = false;
            
            // Check T-Spin status BEFORE clearing lines!
            TSpinType tSpinType = checkTSpinType();
            
            int lines = board.clearLines();
            
            if (tutorialLevel == 1) {
                if (lines > 0) {
                    success = true;
                    com.tetris.util.AchievementManager.unlockAchievement("first_steps");
                }
            } else if (tutorialLevel == 2) {
                if (lines > 0) {
                    success = true;
                }
            } else if (tutorialLevel == 3) {
                if (lines > 0) {
                    success = true;
                }
            } else if (tutorialLevel == 4) {
                if (lines > 0) {
                    success = true;
                }
            } else { // Level 5, 6, 7
                if (tSpinType == TSpinType.REGULAR) {
                    success = true;
                    tSpins++;
                }
            }

            if (success) {
                SoundManager.playSFX("/resources/clear.wav");
                if (lines > 0) {
                    java.awt.Color[] rowColors = new java.awt.Color[Board.COLS];
                    java.util.Arrays.fill(rowColors, currentPiece.getType().getColor());
                    panel.spawnRowClearParticles(popupRow, rowColors, playerNum);
                    panel.triggerScreenshake(lines * 3, 200, playerNum);
                }
                if (tutorialLevel == 7) {
                    com.tetris.util.AchievementManager.unlockAchievement("tspin_master");
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
            SoundManager.playSynthSound(SoundManager.SoundType.LOCK);
        }

        // PVP Garbage Generation
        if (gameMode == GameMode.NET_PVP && playerNum == 1 && (lines > 0 || isTSpin)) {
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
                com.tetris.util.NetworkManager.getInstance().send("GARBAGE:" + garbageToSend);
            }
        } else if ((gameMode == GameMode.PVP || gameMode == GameMode.VS_AI) && opponent != null && (lines > 0 || isTSpin)) {
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

        // Trigger achievements in normal gameplay (only when playing and AI not used)
        if (gameState == GameState.PLAYING && !usedAiThisSession) {
            if (lines >= 1) {
                com.tetris.util.AchievementManager.unlockAchievement("first_clear");
            }
            if (lines == 4) {
                com.tetris.util.AchievementManager.unlockAchievement("tetris_clear");
            }
            if (comboCount >= 5) {
                com.tetris.util.AchievementManager.unlockAchievement("combo_master");
            }
            if (isTSpin && tSpinType == TSpinType.REGULAR) {
                com.tetris.util.AchievementManager.unlockAchievement("tspin_regular");
            }
            if (isPerfectClear) {
                com.tetris.util.AchievementManager.unlockAchievement("perfect_clear");
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
            if (!usedAiThisSession) {
                com.tetris.util.AchievementManager.unlockAchievement("sprint_finisher");
            }
            SoundManager.stopBGM();
            SoundManager.playSFX("/resources/clear.wav");
            recordFinalScore();
            panel.repaint();
            return;
        }

        spawnNewPiece(); // Spawn
        canHoldThisTurn = true; // Reset hold status for the next turn
        if (gameMode == GameMode.NET_PVP && playerNum == 1) {
            sendNetworkGameState();
        }
    }

    public void receiveGarbage(int garbageLinesCount) {
        queueGarbage(garbageLinesCount);
    }

    private void receiveGarbageInternal(int garbageLinesCount) {
        if (isGameOver || isPaused) return;

        // Generate garbage lines on this board
        for (int i = 0; i < garbageLinesCount; i++) {
            int holeCol = rng.nextInt(Board.COLS);
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
        if (gameState == GameState.PLAYING && gameMode == GameMode.ENDLESS && !usedAiThisSession && score >= 50000) {
            com.tetris.util.AchievementManager.unlockAchievement("score_master");
        }
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
            switch (tutorialLevel) {
                case 1:
                case 2:
                case 3:
                    return new Piece(Tetromino.I);
                case 4:
                    return new Piece(Tetromino.O);
                default:
                    return new Piece(Tetromino.T);
            }
        }
        Tetromino[] types = Tetromino.values();
        Tetromino randomType = types[rng.nextInt(types.length)];
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
        if (playerNum == 1) {
            panel.setCurrentPiece(currentPiece);
        }

        // Restart gravity timer by resetting the accumulator
        gravityAccumulator = 0;
 
        // If new piece collision, game over
        if (!board.isValidMove(currentPiece)) {
            isGameOver = true;
            setAiPlay(false); // Stop AI Autoplay
            SaveManager.deleteSave();
        }

        if (isGameOver) {
            if (gameMode == GameMode.NET_PVP && playerNum == 1) {
                synchronized (panel) {
                    if (this.pvpWinner == 0 && opponent != null && opponent.getPvpWinner() == 0) {
                        int winner = 2; // 對手獲勝
                        this.pvpWinner = winner;
                        opponent.setPvpWinner(winner);
                        opponent.isGameOver = true;
                        com.tetris.util.NetworkManager.getInstance().send("GAMEOVER");
                        SoundManager.stopBGM();
                    }
                }
            } else if ((gameMode == GameMode.PVP || gameMode == GameMode.VS_AI) && opponent != null) {
                synchronized (panel) {
                    if (this.pvpWinner == 0 && opponent.getPvpWinner() == 0) {
                        int winner = (this.playerNum == 1) ? 2 : 1;
                        this.pvpWinner = winner;
                        opponent.setPvpWinner(winner);
                        opponent.isGameOver = true;
                        opponent.setAiPlay(false);
                        SoundManager.stopBGM();
                    }
                }
            } else {
                SoundManager.stopBGM();
            }

            // Check if score qualifies for leaderboard
            if (gameMode != GameMode.PVP && gameMode != GameMode.VS_AI && gameMode != GameMode.NET_PVP && !usedAiThisSession && leaderboardManager.qualifiesForLeaderboard(score, secondsElapsed, totalLinesCleared, difficulty, gameMode)) {
                isEnteringName = true;
                nameInputBuffer.setLength(0);
                panel.startAnimationTimer();
            } else {
                recordFinalScore();
            }
        }
    }

    private void recordFinalScore() {
        if (leaderboardRecorded) {
            return;
        }

        leaderboardRecorded = true;
        if (!usedAiThisSession && gameMode != GameMode.PVP && gameMode != GameMode.VS_AI && gameMode != GameMode.NET_PVP) {
            leaderboardManager.recordScore(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, "PLAYER");
        }
    }

    // Get current fall delay in ms (incorporates STAGE mode dynamic speed up)
    public int getFallDelayMs() {
        if (gameMode == GameMode.STAGE) {
            int baseDelay = 500; // Always start at 500ms
            // Decrease delay by 15ms every stage (each 500 score is a new stage)
            int reduction = (getStageLevel() - 1) * 15;
            return Math.max(75, baseDelay - reduction);
        }
        return difficulty.getFallDelayMs();
    }

    public int getStageLevel() {
        return (score / 500) + 1;
    }

    // Save current game state
    public void saveGame() {
        if (gameState != GameState.PLAYING || isGameOver || aiDemoMode || gameMode == GameMode.SPRINT || gameMode == GameMode.ULTRA || gameMode == GameMode.SURVIVAL || gameMode == GameMode.STAGE || gameMode == GameMode.PVP || gameMode == GameMode.VS_AI || gameMode == GameMode.NET_PVP) {
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
        if (playerNum == 1) {
            panel.setCurrentPiece(currentPiece);
        }

        // Restore board grid
        Color[][] currentGrid = board.getGrid();
        for (int r = 0; r < Board.ROWS; r++) {
            System.arraycopy(state.grid[r], 0, currentGrid[r], 0, Board.COLS);
        }

        this.gameState = GameState.PLAYING;

        // Reset logic accumulators
        gravityAccumulator = 0;
        secondAccumulator = 0;
        aiAccumulator = 0;

        // Play BGM but immediately pause it
        SoundManager.playBGM("/resources/bgm.wav");
        SoundManager.pauseBGM();

        panel.repaint();
    }

    // Getters and Setters for PVP and Board state
    public Board getBoard() {
        return board;
    }

    public GamePanel getPanel() {
        return panel;
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

    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
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
        gravityAccumulator = 0; // Reset gravity delay window
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
            if (panel.isShowDisplaySettingsInPause()) {
                panel.setShowDisplaySettingsInPause(false);
            } else if (panel.isShowSettingsInPause()) {
                panel.setShowSettingsInPause(false);
            } else {
                togglePause();
            }
        } else if (gameState == GameState.MENU) {
            if (panel.isShowDisplaySettingsInMenu()) {
                panel.setShowDisplaySettingsInMenu(false);
            } else if (panel.isShowSettingsInMenu()) {
                if (panel.isShowControlSettings()) {
                    panel.setShowControlSettings(false);
                } else {
                    panel.setShowSettingsInMenu(false);
                }
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



    // Move piece left (queued)
    public void movePieceLeft() {
        queueAction(GameAction.MOVE_LEFT);
    }

    private void movePieceLeftInternal() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        if (handleMove(0, -1)) {
            lastMoveWasRotation = false;
            resetLockDelay();
            SoundManager.playSynthSound(SoundManager.SoundType.MOVE);
        }
    }

    // Move piece right (queued)
    public void movePieceRight() {
        queueAction(GameAction.MOVE_RIGHT);
    }

    private void movePieceRightInternal() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        if (handleMove(0, 1)) {
            lastMoveWasRotation = false;
            resetLockDelay();
            SoundManager.playSynthSound(SoundManager.SoundType.MOVE);
        }
    }

    // Rotate piece (queued)
    public void rotatePiece() {
        queueAction(GameAction.ROTATE_CW);
    }

    private void rotatePieceInternal() {
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
            SoundManager.playSynthSound(SoundManager.SoundType.ROTATE);
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
            SoundManager.playSynthSound(SoundManager.SoundType.ROTATE);
        } else {
            // All kicks failed, undo rotation
            currentPiece.undoRotate();
        }
        panel.repaint();
    }

    // Rotate piece counter-clockwise (queued)
    public void rotatePieceCounterClockwise() {
        queueAction(GameAction.ROTATE_CCW);
    }

    private void rotatePieceCounterClockwiseInternal() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        totalActions++;
        
        int fromState = currentPiece.getRotationIndex();
        currentPiece.undoRotate();
        int toState = currentPiece.getRotationIndex();
        
        // 1. Check if the rotated position is immediately valid
        if (board.isValidMove(currentPiece)) {
            lastMoveWasRotation = true;
            lastRotationKickOffset = new int[] {0, 0};
            resetLockDelay();
            SoundManager.playSynthSound(SoundManager.SoundType.ROTATE);
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
            SoundManager.playSynthSound(SoundManager.SoundType.ROTATE);
        } else {
            // All kicks failed, undo counter-clockwise rotation (by rotating clockwise)
            currentPiece.rotate();
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

    // Drop piece (Hard Drop - queued)
    public void dropPiece() {
        queueAction(GameAction.HARD_DROP);
    }

    private void dropPieceInternal() {
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
            panel.spawnDropParticles(currentPiece, playerNum);
            // Do NOT set lastMoveWasRotation = false here, so T-Spins can be triggered on Hard Drop
        }

        // Hard drop = immediate lock, no delay window.
        // Lock delay (with time to rotate/shift) only applies to gravity & soft drop.
        stopLockDelay();
        freezeAndSpawn();
        panel.repaint();
    }

    // Hold the current piece (queued)
    public void holdPiece() {
        queueAction(GameAction.HOLD);
    }

    private void holdPieceInternal() {
        if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || isGameOver || isPaused || isTransitioning)
            return;
        if (!canHoldThisTurn)
            return;
        totalActions++;
        SoundManager.playSynthSound(SoundManager.SoundType.HOLD);

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
            if (playerNum == 1) {
                panel.setCurrentPiece(currentPiece);
            }
            
            // Restart the gravity timer by resetting the accumulator
            gravityAccumulator = 0;
        }
        
        canHoldThisTurn = false;
        needsAiCalculation = true; // Request AI path recalculation
        if (gameMode == GameMode.NET_PVP && playerNum == 1) {
            sendNetworkGameState();
        }
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
        } else {
            aiDemoMode = false;
            aiDemoDelayMultiplier = DEFAULT_AI_DEMO_DELAY_MULTIPLIER;
        }
        aiAccumulator = 0;
        panel.repaint();
    }

    public void setAiDemoMode(boolean active) {
        this.aiDemoMode = active;
        if (!active) {
            this.aiDemoDelayMultiplier = DEFAULT_AI_DEMO_DELAY_MULTIPLIER;
        }
    }

    public boolean isAiDemoMode() {
        return aiDemoMode;
    }

    public double getAiDemoDelayMultiplier() {
        return aiDemoDelayMultiplier;
    }

    public void setAiDemoDelayMultiplier(double multiplier) {
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier <= 0.0) {
            return;
        }
        this.aiDemoDelayMultiplier = multiplier;
    }

    public int getAiStepDelayMs() {
        int baseDelay;
        if (difficulty == Difficulty.EASY) {
            baseDelay = 500;
        } else if (difficulty == Difficulty.HARD) {
            baseDelay = 300;
        } else {
            baseDelay = 400;
        }

        if (aiDemoMode) {
            return Math.max(20, (int) Math.round(baseDelay * aiDemoDelayMultiplier));
        }

        return baseDelay;
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
        gameState = GameState.TUTORIAL_SELECT;
        panel.repaint();
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
        if (level == 1 || level == 2) {
            for (int i = 0; i < 5; i++) {
                nextPieces.add(new Piece(Tetromino.I));
            }
        } else if (level == 3) {
            nextPieces.add(new Piece(Tetromino.Z));
            for (int i = 0; i < 4; i++) {
                nextPieces.add(new Piece(Tetromino.I));
            }
        } else if (level == 4) {
            for (int i = 0; i < 5; i++) {
                nextPieces.add(new Piece(Tetromino.O));
            }
        } else {
            for (int i = 0; i < 5; i++) {
                nextPieces.add(new Piece(Tetromino.T));
            }
        }
        nextPiece = nextPieces.get(0);
        spawnNewPiece();
        
        board.setupTutorialLevel(level);
        
        gameState = GameState.TUTORIAL;
        
        SoundManager.playBGM("/resources/bgm.wav");
        
        // Reset logic accumulators
        gravityAccumulator = 0;
        secondAccumulator = 0;
        aiAccumulator = 0;
        
        panel.repaint();
    }

    public boolean isLastTutorialSuccess() {
        return lastTutorialSuccess;
    }

    private void tutorialSuccess() {
        lastTutorialSuccess = true;
        isTransitioning = true;
        panel.repaint();
        
        javax.swing.Timer delayTimer = new javax.swing.Timer(1200, e -> {
            if (tutorialLevel < 7) {
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

    public synchronized String getNameInputString() {
        return nameInputBuffer.toString();
    }

    public synchronized void appendNameInput(char c) {
        if (nameInputBuffer.length() < 10) {
            nameInputBuffer.append(c);
            panel.repaint();
        }
    }

    public synchronized void backspaceNameInput() {
        if (nameInputBuffer.length() > 0) {
            nameInputBuffer.setLength(nameInputBuffer.length() - 1);
            panel.repaint();
        }
    }

    public synchronized void submitLeaderboardName() {
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

    public synchronized void submitDefaultName() {
        leaderboardManager.recordScore(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, "PLAYER");
        leaderboardManager.submitGlobalScoreAsync(score, secondsElapsed, totalLinesCleared, difficulty, gameMode, "PLAYER");
        isEnteringName = false;
        leaderboardRecorded = true;
        panel.repaint();
    }

    public void setRandomSeed(long seed) {
        this.rng.setSeed(seed);
    }

    public void sendNetworkGameState() {
        if (gameMode == GameMode.NET_PVP && playerNum == 1) {
            String stateStr = serializeGameState();
            com.tetris.util.NetworkManager.getInstance().send("SYNC:" + stateStr);
        }
    }

    public String serializeGameState() {
        String heldType = (heldPiece == null) ? "." : heldPiece.getType().name();
        String curType = (currentPiece == null) ? "." : currentPiece.getType().name();
        int curCol = (currentPiece == null) ? 0 : currentPiece.getCol();
        int curRow = (currentPiece == null) ? 0 : currentPiece.getRow();
        int curRot = (currentPiece == null) ? 0 : currentPiece.getRotationIndex();
        
        StringBuilder gridSb = new StringBuilder();
        Color[][] grid = board.getGrid();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Color color = grid[r][c];
                if (color == null) {
                    gridSb.append('.');
                } else if (color.equals(Color.RED) || color.equals(new Color(230, 70, 70)) || color.equals(new Color(255, 50, 50))) {
                    gridSb.append('Z');
                } else if (color.equals(Color.ORANGE) || color.equals(new Color(230, 130, 40)) || color.equals(new Color(255, 140, 0))) {
                    gridSb.append('L');
                } else if (color.equals(Color.YELLOW) || color.equals(new Color(230, 210, 40)) || color.equals(new Color(255, 220, 0))) {
                    gridSb.append('O');
                } else if (color.equals(Color.GREEN) || color.equals(new Color(70, 200, 70)) || color.equals(new Color(0, 255, 100))) {
                    gridSb.append('S');
                } else if (color.equals(Color.CYAN) || color.equals(new Color(70, 200, 200)) || color.equals(new Color(0, 255, 255))) {
                    gridSb.append('I');
                } else if (color.equals(Color.BLUE) || color.equals(new Color(70, 70, 230)) || color.equals(new Color(30, 80, 255))) {
                    gridSb.append('J');
                } else if (color.equals(new Color(180, 70, 230)) || color.equals(new Color(180, 50, 255)) || color.equals(Color.MAGENTA)) {
                    gridSb.append('T');
                } else if (color.equals(Color.GRAY)) {
                    gridSb.append('G');
                } else {
                    gridSb.append('X');
                }
            }
        }
        return score + "," + totalLinesCleared + "," + comboCount + "," + (isGameOver ? "1" : "0") + "," + heldType + "," + curType + "," + curCol + "," + curRow + "," + curRot + "|" + gridSb.toString();
    }

    public void deserializeGameState(String stateStr) {
        try {
            int pipeIdx = stateStr.indexOf('|');
            if (pipeIdx == -1) return;
            String stats = stateStr.substring(0, pipeIdx);
            String gridStr = stateStr.substring(pipeIdx + 1);
            
            String[] parts = stats.split(",");
            if (parts.length >= 9) {
                this.score = Integer.parseInt(parts[0]);
                this.totalLinesCleared = Integer.parseInt(parts[1]);
                this.comboCount = Integer.parseInt(parts[2]);
                this.isGameOver = parts[3].equals("1");
                
                String heldType = parts[4];
                if (heldType.equals(".")) {
                    this.heldPiece = null;
                } else {
                    this.heldPiece = new Piece(Tetromino.valueOf(heldType));
                }
                
                String curType = parts[5];
                if (curType.equals(".")) {
                    this.currentPiece = null;
                } else {
                    int curCol = Integer.parseInt(parts[6]);
                    int curRow = Integer.parseInt(parts[7]);
                    int curRot = Integer.parseInt(parts[8]);
                    this.currentPiece = new Piece(Tetromino.valueOf(curType), curRow, curCol, curRot);
                }
            }
            
            Color[][] grid = board.getGrid();
            int idx = 0;
            for (int r = 0; r < Board.ROWS; r++) {
                for (int c = 0; c < Board.COLS; c++) {
                    if (idx >= gridStr.length()) break;
                    char ch = gridStr.charAt(idx++);
                    switch (ch) {
                        case '.': grid[r][c] = null; break;
                        case 'Z': grid[r][c] = new Color(230, 70, 70); break;
                        case 'L': grid[r][c] = new Color(230, 130, 40); break;
                        case 'O': grid[r][c] = new Color(230, 210, 40); break;
                        case 'S': grid[r][c] = new Color(70, 200, 70); break;
                        case 'I': grid[r][c] = new Color(70, 200, 200); break;
                        case 'J': grid[r][c] = new Color(70, 70, 230); break;
                        case 'T': grid[r][c] = new Color(180, 70, 230); break;
                        case 'G': grid[r][c] = Color.GRAY; break;
                        default: grid[r][c] = Color.LIGHT_GRAY; break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error deserializing game state: " + e.getMessage());
        }
    }

}