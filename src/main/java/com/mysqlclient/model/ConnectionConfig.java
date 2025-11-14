package com.mysqlclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * MySQL连接配置模型
 */
public class ConnectionConfig {
    private String id;
    private String name;
    private String host;
    private int port;
    private String username;
    private String password; // Base64编码
    private String database;
    private boolean useSSL;

    public ConnectionConfig() {
        this.port = 3306;
        this.useSSL = false;
    }

    public ConnectionConfig(String name, String host, int port, String username, String password) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useSSL = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    @JsonIgnore
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder("jdbc:mysql://");
        url.append(host).append(":").append(port);
        if (database != null && !database.isEmpty()) {
            url.append("/").append(database);
        }
        url.append("?useSSL=").append(useSSL);
        url.append("&allowPublicKeyRetrieval=true");
        url.append("&serverTimezone=UTC");
        return url.toString();
    }

    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }
}
