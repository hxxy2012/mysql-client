package com.mysqlclient.model;

/**
 * 数据库树节点类型
 */
public class DatabaseNode {
    public enum NodeType {
        CONNECTION,
        DATABASE,
        TABLES_FOLDER,
        TABLE,
        COLUMNS_FOLDER,
        COLUMN
    }

    private String name;
    private NodeType type;
    private Object data;

    public DatabaseNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    public DatabaseNode(String name, NodeType type, Object data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return name;
    }
}
