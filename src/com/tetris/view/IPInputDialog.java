package com.tetris.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class IPInputDialog extends JDialog {
    private String ipAddress = null;
    private final JTextField ipField;
    private boolean isConfirmed = false;

    public IPInputDialog(Frame parent, String titleText, String defaultIP) {
        super(parent, true); // Modal dialog
        setUndecorated(true);
        setSize(380, 190);
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

                // Glowing neon border
                g2d.setColor(new Color(0, 255, 255, 50));
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
                
                g2d.setColor(new Color(0, 255, 255));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

                // Subtitle/Title
                g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2d.setColor(new Color(0, 255, 255));
                g2d.drawString(titleText, 25, 40);
            }
        };
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(15, 15, 22));

        // IP input label
        JLabel promptLabel = new JLabel(com.tetris.util.LanguageManager.get("輸入對手 IP Address:", "Opponent IP:"));
        promptLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        promptLabel.setForeground(new Color(180, 180, 200));
        promptLabel.setBounds(25, 60, 330, 20);
        mainPanel.add(promptLabel);

        // Custom styled Text Field
        ipField = new JTextField(defaultIP);
        ipField.setBounds(25, 85, 330, 32);
        ipField.setFont(new Font("SansSerif", Font.BOLD, 15));
        ipField.setBackground(new Color(8, 8, 12));
        ipField.setForeground(Color.WHITE);
        ipField.setCaretColor(Color.WHITE);
        ipField.setSelectedTextColor(Color.BLACK);
        ipField.setSelectionColor(new Color(0, 255, 255));
        ipField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 255, 255, 120), 1),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));
        
        // Add focus effect
        ipField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ipField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 255, 255), 2),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                ipField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 255, 255, 120), 1),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
            }
        });
        mainPanel.add(ipField);

        // Custom stylized Connect button
        JButton btnConnect = new JButton(com.tetris.util.LanguageManager.get("建立連線 (CONNECT)", "CONNECT")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isOver = getModel().isRollover();
                g2d.setColor(isOver ? new Color(0, 255, 255, 55) : new Color(0, 255, 255, 25));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2d.setColor(isOver ? Color.WHITE : new Color(0, 255, 255));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2d.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int textH = fm.getAscent();
                g2d.drawString(getText(), (getWidth() - textW) / 2, (getHeight() + textH) / 2 - 2);
            }
        };
        btnConnect.setBounds(25, 135, 155, 30);
        btnConnect.setContentAreaFilled(false);
        btnConnect.setBorderPainted(false);
        btnConnect.setFocusPainted(false);
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.addActionListener(e -> {
            isConfirmed = true;
            ipAddress = ipField.getText().trim();
            dispose();
        });
        mainPanel.add(btnConnect);

        // Custom stylized Cancel button
        JButton btnCancel = new JButton(com.tetris.util.LanguageManager.get("取消 (CANCEL)", "CANCEL")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isOver = getModel().isRollover();
                g2d.setColor(isOver ? new Color(255, 100, 100, 45) : new Color(255, 100, 100, 20));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2d.setColor(isOver ? Color.WHITE : new Color(255, 100, 100));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2d.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int textH = fm.getAscent();
                g2d.drawString(getText(), (getWidth() - textW) / 2, (getHeight() + textH) / 2 - 2);
            }
        };
        btnCancel.setBounds(200, 135, 155, 30);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> {
            isConfirmed = false;
            dispose();
        });
        mainPanel.add(btnCancel);

        // Bind Enter key to Confirm and ESC to Cancel
        ipField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    isConfirmed = true;
                    ipAddress = ipField.getText().trim();
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    isConfirmed = false;
                    dispose();
                }
            }
        });

        getContentPane().add(mainPanel);
    }

    public String getInputIP() {
        setVisible(true);
        return isConfirmed ? ipAddress : null;
    }
}
