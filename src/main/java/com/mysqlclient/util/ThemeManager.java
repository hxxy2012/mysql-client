package com.mysqlclient.util;

import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 主题管理器
 */
public class ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme;
    private Scene scene;
    private final Path configDir;
    private final Path themeConfigFile;

    public enum Theme {
        LIGHT("Light", "/themes/light.css"),
        DARK("Dark", "/themes/dark.css");

        private final String displayName;
        private final String cssPath;

        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssPath() {
            return cssPath;
        }
    }

    private ThemeManager() {
        String userHome = System.getProperty("user.home");
        this.configDir = Paths.get(userHome, ".mysqlclient");
        this.themeConfigFile = configDir.resolve("theme.txt");
        loadSavedTheme();
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        applyTheme(currentTheme);
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        applyTheme(theme);
        saveTheme();
    }

    public Theme getCurrentTheme() {
        return currentTheme != null ? currentTheme : Theme.LIGHT;
    }

    private void applyTheme(Theme theme) {
        if (scene == null || theme == null) {
            return;
        }

        scene.getStylesheets().clear();

        // 加载主题CSS
        String cssPath = theme.getCssPath();
        try {
            String css = getClass().getResource(cssPath).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Failed to load theme: " + e.getMessage());
        }
    }

    private void loadSavedTheme() {
        if (!Files.exists(themeConfigFile)) {
            currentTheme = Theme.LIGHT;
            return;
        }

        try {
            String themeName = Files.readString(themeConfigFile).trim();
            currentTheme = Theme.valueOf(themeName);
        } catch (Exception e) {
            currentTheme = Theme.LIGHT;
        }
    }

    private void saveTheme() {
        try {
            Files.writeString(themeConfigFile, currentTheme.name());
        } catch (IOException e) {
            System.err.println("Failed to save theme preference: " + e.getMessage());
        }
    }
}
