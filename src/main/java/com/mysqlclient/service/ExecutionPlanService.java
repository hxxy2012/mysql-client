package com.mysqlclient.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL执行计划分析服务
 */
public class ExecutionPlanService {
    private final ConnectionManager connectionManager;

    public ExecutionPlanService() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    /**
     * 获取查询的执行计划
     */
    public ExecutionPlan getExecutionPlan(String connectionId, String sql) throws SQLException {
        ExecutionPlan plan = new ExecutionPlan();
        String explainSql = "EXPLAIN " + sql;

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 获取列名
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            plan.setColumnNames(columnNames);

            // 获取执行计划数据
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.put(columnNames.get(i - 1), value != null ? value.toString() : "NULL");
                }
                plan.addRow(row);
            }
        }

        return plan;
    }

    /**
     * 获取详细的执行计划（包括成本信息）
     */
    public ExecutionPlan getDetailedExecutionPlan(String connectionId, String sql) throws SQLException {
        ExecutionPlan plan = new ExecutionPlan();
        String explainSql = "EXPLAIN FORMAT=JSON " + sql;

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            if (rs.next()) {
                String jsonPlan = rs.getString(1);
                plan.setJsonPlan(jsonPlan);
            }
        }

        return plan;
    }

    /**
     * 分析查询性能
     */
    public QueryAnalysis analyzeQuery(String connectionId, String sql) throws SQLException {
        QueryAnalysis analysis = new QueryAnalysis();

        try (Connection conn = connectionManager.getConnection(connectionId);
             Statement stmt = conn.createStatement()) {

            // 获取执行计划
            ExecutionPlan plan = getExecutionPlan(connectionId, sql);
            analysis.setExecutionPlan(plan);

            // 分析潜在问题
            List<String> warnings = new ArrayList<>();
            for (Map<String, String> row : plan.getRows()) {
                // 检查全表扫描
                String type = row.get("type");
                if ("ALL".equals(type)) {
                    warnings.add("Full table scan detected on table: " + row.get("table"));
                }

                // 检查使用临时表
                String extra = row.get("Extra");
                if (extra != null && extra.contains("Using temporary")) {
                    warnings.add("Using temporary table");
                }

                // 检查文件排序
                if (extra != null && extra.contains("Using filesort")) {
                    warnings.add("Using filesort - consider adding index");
                }
            }
            analysis.setWarnings(warnings);

            // 获取查询统计信息
            try (ResultSet rs = stmt.executeQuery("SHOW SESSION STATUS LIKE 'Last_query_cost'")) {
                if (rs.next()) {
                    analysis.setQueryCost(rs.getDouble(2));
                }
            }
        }

        return analysis;
    }

    /**
     * 执行计划类
     */
    public static class ExecutionPlan {
        private List<String> columnNames;
        private List<Map<String, String>> rows;
        private String jsonPlan;

        public ExecutionPlan() {
            this.columnNames = new ArrayList<>();
            this.rows = new ArrayList<>();
        }

        public void addRow(Map<String, String> row) {
            rows.add(row);
        }

        public List<String> getColumnNames() {
            return columnNames;
        }

        public void setColumnNames(List<String> columnNames) {
            this.columnNames = columnNames;
        }

        public List<Map<String, String>> getRows() {
            return rows;
        }

        public void setRows(List<Map<String, String>> rows) {
            this.rows = rows;
        }

        public String getJsonPlan() {
            return jsonPlan;
        }

        public void setJsonPlan(String jsonPlan) {
            this.jsonPlan = jsonPlan;
        }
    }

    /**
     * 查询分析结果类
     */
    public static class QueryAnalysis {
        private ExecutionPlan executionPlan;
        private List<String> warnings;
        private List<String> suggestions;
        private double queryCost;

        public QueryAnalysis() {
            this.warnings = new ArrayList<>();
            this.suggestions = new ArrayList<>();
        }

        public ExecutionPlan getExecutionPlan() {
            return executionPlan;
        }

        public void setExecutionPlan(ExecutionPlan executionPlan) {
            this.executionPlan = executionPlan;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public double getQueryCost() {
            return queryCost;
        }

        public void setQueryCost(double queryCost) {
            this.queryCost = queryCost;
        }
    }
}
