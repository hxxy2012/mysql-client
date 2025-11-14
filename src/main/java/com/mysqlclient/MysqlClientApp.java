package com.mysqlclient;

import com.mysqlclient.service.ConnectionManager;
import com.mysqlclient.ui.EnhancedMainWindow;
import com.mysqlclient.util.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MySQL客户端主应用程序 - 商用级版本
 */
public class MysqlClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 创建增强版主窗口
            EnhancedMainWindow mainWindow = new EnhancedMainWindow(primaryStage);
            Scene scene = new Scene(mainWindow.getRoot(), 1280, 800);

            // 应用主题
            ThemeManager themeManager = ThemeManager.getInstance();
            themeManager.setScene(scene);

            // 设置窗口属性
            primaryStage.setTitle("MySQL Client Pro - Professional Database Management Tool");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);

            // 窗口关闭时清理资源
            primaryStage.setOnCloseRequest(event -> {
                ConnectionManager.getInstance().closeAllConnections();
            });

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        // 应用关闭时清理资源
        ConnectionManager.getInstance().closeAllConnections();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
