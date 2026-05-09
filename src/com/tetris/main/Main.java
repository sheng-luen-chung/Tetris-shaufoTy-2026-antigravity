package com.tetris.main;

import com.tetris.controller.GameEngine;
import com.tetris.controller.InputHandler;
import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Tetromino;
import com.tetris.view.GamePanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ensure GUI update happens in the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    // Create and show GUI
    private static void createAndShowGUI() {
        Board board = new Board();
        Piece testPiece = new Piece(Tetromino.T);

        GamePanel gamePanel = new GamePanel(board);
        gamePanel.setCurrentPiece(testPiece); // Let Panel know which piece to draw

        GameEngine engine = new GameEngine(board, gamePanel);
        InputHandler inputHandler = new InputHandler(engine);

        // Set JFrame
        JFrame frame = new JFrame("Java Tetris Project");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close when clicking "x"
        frame.setResizable(false); // Fixed window size

        frame.add(gamePanel); // Add game panel to the frame
        frame.pack(); // Adjust frame size to fit panel

        frame.setLocationRelativeTo(null); // Center window
        frame.setVisible(true); // Display window

        frame.addKeyListener(inputHandler); // Key listener

        engine.start();
    }
}