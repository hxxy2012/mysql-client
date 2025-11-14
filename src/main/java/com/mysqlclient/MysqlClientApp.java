package com.mysqlclient;

import com.mysqlclient.service.ConnectionManager;
import com.mysqlclient.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MySQL客户端主应用程序
 */
public class MysqlClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 创建主窗口
            MainWindow mainWindow = new MainWindow();
            Scene scene = new Scene(mainWindow.getRoot(), 1280, 800);

            // 设置窗口属性
            primaryStage.setTitle("MySQL Client");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

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
