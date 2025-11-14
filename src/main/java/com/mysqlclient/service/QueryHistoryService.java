package com.mysqlclient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mysqlclient.model.QueryHistory;
import com.mysqlclient.model.SavedQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询历史和收藏管理服务
 */
public class QueryHistoryService {
    private static QueryHistoryService instance;
    private final List<QueryHistory> history;
    private final List<SavedQuery> savedQueries;
    private final ObjectMapper objectMapper;
    private final Path configDir;
    private final Path historyFile;
    private final Path savedQueriesFile;
    private final int maxHistorySize = 500;

    private QueryHistoryService() {
        this.history = new ArrayList<>();
        this.savedQueries = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        String userHome = System.getProperty("user.home");
        this.configDir = Paths.get(userHome, ".mysqlclient");
        this.historyFile = configDir.resolve("query_history.json");
        this.savedQueriesFile = configDir.resolve("saved_queries.json");

        loadHistory();
        loadSavedQueries();
    }

    public static synchronized QueryHistoryService getInstance() {
        if (instance == null) {
            instance = new QueryHistoryService();
        }
        return instance;
    }

    /**
     * 添加查询历史
     */
    public void addHistory(QueryHistory queryHistory) {
        history.add(0, queryHistory); // 添加到开头

        // 限制历史记录数量
        if (history.size() > maxHistorySize) {
            history.remove(history.size() - 1);
        }

        saveHistory();
    }

    /**
     * 获取所有历史记录
     */
    public List<QueryHistory> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * 搜索历史记录
     */
    public List<QueryHistory> searchHistory(String keyword) {
        return history.stream()
                .filter(h -> h.getSql().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * 清空历史记录
     */
    public void clearHistory() {
        history.clear();
        saveHistory();
    }

    /**
     * 保存查询到收藏夹
     */
    public void saveQuery(SavedQuery query) {
        savedQueries.add(query);
        saveSavedQueries();
    }

    /**
     * 更新已保存的查询
     */
    public void updateSavedQuery(SavedQuery query) {
        for (int i = 0; i < savedQueries.size(); i++) {
            if (savedQueries.get(i).getId().equals(query.getId())) {
                query.setUpdatedAt(java.time.LocalDateTime.now());
                savedQueries.set(i, query);
                saveSavedQueries();
                return;
            }
        }
    }

    /**
     * 删除已保存的查询
     */
    public void deleteSavedQuery(String id) {
        savedQueries.removeIf(q -> q.getId().equals(id));
        saveSavedQueries();
    }

    /**
     * 获取所有已保存的查询
     */
    public List<SavedQuery> getSavedQueries() {
        return new ArrayList<>(savedQueries);
    }

    /**
     * 按文件夹获取查询
     */
    public List<SavedQuery> getSavedQueriesByFolder(String folder) {
        return savedQueries.stream()
                .filter(q -> folder.equals(q.getFolder()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有文件夹
     */
    public List<String> getFolders() {
        return savedQueries.stream()
                .map(SavedQuery::getFolder)
                .filter(f -> f != null && !f.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private void loadHistory() {
        if (!Files.exists(historyFile)) {
            return;
        }

        try {
            List<QueryHistory> loaded = objectMapper.readValue(
                    historyFile.toFile(),
                    new TypeReference<List<QueryHistory>>() {}
            );
            history.addAll(loaded);
        } catch (IOException e) {
            System.err.println("Failed to load query history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try {
            objectMapper.writeValue(historyFile.toFile(), history);
        } catch (IOException e) {
            System.err.println("Failed to save query history: " + e.getMessage());
        }
    }

    private void loadSavedQueries() {
        if (!Files.exists(savedQueriesFile)) {
            return;
        }

        try {
            List<SavedQuery> loaded = objectMapper.readValue(
                    savedQueriesFile.toFile(),
                    new TypeReference<List<SavedQuery>>() {}
            );
            savedQueries.addAll(loaded);
        } catch (IOException e) {
            System.err.println("Failed to load saved queries: " + e.getMessage());
        }
    }

    private void saveSavedQueries() {
        try {
            objectMapper.writeValue(savedQueriesFile.toFile(), savedQueries);
        } catch (IOException e) {
            System.err.println("Failed to save queries: " + e.getMessage());
        }
    }
}
