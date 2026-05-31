package com.tetris.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MessageDialog extends JDialog {
    public MessageDialog(Frame parent, String titleText, String messageText, boolean isError) {
        super(parent, true); // Modal dialog
        setUndecorated(true);
        setSize(380, 160);
        setLocationRelativeTo(parent);

        // Main panel with custom painting for neon card look
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2d.setColor(new Color(15, 15, 22));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Glowing neon border (Cyan for normal message, Red for error message)
                Color themeColor = isError ? new Color(255, 100, 100) : new Color(0, 255, 255);
                Color glowColor = isError ? new Color(255, 100, 100, 50) : new Color(0, 255, 255, 50);

                g2d.setColor(glowColor);
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
                
                g2d.setColor(themeColor);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

                // Title
                g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2d.setColor(themeColor);
                g2d.drawString(titleText, 25, 35);
            }
        };
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(15, 15, 22));

        // Message text area (transparent and wrapped, to avoid label cutoffs)
        JTextArea messageArea = new JTextArea(messageText);
        messageArea.setFont(new Font("SansSerif", Font.BOLD, 13));
        messageArea.setForeground(new Color(200, 200, 220));
        messageArea.setBackground(new Color(15, 15, 22));
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBounds(25, 55, 330, 50);
        mainPanel.add(messageArea);

        // Custom stylized OK button
        Color buttonThemeColor = isError ? new Color(255, 100, 100) : new Color(0, 255, 255);
        JButton btnOK = new JButton(com.tetris.util.LanguageManager.get("確定 (OK)", "OK")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isOver = getModel().isRollover();
                
                Color fillCol = isOver 
                    ? (isError ? new Color(255, 100, 100, 45) : new Color(0, 255, 255, 55)) 
                    : (isError ? new Color(255, 100, 100, 20) : new Color(0, 255, 255, 25));
                g2d.setColor(fillCol);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                
                g2d.setColor(isOver ? Color.WHITE : buttonThemeColor);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2d.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int textH = fm.getAscent();
                g2d.drawString(getText(), (getWidth() - textW) / 2, (getHeight() + textH) / 2 - 2);
            }
        };
        btnOK.setBounds(115, 115, 150, 30);
        btnOK.setContentAreaFilled(false);
        btnOK.setBorderPainted(false);
        btnOK.setFocusPainted(false);
        btnOK.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOK.addActionListener(e -> dispose());
        mainPanel.add(btnOK);

        // Bind Enter/ESC/Space to close the dialog
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || 
                    e.getKeyCode() == KeyEvent.VK_ESCAPE || 
                    e.getKeyCode() == KeyEvent.VK_SPACE) {
                    dispose();
                }
            }
        };
        btnOK.addKeyListener(keyAdapter);
        
        // Also support closing on JDialog itself if it captures key event
        mainPanel.addKeyListener(keyAdapter);

        // Focus the button so that the user can press Enter/Space immediately
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                btnOK.requestFocusInWindow();
            }
        });

        getContentPane().add(mainPanel);
    }

    public static void show(Frame parent, String titleText, String messageText, boolean isError) {
        MessageDialog dialog = new MessageDialog(parent, titleText, messageText, isError);
        dialog.setVisible(true);
    }
}
