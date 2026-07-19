package valkyrie.app.dialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.explorer.GlobalDynamicNodeContext;
import valkyrie.driver.api.*;

import java.util.List;

/**
 * 数据生成对话框
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class DataGenDialog extends Stage
{
        private final ComboBox<UIConnectionNode> connCombo = new ComboBox<>();
        private final ComboBox<String> catalogCombo = new ComboBox<>();
        private final ComboBox<String> tableCombo = new ComboBox<>();
        private final Spinner<Integer> rowsSpinner = new Spinner<>(1, 1000000, 100);
        private final TextArea previewArea = new TextArea();
        private final ProgressBar progressBar = new ProgressBar(0);

        private Driver currentDriver;
        private Session currentSession;

        public DataGenDialog()
        {
                initOwner(Application.primaryStage);
                initModality(Modality.WINDOW_MODAL);
                setTitle("数据生成");
                setWidth(700);
                setHeight(550);

                connCombo.getItems().addAll(GlobalDynamicNodeContext.getConnectionNodes());
                connCombo.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(UIConnectionNode n) { return n != null ? n.getLabel() : ""; }
                        @Override public UIConnectionNode fromString(String s) { return null; }
                });
                connCombo.valueProperty().addListener((obs, old, conn) -> {
                        catalogCombo.getItems().clear();
                        tableCombo.getItems().clear();
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
                        tableCombo.getItems().clear();
                        if (catalog != null && currentDriver != null) {
                                currentSession = Session.ofCatalog(catalog);
                                new Thread(() -> {
                                        try {
                                                List<Table> tables = currentDriver.getTables(currentSession);
                                                Platform.runLater(() -> {
                                                        tables.forEach(t -> tableCombo.getItems().add(t.getName()));
                                                });
                                        } catch (Exception e) {
                                                Platform.runLater(() -> previewArea.setText("错误: " + e.getMessage()));
                                        }
                                }).start();
                        }
                });

                previewArea.setEditable(false);
                previewArea.setPrefRowCount(10);

                Button previewBtn = new Button("预览 100 行");
                previewBtn.setOnAction(e -> preview());

                Button generateBtn = new Button("开始生成");
                generateBtn.setOnAction(e -> generate());

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("连接:"), 0, 0);
                grid.add(connCombo, 1, 0);
                grid.add(new Label("数据库:"), 0, 1);
                grid.add(catalogCombo, 1, 1);
                grid.add(new Label("目标表:"), 0, 2);
                grid.add(tableCombo, 1, 2);
                grid.add(new Label("行数:"), 0, 3);
                grid.add(rowsSpinner, 1, 3);

                HBox buttons = new HBox(10, previewBtn, generateBtn);

                VBox root = new VBox(10, grid, new Label("预览:"), previewArea, progressBar, buttons);
                root.setPadding(new Insets(15));

                setScene(new Scene(root));
        }

        private void preview()
        {
                String table = tableCombo.getValue();
                if (table == null || currentDriver == null || currentSession == null) {
                        new Alert(Alert.AlertType.WARNING, "请选择表").show();
                        return;
                }

                new Thread(() -> {
                        try {
                                List<Column> columns = currentDriver.getColumns(currentSession, table);
                                List<valkyrie.driver.datagen.DataGenerator.ColumnGenConfig> configs = new java.util.ArrayList<>();
                                for (Column col : columns) {
                                        configs.add(new valkyrie.driver.datagen.DataGenerator.ColumnGenConfig(
                                                col, valkyrie.driver.datagen.DataGenerator.inferDefault(col)));
                                }

                                var gen = new valkyrie.driver.datagen.DataGenerator(configs, 100);
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < columns.size(); i++) {
                                        if (i > 0) sb.append("\t");
                                        sb.append(columns.get(i).getName());
                                }
                                sb.append("\n");

                                gen.stream().limit(100).forEach(row -> {
                                        for (int i = 0; i < row.length; i++) {
                                                if (i > 0) sb.append("\t");
                                                sb.append(row[i] != null ? row[i].toString() : "NULL");
                                        }
                                        sb.append("\n");
                                });

                                Platform.runLater(() -> previewArea.setText(sb.toString()));
                        } catch (Exception e) {
                                Platform.runLater(() -> previewArea.setText("错误: " + e.getMessage()));
                        }
                }).start();
        }

        private void generate()
        {
                int rows = rowsSpinner.getValue();
                if (rows > 1000000) {
                        new Alert(Alert.AlertType.WARNING, "行数上限 100 万").show();
                        return;
                }

                String table = tableCombo.getValue();
                if (table == null || currentDriver == null || currentSession == null) return;

                new Thread(() -> {
                        try {
                                List<Column> columns = currentDriver.getColumns(currentSession, table);
                                List<valkyrie.driver.datagen.DataGenerator.ColumnGenConfig> configs = new java.util.ArrayList<>();
                                for (Column col : columns) {
                                        configs.add(new valkyrie.driver.datagen.DataGenerator.ColumnGenConfig(
                                                col, valkyrie.driver.datagen.DataGenerator.inferDefault(col)));
                                }

                                var gen = new valkyrie.driver.datagen.DataGenerator(configs, rows);
                                String insertSql = currentDriver.getDialect().insertSql(table,
                                        columns.stream().map(Column::getName).toList());

                                final int[] count = {0};
                                try (var conn = currentDriver.getDataSource().getConnection()) {
                                        conn.setAutoCommit(false);
                                        try (var ps = conn.prepareStatement(insertSql)) {
                                                final int[] batch = {0};
                                                gen.stream().forEach(row -> {
                                                        try {
                                                                for (int i = 0; i < row.length; i++)
                                                                        ps.setObject(i + 1, row[i]);
                                                                ps.addBatch();
                                                                batch[0]++;
                                                                if (batch[0] >= 1000) {
                                                                        ps.executeBatch();
                                                                        conn.commit();
                                                                        batch[0] = 0;
                                                                }
                                                                count[0]++;
                                                        } catch (Exception e) {
                                                                throw new RuntimeException(e);
                                                        }
                                                });
                                                if (batch[0] > 0) { ps.executeBatch(); conn.commit(); }
                                        }
                                }

                                Platform.runLater(() -> {
                                        progressBar.setProgress(1);
                                        new Alert(Alert.AlertType.INFORMATION, "已生成 " + count[0] + " 行").show();
                                });
                        } catch (Exception e) {
                                Platform.runLater(() ->
                                        new Alert(Alert.AlertType.ERROR, "生成失败: " + e.getMessage()).show());
                        }
                }).start();
        }
}
