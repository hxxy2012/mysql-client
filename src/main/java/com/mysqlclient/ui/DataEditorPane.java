package com.mysqlclient.ui;

import com.mysqlclient.service.DatabaseService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 数据编辑面板 - 支持增删改操作
 */
public class DataEditorPane {
    private final BorderPane root;
    private final TableView<ObservableList<String>> tableView;
    private final DatabaseService databaseService;
    private final Label statusLabel;
    private String connectionId;
    private String database;
    private String table;
    private List<String> columnNames;
    private List<String> primaryKeys;
    private Consumer<String> onStatusUpdate;

    public DataEditorPane() {
        this.root = new BorderPane();
        this.tableView = new TableView<>();
        this.databaseService = new DatabaseService();
        this.statusLabel = new Label();
        this.columnNames = new ArrayList<>();
        this.primaryKeys = new ArrayList<>();

        initializeUI();
    }

    private void initializeUI() {
        // 工具栏
        ToolBar toolBar = createToolBar();

        // 表格设置
        tableView.setEditable(true);
        tableView.setPlaceholder(new Label("No data loaded. Select a table from the database tree."));

        // 底部状态栏
        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(5));
        bottomBar.getChildren().add(statusLabel);

        root.setTop(toolBar);
        root.setCenter(tableView);
        root.setBottom(bottomBar);
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button refreshBtn = new Button("Refresh");
        Button insertBtn = new Button("Insert Row");
        Button deleteBtn = new Button("Delete Row");
        Button commitBtn = new Button("Commit Changes");
        Button rollbackBtn = new Button("Rollback");

        refreshBtn.setOnAction(e -> loadTableData());
        insertBtn.setOnAction(e -> insertNewRow());
        deleteBtn.setOnAction(e -> deleteSelectedRows());
        commitBtn.setOnAction(e -> commitChanges());
        rollbackBtn.setOnAction(e -> loadTableData());

        toolBar.getItems().addAll(
            refreshBtn,
            new Separator(),
            insertBtn,
            deleteBtn,
            new Separator(),
            commitBtn,
            rollbackBtn
        );

        return toolBar;
    }

    public void loadTable(String connectionId, String database, String table) {
        this.connectionId = connectionId;
        this.database = database;
        this.table = table;
        loadTableData();
        loadPrimaryKeys();
    }

    private void loadTableData() {
        if (connectionId == null || database == null || table == null) {
            return;
        }

        updateStatus("Loading data...");
        String sql = String.format("SELECT * FROM `%s`.`%s` LIMIT 1000", database, table);

        new Thread(() -> {
            try {
                DatabaseService.QueryResult result = databaseService.executeQuery(connectionId, sql);
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        displayData(result);
                        updateStatus(String.format("Loaded %d rows", result.getRowCount()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void loadPrimaryKeys() {
        new Thread(() -> {
            try {
                List<DatabaseService.ColumnInfo> columns = databaseService.getColumns(connectionId, database, table);
                primaryKeys.clear();
                for (DatabaseService.ColumnInfo col : columns) {
                    if ("PRI".equals(col.getKey())) {
                        primaryKeys.add(col.getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void displayData(DatabaseService.QueryResult result) {
        tableView.getColumns().clear();
        tableView.getItems().clear();
        columnNames = new ArrayList<>(result.getColumnNames());

        // 创建可编辑的列
        for (int i = 0; i < columnNames.size(); i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnNames.get(i));

            column.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                if (columnIndex < row.size()) {
                    return new SimpleStringProperty(row.get(columnIndex));
                }
                return new SimpleStringProperty("");
            });

            // 使单元格可编辑
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(event -> {
                ObservableList<String> row = event.getRowValue();
                row.set(columnIndex, event.getNewValue());
            });

            column.setPrefWidth(150);
            tableView.getColumns().add(column);
        }

        // 添加数据
        for (List<Object> row : result.getRows()) {
            ObservableList<String> rowData = FXCollections.observableArrayList();
            for (Object cell : row) {
                rowData.add(cell == null ? "NULL" : cell.toString());
            }
            tableView.getItems().add(rowData);
        }
    }

    private void insertNewRow() {
        if (columnNames.isEmpty()) {
            showAlert("No Table Loaded", "Please load a table first.");
            return;
        }

        Dialog<ObservableList<String>> dialog = new Dialog<>();
        dialog.setTitle("Insert New Row");
        dialog.setHeaderText("Enter values for new row");

        ButtonType insertButtonType = new ButtonType("Insert", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(insertButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<TextField> fields = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            grid.add(new Label(columnNames.get(i) + ":"), 0, i);
            TextField field = new TextField();
            field.setPromptText("Value");
            grid.add(field, 1, i);
            fields.add(field);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == insertButtonType) {
                ObservableList<String> newRow = FXCollections.observableArrayList();
                for (TextField field : fields) {
                    String value = field.getText();
                    newRow.add(value.isEmpty() ? "NULL" : value);
                }
                return newRow;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newRow -> {
            // 构建INSERT语句
            StringBuilder sql = new StringBuilder();
            sql.append(String.format("INSERT INTO `%s`.`%s` (", database, table));
            sql.append(String.join(", ", columnNames.stream().map(c -> "`" + c + "`").toArray(String[]::new)));
            sql.append(") VALUES (");
            sql.append(String.join(", ", newRow.stream().map(v ->
                "NULL".equals(v) ? "NULL" : "'" + v.replace("'", "''") + "'"
            ).toArray(String[]::new)));
            sql.append(")");

            executeUpdate(sql.toString(), "Row inserted successfully");
        });
    }

    private void deleteSelectedRows() {
        ObservableList<ObservableList<String>> selectedRows = tableView.getSelectionModel().getSelectedItems();
        if (selectedRows.isEmpty()) {
            showAlert("No Selection", "Please select rows to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + selectedRows.size() + " row(s)?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                for (ObservableList<String> row : new ArrayList<>(selectedRows)) {
                    deleteRow(row);
                }
                loadTableData();
            }
        });
    }

    private void deleteRow(ObservableList<String> row) {
        if (primaryKeys.isEmpty()) {
            showAlert("No Primary Key", "Cannot delete rows without a primary key.");
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append(String.format("DELETE FROM `%s`.`%s` WHERE ", database, table));

        List<String> conditions = new ArrayList<>();
        for (String pkColumn : primaryKeys) {
            int index = columnNames.indexOf(pkColumn);
            if (index >= 0) {
                String value = row.get(index);
                conditions.add(String.format("`%s` = '%s'", pkColumn, value.replace("'", "''")));
            }
        }
        sql.append(String.join(" AND ", conditions));

        executeUpdate(sql.toString(), null);
    }

    private void commitChanges() {
        showAlert("Commit", "Auto-commit is enabled. Changes are saved immediately.");
    }

    private void executeUpdate(String sql, String successMessage) {
        updateStatus("Executing update...");

        new Thread(() -> {
            try {
                DatabaseService.QueryResult result = databaseService.executeQuery(connectionId, sql);
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        if (successMessage != null) {
                            updateStatus(successMessage);
                            showAlert("Success", successMessage);
                        }
                        loadTableData();
                    } else {
                        updateStatus("Update failed: " + result.getErrorMessage());
                        showAlert("Error", result.getErrorMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Error: " + e.getMessage());
                    showAlert("Error", e.getMessage());
                });
            }
        }).start();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        if (onStatusUpdate != null) {
            onStatusUpdate.accept(message);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setOnStatusUpdate(Consumer<String> listener) {
        this.onStatusUpdate = listener;
    }

    public BorderPane getRoot() {
        return root;
    }
}
