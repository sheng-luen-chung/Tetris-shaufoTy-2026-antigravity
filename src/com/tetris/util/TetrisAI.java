package com.tetris.util;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import java.awt.Color;

public class TetrisAI {
    // Heuristic Weights (Dellacherie's Algorithm)
    private static final double WEIGHT_HEIGHT = -0.510066;
    private static final double WEIGHT_LINES = 0.760666;
    private static final double WEIGHT_HOLES = -0.35663;
    private static final double WEIGHT_BUMPINESS = -0.184483;

    public static class Move {
        public final int rotationIndex;
        public final int targetCol;
        public final double score;
        public final boolean useHold;

        public Move(int rotationIndex, int targetCol, double score, boolean useHold) {
            this.rotationIndex = rotationIndex;
            this.targetCol = targetCol;
            this.score = score;
            this.useHold = useHold;
        }
    }

    /**
     * Finds the best move (rotation and column) for the current piece (and optionally held piece).
     */
    public static Move findBestMove(Board board, Piece currentPiece, Piece heldPiece, boolean canHold) {
        // Evaluate moves for the current piece
        Move bestCurrentMove = findBestMoveForPiece(board, currentPiece, false);

        if (!canHold) {
            return bestCurrentMove;
        }

        // If held piece is null, we can hold and evaluate the NEXT piece (or just default next type).
        // For simplicity, if heldPiece is null, let's assume we hold, which spawns the next piece.
        // But since we don't know if next piece is better, we can evaluate the heldPiece if it exists.
        if (heldPiece != null) {
            Move bestHeldMove = findBestMoveForPiece(board, heldPiece, true);
            if (bestHeldMove != null && (bestCurrentMove == null || bestHeldMove.score > bestCurrentMove.score)) {
                return bestHeldMove;
            }
        } else {
            // If held piece is empty, holding is generally good if current piece is in a bad spot,
            // but to be safe we can just default to playing the current piece unless it has a very low score
            // and we want to swap it. Let's just evaluate heldPiece if not null.
        }

        return bestCurrentMove;
    }

    private static Move findBestMoveForPiece(Board board, Piece piece, boolean isHoldMove) {
        Move bestMove = null;
        double bestScore = -999999.0;

        // Try all 4 rotations
        for (int r = 0; r < 4; r++) {
            // Find columns boundary for this rotation
            // A piece's rotation index ranges 0..3
            // Create a temporary piece at row 0 (safe starting row) to find horizontal limits
            Piece tempPiece = new Piece(piece.getType(), 0, 3, r);
            if (!board.isValidMove(tempPiece)) {
                // If it collides even at start col at row 0, it might be too crowded
                // but let's see if we can find a valid start column
                boolean foundValid = false;
                for (int c = 0; c < Board.COLS; c++) {
                    tempPiece = new Piece(piece.getType(), 0, c, r);
                    if (board.isValidMove(tempPiece)) {
                        foundValid = true;
                        break;
                    }
                }
                if (!foundValid) continue;
            }

            // Find min/max columns by shifting left and right
            int minCol = tempPiece.getCol();
            Piece leftPiece = new Piece(piece.getType(), 0, tempPiece.getCol(), r);
            while (board.isValidMove(leftPiece)) {
                minCol = leftPiece.getCol();
                leftPiece.move(0, -1);
            }

            int maxCol = tempPiece.getCol();
            Piece rightPiece = new Piece(piece.getType(), 0, tempPiece.getCol(), r);
            while (board.isValidMove(rightPiece)) {
                maxCol = rightPiece.getCol();
                rightPiece.move(0, 1);
            }

            // Test each column in range [minCol, maxCol]
            for (int c = minCol; c <= maxCol; c++) {
                Piece testPiece = new Piece(piece.getType(), 0, c, r);
                if (!board.isValidMove(testPiece)) {
                    continue;
                }

                // Simulate dropping the piece
                while (board.isValidMove(testPiece)) {
                    testPiece.move(1, 0);
                }
                testPiece.move(-1, 0); // step back to last valid position

                // Create a simulated grid copy
                Color[][] tempGrid = new Color[Board.ROWS][Board.COLS];
                Color[][] originalGrid = board.getGrid();
                for (int i = 0; i < Board.ROWS; i++) {
                    System.arraycopy(originalGrid[i], 0, tempGrid[i], 0, Board.COLS);
                }

                // Freeze test piece on simulated grid
                for (int[] cell : testPiece.getAbsoluteCoords()) {
                    int colIdx = cell[0];
                    int rowIdx = cell[1];
                    if (rowIdx >= 0 && rowIdx < Board.ROWS && colIdx >= 0 && colIdx < Board.COLS) {
                        tempGrid[rowIdx][colIdx] = Color.GRAY; // dummy color
                    }
                }

                // Evaluate the grid score
                double score = evaluateGrid(tempGrid);

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = new Move(r, c, score, isHoldMove);
                }
            }
        }

        return bestMove;
    }

    private static double evaluateGrid(Color[][] grid) {
        int linesCleared = 0;
        
        // 1. Simulate lines cleared
        for (int r = Board.ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < Board.COLS; c++) {
                if (grid[r][c] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                linesCleared++;
                // Shift rows down
                for (int row = r; row > 0; row--) {
                    System.arraycopy(grid[row - 1], 0, grid[row], 0, Board.COLS);
                }
                for (int c = 0; c < Board.COLS; c++) {
                    grid[0][c] = null;
                }
                r++; // recheck same row index
            }
        }

        // 2. Column heights & Aggregate height
        int aggregateHeight = 0;
        int[] heights = new int[Board.COLS];
        for (int c = 0; c < Board.COLS; c++) {
            int h = 0;
            for (int r = 0; r < Board.ROWS; r++) {
                if (grid[r][c] != null) {
                    h = Board.ROWS - r;
                    break;
                }
            }
            heights[c] = h;
            aggregateHeight += h;
        }

        // 3. Holes count
        int holes = 0;
        for (int c = 0; c < Board.COLS; c++) {
            boolean blockAbove = false;
            for (int r = 0; r < Board.ROWS; r++) {
                if (grid[r][c] != null) {
                    blockAbove = true;
                } else if (blockAbove && grid[r][c] == null) {
                    holes++;
                }
            }
        }

        // 4. Bumpiness
        int bumpiness = 0;
        for (int c = 0; c < Board.COLS - 1; c++) {
            bumpiness += Math.abs(heights[c] - heights[c + 1]);
        }

        // 5. Final heuristic formula
        return (aggregateHeight * WEIGHT_HEIGHT) 
             + (linesCleared * WEIGHT_LINES) 
             + (holes * WEIGHT_HOLES) 
             + (bumpiness * WEIGHT_BUMPINESS);
    }
}
