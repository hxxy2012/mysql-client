package com.mysqlclient.service;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * 数据库备份和恢复服务
 */
public class BackupService {
    private final ConnectionManager connectionManager;
    private final DatabaseService databaseService;

    public BackupService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.databaseService = new DatabaseService();
    }

    /**
     * 备份整个数据库
     */
    public void backupDatabase(String connectionId, String database, File outputFile,
                                BackupProgressListener progressListener) throws SQLException, IOException {
        try (Connection conn = connectionManager.getConnection(connectionId);
             FileWriter writer = new FileWriter(outputFile)) {

            // 写入备份头
            writeBackupHeader(writer, database);

            // 获取所有表
            List<String> tables = databaseService.getTables(connectionId, database);
            int totalTables = tables.size();
            int currentTable = 0;

            for (String table : tables) {
                currentTable++;
                if (progressListener != null) {
                    progressListener.onProgress(currentTable, totalTables,
                            "Backing up table: " + table);
                }

                backupTable(conn, database, table, writer);
            }

            writer.write("\n-- Backup completed: " + LocalDateTime.now() + "\n");

            if (progressListener != null) {
                progressListener.onComplete("Backup completed successfully");
            }
        }
    }

    /**
     * 备份单个表
     */
    public void backupTable(Connection conn, String database, String table, FileWriter writer)
            throws SQLException, IOException {
        // 写入表结构
        writer.write("\n--\n");
        writer.write("-- Table structure for table `" + table + "`\n");
        writer.write("--\n\n");
        writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");

        String createTableSql = "SHOW CREATE TABLE `" + database + "`.`" + table + "`";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(createTableSql)) {
            if (rs.next()) {
                writer.write(rs.getString(2) + ";\n\n");
            }
        }

        // 写入数据
        writer.write("--\n");
        writer.write("-- Dumping data for table `" + table + "`\n");
        writer.write("--\n\n");

        String selectSql = "SELECT * FROM `" + database + "`.`" + table + "`";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 获取列名
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add("`" + metaData.getColumnName(i) + "`");
            }
            String columnList = String.join(", ", columnNames);

            while (rs.next()) {
                StringBuilder insert = new StringBuilder();
                insert.append("INSERT INTO `").append(table).append("` (");
                insert.append(columnList).append(") VALUES (");

                List<String> values = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        values.add("NULL");
                    } else if (value instanceof Number) {
                        values.add(value.toString());
                    } else if (value instanceof java.sql.Date ||
                               value instanceof java.sql.Timestamp) {
                        values.add("'" + value.toString() + "'");
                    } else {
                        String strValue = value.toString().replace("'", "''");
                        values.add("'" + strValue + "'");
                    }
                }

                insert.append(String.join(", ", values));
                insert.append(");\n");
                writer.write(insert.toString());
            }
        }

        writer.write("\n");
    }

    /**
     * 恢复数据库
     */
    public void restoreDatabase(String connectionId, File backupFile,
                                BackupProgressListener progressListener) throws SQLException, IOException {
        try (Connection conn = connectionManager.getConnection(connectionId);
             BufferedReader reader = new BufferedReader(new FileReader(backupFile));
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;

                // 跳过注释
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }

                sqlBuilder.append(line).append("\n");

                // 如果以分号结尾，执行SQL
                if (line.trim().endsWith(";")) {
                    String sql = sqlBuilder.toString();
                    try {
                        stmt.execute(sql);
                        if (progressListener != null && lineCount % 100 == 0) {
                            progressListener.onProgress(lineCount, -1,
                                    "Executed " + lineCount + " lines");
                        }
                    } catch (SQLException e) {
                        System.err.println("Error executing SQL at line " + lineCount + ": " + e.getMessage());
                    }
                    sqlBuilder.setLength(0);
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

            if (progressListener != null) {
                progressListener.onComplete("Restore completed successfully");
            }
        }
    }

    private void writeBackupHeader(FileWriter writer, String database) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        writer.write("-- MySQL Client Database Backup\n");
        writer.write("-- Database: " + database + "\n");
        writer.write("-- Backup Time: " + LocalDateTime.now().format(formatter) + "\n");
        writer.write("-- ------------------------------------------------------\n\n");
        writer.write("SET FOREIGN_KEY_CHECKS=0;\n");
        writer.write("SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';\n\n");
    }

    @FunctionalInterface
    public interface BackupProgressListener {
        void onProgress(int current, int total, String message);

        default void onComplete(String message) {}
    }
}
