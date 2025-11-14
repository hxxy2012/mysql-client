package com.mysqlclient.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入服务 - 支持CSV、Excel格式导入
 */
public class ImportService {
    private final ConnectionManager connectionManager;

    public ImportService() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    /**
     * 从CSV导入数据
     */
    public int importFromCSV(String connectionId, String database, String table, File file, boolean hasHeader)
            throws SQLException, IOException {
        int importedRows = 0;

        try (Connection conn = connectionManager.getConnection(connectionId);
             FileReader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

            List<CSVRecord> records = csvParser.getRecords();
            if (records.isEmpty()) {
                return 0;
            }

            int startRow = hasHeader ? 1 : 0;
            CSVRecord firstDataRow = records.get(startRow);
            int columnCount = firstDataRow.size();

            // 构建INSERT语句
            StringBuilder sql = new StringBuilder();
            sql.append(String.format("INSERT INTO `%s`.`%s` VALUES (", database, table));
            for (int i = 0; i < columnCount; i++) {
                sql.append(i > 0 ? ",?" : "?");
            }
            sql.append(")");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                conn.setAutoCommit(false);

                for (int i = startRow; i < records.size(); i++) {
                    CSVRecord record = records.get(i);
                    for (int j = 0; j < columnCount; j++) {
                        String value = record.get(j);
                        if (value == null || value.isEmpty() || "NULL".equalsIgnoreCase(value)) {
                            pstmt.setNull(j + 1, java.sql.Types.VARCHAR);
                        } else {
                            pstmt.setString(j + 1, value);
                        }
                    }
                    pstmt.addBatch();
                    importedRows++;

                    // 每1000行提交一次
                    if (importedRows % 1000 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }

                pstmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }
        }

        return importedRows;
    }

    /**
     * 从Excel导入数据
     */
    public int importFromExcel(String connectionId, String database, String table, File file, boolean hasHeader)
            throws SQLException, IOException {
        int importedRows = 0;

        try (Connection conn = connectionManager.getConnection(connectionId);
             Workbook workbook = WorkbookFactory.create(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            int firstRowNum = hasHeader ? 1 : 0;

            if (sheet.getPhysicalNumberOfRows() <= firstRowNum) {
                return 0;
            }

            Row firstDataRow = sheet.getRow(firstRowNum);
            if (firstDataRow == null) {
                return 0;
            }

            int columnCount = firstDataRow.getLastCellNum();

            // 构建INSERT语句
            StringBuilder sql = new StringBuilder();
            sql.append(String.format("INSERT INTO `%s`.`%s` VALUES (", database, table));
            for (int i = 0; i < columnCount; i++) {
                sql.append(i > 0 ? ",?" : "?");
            }
            sql.append(")");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                conn.setAutoCommit(false);

                for (int i = firstRowNum; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    for (int j = 0; j < columnCount; j++) {
                        Cell cell = row.getCell(j);
                        setCellValue(pstmt, j + 1, cell);
                    }
                    pstmt.addBatch();
                    importedRows++;

                    // 每1000行提交一次
                    if (importedRows % 1000 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }

                pstmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }
        }

        return importedRows;
    }

    private void setCellValue(PreparedStatement pstmt, int index, Cell cell) throws SQLException {
        if (cell == null) {
            pstmt.setNull(index, java.sql.Types.VARCHAR);
            return;
        }

        switch (cell.getCellType()) {
            case STRING:
                pstmt.setString(index, cell.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    pstmt.setDate(index, new java.sql.Date(cell.getDateCellValue().getTime()));
                } else {
                    pstmt.setDouble(index, cell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                pstmt.setBoolean(index, cell.getBooleanCellValue());
                break;
            case FORMULA:
                pstmt.setString(index, cell.getCellFormula());
                break;
            default:
                pstmt.setNull(index, java.sql.Types.VARCHAR);
        }
    }
}
