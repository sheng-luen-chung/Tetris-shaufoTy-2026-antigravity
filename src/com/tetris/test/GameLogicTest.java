package com.tetris.test;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import com.tetris.controller.GameEngine;
import com.tetris.view.GamePanel;
import com.tetris.util.AchievementManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class GameLogicTest {

    public static void runAllTests() throws Exception {
        System.out.println("Running Tetris Game Logic Tests...");
        testBoardLineClear();
        testPieceMovement();
        testPieceRotation();
        testCollisionChecking();
        
        // New Tests
        test7BagRandomizer();
        testCCWWallKick();
        testTSpinMiniUpgrade();
        testBackToBackScoring();
        testComboAchievementSync();
        
        System.out.println("All tests passed successfully!");
    }

    private static void testBoardLineClear() throws Exception {
        Board board = new Board();
        Color[][] grid = board.getGrid();
        for (int c = 0; c < Board.COLS; c++) {
            grid[Board.ROWS - 1][c] = Color.BLUE;
        }

        int cleared = board.clearLines();
        if (cleared != 1) {
            throw new Exception("Board failed to clear exactly 1 line. Cleared: " + cleared);
        }

        for (int c = 0; c < Board.COLS; c++) {
            if (grid[Board.ROWS - 1][c] != null) {
                throw new Exception("Cell in bottom row should be null after line clear!");
            }
        }
        System.out.println("  - testBoardLineClear: PASSED");
    }

    private static void testPieceMovement() throws Exception {
        Piece piece = new Piece(Tetromino.I);
        int initialRow = piece.getRow();
        int initialCol = piece.getCol();

        piece.move(1, -1);
        if (piece.getRow() != initialRow + 1 || piece.getCol() != initialCol - 1) {
            throw new Exception("Piece failed to move row +1, col -1 properly.");
        }
        System.out.println("  - testPieceMovement: PASSED");
    }

    private static void testPieceRotation() throws Exception {
        Piece piece = new Piece(Tetromino.T);
        if (piece.getRotationIndex() != 0) {
            throw new Exception("Initial rotation index should be 0");
        }

        piece.rotate();
        if (piece.getRotationIndex() != 1) {
            throw new Exception("Rotation index should be 1 after rotate()");
        }

        piece.undoRotate();
        if (piece.getRotationIndex() != 0) {
            throw new Exception("Rotation index should be 0 after undoRotate()");
        }
        System.out.println("  - testPieceRotation: PASSED");
    }

    private static void testCollisionChecking() throws Exception {
        Board board = new Board();
        Piece piece = new Piece(Tetromino.O, 0, 3, 0);

        if (!board.isValidMove(piece)) {
            throw new Exception("Piece at (0, 3) on empty board should be in a valid position!");
        }

        Piece outOfBoundsPiece = new Piece(Tetromino.O, 0, -5, 0);
        if (board.isValidMove(outOfBoundsPiece)) {
            throw new Exception("Piece out of bounds left should be invalid!");
        }

        Piece belowBoardPiece = new Piece(Tetromino.O, 25, 3, 0);
        if (board.isValidMove(belowBoardPiece)) {
            throw new Exception("Piece below board should be invalid!");
        }

        Color[][] grid = board.getGrid();
        grid[2][4] = Color.RED;
        Piece collidingPiece = new Piece(Tetromino.O, 1, 3, 0);
        if (board.isValidMove(collidingPiece)) {
            throw new Exception("Piece colliding with frozen block should be invalid!");
        }
        System.out.println("  - testCollisionChecking: PASSED");
    }

    // Reflective helper to invoke private methods
    private static Object invokeMethod(Object obj, String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        java.lang.reflect.Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    // Reflective helper to set private fields
    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    // Reflective helper to get private fields
    private static Object getField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private static void setAchievementUnlocked(String id, boolean unlocked) throws Exception {
        java.lang.reflect.Field achievementsField = AchievementManager.class.getDeclaredField("achievements");
        achievementsField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, ?> achievements = (Map<String, ?>) achievementsField.get(null);
        Object info = achievements.get(id);
        if (info == null) {
            throw new Exception("Unknown achievement id: " + id);
        }

        java.lang.reflect.Field unlockedField = info.getClass().getDeclaredField("unlocked");
        unlockedField.setAccessible(true);
        unlockedField.setBoolean(info, unlocked);
    }

    private static void test7BagRandomizer() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // Clear the bag first to align our loop perfectly with the bag boundary
        java.util.List<?> bag = (java.util.List<?>) getField(engine, "tetrominoBag");
        bag.clear();

        // Generate 14 pieces and track counts.
        // Each bag of 7 tetrominoes should contain exactly one of each of the 7 types.
        Map<Tetromino, Integer> firstBag = new HashMap<>();
        Map<Tetromino, Integer> secondBag = new HashMap<>();

        for (int i = 0; i < 7; i++) {
            Piece p = (Piece) invokeMethod(engine, "generateRandomPiece", new Class<?>[0], new Object[0]);
            firstBag.put(p.getType(), firstBag.getOrDefault(p.getType(), 0) + 1);
        }

        for (int i = 0; i < 7; i++) {
            Piece p = (Piece) invokeMethod(engine, "generateRandomPiece", new Class<?>[0], new Object[0]);
            secondBag.put(p.getType(), secondBag.getOrDefault(p.getType(), 0) + 1);
        }

        for (Tetromino type : Tetromino.values()) {
            if (firstBag.getOrDefault(type, 0) != 1) {
                throw new Exception("7-bag first bag failed to distribute evenly: " + firstBag);
            }
            if (secondBag.getOrDefault(type, 0) != 1) {
                throw new Exception("7-bag second bag failed to distribute evenly: " + secondBag);
            }
        }
        System.out.println("  - test7BagRandomizer: PASSED");
    }

    private static void testCCWWallKick() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // Access getSrsKickOffsets
        int[][] ccwOffsets = (int[][]) invokeMethod(engine, "getSrsKickOffsets",
                new Class<?>[]{Tetromino.class, int.class, int.class},
                new Object[]{Tetromino.I, 1, 0}); // CCW 1 -> 0

        // CW 0 -> 1 of I-piece is { {0, -2}, {0, 1}, {1, -2}, {-2, 1} }
        // CCW 1 -> 0 of I-piece should be negated: { {0, 2}, {0, -1}, {-1, 2}, {2, -1} }
        int[][] expected = { {0, 2}, {0, -1}, {-1, 2}, {2, -1} };
        if (ccwOffsets.length != expected.length) {
            throw new Exception("CCW offset list length mismatch");
        }

        for (int i = 0; i < expected.length; i++) {
            if (ccwOffsets[i][0] != expected[i][0] || ccwOffsets[i][1] != expected[i][1]) {
                throw new Exception("CCW offsets negation failed at index " + i + ": got (" + ccwOffsets[i][0] + "," + ccwOffsets[i][1] + ")");
            }
        }
        System.out.println("  - testCCWWallKick: PASSED");
    }

    private static void testTSpinMiniUpgrade() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // Setup a T-piece in a rotation state requiring Test 5 (Test index 3)
        // Transition: 0 -> 1 CW (kick offset index 3 is {2, -1})
        Piece tPiece = new Piece(Tetromino.T, 2, 3, 1); // rotation state 1
        setField(engine, "currentPiece", tPiece);
        setField(engine, "lastMoveWasRotation", true);
        setField(engine, "lastRotationFromState", 0);
        setField(engine, "lastRotationToState", 1);
        setField(engine, "lastRotationKickOffset", new int[]{2, -1}); // Test 5 kick

        // Setup corners so 3-corner rule is satisfied, but only 1 front corner is occupied
        // Center of T-Piece is at (r+1, c+1) = (3, 4)
        // Corners:
        // Front (pointing right): TR (2, 5), BR (4, 5)
        // Back (left side): TL (2, 3), BL (4, 3)
        // 3 Corners occupied: TL, BL (both back corners), and TR (1 front corner). BR is empty.
        Color[][] grid = board.getGrid();
        grid[2][3] = Color.BLUE; // TL
        grid[4][3] = Color.BLUE; // BL
        grid[2][5] = Color.BLUE; // TR
        grid[4][5] = null;       // BR (empty)

        // Invoke checkTSpinType
        Object result = invokeMethod(engine, "checkTSpinType", new Class<?>[0], new Object[0]);
        // TSpinType.REGULAR since it used Test 5 kick!
        if (!result.toString().equals("REGULAR")) {
            throw new Exception("T-Spin mini did not upgrade to REGULAR on Test 5 kick: got " + result);
        }
        System.out.println("  - testTSpinMiniUpgrade: PASSED");
    }

    private static void testBackToBackScoring() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // Verify isBackToBackActive starts as false
        boolean b2b = (boolean) getField(engine, "isBackToBackActive");
        if (b2b) {
            throw new Exception("isBackToBackActive should initialize to false");
        }

        // Setup board for a Tetris clear (4 rows filled except column 9)
        Color[][] grid = board.getGrid();
        grid[0][0] = Color.RED; // Non-clearing block to avoid Perfect Clear bonus
        for (int r = Board.ROWS - 4; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS - 1; c++) {
                grid[r][c] = Color.BLUE;
            }
        }

        // 1st Tetris: spawn vertical I piece in column 9 (rotation 1, col 7 -> absolute col 7 + 2 = 9)
        Piece iPiece1 = new Piece(Tetromino.I, Board.ROWS - 4, 7, 1);
        setField(engine, "currentPiece", iPiece1);

        // Call freezeAndSpawn directly using reflection
        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        // Points for 1st Tetris should be 800, and isBackToBackActive should become true
        int score1 = (int) getField(engine, "score");
        boolean b2b1 = (boolean) getField(engine, "isBackToBackActive");

        if (score1 != 800) {
            throw new Exception("1st Tetris score should be 800, got: " + score1);
        }
        if (!b2b1) {
            throw new Exception("isBackToBackActive should be true after 1st Tetris");
        }

        // Setup board for 2nd Tetris
        for (int r = Board.ROWS - 4; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS - 1; c++) {
                grid[r][c] = Color.BLUE;
            }
        }

        // 2nd Tetris
        Piece iPiece2 = new Piece(Tetromino.I, Board.ROWS - 4, 7, 1);
        setField(engine, "currentPiece", iPiece2);

        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        // Points for 2nd Tetris should receive B2B bonus and a 1-combo: 800 + 1200 + 50 = 2050.
        int score2 = (int) getField(engine, "score");
        boolean b2b2 = (boolean) getField(engine, "isBackToBackActive");

        if (score2 != 2050) {
            throw new Exception("2nd Tetris (B2B) score should sum up to 2050 (800 + 1200 + 50 combo), got: " + score2);
        }
        if (!b2b2) {
            throw new Exception("isBackToBackActive should remain true after consecutive Tetris");
        }

        // Setup a single line clear to break B2B
        for (int c = 0; c < Board.COLS - 1; c++) {
            grid[Board.ROWS - 1][c] = Color.BLUE;
        }
        Piece iPiece3 = new Piece(Tetromino.I, Board.ROWS - 2, 6, 0);
        setField(engine, "currentPiece", iPiece3);

        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        // B2B should now be false
        boolean b2b3 = (boolean) getField(engine, "isBackToBackActive");
        if (b2b3) {
            throw new Exception("isBackToBackActive should be false after a normal single line clear");
        }

        System.out.println("  - testBackToBackScoring: PASSED");
    }

    private static void testComboAchievementSync() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // Make sure the achievement starts locked for this test.
        setAchievementUnlocked("combo_master", false);

        // Simulate that the player already has a 4-combo streak before the next clear.
        setField(engine, "gameState", GameEngine.GameState.PLAYING);
        setField(engine, "comboCount", 4);
        setField(engine, "maxCombo", 4);

        // Prepare a single line clear with an O piece that completes the bottom row.
        Color[][] grid = board.getGrid();
        for (int c = 0; c < Board.COLS; c++) {
            grid[Board.ROWS - 1][c] = (c == 4 || c == 5) ? null : Color.BLUE;
        }
        Piece oPiece = new Piece(Tetromino.O, Board.ROWS - 2, 3, 0);
        setField(engine, "currentPiece", oPiece);

        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        int comboAfter = (int) getField(engine, "comboCount");
        if (comboAfter != 5) {
            throw new Exception("comboCount should become 5 after the 5th consecutive clear, got: " + comboAfter);
        }

        if (!AchievementManager.isUnlocked("combo_master")) {
            throw new Exception("combo_master achievement should unlock when comboCount reaches 5");
        }

        System.out.println("  - testComboAchievementSync: PASSED");
    }
}
