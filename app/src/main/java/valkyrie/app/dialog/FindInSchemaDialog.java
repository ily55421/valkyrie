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

import java.util.ArrayList;
import java.util.List;

/**
 * 在数据库或模式中查找对话框
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class FindInSchemaDialog extends Stage
{
        private final TextField keywordField = new TextField();
        private final ComboBox<UIConnectionNode> connCombo = new ComboBox<>();
        private final ComboBox<String> catalogCombo = new ComboBox<>();
        private final ListView<String> resultList = new ListView<>();
        private final CheckBox tableChk = new CheckBox("表");
        private final CheckBox columnChk = new CheckBox("列");
        private final CheckBox indexChk = new CheckBox("索引");

        private Driver currentDriver;
        private Session currentSession;

        public FindInSchemaDialog()
        {
                initOwner(Application.primaryStage);
                initModality(Modality.WINDOW_MODAL);
                setTitle("在数据库或模式中查找");
                setWidth(700);
                setHeight(500);

                tableChk.setSelected(true);

                connCombo.getItems().addAll(GlobalDynamicNodeContext.getConnectionNodes());
                connCombo.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(UIConnectionNode n) { return n != null ? n.getLabel() : ""; }
                        @Override public UIConnectionNode fromString(String s) { return null; }
                });
                connCombo.valueProperty().addListener((obs, old, conn) -> {
                        catalogCombo.getItems().clear();
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
                        if (catalog != null && currentDriver != null) {
                                currentSession = Session.ofCatalog(catalog);
                        }
                });

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("关键字:"), 0, 0);
                grid.add(keywordField, 1, 0);
                grid.add(new Label("连接:"), 0, 1);
                grid.add(connCombo, 1, 1);
                grid.add(new Label("数据库:"), 0, 2);
                grid.add(catalogCombo, 1, 2);
                GridPane.setColumnSpan(keywordField, 2);

                HBox typeBox = new HBox(10, new Label("查找类型:"), tableChk, columnChk, indexChk);

                Button searchBtn = new Button("查找");
                searchBtn.setOnAction(e -> search());

                HBox toolbar = new HBox(10, searchBtn);
                toolbar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                VBox top = new VBox(10, grid, typeBox, toolbar);
                top.setPadding(new Insets(15));

                BorderPane root = new BorderPane();
                root.setTop(top);
                root.setCenter(resultList);
                root.setPadding(new Insets(10));

                setScene(new Scene(root));
        }

        private void search()
        {
                String keyword = keywordField.getText().trim();
                if (keyword.isEmpty()) return;
                if (currentDriver == null || currentSession == null) {
                        new Alert(Alert.AlertType.WARNING, "请选择连接和数据库").show();
                        return;
                }

                resultList.getItems().clear();
                resultList.getItems().add("查找中...");

                new Thread(() -> {
                        List<String> results = new ArrayList<>();
                        try {
                                List<Table> tables = currentDriver.getTables(currentSession);
                                String kw = keyword.toLowerCase();

                                for (Table table : tables) {
                                        if (Thread.currentThread().isInterrupted()) break;

                                        if (tableChk.isSelected() && table.getName().toLowerCase().contains(kw)) {
                                                results.add("[表] " + table.getName());
                                        }

                                        if (columnChk.isSelected()) {
                                                try {
                                                        var columns = currentDriver.getColumns(currentSession, table.getName());
                                                        for (var col : columns) {
                                                                if (col.getName().toLowerCase().contains(kw)) {
                                                                        results.add("[列] " + table.getName() + "." + col.getName() + " (" + col.getType() + ")");
                                                                }
                                                        }
                                                } catch (Exception ignored) {}
                                        }
                                }
                        } catch (Exception e) {
                                results.add("查找失败: " + e.getMessage());
                        }

                        Platform.runLater(() -> {
                                resultList.getItems().clear();
                                if (results.isEmpty())
                                        resultList.getItems().add("未找到匹配项");
                                else
                                        resultList.getItems().addAll(results);
                        });
                }).start();
        }
}
