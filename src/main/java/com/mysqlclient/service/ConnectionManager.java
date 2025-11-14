package com.mysqlclient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mysqlclient.model.ConnectionConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * 连接管理器 - 管理MySQL连接配置和连接池
 */
public class ConnectionManager {
    private static ConnectionManager instance;
    private final Map<String, HikariDataSource> dataSources;
    private final List<ConnectionConfig> connections;
    private final ObjectMapper objectMapper;
    private final Path configDir;
    private final Path configFile;

    private ConnectionManager() {
        this.dataSources = new HashMap<>();
        this.connections = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 配置目录：用户目录/.mysqlclient/
        String userHome = System.getProperty("user.home");
        this.configDir = Paths.get(userHome, ".mysqlclient");
        this.configFile = configDir.resolve("connections.json");

        initConfigDirectory();
        loadConnections();
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    private void initConfigDirectory() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }
    }

    /**
     * 加载连接配置
     */
    private void loadConnections() {
        if (!Files.exists(configFile)) {
            return;
        }

        try {
            List<ConnectionConfig> loaded = objectMapper.readValue(
                    configFile.toFile(),
                    new TypeReference<List<ConnectionConfig>>() {}
            );
            connections.addAll(loaded);
        } catch (IOException e) {
            System.err.println("Failed to load connections: " + e.getMessage());
        }
    }

    /**
     * 保存连接配置
     */
    public void saveConnections() {
        try {
            objectMapper.writeValue(configFile.toFile(), connections);
        } catch (IOException e) {
            System.err.println("Failed to save connections: " + e.getMessage());
            throw new RuntimeException("Failed to save connections", e);
        }
    }

    /**
     * 添加连接配置
     */
    public void addConnection(ConnectionConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        connections.add(config);
        saveConnections();
    }

    /**
     * 更新连接配置
     */
    public void updateConnection(ConnectionConfig config) {
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).getId().equals(config.getId())) {
                connections.set(i, config);
                saveConnections();
                // 关闭旧的连接池
                closeConnection(config.getId());
                return;
            }
        }
    }

    /**
     * 删除连接配置
     */
    public void deleteConnection(String id) {
        connections.removeIf(c -> c.getId().equals(id));
        closeConnection(id);
        saveConnections();
    }

    /**
     * 获取所有连接配置
     */
    public List<ConnectionConfig> getConnections() {
        return new ArrayList<>(connections);
    }

    /**
     * 根据ID获取连接配置
     */
    public ConnectionConfig getConnectionById(String id) {
        return connections.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 测试连接
     */
    public boolean testConnection(ConnectionConfig config) {
        try {
            HikariConfig hikariConfig = createHikariConfig(config);
            try (HikariDataSource testDs = new HikariDataSource(hikariConfig);
                 Connection conn = testDs.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection(String connectionId) throws SQLException {
        HikariDataSource dataSource = dataSources.get(connectionId);
        if (dataSource == null) {
            ConnectionConfig config = getConnectionById(connectionId);
            if (config == null) {
                throw new SQLException("Connection configuration not found: " + connectionId);
            }
            dataSource = createDataSource(config);
            dataSources.put(connectionId, dataSource);
        }
        return dataSource.getConnection();
    }

    /**
     * 创建数据源
     */
    private HikariDataSource createDataSource(ConnectionConfig config) {
        HikariConfig hikariConfig = createHikariConfig(config);
        return new HikariDataSource(hikariConfig);
    }

    /**
     * 创建HikariCP配置
     */
    private HikariConfig createHikariConfig(ConnectionConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(decodePassword(config.getPassword()));
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        return hikariConfig;
    }

    /**
     * 关闭连接池
     */
    public void closeConnection(String connectionId) {
        HikariDataSource dataSource = dataSources.remove(connectionId);
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * 关闭所有连接池
     */
    public void closeAllConnections() {
        dataSources.values().forEach(ds -> {
            if (!ds.isClosed()) {
                ds.close();
            }
        });
        dataSources.clear();
    }

    /**
     * Base64编码密码
     */
    public static String encodePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(password.getBytes());
    }

    /**
     * Base64解码密码
     */
    public static String decodePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encodedPassword));
        } catch (IllegalArgumentException e) {
            return encodedPassword; // 如果不是Base64，直接返回
        }
    }
}
