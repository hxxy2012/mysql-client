package com.mysqlclient.ui;

import com.mysqlclient.service.DatabaseService;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * SQL编辑器面板
 */
public class SqlEditorPane {
    private final BorderPane root;
    private final TabPane tabPane;
    private final DatabaseService databaseService;
    private Consumer<String> onStatusUpdate;
    private int tabCounter = 1;

    public SqlEditorPane() {
        this.root = new BorderPane();
        this.tabPane = new TabPane();
        this.databaseService = new DatabaseService();

        initializeUI();
        addNewTab();
    }

    private void initializeUI() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        root.setCenter(tabPane);
    }

    public void addNewTab() {
        addNewTabWithQuery("Query " + tabCounter++, "", null);
    }

    public void addNewTabWithQuery(String title, String query, String connectionId) {
        Tab tab = new Tab(title);
        QueryTab queryTab = new QueryTab(query, connectionId);
        tab.setContent(queryTab.getRoot());
        tab.setClosable(true);

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    public void executeCurrentQuery() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getContent() instanceof BorderPane) {
            BorderPane content = (BorderPane) selectedTab.getContent();
            if (content.getUserData() instanceof QueryTab) {
                QueryTab queryTab = (QueryTab) content.getUserData();
                queryTab.executeQuery();
            }
        }
    }

    public void setOnStatusUpdate(Consumer<String> listener) {
        this.onStatusUpdate = listener;
    }

    public BorderPane getRoot() {
        return root;
    }

    /**
     * 查询标签页
     */
    private class QueryTab {
        private final BorderPane root;
        private final SwingNode editorNode;
        private final RSyntaxTextArea textArea;
        private final TableView<javafx.collections.ObservableList<String>> resultTable;
        private final TextArea messageArea;
        private final SplitPane splitPane;
        private String connectionId;

        public QueryTab(String initialQuery, String connectionId) {
            this.root = new BorderPane();
            this.connectionId = connectionId;
            this.editorNode = new SwingNode();
            this.textArea = createSqlEditor(initialQuery);
            this.resultTable = new TableView<>();
            this.messageArea = new TextArea();

            initializeUI();
        }

        private RSyntaxTextArea createSqlEditor(String initialQuery) {
            RSyntaxTextArea editor = new RSyntaxTextArea(20, 60);
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
            editor.setCodeFoldingEnabled(true);
            editor.setAntiAliasingEnabled(true);
            editor.setText(initialQuery);

            // 应用主题
            try {
                Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
                if (theme != null) {
                    theme.apply(editor);
                }
            } catch (IOException | NullPointerException e) {
                // 使用默认主题
            }

            return editor;
        }

        private void initializeUI() {
            // SQL编辑器
            SwingUtilities.invokeLater(() -> {
                RTextScrollPane scrollPane = new RTextScrollPane(textArea);
                scrollPane.setLineNumbersEnabled(true);
                editorNode.setContent(scrollPane);
            });

            // 结果区域
            TabPane resultTabPane = new TabPane();
            resultTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            // 结果表格标签页
            Tab resultTab = new Tab("Results");
            resultTable.setPlaceholder(new Label("No data to display. Execute a query to see results."));
            resultTab.setContent(resultTable);

            // 消息标签页
            Tab messageTab = new Tab("Messages");
            messageArea.setEditable(false);
            messageArea.setWrapText(true);
            messageTab.setContent(messageArea);

            resultTabPane.getTabs().addAll(resultTab, messageTab);

            // 分割面板
            splitPane = new SplitPane();
            splitPane.setOrientation(Orientation.VERTICAL);
            splitPane.getItems().addAll(editorNode, resultTabPane);
            splitPane.setDividerPositions(0.5);

            root.setCenter(splitPane);
            root.setUserData(this);
        }

        public void executeQuery() {
            String sql = textArea.getText().trim();
            if (sql.isEmpty()) {
                updateMessage("Please enter a SQL query.");
                return;
            }

            if (connectionId == null || connectionId.isEmpty()) {
                updateMessage("No connection selected. Please double-click a table in the database tree.");
                showConnectionDialog();
                return;
            }

            updateMessage("Executing query...");
            if (onStatusUpdate != null) {
                onStatusUpdate.accept("Executing query...");
            }

            // 后台执行查询
            new Thread(() -> {
                try {
                    DatabaseService.QueryResult result = databaseService.executeQuery(connectionId, sql);

                    Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            if (result.isUpdateResult()) {
                                updateMessage("Query executed successfully. " + result.getRowCount() +
                                        " row(s) affected. Execution time: " + result.getExecutionTime() + "ms");
                                clearResultTable();
                            } else {
                                displayResults(result);
                                updateMessage("Query executed successfully. " + result.getRowCount() +
                                        " row(s) returned. Execution time: " + result.getExecutionTime() + "ms");
                            }
                            if (onStatusUpdate != null) {
                                onStatusUpdate.accept("Query completed in " + result.getExecutionTime() + "ms");
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        updateMessage("Error: " + e.getMessage());
                        if (onStatusUpdate != null) {
                            onStatusUpdate.accept("Query failed");
                        }
                        clearResultTable();
                    });
                }
            }).start();
        }

        private void displayResults(DatabaseService.QueryResult result) {
            resultTable.getColumns().clear();
            resultTable.getItems().clear();

            // 创建列
            for (int i = 0; i < result.getColumnNames().size(); i++) {
                final int columnIndex = i;
                TableColumn<javafx.collections.ObservableList<String>, String> column =
                        new TableColumn<>(result.getColumnNames().get(i));
                column.setCellValueFactory(param ->
                        new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get(columnIndex)
                        )
                );
                column.setPrefWidth(150);
                resultTable.getColumns().add(column);
            }

            // 添加数据
            for (java.util.List<Object> row : result.getRows()) {
                javafx.collections.ObservableList<String> rowData =
                        javafx.collections.FXCollections.observableArrayList();
                for (Object cell : row) {
                    rowData.add(cell == null ? "NULL" : cell.toString());
                }
                resultTable.getItems().add(rowData);
            }
        }

        private void clearResultTable() {
            resultTable.getColumns().clear();
            resultTable.getItems().clear();
        }

        private void updateMessage(String message) {
            String timestamp = java.time.LocalTime.now().toString();
            messageArea.appendText("[" + timestamp + "] " + message + "\n");
        }

        private void showConnectionDialog() {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Connection");
            alert.setHeaderText("No database connection selected");
            alert.setContentText("Please select a connection by double-clicking a table in the database tree, " +
                    "or manually select a connection from the dropdown.");
            alert.showAndWait();
        }

        public BorderPane getRoot() {
            return root;
        }
    }
}
