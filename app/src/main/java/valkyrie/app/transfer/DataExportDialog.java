package valkyrie.app.transfer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.explorer.GlobalDynamicNodeContext;
import valkyrie.driver.api.*;
import valkyrie.driver.transfer.CsvTarget;
import valkyrie.driver.transfer.JsonTarget;
import valkyrie.driver.transfer.SqlInsertTarget;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * 数据导出对话框
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class DataExportDialog extends Stage
{
        private final ComboBox<UIConnectionNode> connCombo = new ComboBox<>();
        private final ComboBox<String> catalogCombo = new ComboBox<>();
        private final ListView<String> tableList = new ListView<>();
        private final ComboBox<String> formatCombo = new ComboBox<>();
        private final CheckBox includeHeaderChk = new CheckBox("含表头");
        private final ProgressBar progressBar = new ProgressBar(0);
        private final Label statusLabel = new Label("Ready");

        private Driver currentDriver;
        private Session currentSession;

        public DataExportDialog()
        {
                initOwner(Application.primaryStage);
                initModality(Modality.WINDOW_MODAL);
                setTitle("数据导出");
                setWidth(600);
                setHeight(500);

                connCombo.getItems().addAll(GlobalDynamicNodeContext.getConnectionNodes());
                connCombo.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(UIConnectionNode n) { return n != null ? n.getLabel() : ""; }
                        @Override public UIConnectionNode fromString(String s) { return null; }
                });
                connCombo.valueProperty().addListener((obs, old, conn) -> {
                        catalogCombo.getItems().clear();
                        tableList.getItems().clear();
                        if (conn != null) {
                                new Thread(() -> {
                                        List<String> catalogs = valkyrie.app.utils.DbUtils.loadCatalogs(conn);
                                        currentDriver = conn.getDriver();
                                        Platform.runLater(() -> {
                                                catalogCombo.getItems().addAll(catalogs);
                                                if (!catalogs.isEmpty())
                                                        catalogCombo.setValue(catalogs.get(0));
                                        });
                                }).start();
                        }
                });

                catalogCombo.valueProperty().addListener((obs, old, catalog) -> {
                        tableList.getItems().clear();
                        if (catalog != null && currentDriver != null) {
                                currentSession = Session.ofCatalog(catalog);
                                new Thread(() -> {
                                        try {
                                                List<Table> tables = currentDriver.getTables(currentSession);
                                                Platform.runLater(() -> {
                                                        tables.forEach(t -> tableList.getItems().add(t.getName()));
                                                });
                                        } catch (Exception e) {
                                                Platform.runLater(() -> statusLabel.setText("加载表失败: " + e.getMessage()));
                                        }
                                }).start();
                        }
                });

                formatCombo.getItems().addAll("CSV", "JSON", "SQL INSERT");
                formatCombo.setValue("CSV");
                includeHeaderChk.setSelected(true);
                tableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                Button exportBtn = new Button("导出");
                exportBtn.setOnAction(e -> doExport());

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("连接:"), 0, 0);
                grid.add(connCombo, 1, 0);
                grid.add(new Label("数据库:"), 0, 1);
                grid.add(catalogCombo, 1, 1);
                grid.add(new Label("格式:"), 0, 2);
                grid.add(formatCombo, 1, 2);
                grid.add(includeHeaderChk, 2, 2);

                HBox statusBox = new HBox(10, progressBar, statusLabel);
                HBox.setHgrow(progressBar, Priority.ALWAYS);

                VBox root = new VBox(10, grid, new Label("选择表:"), tableList, statusBox, exportBtn);
                root.setPadding(new Insets(15));

                setScene(new Scene(root));
        }

        private void doExport()
        {
                List<String> selected = tableList.getSelectionModel().getSelectedItems();
                if (selected.isEmpty()) {
                        new Alert(Alert.AlertType.WARNING, "请选择至少一个表").show();
                        return;
                }

                FileChooser fc = new FileChooser();
                fc.setTitle("选择导出目录");
                fc.setInitialFileName(selected.get(0));
                File dir = fc.showSaveDialog(this);
                if (dir == null) return;

                String format = formatCombo.getValue();

                new Thread(() -> {
                        long totalRows = 0;
                        for (String table : selected) {
                                try {
                                        Platform.runLater(() -> statusLabel.setText("导出: " + table));
                                        List<Column> columns = currentDriver.getColumns(currentSession, table);
                                        QueryResult result = currentDriver.selectByPage(currentSession, table, 0, 100000);
                                        List<GridRow> rows = result.getRows();

                                        String ext = "CSV".equals(format) ? ".csv" : "JSON".equals(format) ? ".json" : ".sql";
                                        Path filePath = Path.of(dir.getParent(), table + ext);

                                        if ("CSV".equals(format)) {
                                                try (var target = new CsvTarget(filePath, "UTF-8", ',', includeHeaderChk.isSelected())) {
                                                        target.open(columns);
                                                        for (GridRow row : rows) target.writeRow(row.toArray());
                                                }
                                        } else if ("JSON".equals(format)) {
                                                try (var target = new JsonTarget(filePath, "UTF-8", false)) {
                                                        target.open(columns);
                                                        for (GridRow row : rows) target.writeRow(row.toArray());
                                                }
                                        } else {
                                                try (var target = new SqlInsertTarget(filePath, table, "UTF-8", currentDriver.getDialect())) {
                                                        target.open(columns);
                                                        for (GridRow row : rows) target.writeRow(row.toArray());
                                                }
                                        }

                                        totalRows += rows.size();
                                        final long done = totalRows;
                                        Platform.runLater(() -> {
                                                progressBar.setProgress((double) selected.indexOf(table) / selected.size());
                                                statusLabel.setText("已导出 " + done + " 行");
                                        });
                                } catch (Exception e) {
                                        Platform.runLater(() -> statusLabel.setText("错误: " + e.getMessage()));
                                }
                        }
                        final long finalRows = totalRows;
                        Platform.runLater(() -> {
                                progressBar.setProgress(1);
                                statusLabel.setText("完成! 共导出 " + finalRows + " 行");
                        });
                }).start();
        }
}
