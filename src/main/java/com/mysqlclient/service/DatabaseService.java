package com.mysqlclient.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作服务
 */
public class DatabaseService {
    private final ConnectionManager connectionManager;

    public DatabaseService() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    /**
     * 获取所有数据库列表
     */
    public List<String> getDatabases(String connectionId) throws SQLException {
        List<String> databases = new ArrayList<>();
        String sql = "SHOW DATABASES";

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        }

        return databases;
    }

    /**
     * 获取数据库中的所有表
     */
    public List<String> getTables(String connectionId, String database) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SHOW TABLES FROM `" + database + "`";

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }

        return tables;
    }

    /**
     * 获取表的列信息
     */
    public List<ColumnInfo> getColumns(String connectionId, String database, String table) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY, EXTRA " +
                     "FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                     "ORDER BY ORDINAL_POSITION";

        try (Connection conn = connectionManager.getConnection(connectionId);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, database);
            pstmt.setString(2, table);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColumnInfo column = new ColumnInfo();
                    column.setName(rs.getString("COLUMN_NAME"));
                    column.setType(rs.getString("DATA_TYPE"));
                    column.setNullable("YES".equals(rs.getString("IS_NULLABLE")));
                    column.setKey(rs.getString("COLUMN_KEY"));
                    column.setExtra(rs.getString("EXTRA"));
                    columns.add(column);
                }
            }
        }

        return columns;
    }

    /**
     * 执行查询SQL
     */
    public QueryResult executeQuery(String connectionId, String sql) throws SQLException {
        QueryResult result = new QueryResult();
        long startTime = System.currentTimeMillis();

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement()) {

            // 设置查询超时
            stmt.setQueryTimeout(30);

            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // 设置列名
                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(metaData.getColumnLabel(i));
                    }
                    result.setColumnNames(columnNames);

                    // 读取数据
                    List<List<Object>> rows = new ArrayList<>();
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    result.setRows(rows);
                    result.setRowCount(rows.size());
                }
            } else {
                // 更新操作
                int updateCount = stmt.getUpdateCount();
                result.setRowCount(updateCount);
                result.setUpdateResult(true);
            }

            long endTime = System.currentTimeMillis();
            result.setExecutionTime(endTime - startTime);
            result.setSuccess(true);

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        }

        return result;
    }

    /**
     * 查询结果类
     */
    public static class QueryResult {
        private boolean success;
        private List<String> columnNames;
        private List<List<Object>> rows;
        private int rowCount;
        private long executionTime;
        private String errorMessage;
        private boolean updateResult;

        public QueryResult() {
            this.columnNames = new ArrayList<>();
            this.rows = new ArrayList<>();
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public List<String> getColumnNames() {
            return columnNames;
        }

        public void setColumnNames(List<String> columnNames) {
            this.columnNames = columnNames;
        }

        public List<List<Object>> getRows() {
            return rows;
        }

        public void setRows(List<List<Object>> rows) {
            this.rows = rows;
        }

        public int getRowCount() {
            return rowCount;
        }

        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isUpdateResult() {
            return updateResult;
        }

        public void setUpdateResult(boolean updateResult) {
            this.updateResult = updateResult;
        }
    }

    /**
     * 列信息类
     */
    public static class ColumnInfo {
        private String name;
        private String type;
        private boolean nullable;
        private String key;
        private String extra;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" (").append(type);
            if ("PRI".equals(key)) {
                sb.append(", PRIMARY KEY");
            }
            if (!nullable) {
                sb.append(", NOT NULL");
            }
            if (extra != null && !extra.isEmpty()) {
                sb.append(", ").append(extra);
            }
            sb.append(")");
            return sb.toString();
        }
    }
}
