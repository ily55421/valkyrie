package valkyrie.app.console;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import valkyrie.app.explorer.UICatalogNode;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.explorer.GlobalDynamicNodeContext;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.sql.SQL;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * SQL 命令列界面
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class SqlConsolePane extends BorderPane
{
        private final ComboBox<UIConnectionNode> connCombo = new ComboBox<>();
        private final ComboBox<UICatalogNode> catalogCombo = new ComboBox<>();
        private final TextArea outputArea = new TextArea();
        private final TextField inputField = new TextField();
        private final Label statusLabel = new Label();

        private final Deque<String> history = new ArrayDeque<>();
        private final int MAX_HISTORY = 1000;
        private String[] historyArray = new String[0];
        private int historyIndex = -1;

        public SqlConsolePane()
        {
                setPadding(new Insets(5));

                // Toolbar
                Button clearBtn = new Button("清屏");
                clearBtn.setOnAction(e -> outputArea.clear());

                HBox toolbar = new HBox(8, new Label("连接:"), connCombo,
                        new Label("数据库:"), catalogCombo, clearBtn);
                toolbar.setPadding(new Insets(5));

                // Output area
                outputArea.setEditable(false);
                outputArea.setWrapText(true);
                outputArea.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");

                // Input area
                inputField.setPromptText("输入 SQL 并按 Enter 执行 (↑↓ 浏览历史)");
                inputField.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
                inputField.setOnKeyPressed(this::onKeyPressed);

                VBox center = new VBox(5, outputArea, inputField);
                VBox.setVgrow(outputArea, Priority.ALWAYS);
                center.setPadding(new Insets(5));

                // Status bar
                statusLabel.setText("Ready");
                statusLabel.setStyle("-fx-text-fill: -vk-text-secondary; -fx-font-size: 12px; -fx-padding: 2 8;");

                setTop(toolbar);
                setCenter(center);
                setBottom(statusLabel);

                // Populate connections
                connCombo.getItems().addAll(GlobalDynamicNodeContext.getConnectionNodes());
                connCombo.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(UIConnectionNode n) { return n != null ? n.getLabel() : ""; }
                        @Override public UIConnectionNode fromString(String s) { return null; }
                });
                connCombo.valueProperty().addListener((obs, old, conn) -> {
                        catalogCombo.getItems().clear();
                        if (conn != null && conn.isConnect()) {
                                for (var child : conn.getChildren()) {
                                        if (child instanceof UICatalogNode cat)
                                                catalogCombo.getItems().add(cat);
                                }
                        }
                });
                catalogCombo.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(UICatalogNode n) { return n != null ? n.getLabel() : ""; }
                        @Override public UICatalogNode fromString(String s) { return null; }
                });
        }

        private void onKeyPressed(KeyEvent e)
        {
                if (e.getCode() == KeyCode.ENTER) {
                        executeSql();
                } else if (e.getCode() == KeyCode.UP) {
                        e.consume();
                        navigateHistory(-1);
                } else if (e.getCode() == KeyCode.DOWN) {
                        e.consume();
                        navigateHistory(1);
                }
        }

        private void navigateHistory(int direction)
        {
                if (historyArray.length == 0) return;
                historyIndex = Math.max(0, Math.min(historyArray.length - 1, historyIndex + direction));
                if (historyIndex >= 0 && historyIndex < historyArray.length)
                        inputField.setText(historyArray[historyIndex]);
        }

        private void executeSql()
        {
                String sql = inputField.getText().trim();
                if (sql.isEmpty()) return;

                // Add to history
                history.addFirst(sql);
                if (history.size() > MAX_HISTORY) history.removeLast();
                historyArray = history.toArray(new String[0]);
                historyIndex = -1;

                inputField.clear();
                outputArea.appendText("valkyrie> " + sql + "\n");

                UIConnectionNode conn = connCombo.getValue();
                if (conn == null || !conn.isConnect() || conn.getDriver() == null) {
                        outputArea.appendText("ERROR: 未选择有效连接\n\n");
                        return;
                }

                new Thread(() -> {
                        try {
                                Driver driver = conn.getDriver();
                                Session session = catalogCombo.getValue() != null
                                        ? catalogCombo.getValue().getSession()
                                        : Session.ofCatalog("");

                                long jobId = System.currentTimeMillis();
                                var result = driver.execute(jobId, session, new SQL(sql));

                                Platform.runLater(() -> {
                                        if (result != null) {
                                                int rows = result.getRows() != null ? result.getRows().size() : 0;
                                                outputArea.appendText("OK (" + rows + " rows)\n\n");
                                        } else {
                                                outputArea.appendText("OK\n\n");
                                        }
                                        statusLabel.setText("Last: " + sql.substring(0, Math.min(50, sql.length())));
                                });
                        } catch (Exception ex) {
                                Platform.runLater(() -> {
                                        outputArea.appendText("ERROR: " + ex.getMessage() + "\n\n");
                                });
                        }
                }).start();
        }
}
