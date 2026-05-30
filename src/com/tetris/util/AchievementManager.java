package com.tetris.util;

import com.tetris.controller.GameEngine;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AchievementManager {
    // File name used to persist unlocked achievements in the user's home directory
    private static final String FILE_NAME = "achievements.csv";

    // Reference to the running GameEngine so the manager can trigger UI toasts
    private static GameEngine gameEngine = null;

    /**
     * Data container for a single achievement.
     * Contains localized title, description and requirement strings (ZH/EN),
     * an identifier, and an unlocked flag.
     */
    public static class AchievementInfo {
        public String id;
        public String titleZh;
        public String titleEn;
        public String descZh;
        public String descEn;
        public String reqZh;
        public String reqEn;
        public boolean unlocked;

        /**
         * Construct an AchievementInfo with localized fields.
         */
        public AchievementInfo(String id, String titleZh, String titleEn, String descZh, String descEn, String reqZh, String reqEn) {
            this.id = id;
            this.titleZh = titleZh;
            this.titleEn = titleEn;
            this.descZh = descZh;
            this.descEn = descEn;
            this.reqZh = reqZh;
            this.reqEn = reqEn;
            this.unlocked = false;
        }

        /**
         * Return the title in the currently selected language.
         */
        public String getTitle() {
            return LanguageManager.get(titleZh, titleEn);
        }

        /**
         * Return the description in the currently selected language.
         */
        public String getDesc() {
            return LanguageManager.get(descZh, descEn);
        }

        /**
         * Return the requirement text in the currently selected language.
         */
        public String getRequirement() {
            return LanguageManager.get(reqZh, reqEn);
        }
    }

    private static final Map<String, AchievementInfo> achievements = new LinkedHashMap<>();

    static {
        // Define all achievements used by the game. Each call registers
        // an achievement id along with localized text for display.
        add("first_steps", "初試身手", "First Steps", "完成基礎移動與軟降教學關卡", "Complete basic movement and soft drop tutorial", "通過教學關卡 1", "Clear Tutorial Level 1");
        add("tspin_master", "T-Spin 大師", "T-Spin Master", "通過所有教學與 T-Spin 練習關卡", "Clear all tutorial levels", "通過教學關卡 7", "Clear Tutorial Level 7");
        add("first_clear", "初次消行", "First Clear", "在一般遊戲中成功消除任一行", "Clear any line in a normal game", "消除 ≥ 1 行", "Clear >= 1 Line");
        add("tetris_clear", "完美消四行", "Tetris!", "在一般遊戲中單次消除 4 行", "Clear 4 lines at once in a normal game", "單次消除 4 行", "Clear 4 lines in a single action");
        add("combo_master", "連擊狂人", "Combo Master", "在一般遊戲中達成 5 次以上 Combo 連擊", "Achieve 5 or more Combos in a normal game", "Combo 連擊數 ≥ 5", "Combo count >= 5");
        add("tspin_regular", "旋轉乾坤", "T-Spin Masterclass", "在一般遊戲中觸發一次 Regular T-Spin", "Trigger a Regular T-Spin in a normal game", "觸發 Regular T-Spin", "Trigger a Regular T-Spin");
        add("perfect_clear", "全身退場", "Perfect Clear", "在一般遊戲中達成 Perfect Clear 版面全消", "Achieve a Perfect Clear in a normal game", "版面完全清空", "Completely clear the board");
        add("sprint_finisher", "長跑選手", "Sprint Finisher", "成功完成 40 行 SPRINT 競速模式", "Complete the 40-line Sprint mode", "完成 40 行競速", "Finish 40-line Sprint");
        add("score_master", "分數達人", "Score Master", "在 ENDLESS 無盡模式單局獲得 50,000 以上高分", "Get over 50,000 points in a single Endless game", "單局分數 ≥ 50,000", "Score >= 50,000");
        add("survival_master", "生存大師", "Survival Expert", "在 SURVIVAL 生存模式中存活 3 分鐘 (180秒) 以上", "Survive for 3 minutes (180 seconds) or more in Survival mode", "生存時間 ≥ 180 秒", "Survive time >= 180s");
    }

    private static void add(String id, String titleZh, String titleEn, String descZh, String descEn, String reqZh, String reqEn) {
        achievements.put(id, new AchievementInfo(id, titleZh, titleEn, descZh, descEn, reqZh, reqEn));
    }

    private static Path getFilePath() {
        // Store the achievements file under the user's home directory in a
        // hidden `.tetris` folder so it persists across runs.
        return Paths.get(System.getProperty("user.home"), ".tetris", FILE_NAME);
    }

    public static void init(GameEngine engine) {
        // Save a reference to the engine so UI notifications can be shown
        // and then load persisted achievement state from disk.
        gameEngine = engine;
        load();
    }

    public static List<AchievementInfo> getAchievements() {
        return new ArrayList<>(achievements.values());
    }

    public static boolean isUnlocked(String id) {
        AchievementInfo info = achievements.get(id);
        return info != null && info.unlocked;
    }

    public static void unlockAchievement(String id) {
        AchievementInfo info = achievements.get(id);
        if (info != null && !info.unlocked) {
            info.unlocked = true;
            save();
            // Trigger a UI toast if the game engine is available. This gives
            // immediate feedback to the player that they unlocked an achievement.
            if (gameEngine != null && gameEngine.getPanel() != null) {
                gameEngine.getPanel().showAchievementToast(info.getTitle(), info.getDesc());
            }
        }
    }

    public static void save() {
        Path path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                for (AchievementInfo info : achievements.values()) {
                    // Persist each achievement as `id=true/false` on its own line.
                    writer.write(info.id + "=" + info.unlocked + "\n");
                }
            }
        } catch (IOException e) {
            // Log saving failures to stderr; nothing else should crash the game
            // if achievements cannot be written.
            System.err.println("Failed to save achievements: " + e.getMessage());
        }
    }

    public static void load() {
        Path path = getFilePath();
        if (!Files.exists(path)) {
            // If the file doesn't exist yet, keep all achievements locked.
            for (AchievementInfo info : achievements.values()) {
                info.unlocked = false;
            }
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String id = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    AchievementInfo info = achievements.get(id);
                    if (info != null) {
                        // Parse and apply the saved unlocked state for this id.
                        info.unlocked = Boolean.parseBoolean(val);
                    }
                }
            }
        } catch (Exception e) {
            // If loading fails, log the error and leave achievements at
            // their default (locked) state so gameplay is not interrupted.
            System.err.println("Failed to load achievements: " + e.getMessage());
        }
    }
}
