package com.tetris.test;

import com.tetris.controller.GameEngine;
import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import com.tetris.util.AchievementManager;
import com.tetris.view.GamePanel;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * 集中式的簡易測試集合 (非 JUnit)。
 *
 * 此類使用靜態方法來驗證遊戲邏輯（例如消行、旋轉、7-bag 隨機化、B2B 計分等）。
 * 為了測試私有成員與方法，部分測試透過反射存取/呼叫。
 */
public class GameLogicTest {

    /**
     * 執行所有測試方法的入口。若任一測試失敗會丟出例外並停止執行。
     */
    public static void runAllTests() throws Exception {
        System.out.println("Running Tetris Game Logic Tests...");
        testBoardLineClear();      // 測試單行消除
        testPieceMovement();       // 測試方塊移動
        testPieceRotation();       // 測試方塊旋轉
        testCollisionChecking();   // 測試碰撞 / 邊界檢查

        // 進階測項
        test7BagRandomizer();      // 測試 7-bag 隨機器
        testCCWWallKick();         // 測試牆壁補位 (CCW)
        testTSpinMiniUpgrade();    // 測試 T-Spin mini 升級為 REGULAR 的情形
        testBackToBackScoring();   // 測試 B2B 計分與分數累計
        testGarbageRuleTable();    // 測試送垃圾行規則表
        testComboAchievementSync(); // 測試連擊與成就同步

        System.out.println("All tests passed successfully!");
    }

    // --- 基本單元測試 ---
    private static void testBoardLineClear() throws Exception {
        Board board = new Board();
        Color[][] grid = board.getGrid();
        // 填滿最底一列
        for (int c = 0; c < Board.COLS; c++) {
            grid[Board.ROWS - 1][c] = Color.BLUE;
        }

        int cleared = board.clearLines();
        if (cleared != 1) {
            throw new Exception("Board failed to clear exactly 1 line. Cleared: " + cleared);
        }

        // 確認該列已被清空
        for (int c = 0; c < Board.COLS; c++) {
            if (grid[Board.ROWS - 1][c] != null) {
                throw new Exception("Cell in bottom row should be null after line clear!");
            }
        }
        System.out.println("  - testBoardLineClear: PASSED");
    }

    private static void testPieceMovement() throws Exception {
        // 測試 move() 對 row / col 的影響
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
        // 測試 rotate() / undoRotate() 是否正確更新 rotation index
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
        // 測試有效位置判定與碰撞偵測
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

        // 放一格已凍結的方塊，測試碰撞
        Color[][] grid = board.getGrid();
        grid[2][4] = Color.RED;
        Piece collidingPiece = new Piece(Tetromino.O, 1, 3, 0);
        if (board.isValidMove(collidingPiece)) {
            throw new Exception("Piece colliding with frozen block should be invalid!");
        }
        System.out.println("  - testCollisionChecking: PASSED");
    }

    // --- 反射輔助方法（用於存取私有欄位／方法以便測試內部細節） ---
    private static Object invokeMethod(Object obj, String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        java.lang.reflect.Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static Object getField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private static void setAchievementUnlocked(String id, boolean unlocked) throws Exception {
        // 透過反射修改 AchievementManager 的靜態成就資料，確保測試可重複執行
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

    // --- 進階邏輯測試 ---
    private static void test7BagRandomizer() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // 清空 bag 以便精準測試每個 7-bag 是否包含完整 7 種型態
        java.util.List<?> bag = (java.util.List<?>) getField(engine, "tetrominoBag");
        bag.clear();

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

        // 以反射取得 SRS 的 CCW kick offsets，並比對期望的反號結果
        int[][] ccwOffsets = (int[][]) invokeMethod(engine, "getSrsKickOffsets",
                new Class<?>[]{Tetromino.class, int.class, int.class},
                new Object[]{Tetromino.I, 1, 0}); // CCW 1 -> 0

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

        // 模擬一個使用 Test 5 kick 的旋轉，使 mini 升級為 REGULAR
        Piece tPiece = new Piece(Tetromino.T, 2, 3, 1); // rotation state 1
        setField(engine, "currentPiece", tPiece);
        setField(engine, "lastMoveWasRotation", true);
        setField(engine, "lastRotationFromState", 0);
        setField(engine, "lastRotationToState", 1);
        setField(engine, "lastRotationKickOffset", new int[]{2, -1}); // Test 5 kick

        // 以角落佔據設定滿足 3-corner 規則
        Color[][] grid = board.getGrid();
        grid[2][3] = Color.BLUE; // TL
        grid[4][3] = Color.BLUE; // BL
        grid[2][5] = Color.BLUE; // TR
        grid[4][5] = null;       // BR (empty)

        Object result = invokeMethod(engine, "checkTSpinType", new Class<?>[0], new Object[0]);
        if (!result.toString().equals("REGULAR")) {
            throw new Exception("T-Spin mini did not upgrade to REGULAR on Test 5 kick: got " + result);
        }
        System.out.println("  - testTSpinMiniUpgrade: PASSED");
    }

    private static void testBackToBackScoring() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // 檢查初始 B2B 狀態
        boolean b2b = (boolean) getField(engine, "isBackToBackActive");
        if (b2b) {
            throw new Exception("isBackToBackActive should initialize to false");
        }

        // 準備第一個 Tetris 清除的板面
        Color[][] grid = board.getGrid();
        grid[0][0] = Color.RED; // 非清除方塊，避免 Perfect Clear
        for (int r = Board.ROWS - 4; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS - 1; c++) {
                grid[r][c] = Color.BLUE;
            }
        }

        // 1st Tetris
        Piece iPiece1 = new Piece(Tetromino.I, Board.ROWS - 4, 7, 1);
        setField(engine, "currentPiece", iPiece1);
        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        int score1 = (int) getField(engine, "score");
        boolean b2b1 = (boolean) getField(engine, "isBackToBackActive");

        if (score1 != 800) {
            throw new Exception("1st Tetris score should be 800, got: " + score1);
        }
        if (!b2b1) {
            throw new Exception("isBackToBackActive should be true after 1st Tetris");
        }

        // 2nd Tetris (連續)
        for (int r = Board.ROWS - 4; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS - 1; c++) {
                grid[r][c] = Color.BLUE;
            }
        }

        Piece iPiece2 = new Piece(Tetromino.I, Board.ROWS - 4, 7, 1);
        setField(engine, "currentPiece", iPiece2);
        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        int score2 = (int) getField(engine, "score");
        boolean b2b2 = (boolean) getField(engine, "isBackToBackActive");

        if (score2 != 2050) {
            throw new Exception("2nd Tetris (B2B) score should sum up to 2050 (800 + 1200 + 50 combo), got: " + score2);
        }
        if (!b2b2) {
            throw new Exception("isBackToBackActive should remain true after consecutive Tetris");
        }

        // 單行消除中斷 B2B
        for (int c = 0; c < Board.COLS - 1; c++) {
            grid[Board.ROWS - 1][c] = Color.BLUE;
        }
        Piece iPiece3 = new Piece(Tetromino.I, Board.ROWS - 2, 6, 0);
        setField(engine, "currentPiece", iPiece3);
        invokeMethod(engine, "freezeAndSpawn", new Class<?>[0], new Object[0]);

        boolean b2b3 = (boolean) getField(engine, "isBackToBackActive");
        if (b2b3) {
            throw new Exception("isBackToBackActive should be false after a normal single line clear");
        }

        System.out.println("  - testBackToBackScoring: PASSED");
    }

    private static void testGarbageRuleTable() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // 使用表驅動的方式驗證不同情境應送出的垃圾行數
        assertGarbageLines(engine, 1, GameEngine.TSpinType.NONE, false, 1, 0, "single");
        assertGarbageLines(engine, 2, GameEngine.TSpinType.NONE, false, 1, 1, "double");
        assertGarbageLines(engine, 3, GameEngine.TSpinType.NONE, false, 1, 2, "triple");
        assertGarbageLines(engine, 4, GameEngine.TSpinType.NONE, false, 1, 4, "tetris");

        assertGarbageLines(engine, 1, GameEngine.TSpinType.REGULAR, false, 1, 2, "t-spin single");
        assertGarbageLines(engine, 2, GameEngine.TSpinType.REGULAR, false, 1, 4, "t-spin double");
        assertGarbageLines(engine, 3, GameEngine.TSpinType.REGULAR, false, 1, 6, "t-spin triple");

        assertGarbageLines(engine, 1, GameEngine.TSpinType.MINI, false, 1, 1, "mini single");
        assertGarbageLines(engine, 2, GameEngine.TSpinType.MINI, false, 1, 2, "mini double");

        assertGarbageLines(engine, 4, GameEngine.TSpinType.NONE, true, 1, 5, "b2b tetris");
        assertGarbageLines(engine, 2, GameEngine.TSpinType.NONE, false, 3, 3, "combo double");

        System.out.println("  - testGarbageRuleTable: PASSED");
    }

    private static void assertGarbageLines(GameEngine engine, int lines, GameEngine.TSpinType tSpinType, boolean isB2B, int comboCount, int expected, String label) throws Exception {
        Object result = invokeMethod(engine, "getGarbageLinesToSend", new Class<?>[] { int.class, GameEngine.TSpinType.class, boolean.class, int.class }, new Object[] { lines, tSpinType, isB2B, comboCount });
        int actual = (int) result;
        if (actual != expected) {
            throw new Exception("Garbage rule mismatch for " + label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void testComboAchievementSync() throws Exception {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        GameEngine engine = new GameEngine(board, panel);

        // 確保測試開始時成就還未解鎖
        setAchievementUnlocked("combo_master", false);

        // 模擬已經有 4 連擊
        setField(engine, "gameState", GameEngine.GameState.PLAYING);
        setField(engine, "comboCount", 4);
        setField(engine, "maxCombo", 4);

        // 準備一個能完成底列的 O 方塊，以觸發第 5 次連擊
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
