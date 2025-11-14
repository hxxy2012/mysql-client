package com.mysqlclient.ui;

import com.mysqlclient.model.ConnectionConfig;
import com.mysqlclient.model.DatabaseNode;
import com.mysqlclient.service.ConnectionManager;
import com.mysqlclient.service.DatabaseService;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.List;

/**
 * 数据库树形视图
 */
public class DatabaseTreeView {
    private final BorderPane root;
    private final TreeView<DatabaseNode> treeView;
    private final TreeItem<DatabaseNode> rootItem;
    private final ConnectionManager connectionManager;
    private final DatabaseService databaseService;
    private TableSelectionListener onTableSelected;

    @FunctionalInterface
    public interface TableSelectionListener {
        void onTableSelected(String connectionId, String database, String table);
    }

    public DatabaseTreeView() {
        this.root = new BorderPane();
        this.rootItem = new TreeItem<>(new DatabaseNode("Connections", DatabaseNode.NodeType.CONNECTION));
        this.rootItem.setExpanded(true);
        this.treeView = new TreeView<>(rootItem);
        this.connectionManager = ConnectionManager.getInstance();
        this.databaseService = new DatabaseService();

        initializeUI();
        loadConnections();
    }

    private void initializeUI() {
        treeView.setShowRoot(true);
        treeView.setPrefWidth(250);
        treeView.setMinWidth(150);

        // 双击事件
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<DatabaseNode> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    handleDoubleClick(item);
                }
            }
        });

        // 右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem refreshItem = new MenuItem("Refresh");
        MenuItem disconnectItem = new MenuItem("Disconnect");
        refreshItem.setOnAction(e -> refresh());
        contextMenu.getItems().addAll(refreshItem, disconnectItem);
        treeView.setContextMenu(contextMenu);

        root.setCenter(treeView);
    }

    private void loadConnections() {
        List<ConnectionConfig> connections = connectionManager.getConnections();
        for (ConnectionConfig config : connections) {
            addConnection(config);
        }
    }

    public void addConnection(ConnectionConfig config) {
        TreeItem<DatabaseNode> connItem = new TreeItem<>(
                new DatabaseNode(config.getName(), DatabaseNode.NodeType.CONNECTION, config)
        );
        rootItem.getChildren().add(connItem);

        // 延迟加载数据库列表
        connItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded && connItem.getChildren().isEmpty()) {
                loadDatabases(connItem, config);
            }
        });
    }

    private void loadDatabases(TreeItem<DatabaseNode> connItem, ConnectionConfig config) {
        // 显示加载指示器
        TreeItem<DatabaseNode> loadingItem = new TreeItem<>(
                new DatabaseNode("Loading...", DatabaseNode.NodeType.DATABASE)
        );
        connItem.getChildren().add(loadingItem);

        // 后台加载
        new Thread(() -> {
            try {
                List<String> databases = databaseService.getDatabases(config.getId());
                Platform.runLater(() -> {
                    connItem.getChildren().clear();
                    for (String dbName : databases) {
                        TreeItem<DatabaseNode> dbItem = new TreeItem<>(
                                new DatabaseNode(dbName, DatabaseNode.NodeType.DATABASE)
                        );
                        connItem.getChildren().add(dbItem);

                        // 延迟加载表列表
                        dbItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                            if (isExpanded && dbItem.getChildren().isEmpty()) {
                                loadTables(dbItem, config.getId(), dbName);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connItem.getChildren().clear();
                    TreeItem<DatabaseNode> errorItem = new TreeItem<>(
                            new DatabaseNode("Error: " + e.getMessage(), DatabaseNode.NodeType.DATABASE)
                    );
                    connItem.getChildren().add(errorItem);
                });
            }
        }).start();
    }

    private void loadTables(TreeItem<DatabaseNode> dbItem, String connectionId, String database) {
        // 显示加载指示器
        TreeItem<DatabaseNode> loadingItem = new TreeItem<>(
                new DatabaseNode("Loading...", DatabaseNode.NodeType.TABLE)
        );
        dbItem.getChildren().add(loadingItem);

        // 后台加载
        new Thread(() -> {
            try {
                List<String> tables = databaseService.getTables(connectionId, database);
                Platform.runLater(() -> {
                    dbItem.getChildren().clear();

                    // 添加表文件夹
                    TreeItem<DatabaseNode> tablesFolder = new TreeItem<>(
                            new DatabaseNode("Tables", DatabaseNode.NodeType.TABLES_FOLDER)
                    );
                    tablesFolder.setExpanded(true);
                    dbItem.getChildren().add(tablesFolder);

                    for (String tableName : tables) {
                        TreeItem<DatabaseNode> tableItem = new TreeItem<>(
                                new DatabaseNode(tableName, DatabaseNode.NodeType.TABLE)
                        );
                        tablesFolder.getChildren().add(tableItem);

                        // 延迟加载列信息
                        tableItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                            if (isExpanded && tableItem.getChildren().isEmpty()) {
                                loadColumns(tableItem, connectionId, database, tableName);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dbItem.getChildren().clear();
                    TreeItem<DatabaseNode> errorItem = new TreeItem<>(
                            new DatabaseNode("Error: " + e.getMessage(), DatabaseNode.NodeType.TABLE)
                    );
                    dbItem.getChildren().add(errorItem);
                });
            }
        }).start();
    }

    private void loadColumns(TreeItem<DatabaseNode> tableItem, String connectionId, String database, String table) {
        // 显示加载指示器
        TreeItem<DatabaseNode> loadingItem = new TreeItem<>(
                new DatabaseNode("Loading...", DatabaseNode.NodeType.COLUMN)
        );
        tableItem.getChildren().add(loadingItem);

        // 后台加载
        new Thread(() -> {
            try {
                List<DatabaseService.ColumnInfo> columns = databaseService.getColumns(connectionId, database, table);
                Platform.runLater(() -> {
                    tableItem.getChildren().clear();

                    TreeItem<DatabaseNode> columnsFolder = new TreeItem<>(
                            new DatabaseNode("Columns", DatabaseNode.NodeType.COLUMNS_FOLDER)
                    );
                    columnsFolder.setExpanded(true);
                    tableItem.getChildren().add(columnsFolder);

                    for (DatabaseService.ColumnInfo column : columns) {
                        TreeItem<DatabaseNode> columnItem = new TreeItem<>(
                                new DatabaseNode(column.toString(), DatabaseNode.NodeType.COLUMN)
                        );
                        columnsFolder.getChildren().add(columnItem);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    tableItem.getChildren().clear();
                    TreeItem<DatabaseNode> errorItem = new TreeItem<>(
                            new DatabaseNode("Error: " + e.getMessage(), DatabaseNode.NodeType.COLUMN)
                    );
                    tableItem.getChildren().add(errorItem);
                });
            }
        }).start();
    }

    private void handleDoubleClick(TreeItem<DatabaseNode> item) {
        DatabaseNode node = item.getValue();
        if (node.getType() == DatabaseNode.NodeType.TABLE) {
            // 找到数据库和连接信息
            TreeItem<DatabaseNode> parent = item.getParent(); // Tables folder
            if (parent != null) {
                parent = parent.getParent(); // Database
                if (parent != null) {
                    String database = parent.getValue().getName();
                    TreeItem<DatabaseNode> connItem = parent.getParent(); // Connection
                    if (connItem != null && connItem.getValue().getData() instanceof ConnectionConfig) {
                        ConnectionConfig config = (ConnectionConfig) connItem.getValue().getData();
                        String table = node.getName();
                        if (onTableSelected != null) {
                            onTableSelected.onTableSelected(config.getId(), database, table);
                        }
                    }
                }
            }
        }
    }

    public void refresh() {
        rootItem.getChildren().clear();
        loadConnections();
    }

    public void setOnTableSelected(TableSelectionListener listener) {
        this.onTableSelected = listener;
    }

    public BorderPane getRoot() {
        return root;
    }
}
