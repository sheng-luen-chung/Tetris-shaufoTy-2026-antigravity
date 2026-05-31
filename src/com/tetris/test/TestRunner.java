package com.tetris.test;

public class TestRunner {
    public static void main(String[] args) {
        try {
            GameLogicTest.runAllTests();
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("TEST RUNNER ENCOUNTERED FAILURE:");
            t.printStackTrace();
            System.exit(1); // Non-zero status indicates failure to the OS shell / CI pipeline
        }
    }
}
