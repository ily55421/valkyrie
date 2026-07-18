package valkyrie.app.sync;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.explorer.GlobalDynamicNodeContext;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;
import valkyrie.driver.sync.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨库同步向导
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class SyncWizardDialog extends Stage
{
        private final BorderPane root = new BorderPane();
        private final ComboBox<UIConnectionNode> sourceConn = new ComboBox<>();
        private final ComboBox<UIConnectionNode> targetConn = new ComboBox<>();
        private final ListView<String> tableList = new ListView<>();
        private final TextArea logArea = new TextArea();
        private final ProgressBar progressBar = new ProgressBar(0);
        private final Label statusLabel = new Label("Ready");

        private SyncOptions options = new SyncOptions();

        public SyncWizardDialog()
        {
                initOwner(Application.primaryStage);
                initModality(Modality.WINDOW_MODAL);
                setTitle("跨库同步向导");
                setWidth(800);
                setHeight(600);

                root.setPadding(new Insets(10));
                buildStep1();
                setScene(new javafx.scene.Scene(root));
        }

        private void buildStep1()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                Label title = new Label("Step 1: 选择源和目标连接");
                title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                // Populate connections
                List<UIConnectionNode> connections = new ArrayList<>(GlobalDynamicNodeContext.getConnectionNodes());
                sourceConn.getItems().addAll(connections);
                targetConn.getItems().addAll(connections);

                sourceConn.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(UIConnectionNode n) { return n != null ? n.getLabel() : ""; }
                        @Override public UIConnectionNode fromString(String s) { return null; }
                });
                targetConn.setConverter(sourceConn.getConverter());

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("源连接:"), 0, 0);
                grid.add(sourceConn, 1, 0);
                grid.add(new Label("目标连接:"), 0, 1);
                grid.add(targetConn, 1, 1);

                // Options
                CheckBox structureChk = new CheckBox("同步结构");
                structureChk.setSelected(true);
                CheckBox dataChk = new CheckBox("同步数据");
                CheckBox truncateChk = new CheckBox("清空目标表数据");

                HBox optionsBox = new HBox(10, structureChk, dataChk, truncateChk);

                Button next = new Button("下一步 →");
                next.setOnAction(e -> {
                        if (sourceConn.getValue() == null || targetConn.getValue() == null) {
                                new Alert(Alert.AlertType.WARNING, "请选择源和目标连接").show();
                                return;
                        }
                        options.setIncludeStructure(structureChk.isSelected());
                        options.setIncludeData(dataChk.isSelected());
                        if (truncateChk.isSelected())
                                options.setExistingDataPolicy(SyncOptions.ExistingDataPolicy.TRUNCATE);
                        buildStep2();
                });

                box.getChildren().addAll(title, grid, optionsBox, next);
                root.setCenter(box);
        }

        private void buildStep2()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                Label title = new Label("Step 2: 选择要同步的表");
                title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                // Load tables from source
                UIConnectionNode src = sourceConn.getValue();
                if (src.isConnect() && src.getDriver() != null) {
                        try {
                                Driver driver = src.getDriver();
                                Session session = Session.ofCatalog("");
                                driver.getTables(session).forEach(t ->
                                        tableList.getItems().add(t.getName()));
                        } catch (Exception e) {
                                logArea.appendText("加载表失败: " + e.getMessage() + "\n");
                        }
                }
                tableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                Button back = new Button("← 上一步");
                back.setOnAction(e -> buildStep1());

                Button next = new Button("开始同步 →");
                next.setOnAction(e -> {
                        List<String> selected = tableList.getSelectionModel().getSelectedItems().stream().toList();
                        if (selected.isEmpty()) {
                                new Alert(Alert.AlertType.WARNING, "请选择至少一个表").show();
                                return;
                        }
                        runSync(selected);
                });

                HBox buttons = new HBox(10, back, next);
                box.getChildren().addAll(title, tableList, buttons);
                root.setCenter(box);
        }

        private void runSync(List<String> tables)
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                Label title = new Label("Step 3: 同步执行");
                title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                logArea.setEditable(false);
                logArea.setPrefRowCount(20);

                HBox statusBox = new HBox(10, progressBar, statusLabel);
                HBox.setHgrow(progressBar, Priority.ALWAYS);

                Button close = new Button("关闭");
                close.setOnAction(e -> close());
                close.setDisable(true);

                box.getChildren().addAll(title, statusBox, logArea, close);
                root.setCenter(box);

                // Run sync in background
                new Thread(() -> {
                        SyncProgressListener listener = new SyncProgressListener() {
                                @Override public void onTableStart(String table, long total) {
                                        Platform.runLater(() -> {
                                                statusLabel.setText("Syncing: " + table);
                                                logArea.appendText("Starting: " + table + "\n");
                                        });
                                }
                                @Override public void onRows(String table, long done) {
                                        Platform.runLater(() -> {
                                                progressBar.setProgress(-1);
                                        });
                                }
                                @Override public void onTableEnd(String table, boolean success, String msg) {
                                        Platform.runLater(() -> {
                                                logArea.appendText((success ? "✓ " : "✗ ") + table + ": " + msg + "\n");
                                        });
                                }
                                @Override public void onMessage(String msg) {
                                        Platform.runLater(() -> logArea.appendText(msg + "\n"));
                                }
                                @Override public boolean isCancelled() { return false; }
                        };

                        try {
                                UIConnectionNode src = sourceConn.getValue();
                                UIConnectionNode tgt = targetConn.getValue();
                                Driver srcDriver = src.getDriver();
                                Driver tgtDriver = tgt.getDriver();
                                Session srcSession = Session.ofCatalog("");
                                Session tgtSession = Session.ofCatalog("");

                                if (options.isIncludeStructure()) {
                                        Platform.runLater(() -> logArea.appendText("=== Structure Sync ===\n"));
                                        var plan = StructureSyncService.plan(
                                                srcDriver, srcSession, tables,
                                                tgtDriver, tgtSession, options);
                                        SyncReport report = StructureSyncService.execute(plan, tgtDriver, tgtSession, listener);
                                        Platform.runLater(() -> {
                                                logArea.appendText("Structure sync done: " + report.getSuccessRows() + " tables\n");
                                        });
                                }

                                if (options.isIncludeData()) {
                                        Platform.runLater(() -> logArea.appendText("=== Data Sync ===\n"));
                                        for (String table : tables) {
                                                DataSyncService.copy(srcDriver, srcSession, table,
                                                        tgtDriver, tgtSession,
                                                        options.getPageSize(), options.getBatchSize(),
                                                        DataSyncService.ExistingDataPolicy.valueOf(
                                                                options.getExistingDataPolicy().name()),
                                                        listener);
                                        }
                                }

                                Platform.runLater(() -> {
                                        statusLabel.setText("Done");
                                        progressBar.setProgress(1);
                                        close.setDisable(false);
                                });
                        } catch (Exception e) {
                                Platform.runLater(() -> {
                                        logArea.appendText("Error: " + e.getMessage() + "\n");
                                        statusLabel.setText("Failed");
                                        close.setDisable(false);
                                });
                        }
                }).start();
        }
}
