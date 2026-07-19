package valkyrie.app.monitor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.ProcessInfo;
import valkyrie.driver.api.Session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务器监控面板
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class ServerMonitorPane extends TabPane
{
        private final Driver driver;
        private final Session session;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Process tab
        private final TableView<ProcessInfo> processTable = new TableView<>();
        private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();
        private final CheckBox autoRefreshChk = new CheckBox("自动刷新 5s");

        // Status tab
        private final TableView<Map.Entry<String, String>> statusTable = new TableView<>();
        private final ObservableList<Map.Entry<String, String>> statusData = FXCollections.observableArrayList();

        // Variables tab
        private final TableView<Map.Entry<String, String>> varTable = new TableView<>();
        private final ObservableList<Map.Entry<String, String>> varData = FXCollections.observableArrayList();

        public ServerMonitorPane(Driver driver, Session session)
        {
                this.driver = driver;
                this.session = session;

                getTabs().addAll(createProcessTab(), createStatusTab(), createVarTab());

                // Auto refresh
                autoRefreshChk.setSelected(true);
                autoRefreshChk.selectedProperty().addListener((o, a, selected) -> {
                        if (selected) startAutoRefresh();
                        else scheduler.shutdown();
                });
                startAutoRefresh();
                refreshAll();
        }

        private void startAutoRefresh()
        {
                scheduler.scheduleAtFixedRate(this::refreshProcess, 0, 5, TimeUnit.SECONDS);
        }

        private Tab createProcessTab()
        {
                var idCol = new TableColumn<ProcessInfo, String>("ID");
                idCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().id())));
                var userCol = new TableColumn<ProcessInfo, String>("User");
                userCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().user()));
                var hostCol = new TableColumn<ProcessInfo, String>("Host");
                hostCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().host()));
                var dbCol = new TableColumn<ProcessInfo, String>("DB");
                dbCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().db()));
                var cmdCol = new TableColumn<ProcessInfo, String>("Command");
                cmdCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().command()));
                var timeCol = new TableColumn<ProcessInfo, String>("Time");
                timeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().time())));
                var stateCol = new TableColumn<ProcessInfo, String>("State");
                stateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().state()));
                var infoCol = new TableColumn<ProcessInfo, String>("Info");
                infoCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().info()));

                processTable.getColumns().addAll(idCol, userCol, hostCol, dbCol, cmdCol, timeCol, stateCol, infoCol);
                processTable.setItems(processData);

                Button refreshBtn = new Button("刷新");
                refreshBtn.setOnAction(e -> refreshProcess());

                Button killBtn = new Button("结束选中进程");
                killBtn.setOnAction(e -> {
                        ProcessInfo selected = processTable.getSelectionModel().getSelectedItem();
                        if (selected == null) return;
                        if (new Alert(Alert.AlertType.CONFIRMATION, "确定结束进程 " + selected.id() + "?")
                                .showAndWait().orElse(null) == ButtonType.OK) {
                                try {
                                        driver.killProcess(session, selected.id());
                                        refreshProcess();
                                } catch (Exception ex) {
                                        new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
                                }
                        }
                });

                HBox toolbar = new HBox(8, refreshBtn, autoRefreshChk, killBtn);
                toolbar.setPadding(new Insets(5));

                VBox box = new VBox(5, toolbar, processTable);
                VBox.setVgrow(processTable, Priority.ALWAYS);
                return new Tab("进程", box);
        }

        private Tab createStatusTab()
        {
                var keyCol = new TableColumn<Map.Entry<String, String>, String>("Key");
                keyCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getKey()));
                var valCol = new TableColumn<Map.Entry<String, String>, String>("Value");
                valCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getValue()));
                statusTable.getColumns().addAll(keyCol, valCol);
                statusTable.setItems(statusData);

                Button refreshBtn = new Button("刷新");
                refreshBtn.setOnAction(e -> refreshStatus());

                HBox toolbar = new HBox(8, refreshBtn);
                toolbar.setPadding(new Insets(5));

                VBox box = new VBox(5, toolbar, statusTable);
                VBox.setVgrow(statusTable, Priority.ALWAYS);
                return new Tab("状态", box);
        }

        private Tab createVarTab()
        {
                var keyCol = new TableColumn<Map.Entry<String, String>, String>("Key");
                keyCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getKey()));
                var valCol = new TableColumn<Map.Entry<String, String>, String>("Value");
                valCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getValue()));
                varTable.getColumns().addAll(keyCol, valCol);
                varTable.setItems(varData);

                TextField search = new TextField();
                search.setPromptText("搜索变量...");
                Button refreshBtn = new Button("刷新");
                refreshBtn.setOnAction(e -> refreshVariables());

                HBox toolbar = new HBox(8, search, refreshBtn);
                toolbar.setPadding(new Insets(5));

                VBox box = new VBox(5, toolbar, varTable);
                VBox.setVgrow(varTable, Priority.ALWAYS);
                return new Tab("变量", box);
        }

        private void refreshAll() { refreshProcess(); refreshStatus(); refreshVariables(); }

        private void refreshProcess()
        {
                new Thread(() -> {
                        try {
                                List<ProcessInfo> list = driver.getProcessList(session);
                                Platform.runLater(() -> {
                                        processData.clear();
                                        processData.addAll(list);
                                });
                        } catch (UnsupportedOperationException e) {
                                Platform.runLater(() -> processData.clear());
                        } catch (Exception e) {
                                Platform.runLater(() -> processData.clear());
                        }
                }).start();
        }

        private void refreshStatus()
        {
                new Thread(() -> {
                        try {
                                Map<String, String> map = driver.getServerStatus(session);
                                Platform.runLater(() -> {
                                        statusData.clear();
                                        statusData.addAll(map.entrySet());
                                });
                        } catch (Exception e) {
                                Platform.runLater(() -> statusData.clear());
                        }
                }).start();
        }

        private void refreshVariables()
        {
                new Thread(() -> {
                        try {
                                Map<String, String> map = driver.getServerVariables(session);
                                Platform.runLater(() -> {
                                        varData.clear();
                                        varData.addAll(map.entrySet());
                                });
                        } catch (Exception e) {
                                Platform.runLater(() -> varData.clear());
                        }
                }).start();
        }

        public void shutdown()
        {
                scheduler.shutdown();
        }
}
