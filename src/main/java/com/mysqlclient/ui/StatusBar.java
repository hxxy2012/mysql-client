package com.mysqlclient.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * 状态栏
 */
public class StatusBar {
    private final HBox root;
    private final Label messageLabel;
    private final Label connectionLabel;

    public StatusBar() {
        this.root = new HBox(10);
        this.messageLabel = new Label("Ready");
        this.connectionLabel = new Label("");

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(5, 10, 5, 10));
        root.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        connectionLabel.setStyle("-fx-text-fill: #666666;");

        root.getChildren().addAll(messageLabel, connectionLabel);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setConnection(String connection) {
        connectionLabel.setText(connection);
    }

    public HBox getRoot() {
        return root;
    }
}
