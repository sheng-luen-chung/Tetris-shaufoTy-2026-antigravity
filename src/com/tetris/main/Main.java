package com.tetris.main;

import com.tetris.controller.GameEngine;
import com.tetris.controller.InputHandler;
import com.tetris.model.Board;
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
        // Game Initialization
        Board board = new Board();

        GamePanel gamePanel = new GamePanel(board);

        GameEngine engine = new GameEngine(board, gamePanel);
        gamePanel.setGameEngine(engine); // Link engine to panel for info display

        InputHandler inputHandler = new InputHandler(engine);
        engine.setInputHandler(inputHandler);

        // Set JFrame
        JFrame frame = new JFrame("Java Tetris Project");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close when clicking "X"
        frame.setResizable(false); // Fixed window size

        frame.add(gamePanel); // Add game panel to the frame
        frame.pack(); // Adjust frame size to fit panel

        frame.setLocationRelativeTo(null); // Center window
        frame.setVisible(true); // Display window

        frame.addKeyListener(inputHandler); // Key listener
        gamePanel.addKeyListener(inputHandler);
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();

        engine.start();
    }

}