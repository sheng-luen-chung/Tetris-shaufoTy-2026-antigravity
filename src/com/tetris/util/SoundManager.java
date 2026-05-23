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
 */
public class SoundManager {

    private static Clip bgmClip;
    private static float bgmVolume = 0.25f; // 初始 BGM 再小聲一點點 (預設 25%)
    private static float sfxVolume = 0.8f;  // 預設 80%
    private static boolean bgmMuted = false;
    private static boolean sfxMuted = false;
    
    // 使用執行緒池來非同步播放 SFX，避免頻繁建立執行緒造成的資源開銷。
    // 設定為 Daemon 執行緒，使得遊戲關閉時這些執行緒會自動結束。
    private static final ExecutorService sfxExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("Tetris-SFX-Thread");
        return thread;
    });

    /**
     * 循環播放背景音樂 (BGM)。
     * 如果已有背景音樂在播放，會先將其停止並關閉。
     *
     * @param filePath 音訊檔案路徑 (支援 Classpath 資源路徑與實體檔案路徑)
     */
    public static synchronized void playBGM(String filePath) {
        // 先停止並釋放當前的 BGM
        stopBGM();

        try {
            AudioInputStream audioStream = getAudioStream(filePath);
            if (audioStream == null) {
                System.err.println("[SoundManager] BGM 檔案未找到: " + filePath);
                return;
            }

            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            
            // 設定初始音量與靜音狀態
            setClipVolume(bgmClip, bgmVolume, bgmMuted);
            
            // 設定循環播放
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) {
            System.err.println("[SoundManager] 播放 BGM 失敗: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * 停止當前播放的背景音樂 (BGM) 並釋放資源。
     */
    public static synchronized void stopBGM() {
        if (bgmClip != null) {
            try {
                if (bgmClip.isRunning()) {
                    bgmClip.stop();
                }
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
     * 暫停背景音樂 (BGM)。
     * 停止播放（立即生效），不釋放資源也不重設播放位置。
     */
    public static synchronized void pauseBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop(); // Immediate: halts audio output at the OS/driver level
        }
    }

    /**
     * 恢復播放背景音樂 (BGM)。
     * 從暫停處繼續播放（立即生效）。
     */
    public static synchronized void resumeBGM() {
        if (bgmClip != null && !bgmClip.isRunning()) {
            setClipVolume(bgmClip, bgmVolume, bgmMuted); // ensure correct volume before starting
            bgmClip.start();
        }
    }

    /**
     * 暂時啟動 BGM Clip 供設定區預聴音量。
     * 僅在「暫停狀態下開啟設定」時呼叫，讓使用者拉動滑桿時能即時聽到 BGM 的變化。
     * 關閉設定區後應呼叫 pauseBGM() 再次停止。
     */
    public static synchronized void previewBGM() {
        if (bgmClip != null && !bgmClip.isRunning()) {
            setClipVolume(bgmClip, bgmVolume, bgmMuted);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /**
     * 非同步單次播放音效 (SFX)，不會阻塞主執行緒。
     * 播放完畢後會自動關閉並釋放相關音訊資源。
     *
     * @param filePath 音訊檔案路徑 (支援 Classpath 資源路徑與實體檔案路徑)
     */
    public static void playSFX(String filePath) {
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

                // 設定初始音量與靜音狀態
                setClipVolume(clip, sfxVolume, sfxMuted);

                // 為了防止記憶體洩漏，使用 LineListener 在音效播放完畢後自動關閉 Clip 與 AudioInputStream
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
                // 發生異常時確保資源有被關閉
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
     * 獲取音訊輸入串流，支援多種路徑載入策略（包含 Classpath 資源與檔案系統）。
     *
     * @param filePath 音訊路徑
     * @return AudioInputStream 物件，若找不到則回傳 null
     */
    private static AudioInputStream getAudioStream(String filePath) throws UnsupportedAudioFileException, IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }

        // 1. 嘗試從 Classpath 載入 (URL 方式)
        URL resourceUrl = SoundManager.class.getResource(filePath);
        if (resourceUrl != null) {
            return AudioSystem.getAudioInputStream(resourceUrl);
        }

        // 2. 嘗試從 Classpath 載入 (作為 Stream，適用於打包後的 JAR)
        InputStream is = SoundManager.class.getResourceAsStream(filePath);
        if (is != null) {
            return AudioSystem.getAudioInputStream(new BufferedInputStream(is));
        }

        // 3. 嘗試直接從實體檔案系統載入
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return AudioSystem.getAudioInputStream(file);
        }

        // 3.5. 嘗試從開發環境下的 src 目錄載入 (本地開發路徑補償)
        String srcPath = filePath.startsWith("/") ? "src" + filePath : "src/" + filePath;
        File srcFile = new File(srcPath);
        if (srcFile.exists() && srcFile.isFile()) {
            return AudioSystem.getAudioInputStream(srcFile);
        }

        // 4. 嘗試對路徑前綴進行修正 (例如加上或去掉 '/')
        String altPath = filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath;
        
        resourceUrl = SoundManager.class.getResource(altPath);
        if (resourceUrl != null) {
            return AudioSystem.getAudioInputStream(resourceUrl);
        }

        is = SoundManager.class.getResourceAsStream(altPath);
        if (is != null) {
            return AudioSystem.getAudioInputStream(new BufferedInputStream(is));
        }

        return null;
    }

    /**
     * 設定指定 Clip 的音量，若 muted 則設定為最低音量。
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
                    // 將線性音量 [0.0, 1.0] 轉換為分貝對數值
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
        }
    }

    public static synchronized boolean isSFXMuted() {
        return sfxMuted;
    }

    public static synchronized void setSFXMuted(boolean muted) {
        sfxMuted = muted;
    }
}
