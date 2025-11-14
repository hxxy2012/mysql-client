package com.mysqlclient.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出服务 - 支持CSV、Excel、SQL格式导出
 */
public class ExportService {
    private final ConnectionManager connectionManager;

    public ExportService() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    /**
     * 导出为CSV文件
     */
    public void exportToCSV(String connectionId, String sql, File file) throws SQLException, IOException {
        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             FileWriter writer = new FileWriter(file);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 写入表头
            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                headers.add(metaData.getColumnLabel(i));
            }
            csvPrinter.printRecord(headers);

            // 写入数据
            while (rs.next()) {
                List<Object> record = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    record.add(rs.getObject(i));
                }
                csvPrinter.printRecord(record);
            }
        }
    }

    /**
     * 导出为Excel文件
     */
    public void exportToExcel(String connectionId, String sql, File file) throws SQLException, IOException {
        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Data");
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 写入表头
            Row headerRow = sheet.createRow(0);
            for (int i = 1; i <= columnCount; i++) {
                Cell cell = headerRow.createCell(i - 1);
                cell.setCellValue(metaData.getColumnLabel(i));
                cell.setCellStyle(headerStyle);
            }

            // 写入数据
            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = row.createCell(i - 1);
                    Object value = rs.getObject(i);
                    if (value != null) {
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof java.util.Date) {
                            cell.setCellValue((java.util.Date) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < columnCount; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOut);
        }
    }

    /**
     * 导出为SQL INSERT语句
     */
    public void exportToSQL(String connectionId, String database, String table, File file) throws SQLException, IOException {
        String sql = String.format("SELECT * FROM `%s`.`%s`", database, table);

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             FileWriter writer = new FileWriter(file)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 写入表头注释
            writer.write("-- MySQL dump for table: " + table + "\n");
            writer.write("-- Database: " + database + "\n");
            writer.write("-- Generated: " + new java.util.Date() + "\n\n");

            // 获取列名
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add("`" + metaData.getColumnLabel(i) + "`");
            }

            String columnList = String.join(", ", columnNames);

            // 写入INSERT语句
            while (rs.next()) {
                StringBuilder insertStmt = new StringBuilder();
                insertStmt.append(String.format("INSERT INTO `%s` (", table));
                insertStmt.append(columnList);
                insertStmt.append(") VALUES (");

                List<String> values = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        values.add("NULL");
                    } else if (value instanceof Number) {
                        values.add(value.toString());
                    } else {
                        // 转义单引号
                        String strValue = value.toString().replace("'", "''");
                        values.add("'" + strValue + "'");
                    }
                }

                insertStmt.append(String.join(", ", values));
                insertStmt.append(");\n");
                writer.write(insertStmt.toString());
            }

            writer.write("\n-- End of dump\n");
        }
    }

    /**
     * 导出表结构
     */
    public void exportTableStructure(String connectionId, String database, String table, File file) throws SQLException, IOException {
        String sql = String.format("SHOW CREATE TABLE `%s`.`%s`", database, table);

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             FileWriter writer = new FileWriter(file)) {

            writer.write("-- Table structure for: " + table + "\n");
            writer.write("-- Database: " + database + "\n\n");

            if (rs.next()) {
                String createTable = rs.getString(2);
                writer.write(createTable + ";\n");
            }
        }
    }
}
