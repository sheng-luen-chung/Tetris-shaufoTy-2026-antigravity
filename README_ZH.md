# Tetris

這是一個使用 Java 與 Swing 開發的俄羅斯方塊專案，核心目標是提供完整可玩的 Tetris 體驗，並整合遊戲邏輯、UI、音效、存檔、排行榜、成就與多種遊玩模式。

## 專案特色

- 完整的俄羅斯方塊核心玩法：方塊生成、移動、旋轉、碰撞、鎖定與消行
- 支援多種遊戲模式：`STAGE`、`ENDLESS`、`SURVIVAL`、`SPRINT`、`ULTRA`、`VS_AI`、`LOCAL_PVP`、`NET_PVP`
- 難度切換：可在遊戲中即時切換 `EASY`、`NORMAL`、`HARD`
- 進階判定：`T-Spin`、`Combo`、`Back-to-Back`、垃圾列規則
- UI 與視覺效果：下一塊預覽、Hold、側欄資訊、得分動畫、粒子效果、成就提示
- 遊戲周邊功能：本地排行榜、存檔、音效與背景音樂、語言切換、主題切換、色盲模式
- 測試支援：內建遊戲邏輯測試，可獨立執行驗證核心規則

## 操作說明

- 預設按鍵設定：
    - 左右方向鍵：移動方塊
    - 上方向鍵：旋轉方塊
    - 下方向鍵：加速下落
    - 空白鍵：Hard Drop，直接落到底並鎖定
    - P 或 Esc：暫停或恢復遊戲

- 若不想使用預設鍵位，可於設定中自行綁定鍵位

## 已實作功能

- 七種 Tetromino 方塊與基本移動、旋轉機制
- 消行與計分
- 難度速度控制
- 下一塊預覽與 Hold
- 暫停、結束判定與重新開始流程
- 排行榜管理
- 存檔與設定保存
- 音效與背景音樂管理
- AI 對戰與網路對戰相關模組
- 主題、語言與可視化調整功能
- 單元測試與測試啟動器

## 專案結構

```text
Tetris/
  src/
    com/tetris/main/Main.java
    com/tetris/controller/
      GameEngine.java
      InputHandler.java
      LeaderboardManager.java
    com/tetris/model/
      Board.java
      GameMode.java
      LeaderboardEntry.java
      Piece.java
      Tetromino.java
    com/tetris/util/
      AchievementManager.java
      LanguageManager.java
      NetworkManager.java
      SaveManager.java
      SoundManager.java
      TetrisAI.java
      ThemeManager.java
    com/tetris/view/
      GamePanel.java
      IPInputDialog.java
      MessageDialog.java
    com/tetris/test/
      GameLogicTest.java
      TestRunner.java
  README.md
  README_NEW.md
  build_and_run.bat
  run_tests.bat
  manifest.txt
```

## 執行需求

- 直接執行：`Tetris.jar`
- 執行 `Tetris.jar` 時需要安裝 `JRE 17` 或以上
- 若要自行編譯原始碼，則需要 `JDK 17` 或以上
- Windows 環境可直接使用提供的批次檔
- 其他平台可用 `javac` 與 `java` 手動編譯執行

## 啟動方式

### 方式一：使用批次檔

在專案根目錄下執行：

```bat
build_and_run.bat
```

### 方式二：手動編譯與執行

```bash
mkdir bin
javac -d bin src/com/tetris/main/Main.java src/com/tetris/controller/*.java src/com/tetris/model/*.java src/com/tetris/view/*.java src/com/tetris/util/*.java src/com/tetris/test/*.java
java -cp bin com.tetris.main.Main
```

若要依照 `manifest.txt` 打包成可執行 JAR，也可把 `Main-Class` 指向 `com.tetris.main.Main`。

## 測試

如需執行遊戲邏輯測試，可使用專案提供的測試腳本：

```bat
run_tests.bat
```

或直接執行測試入口類別 `com.tetris.test.TestRunner`。

## 版本資訊

- 目前專案版本：3.0.2

## 備註