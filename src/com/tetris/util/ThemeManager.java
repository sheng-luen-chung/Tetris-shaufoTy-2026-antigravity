package com.tetris.util;

import java.awt.Color;

public class ThemeManager {
    public enum Theme {
        CYBERPUNK_NEON("CYBERPUNK", new Color[] {
            new Color(0, 255, 255),   // I: Cyan
            new Color(30, 80, 255),   // J: Neon Blue
            new Color(255, 140, 0),   // L: Orange
            new Color(255, 220, 0),   // O: Yellow
            new Color(0, 255, 100),   // S: Green
            new Color(180, 50, 255),  // T: Purple
            new Color(255, 50, 50)    // Z: Red
        }),
        RETRO_GAMEBOY("GAMEBOY", new Color[] {
            new Color(15, 56, 15),    // I: Dark Forest Green
            new Color(48, 98, 48),    // J: Medium Green
            new Color(139, 172, 15),  // L: Light Green
            new Color(155, 188, 15),  // O: Very Light Green
            new Color(48, 98, 48),    // S: Medium Green
            new Color(139, 172, 15),  // T: Light Green
            new Color(15, 56, 15)     // Z: Dark Forest Green
        }),
        CLASSIC_ARCADE("ARCADE", new Color[] {
            Color.CYAN,
            Color.BLUE,
            Color.ORANGE,
            Color.YELLOW,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED
        }),
        MIDNIGHT_STEALTH("STEALTH", new Color[] {
            new Color(130, 220, 220), // I: Pastel Cyan
            new Color(140, 170, 230), // J: Pastel Blue
            new Color(240, 180, 120), // L: Pastel Orange
            new Color(240, 230, 140), // O: Pastel Yellow
            new Color(140, 220, 160), // S: Pastel Green
            new Color(200, 160, 230), // T: Pastel Purple
            new Color(230, 140, 140)  // Z: Pastel Red
        });

        private final String label;
        private final Color[] colors;

        Theme(String label, Color[] colors) {
            this.label = label;
            this.colors = colors;
        }

        public String getLabel() {
            return label;
        }

        public Color getColor(int index) {
            return colors[index % colors.length];
        }
    }

    private static Theme currentTheme = Theme.CYBERPUNK_NEON;

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(Theme theme) {
        if (theme != null) {
            currentTheme = theme;
        }
    }

    public static void nextTheme() {
        Theme[] values = Theme.values();
        currentTheme = values[(currentTheme.ordinal() + 1) % values.length];
    }

    // Color of the grid lines based on theme
    public static Color getGridLineColor() {
        switch (currentTheme) {
            case RETRO_GAMEBOY:
                return new Color(48, 98, 48, 60);
            case CLASSIC_ARCADE:
                return new Color(100, 100, 100, 80);
            case MIDNIGHT_STEALTH:
                return new Color(255, 255, 255, 15);
            case CYBERPUNK_NEON:
            default:
                return new Color(40, 40, 40);
        }
    }

    // Border and UI accents for dashboard
    public static Color getThemeAccentColor() {
        switch (currentTheme) {
            case RETRO_GAMEBOY:
                return new Color(15, 56, 15);
            case CLASSIC_ARCADE:
                return Color.RED;
            case MIDNIGHT_STEALTH:
                return new Color(200, 160, 230);
            case CYBERPUNK_NEON:
            default:
                return new Color(0, 255, 255);
        }
    }

    public static Color getGameBoyDarkColor() {
        return new Color(15, 56, 15);
    }

    public static Color getMappedColor(Color originalColor) {
        if (originalColor == null) return null;
        
        int alpha = originalColor.getAlpha();
        int rgb = originalColor.getRGB() & 0xFFFFFF;
        
        int cyanRGB = Color.CYAN.getRGB() & 0xFFFFFF;
        int blueRGB = Color.BLUE.getRGB() & 0xFFFFFF;
        int orangeRGB = Color.ORANGE.getRGB() & 0xFFFFFF;
        int yellowRGB = Color.YELLOW.getRGB() & 0xFFFFFF;
        int greenRGB = Color.GREEN.getRGB() & 0xFFFFFF;
        int magentaRGB = Color.MAGENTA.getRGB() & 0xFFFFFF;
        int redRGB = Color.RED.getRGB() & 0xFFFFFF;
        
        int shapeIndex = -1;
        if (rgb == cyanRGB) shapeIndex = 0;
        else if (rgb == blueRGB) shapeIndex = 1;
        else if (rgb == orangeRGB) shapeIndex = 2;
        else if (rgb == yellowRGB) shapeIndex = 3;
        else if (rgb == greenRGB) shapeIndex = 4;
        else if (rgb == magentaRGB) shapeIndex = 5;
        else if (rgb == redRGB) shapeIndex = 6;
        
        if (shapeIndex == -1) {
            return originalColor;
        }
        
        Color mapped = currentTheme.getColor(shapeIndex);
        if (alpha < 255) {
            return new Color(mapped.getRed(), mapped.getGreen(), mapped.getBlue(), alpha);
        }
        return mapped;
    }
}
