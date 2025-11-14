package com.mysqlclient.ui;

import com.mysqlclient.service.ConnectionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 表结构设计器对话框
 */
public class TableDesignerDialog extends Dialog<Boolean> {
    private final String connectionId;
    private final String database;
    private final TextField tableNameField;
    private final TextArea commentField;
    private final TableView<ColumnDefinition> columnsTable;
    private final ObservableList<ColumnDefinition> columns;

    public TableDesignerDialog(String connectionId, String database) {
        this.connectionId = connectionId;
        this.database = database;
        this.tableNameField = new TextField();
        this.commentField = new TextArea();
        this.columns = FXCollections.observableArrayList();
        this.columnsTable = new TableView<>(columns);

        setTitle("Table Designer");
        setHeaderText("Create New Table");
        initializeUI();
    }

    private void initializeUI() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // 表信息
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);

        infoGrid.add(new Label("Table Name:"), 0, 0);
        infoGrid.add(tableNameField, 1, 0);
        tableNameField.setPromptText("table_name");

        infoGrid.add(new Label("Comment:"), 0, 1);
        commentField.setPrefRowCount(2);
        commentField.setPromptText("Table comment (optional)");
        infoGrid.add(commentField, 1, 1);

        // 列定义表格
        setupColumnsTable();

        // 按钮
        ToolBar toolBar = new ToolBar();
        Button addColumnBtn = new Button("Add Column");
        Button removeColumnBtn = new Button("Remove Column");
        addColumnBtn.setOnAction(e -> addColumn());
        removeColumnBtn.setOnAction(e -> removeSelectedColumn());
        toolBar.getItems().addAll(addColumnBtn, removeColumnBtn);

        content.getChildren().addAll(
                new Label("Table Information:"),
                infoGrid,
                new Separator(),
                new Label("Columns:"),
                toolBar,
                columnsTable
        );

        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(800, 600);

        ButtonType createButtonType = new ButtonType("Create Table", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // 添加一个默认列
        addColumn();

        setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return createTable();
            }
            return false;
        });
    }

    private void setupColumnsTable() {
        columnsTable.setEditable(true);
        columnsTable.setPrefHeight(300);

        // 列名
        TableColumn<ColumnDefinition, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));
        nameCol.setPrefWidth(150);

        // 数据类型
        TableColumn<ColumnDefinition, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(param -> param.getValue().typeProperty());
        ObservableList<String> dataTypes = FXCollections.observableArrayList(
                "INT", "VARCHAR(255)", "TEXT", "DATETIME", "DECIMAL(10,2)",
                "BIGINT", "DOUBLE", "FLOAT", "DATE", "TIME", "TIMESTAMP",
                "CHAR(10)", "TINYINT", "SMALLINT", "BOOLEAN", "BLOB"
        );
        typeCol.setCellFactory(ComboBoxTableCell.forTableColumn(dataTypes));
        typeCol.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));
        typeCol.setPrefWidth(150);

        // NULL
        TableColumn<ColumnDefinition, String> nullCol = new TableColumn<>("Null");
        nullCol.setCellValueFactory(param -> param.getValue().nullableProperty());
        nullCol.setCellFactory(ComboBoxTableCell.forTableColumn("YES", "NO"));
        nullCol.setOnEditCommit(e -> e.getRowValue().setNullable(e.getNewValue()));
        nullCol.setPrefWidth(80);

        // 默认值
        TableColumn<ColumnDefinition, String> defaultCol = new TableColumn<>("Default");
        defaultCol.setCellValueFactory(param -> param.getValue().defaultValueProperty());
        defaultCol.setCellFactory(TextFieldTableCell.forTableColumn());
        defaultCol.setOnEditCommit(e -> e.getRowValue().setDefaultValue(e.getNewValue()));
        defaultCol.setPrefWidth(100);

        // 主键
        TableColumn<ColumnDefinition, String> pkCol = new TableColumn<>("Primary Key");
        pkCol.setCellValueFactory(param -> param.getValue().primaryKeyProperty());
        pkCol.setCellFactory(ComboBoxTableCell.forTableColumn("YES", "NO"));
        pkCol.setOnEditCommit(e -> e.getRowValue().setPrimaryKey(e.getNewValue()));
        pkCol.setPrefWidth(100);

        // 自增
        TableColumn<ColumnDefinition, String> aiCol = new TableColumn<>("Auto Increment");
        aiCol.setCellValueFactory(param -> param.getValue().autoIncrementProperty());
        aiCol.setCellFactory(ComboBoxTableCell.forTableColumn("YES", "NO"));
        aiCol.setOnEditCommit(e -> e.getRowValue().setAutoIncrement(e.getNewValue()));
        aiCol.setPrefWidth(120);

        columnsTable.getColumns().addAll(nameCol, typeCol, nullCol, defaultCol, pkCol, aiCol);
    }

    private void addColumn() {
        columns.add(new ColumnDefinition());
    }

    private void removeSelectedColumn() {
        ColumnDefinition selected = columnsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            columns.remove(selected);
        }
    }

    private boolean createTable() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            showError("Table name is required");
            return false;
        }

        if (columns.isEmpty()) {
            showError("At least one column is required");
            return false;
        }

        String createTableSQL = buildCreateTableSQL(tableName);

        try (Connection conn = ConnectionManager.getInstance().getConnection(connectionId);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            return true;

        } catch (Exception e) {
            showError("Failed to create table: " + e.getMessage());
            return false;
        }
    }

    private String buildCreateTableSQL(String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(database).append("`.`").append(tableName).append("` (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ColumnDefinition col : columns) {
            if (col.getName().trim().isEmpty()) continue;

            StringBuilder colDef = new StringBuilder();
            colDef.append("  `").append(col.getName()).append("` ");
            colDef.append(col.getType());

            if ("NO".equals(col.getNullable())) {
                colDef.append(" NOT NULL");
            }

            if (col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
                colDef.append(" DEFAULT '").append(col.getDefaultValue()).append("'");
            }

            if ("YES".equals(col.getAutoIncrement())) {
                colDef.append(" AUTO_INCREMENT");
            }

            columnDefs.add(colDef.toString());

            if ("YES".equals(col.getPrimaryKey())) {
                primaryKeys.add(col.getName());
            }
        }

        sql.append(String.join(",\n", columnDefs));

        if (!primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (");
            sql.append(String.join(", ", primaryKeys.stream()
                    .map(pk -> "`" + pk + "`")
                    .toArray(String[]::new)));
            sql.append(")");
        }

        sql.append("\n)");

        if (!commentField.getText().trim().isEmpty()) {
            sql.append(" COMMENT='").append(commentField.getText().trim()).append("'");
        }

        sql.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        return sql.toString();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 列定义类
     */
    public static class ColumnDefinition {
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;
        private final SimpleStringProperty nullable;
        private final SimpleStringProperty defaultValue;
        private final SimpleStringProperty primaryKey;
        private final SimpleStringProperty autoIncrement;

        public ColumnDefinition() {
            this.name = new SimpleStringProperty("");
            this.type = new SimpleStringProperty("VARCHAR(255)");
            this.nullable = new SimpleStringProperty("YES");
            this.defaultValue = new SimpleStringProperty("");
            this.primaryKey = new SimpleStringProperty("NO");
            this.autoIncrement = new SimpleStringProperty("NO");
        }

        public SimpleStringProperty nameProperty() { return name; }
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }

        public SimpleStringProperty typeProperty() { return type; }
        public String getType() { return type.get(); }
        public void setType(String value) { type.set(value); }

        public SimpleStringProperty nullableProperty() { return nullable; }
        public String getNullable() { return nullable.get(); }
        public void setNullable(String value) { nullable.set(value); }

        public SimpleStringProperty defaultValueProperty() { return defaultValue; }
        public String getDefaultValue() { return defaultValue.get(); }
        public void setDefaultValue(String value) { defaultValue.set(value); }

        public SimpleStringProperty primaryKeyProperty() { return primaryKey; }
        public String getPrimaryKey() { return primaryKey.get(); }
        public void setPrimaryKey(String value) { primaryKey.set(value); }

        public SimpleStringProperty autoIncrementProperty() { return autoIncrement; }
        public String getAutoIncrement() { return autoIncrement.get(); }
        public void setAutoIncrement(String value) { autoIncrement.set(value); }
    }
}
