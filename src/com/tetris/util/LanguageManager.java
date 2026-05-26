package com.tetris.util;

/**
 * 語言管理類別，支援繁體中文與英文的切換。
 */
public class LanguageManager {

    public enum Language {
        ZH("繁體中文", "Traditional Chinese"),
        EN("English", "English");

        private final String labelZh;
        private final String labelEn;

        Language(String labelZh, String labelEn) {
            this.labelZh = labelZh;
            this.labelEn = labelEn;
        }

        public String getLabel() {
            return (currentLanguage == Language.ZH) ? labelZh : labelEn;
        }
    }

    private static volatile Language currentLanguage = Language.ZH;

    public static synchronized Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static synchronized void setCurrentLanguage(Language lang) {
        if (lang != null) {
            currentLanguage = lang;
        }
    }

    public static synchronized void nextLanguage() {
        Language[] values = Language.values();
        currentLanguage = values[(currentLanguage.ordinal() + 1) % values.length];
    }

    /**
     * 依據當前語言回傳對應的字串。
     *
     * @param zh 繁體中文對應字串
     * @param en 英文對應字串
     * @return 當前語言的字串
     */
    public static String get(String zh, String en) {
        return (currentLanguage == Language.ZH) ? zh : en;
    }
}
