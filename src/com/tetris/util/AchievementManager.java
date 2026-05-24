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
    private static final String FILE_NAME = "achievements.csv";
    private static GameEngine gameEngine = null;

    public static class AchievementInfo {
        public String id;
        public String title;
        public String desc;
        public String requirement;
        public boolean unlocked;

        public AchievementInfo(String id, String title, String desc, String requirement) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.requirement = requirement;
            this.unlocked = false;
        }
    }

    private static final Map<String, AchievementInfo> achievements = new LinkedHashMap<>();

    static {
        // Define all 10 achievements
        add("first_steps", "初試身手", "完成基礎移動與軟降教學關卡", "通過教學關卡 1");
        add("tspin_master", "T-Spin 大師", "通過所有教學與 T-Spin 練習關卡", "通過教學關卡 7");
        add("first_clear", "初次消行", "在一般遊戲中成功消除任一行", "消除 ≥ 1 行");
        add("tetris_clear", "完美消四行", "在一般遊戲中單次消除 4 行", "單次消除 4 行");
        add("combo_master", "連擊狂人", "在一般遊戲中達成 5 次以上 Combo 連擊", "Combo 連擊數 ≥ 5");
        add("tspin_regular", "旋轉乾坤", "在一般遊戲中觸發一次 Regular T-Spin", "觸發 Regular T-Spin");
        add("perfect_clear", "全身退場", "在一般遊戲中達成 Perfect Clear 版面全消", "版面完全清空");
        add("sprint_finisher", "長跑選手", "成功完成 40 行 SPRINT 競速模式", "完成 40 行競速");
        add("score_master", "分數達人", "在 ENDLESS 無盡模式單局獲得 50,000 以上高分", "單局分數 ≥ 50,000");
        add("survival_master", "生存大師", "在 SURVIVAL 生存模式中存活 3 分鐘 (180秒) 以上", "生存時間 ≥ 180 秒");
    }

    private static void add(String id, String title, String desc, String requirement) {
        achievements.put(id, new AchievementInfo(id, title, desc, requirement));
    }

    private static Path getFilePath() {
        return Paths.get(System.getProperty("user.home"), ".tetris", FILE_NAME);
    }

    public static void init(GameEngine engine) {
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
            
            // Trigger toast notification in UI
            if (gameEngine != null && gameEngine.getPanel() != null) {
                gameEngine.getPanel().showAchievementToast(info.title, info.desc);
            }
        }
    }

    public static void save() {
        Path path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                for (AchievementInfo info : achievements.values()) {
                    writer.write(info.id + "=" + info.unlocked + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save achievements: " + e.getMessage());
        }
    }

    public static void load() {
        Path path = getFilePath();
        if (!Files.exists(path)) {
            // Reset all to locked if file does not exist
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
                        info.unlocked = Boolean.parseBoolean(val);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load achievements: " + e.getMessage());
        }
    }
}
