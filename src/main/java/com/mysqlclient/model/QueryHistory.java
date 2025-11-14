package com.mysqlclient.model;

import java.time.LocalDateTime;

/**
 * SQL查询历史记录
 */
public class QueryHistory {
    private String id;
    private String sql;
    private String connectionName;
    private String database;
    private LocalDateTime executedAt;
    private long executionTime;
    private boolean success;
    private int rowsAffected;

    public QueryHistory() {
        this.id = java.util.UUID.randomUUID().toString();
        this.executedAt = LocalDateTime.now();
    }

    public QueryHistory(String sql, String connectionName, String database) {
        this();
        this.sql = sql;
        this.connectionName = connectionName;
        this.database = database;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRowsAffected() {
        return rowsAffected;
    }

    public void setRowsAffected(int rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %dms",
            executedAt.toString(),
            sql.length() > 50 ? sql.substring(0, 50) + "..." : sql,
            executionTime);
    }
}
