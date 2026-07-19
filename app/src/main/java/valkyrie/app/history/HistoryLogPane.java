package valkyrie.app.history;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import valkyrie.core.repository.HistoryRepository;
import valkyrie.app.Publisher;

import java.util.List;

/**
 * 历史日志面板
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class HistoryLogPane extends BorderPane
{
        private final TableView<HistoryRepository.HistoryEntry> table = new TableView<>();
        private final TextField searchField = new TextField();
        private final ComboBox<String> statusFilter = new ComboBox<>();

        public HistoryLogPane()
        {
                setPadding(new Insets(5));

                // Columns
                var timeCol = new TableColumn<HistoryRepository.HistoryEntry, String>("时间");
                timeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTime()));
                timeCol.setPrefWidth(180);

                var connCol = new TableColumn<HistoryRepository.HistoryEntry, String>("连接");
                connCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getConnection()));
                connCol.setPrefWidth(120);

                var sqlCol = new TableColumn<HistoryRepository.HistoryEntry, String>("SQL");
                sqlCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getSql()));
                sqlCol.setPrefWidth(400);

                var durCol = new TableColumn<HistoryRepository.HistoryEntry, String>("耗时");
                durCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDurationMs() + "ms"));
                durCol.setPrefWidth(80);

                var statusCol = new TableColumn<HistoryRepository.HistoryEntry, String>("状态");
                statusCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus()));
                statusCol.setPrefWidth(80);

                table.getColumns().addAll(timeCol, connCol, sqlCol, durCol, statusCol);

                // Double click to re-execute
                table.setRowFactory(tv -> {
                        TableRow<HistoryRepository.HistoryEntry> row = new TableRow<>();
                        row.setOnMouseClicked(e -> {
                                if (e.getClickCount() == 2 && !row.isEmpty()) {
                                        Publisher.openQueryEditor(row.getItem().getSql(), "history.sql");
                                }
                        });
                        return row;
                });

                // Toolbar
                searchField.setPromptText("搜索 SQL...");
                searchField.textProperty().addListener((o, a, n) -> refresh());

                statusFilter.getItems().addAll("全部", "OK", "ERROR");
                statusFilter.setValue("全部");
                statusFilter.setOnAction(e -> refresh());

                Button refreshBtn = new Button("刷新");
                refreshBtn.setOnAction(e -> refresh());

                Button clearBtn = new Button("清空全部");
                clearBtn.setOnAction(e -> {
                        if (new Alert(Alert.AlertType.CONFIRMATION, "确定清空所有历史?").showAndWait().orElse(null) == ButtonType.OK) {
                                HistoryRepository.clearAll();
                                refresh();
                        }
                });

                HBox toolbar = new HBox(8, searchField, statusFilter, refreshBtn, clearBtn);
                toolbar.setPadding(new Insets(5));

                setTop(toolbar);
                setCenter(table);

                refresh();
        }

        private void refresh()
        {
                new Thread(() -> {
                        String kw = searchField.getText();
                        String status = statusFilter.getValue();
                        String statusFilterVal = "全部".equals(status) ? null : status;
                        List<HistoryRepository.HistoryEntry> entries = HistoryRepository.query(kw, null, statusFilterVal);
                        Platform.runLater(() -> {
                                table.getItems().clear();
                                table.getItems().addAll(entries);
                        });
                }).start();
        }
}
