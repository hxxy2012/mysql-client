package com.mysqlclient.ui;

import com.mysqlclient.model.ConnectionConfig;
import com.mysqlclient.service.ConnectionManager;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * 主窗口UI
 */
public class MainWindow {
    private final BorderPane root;
    private final DatabaseTreeView databaseTreeView;
    private final SqlEditorPane sqlEditorPane;
    private final StatusBar statusBar;
    private final ConnectionManager connectionManager;

    public MainWindow() {
        this.root = new BorderPane();
        this.connectionManager = ConnectionManager.getInstance();
        this.databaseTreeView = new DatabaseTreeView();
        this.sqlEditorPane = new SqlEditorPane();
        this.statusBar = new StatusBar();

        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        // 创建菜单栏
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // 创建工具栏
        ToolBar toolBar = createToolBar();
        BorderPane topPane = new BorderPane();
        topPane.setTop(menuBar);
        topPane.setBottom(toolBar);
        root.setTop(topPane);

        // 创建左侧数据库浏览器
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        mainSplitPane.getItems().addAll(
                databaseTreeView.getRoot(),
                sqlEditorPane.getRoot()
        );
        mainSplitPane.setDividerPositions(0.2);

        root.setCenter(mainSplitPane);

        // 设置状态栏
        root.setBottom(statusBar.getRoot());
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // 文件菜单
        Menu fileMenu = new Menu("File");
        MenuItem newConnectionItem = new MenuItem("New Connection...");
        MenuItem exitItem = new MenuItem("Exit");

        newConnectionItem.setOnAction(e -> showNewConnectionDialog());
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(newConnectionItem, new SeparatorMenuItem(), exitItem);

        // 编辑菜单
        Menu editMenu = new Menu("Edit");
        MenuItem newQueryItem = new MenuItem("New Query Tab");
        MenuItem executeQueryItem = new MenuItem("Execute Query");

        newQueryItem.setOnAction(e -> sqlEditorPane.addNewTab());
        executeQueryItem.setOnAction(e -> sqlEditorPane.executeCurrentQuery());

        editMenu.getItems().addAll(newQueryItem, executeQueryItem);

        // 帮助菜单
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button newConnBtn = new Button("New Connection");
        Button refreshBtn = new Button("Refresh");
        Button newQueryBtn = new Button("New Query");
        Button executeBtn = new Button("Execute (F5)");

        newConnBtn.setOnAction(e -> showNewConnectionDialog());
        refreshBtn.setOnAction(e -> databaseTreeView.refresh());
        newQueryBtn.setOnAction(e -> sqlEditorPane.addNewTab());
        executeBtn.setOnAction(e -> sqlEditorPane.executeCurrentQuery());

        toolBar.getItems().addAll(
                newConnBtn,
                new Separator(),
                refreshBtn,
                new Separator(),
                newQueryBtn,
                executeBtn
        );

        return toolBar;
    }

    private void setupEventHandlers() {
        // 数据库树选择事件
        databaseTreeView.setOnTableSelected((connectionId, database, table) -> {
            String sql = "SELECT * FROM `" + database + "`.`" + table + "` LIMIT 100;";
            sqlEditorPane.addNewTabWithQuery(table, sql, connectionId);
        });

        // SQL编辑器状态更新
        sqlEditorPane.setOnStatusUpdate(message -> statusBar.setMessage(message));
    }

    private void showNewConnectionDialog() {
        Dialog<ConnectionConfig> dialog = new Dialog<>();
        dialog.setTitle("New MySQL Connection");
        dialog.setHeaderText("Enter connection details");

        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // 创建表单
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("My Connection");
        TextField hostField = new TextField();
        hostField.setPromptText("localhost");
        hostField.setText("localhost");
        TextField portField = new TextField();
        portField.setPromptText("3306");
        portField.setText("3306");
        TextField userField = new TextField();
        userField.setPromptText("root");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("password");

        grid.add(new Label("Connection Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Host:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("Port:"), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label("Username:"), 0, 3);
        grid.add(userField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passwordField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 请求焦点
        Platform.runLater(() -> nameField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                try {
                    ConnectionConfig config = new ConnectionConfig();
                    config.setName(nameField.getText());
                    config.setHost(hostField.getText());
                    config.setPort(Integer.parseInt(portField.getText()));
                    config.setUsername(userField.getText());
                    config.setPassword(ConnectionManager.encodePassword(passwordField.getText()));
                    return config;
                } catch (NumberFormatException e) {
                    showError("Invalid port number");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(config -> {
            // 测试连接
            statusBar.setMessage("Testing connection...");
            new Thread(() -> {
                boolean success = connectionManager.testConnection(config);
                Platform.runLater(() -> {
                    if (success) {
                        connectionManager.addConnection(config);
                        databaseTreeView.addConnection(config);
                        statusBar.setMessage("Connection '" + config.getName() + "' added successfully");
                    } else {
                        showError("Failed to connect to database. Please check your connection settings.");
                        statusBar.setMessage("Connection failed");
                    }
                });
            }).start();
        });
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About MySQL Client");
        alert.setHeaderText("MySQL Client v1.0");
        alert.setContentText(
                "A JavaFX-based MySQL client application\n\n" +
                "Features:\n" +
                "- Connection management with HikariCP\n" +
                "- SQL syntax highlighting\n" +
                "- Database structure browsing\n" +
                "- Query execution and result display\n\n" +
                "Built with JavaFX 21 and Java 17"
        );
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
