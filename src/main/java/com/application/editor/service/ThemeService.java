package com.application.editor.service;

import com.application.editor.adapter.AceEditorAdapter;
import com.application.editor.model.Theme;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for applying application themes to JavaFX views and Ace Editor instances.
 */
public class ThemeService {
    private static final Logger logger = LoggerFactory.getLogger(ThemeService.class);

    private Theme currentTheme = Theme.DARK;

    public ThemeService(Theme theme) {
        this.currentTheme = theme;
    }
    public ThemeService() {}

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(Theme newTheme) {
        this.currentTheme = newTheme;
    }

    public void setCurrentTheme(Theme newTheme, Runnable runnable) {
        this.currentTheme = newTheme;
        runnable.run();
    }
}