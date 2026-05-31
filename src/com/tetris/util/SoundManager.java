package com.tetris.util;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音效管理類別，使用 Java 內建的 javax.sound.sampled 實作。
 * 提供背景音樂 (BGM) 的循環播放與停止，以及音效 (SFX) 的非同步單次播放功能。
 * 同時新增了動態 PCM 音頻合成引擎，支援切換不同的音效套件。
 */
public class SoundManager {

    public enum SoundPack {
        CLASSIC("經典原聲"),
        RETRO_8BIT("復古 8-Bit"),
        CYBER_SYNTH("賽博電子"),
        MINIMAL_POP("極簡微音");

        private final String label;
        SoundPack(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum SoundType {
        MOVE, ROTATE, HOLD, LOCK, CLEAR, GAME_OVER
    }

    private static Clip bgmClip;
    private static String currentBGMPath = null;
    private static float bgmVolume = 0.25f;
    private static float sfxVolume = 0.8f;
    private static boolean bgmMuted = false;
    private static boolean sfxMuted = false;
    private static boolean bgmPaused = false;
    private static SoundPack currentSoundPack = SoundPack.CLASSIC;
    
    // Memory cache for audio file bytes to avoid repeated disk IO
    private static final java.util.Map<String, byte[]> audioCache = new java.util.concurrent.ConcurrentHashMap<>();

    // 使用執行緒池來非同步播放 SFX，避免頻繁建立執行緒造成的資源開銷。
    private static final ExecutorService sfxExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("Tetris-SFX-Thread");
        return thread;
    });

    static {
        preloadResources();
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public static void preloadResources() {
        sfxExecutor.submit(() -> {
            String[] files = { "/resources/bgm.wav", "/resources/clear.wav" };
            for (String file : files) {
                try {
                    InputStream is = SoundManager.class.getResourceAsStream(file);
                    if (is == null) {
                        String alt = file.startsWith("/") ? file.substring(1) : "/" + file;
                        is = SoundManager.class.getResourceAsStream(alt);
                    }
                    if (is == null) {
                        File f = new File(file);
                        if (!f.exists() || !f.isFile()) {
                            String srcPath = file.startsWith("/") ? "src" + file : "src/" + file;
                            f = new File(srcPath);
                        }
                        if (f.exists() && f.isFile()) {
                            is = new java.io.FileInputStream(f);
                        }
                    }
                    if (is != null) {
                        try (InputStream bis = new BufferedInputStream(is)) {
                            byte[] bytes = readAllBytes(bis);
                            audioCache.put(file, bytes);
                            System.out.println("[SoundManager] Preloaded: " + file + " (" + bytes.length + " bytes)");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[SoundManager] Error preloading: " + file + " - " + e.getMessage());
                }
            }
        });
    }

    public static synchronized SoundPack getCurrentSoundPack() {
        return currentSoundPack;
    }

    public static synchronized void setCurrentSoundPack(SoundPack pack) {
        if (pack != null) {
            currentSoundPack = pack;
        }
    }

    public static synchronized void nextSoundPack() {
        SoundPack[] values = SoundPack.values();
        currentSoundPack = values[(currentSoundPack.ordinal() + 1) % values.length];
    }

    /**
     * 循環播放背景音樂 (BGM)。
     */
    public static synchronized void playBGM(String filePath) {
        if (filePath != null && filePath.equals(currentBGMPath) && bgmClip != null && bgmClip.isOpen()) {
            if (!bgmMuted && bgmVolume > 0.0f) {
                if (!bgmClip.isRunning()) {
                    bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                    bgmClip.start();
                }
            }
            return;
        }

        stopBGM();

        try {
            AudioInputStream audioStream = getAudioStream(filePath);
            if (audioStream == null) {
                System.err.println("[SoundManager] BGM 檔案未找到: " + filePath);
                return;
            }

            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            currentBGMPath = filePath;
            
            setClipVolume(bgmClip, bgmVolume, bgmMuted);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            if (!bgmMuted && bgmVolume > 0.0f) {
                bgmClip.start();
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] 播放 BGM 失敗: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * 停止背景音樂。
     */
    public static synchronized void stopBGM() {
        bgmPaused = false;
        currentBGMPath = null;
        if (bgmClip != null) {
            try {
                if (bgmClip.isRunning()) bgmClip.stop();
                bgmClip.close();
            } catch (Exception e) {
                System.err.println("[SoundManager] 停止 BGM 時發生錯誤");
                e.printStackTrace();
            } finally {
                bgmClip = null;
            }
        }
    }

    /**
     * 暫停背景音樂。
     */
    public static synchronized void pauseBGM() {
        bgmPaused = true;
        if (bgmClip != null) {
            if (bgmClip.isRunning()) {
                bgmClip.stop();
            }
        }
    }

    /**
     * 恢復背景音樂。
     */
    public static synchronized void resumeBGM() {
        bgmPaused = false;
        if (bgmClip != null) {
            setClipVolume(bgmClip, bgmVolume, bgmMuted);
            if (!bgmMuted && bgmVolume > 0.0f) {
                if (!bgmClip.isRunning()) {
                    bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                    bgmClip.start();
                }
            }
        }
    }

    /**
     * 非同步單次播放音效 (SFX)。
     */
    public static void playSFX(String filePath) {
        // 如果是消行音效且不是經典包，則改為動態合成的消行音效
        if (filePath != null && filePath.contains("clear.wav") && currentSoundPack != SoundPack.CLASSIC) {
            playSynthSound(SoundType.CLEAR);
            return;
        }

        sfxExecutor.submit(() -> {
            Clip clip = null;
            AudioInputStream audioStream = null;
            try {
                audioStream = getAudioStream(filePath);
                if (audioStream == null) {
                    System.err.println("[SoundManager] SFX 檔案未找到: " + filePath);
                    return;
                }

                clip = AudioSystem.getClip();
                clip.open(audioStream);
                setClipVolume(clip, sfxVolume, sfxMuted);

                final Clip finalClip = clip;
                final AudioInputStream finalStream = audioStream;
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        finalClip.close();
                        try {
                            finalStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                clip.start();
            } catch (Exception e) {
                System.err.println("[SoundManager] 播放 SFX 失敗: " + filePath);
                e.printStackTrace();
                if (clip != null) {
                    try { clip.close(); } catch (Exception ignored) {}
                }
                if (audioStream != null) {
                    try { audioStream.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * 動態播放合成音效，利用 PCM 數據低延遲播放。
     */
    public static void playSynthSound(SoundType type) {
        if (sfxMuted || sfxVolume <= 0.0f) return;

        sfxExecutor.submit(() -> {
            Clip clip = null;
            try {
                byte[] buffer = generateTone(type, currentSoundPack);
                if (buffer == null) return;

                AudioFormat format = new AudioFormat(22050, 8, 1, true, false); // 22050Hz, 8-bit, mono, signed
                clip = AudioSystem.getClip();
                clip.open(format, buffer, 0, buffer.length);
                setClipVolume(clip, sfxVolume, sfxMuted);

                final Clip finalClip = clip;
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        finalClip.close();
                    }
                });

                clip.start();
            } catch (Exception e) {
                System.err.println("[SoundManager] 播放合成音效失敗: " + type);
                e.printStackTrace();
                if (clip != null) {
                    try { clip.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * 根據音效包與事件類型，以數學波形產生 8-bit signed PCM 資料位元組。
     */
    private static byte[] generateTone(SoundType type, SoundPack pack) {
        double sampleRate = 22050.0;
        double duration;
        
        switch (type) {
            case MOVE: duration = 0.04; break;
            case ROTATE: duration = 0.06; break;
            case HOLD: duration = 0.12; break;
            case LOCK: duration = 0.08; break;
            case CLEAR: duration = 0.40; break;
            case GAME_OVER: duration = 0.80; break;
            default: duration = 0.10;
        }
        
        int samples = (int) (sampleRate * duration);
        byte[] buffer = new byte[samples];
        
        for (int i = 0; i < samples; i++) {
            double t = i / sampleRate;
            double value = 0;
            double envelope = 1.0;
            
            // 標準線性衰減封包 (Linear Decay Envelope)
            if (t < duration * 0.1) {
                envelope = t / (duration * 0.1); // 漸入
            } else {
                envelope = 1.0 - (t - duration * 0.1) / (duration * 0.9); // 漸出
            }
            if (envelope < 0) envelope = 0;
            
            if (pack == SoundPack.RETRO_8BIT) {
                // 復古 8-Bit 使用方波 (Square Wave) 與明顯的音高滑音/琶音
                double freq = 440.0;
                if (type == SoundType.MOVE) {
                    freq = 600.0;
                } else if (type == SoundType.ROTATE) {
                    // 音高向上滑移
                    freq = 300.0 + (t / duration) * 400.0;
                } else if (type == SoundType.HOLD) {
                    // 快速升音階琶音
                    double[] notes = {440.0, 554.37, 659.25, 880.0};
                    int idx = (int) (t / (duration / 4.0));
                    freq = notes[Math.min(idx, 3)];
                } else if (type == SoundType.LOCK) {
                    // 音高向下滑移
                    freq = 220.0 - (t / duration) * 120.0;
                } else if (type == SoundType.CLEAR) {
                    // 歡樂大三和弦琶音 C5 -> E5 -> G5 -> C6
                    double[] notes = {523.25, 659.25, 783.99, 1046.50};
                    int idx = (int) (t / (duration / 4.0));
                    freq = notes[Math.min(idx, 3)];
                } else if (type == SoundType.GAME_OVER) {
                    // 悲傷下行琶音
                    double[] notes = {261.63, 196.00, 164.81, 130.81};
                    int idx = (int) (t / (duration / 4.0));
                    freq = notes[Math.min(idx, 3)];
                    freq -= (t / duration) * 30.0; // 額外輕微滑音
                }
                
                double period = 1.0 / freq;
                double phase = (t % period) / period;
                value = (phase < 0.5) ? 1.0 : -1.0;
                value *= 0.25; // 方波較為刺耳，調低基礎音量
                
            } else if (pack == SoundPack.CYBER_SYNTH) {
                // 賽博電子使用三角波 (Triangle Wave) 帶有未來感的滑音
                double freq = 440.0;
                if (type == SoundType.MOVE) {
                    freq = 800.0 - (t / duration) * 400.0;
                } else if (type == SoundType.ROTATE) {
                    freq = 1000.0 - (t / duration) * 600.0;
                } else if (type == SoundType.HOLD) {
                    // 加入顫音調變 (Vibrato)
                    freq = 300.0 + Math.sin(t * Math.PI * 2 * 10) * 40.0 + (t / duration) * 200.0;
                } else if (type == SoundType.LOCK) {
                    freq = 150.0 - (t / duration) * 80.0;
                } else if (type == SoundType.CLEAR) {
                    // 科幻琶音 C5 -> Bb5 -> C6 -> G6
                    double[] notes = {523.25, 932.33, 1046.50, 1567.98};
                    int idx = (int) (t / (duration / 4.0));
                    freq = notes[Math.min(idx, 3)];
                } else if (type == SoundType.GAME_OVER) {
                    freq = 180.0 - (t / duration) * 120.0;
                }
                
                double period = 1.0 / freq;
                double phase = (t % period) / period;
                if (phase < 0.25) {
                    value = phase * 4.0;
                } else if (phase < 0.75) {
                    value = 2.0 - phase * 4.0;
                } else {
                    value = phase * 4.0 - 4.0;
                }
                value *= 0.4;
                
            } else if (pack == SoundPack.MINIMAL_POP) {
                // MINIMAL_POP 使用極清脆的正弦波 (Sine Wave) 指數衰減
                double freq = 500.0;
                if (type == SoundType.MOVE) {
                    freq = 700.0;
                    envelope = Math.exp(-t * 120.0);
                } else if (type == SoundType.ROTATE) {
                    freq = 900.0;
                    envelope = Math.exp(-t * 100.0);
                } else if (type == SoundType.HOLD) {
                    // 雙點擊清脆聲 (Double Pop)
                    freq = (t < duration * 0.5) ? 600.0 : 900.0;
                    double subT = t % (duration * 0.5);
                    envelope = Math.exp(-subT * 80.0);
                } else if (type == SoundType.LOCK) {
                    freq = 200.0;
                    envelope = Math.exp(-t * 60.0);
                } else if (type == SoundType.CLEAR) {
                    // 溫和清澈的敲擊風琴 C6 -> E6 -> G6
                    double[] notes = {1046.50, 1318.51, 1567.98};
                    int idx = (int) (t / (duration / 3.0));
                    freq = notes[Math.min(idx, 2)];
                    envelope = Math.exp(-(t % (duration / 3.0)) * 25.0);
                } else if (type == SoundType.GAME_OVER) {
                    // 漸弱下行琴聲
                    double[] notes = {523.25, 493.88, 440.00, 349.23};
                    int idx = (int) (t / (duration / 4.0));
                    freq = notes[Math.min(idx, 3)];
                    envelope = Math.exp(-(t % (duration / 4.0)) * 15.0);
                }
                
                value = Math.sin(2.0 * Math.PI * freq * t);
            } else {
                // CLASSIC 的輔助 SFX：使用具備街機質感的正弦/三角混合音效，做出懷舊遊戲機的阻尼感
                double freq = 440.0;
                if (type == SoundType.MOVE) {
                    freq = 350.0;
                    envelope = Math.exp(-t * 90.0); // 街機移動聲：稍沉的啵啵聲
                    value = Math.sin(2.0 * Math.PI * freq * t);
                } else if (type == SoundType.ROTATE) {
                    // 旋轉聲：向上的小啾聲
                    freq = 400.0 + (t / duration) * 300.0;
                    envelope = Math.exp(-t * 60.0);
                    // 混合三角波與正弦波，讓音色更有機械感
                    double period = 1.0 / freq;
                    double phase = (t % period) / period;
                    double tri = (phase < 0.5) ? (4.0 * phase - 1.0) : (3.0 - 4.0 * phase);
                    value = 0.5 * tri + 0.5 * Math.sin(2.0 * Math.PI * freq * t);
                } else if (type == SoundType.HOLD) {
                    // 暫存聲：街機特色的雙音快速遞增
                    freq = (t < duration * 0.5) ? 480.0 : 640.0;
                    double subT = t % (duration * 0.5);
                    envelope = Math.exp(-subT * 40.0);
                    value = Math.sin(2.0 * Math.PI * freq * t);
                } else if (type == SoundType.LOCK) {
                    // 鎖定聲：沉悶的落地撞擊
                    freq = 150.0;
                    envelope = Math.exp(-t * 40.0);
                    value = Math.sin(2.0 * Math.PI * freq * t);
                } else if (type == SoundType.CLEAR) {
                    // 消行：經典包通常播放原有的 clear.wav，此處僅作為防呆備份
                    double[] notes = {523.25, 659.25, 783.99, 1046.50};
                    int idx = (int) (t / (duration / 4.0));
                    freq = notes[Math.min(idx, 3)];
                    value = Math.sin(2.0 * Math.PI * freq * t);
                } else if (type == SoundType.GAME_OVER) {
                    // 遊戲結束：緩慢降調的街機蜂鳴聲
                    freq = 300.0 - (t / duration) * 200.0;
                    value = Math.sin(2.0 * Math.PI * freq * t);
                }
            }
            
            double sample = value * envelope * 127.0;
            if (sample > 127) sample = 127;
            if (sample < -128) sample = -128;
            buffer[i] = (byte) sample;
        }
        
        return buffer;
    }

    /**
     * 獲取音訊輸入串流，支援多種路徑載入策略。
     */
    private static AudioInputStream getAudioStream(String filePath) throws UnsupportedAudioFileException, IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }

        // Check memory cache first
        byte[] cachedBytes = audioCache.get(filePath);
        if (cachedBytes != null) {
            return AudioSystem.getAudioInputStream(new java.io.ByteArrayInputStream(cachedBytes));
        }

        // Fallback to load and cache
        InputStream is = null;
        URL resourceUrl = SoundManager.class.getResource(filePath);
        if (resourceUrl != null) {
            is = resourceUrl.openStream();
        } else {
            is = SoundManager.class.getResourceAsStream(filePath);
            if (is == null) {
                File file = new File(filePath);
                if (file.exists() && file.isFile()) {
                    is = new java.io.FileInputStream(file);
                } else {
                    String srcPath = filePath.startsWith("/") ? "src" + filePath : "src/" + filePath;
                    File srcFile = new File(srcPath);
                    if (srcFile.exists() && srcFile.isFile()) {
                        is = new java.io.FileInputStream(srcFile);
                    }
                }
            }
        }

        if (is != null) {
            try (InputStream bis = new BufferedInputStream(is)) {
                byte[] bytes = readAllBytes(bis);
                audioCache.put(filePath, bytes);
                return AudioSystem.getAudioInputStream(new java.io.ByteArrayInputStream(bytes));
            }
        }

        return null;
    }

    /**
     * 設定指定 Clip 的音量。
     */
    private static void setClipVolume(Clip clip, float volume, boolean muted) {
        if (clip == null) return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (muted || volume <= 0.0f) {
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    float dB = (float) (Math.log10(volume) * 20.0);
                    if (dB < min) dB = min;
                    if (dB > max) dB = max;
                    gainControl.setValue(dB);
                }
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] 設定音量時發生錯誤: " + e.getMessage());
        }
    }

    public static synchronized float getBGMVolume() {
        return bgmVolume;
    }

    public static synchronized void setBGMVolume(float volume) {
        bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmClip != null) {
            setClipVolume(bgmClip, bgmVolume, bgmMuted);
            if (!bgmPaused) {
                if (!bgmMuted && bgmVolume > 0.0f) {
                    if (!bgmClip.isRunning()) {
                        bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                        bgmClip.start();
                    }
                } else {
                    if (bgmClip.isRunning()) {
                        bgmClip.stop();
                    }
                }
            }
        }
    }

    public static synchronized float getSFXVolume() {
        return sfxVolume;
    }

    public static synchronized void setSFXVolume(float volume) {
        sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public static synchronized boolean isBGMMuted() {
        return bgmMuted;
    }

    public static synchronized void setBGMMuted(boolean muted) {
        bgmMuted = muted;
        if (bgmClip != null) {
            setClipVolume(bgmClip, bgmVolume, bgmMuted);
            if (!bgmPaused) {
                if (!bgmMuted && bgmVolume > 0.0f) {
                    if (!bgmClip.isRunning()) {
                        bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                        bgmClip.start();
                    }
                } else {
                    if (bgmClip.isRunning()) {
                        bgmClip.stop();
                    }
                }
            }
        }
    }

    public static synchronized boolean isSFXMuted() {
        return sfxMuted;
    }

    public static synchronized void setSFXMuted(boolean muted) {
        sfxMuted = muted;
    }
}
