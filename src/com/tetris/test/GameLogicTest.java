package com.tetris.test;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import java.awt.Color;

public class GameLogicTest {

    public static void runAllTests() throws Exception {
        System.out.println("Running Tetris Game Logic Tests...");
        testBoardLineClear();
        testPieceMovement();
        testPieceRotation();
        testCollisionChecking();
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
}
