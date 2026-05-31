package com.tetris.test;

/**
 * 簡易測試啟動器。
 *
 * 執行 `GameLogicTest.runAllTests()`，並依結果以非零退出碼回報給外部程式或 CI。
 */
public class TestRunner {
    public static void main(String[] args) {
        try {
            // 呼叫集中式測試集合入口，若有錯誤會以例外回報
            GameLogicTest.runAllTests();
            System.exit(0);
        } catch (Throwable t) {
            // 印出錯誤堆疊，並回傳非零狀態碼以供 CI/外部判斷失敗
            System.err.println("TEST RUNNER ENCOUNTERED FAILURE:");
            t.printStackTrace();
            System.exit(1);
        }
    }
}