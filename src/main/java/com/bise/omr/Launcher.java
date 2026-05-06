package com.bise.omr;

/**
 * Launcher wrapper for JavaFX application.
 * This class is required so the JVM does not enforce the JavaFX module check
 * at startup (which fails when JavaFX JARs are on the classpath via Maven,
 * not on the explicit module path). Use this as the main class in VS Code's
 * launch configuration instead of MainApplication directly.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}
