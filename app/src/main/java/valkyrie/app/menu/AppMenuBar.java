package valkyrie.app.menu;

import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;
import valkyrie.app.Publisher;
import valkyrie.app.console.SqlConsolePane;
import valkyrie.app.dialog.DataGenDialog;
import valkyrie.app.dialog.FindInSchemaDialog;
import valkyrie.app.dialog.PreferencesDialog;
import valkyrie.app.history.HistoryLogPane;
import valkyrie.app.sync.SyncWizardDialog;
import valkyrie.app.theme.ThemeManager;
import valkyrie.app.transfer.DataExportDialog;
import valkyrie.utils.system.OS;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * 菜单栏（v1.2 重组：文件|编辑|查看|代码|运行|收藏夹|工具|窗口|帮助）
 *
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class AppMenuBar extends MenuBar
{
        public AppMenuBar()
        {
                if (OS.isMacOS()) {
                        setUseSystemMenuBar(true);
                        setMinHeight(0);
                        setMaxHeight(0);
                        setMouseTransparent(true);
                }

                getMenus().addAll(
                        buildFileMenu(),
                        buildEditMenu(),
                        buildViewMenu(),
                        buildCodeMenu(),
                        buildRunMenu(),
                        buildFavoritesMenu(),
                        buildToolsMenu(),
                        buildWindowMenu(),
                        buildHelpMenu()
                );
        }

        // ===== 文件 =====

        private Menu buildFileMenu()
        {
                Menu menu = new Menu("文件");

                // 新建查询
                MenuItem newQueryItem = new MenuItem("新建查询");
                newQueryItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));
                newQueryItem.setOnAction(e -> Publisher.openQueryEditor());

                // 打开外部文件
                MenuItem openFileItem = new MenuItem("打开外部文件...");
                openFileItem.setOnAction(e -> openExternalFile());

                // 打开最近使用过的
                Menu recentMenu = new Menu("打开最近使用过的");
                recentMenu.setOnShowing(e -> refreshRecentMenu(recentMenu));

                // 导入连接
                MenuItem importConnItem = new MenuItem("导入连接...");
                importConnItem.setOnAction(e -> importConnections());

                // 导出连接
                MenuItem exportConnItem = new MenuItem("导出连接...");
                exportConnItem.setOnAction(e -> exportConnections());

                // 退出
                MenuItem exitItem = new MenuItem("退出");
                exitItem.setOnAction(e -> javafx.application.Platform.exit());

                menu.getItems().addAll(
                        ConnectionMenuBuilder.buildMenu(),
                        newQueryItem,
                        openFileItem,
                        recentMenu,
                        new SeparatorMenuItem(),
                        importConnItem,
                        exportConnItem,
                        new SeparatorMenuItem(),
                        exitItem
                );

                return menu;
        }

        private void openExternalFile()
        {
                FileChooser fc = new FileChooser();
                fc.setTitle("打开 SQL 文件");
                fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("SQL 文件", "*.sql"),
                        new FileChooser.ExtensionFilter("文本文件", "*.txt"),
                        new FileChooser.ExtensionFilter("所有文件", "*.*")
                );
                File file = fc.showOpenDialog(null);
                if (file == null) return;

                try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        String content;
                        // Encoding detection: try UTF-8 first, fallback to GBK
                        try {
                                content = new String(bytes, "UTF-8");
                        } catch (Exception ex) {
                                content = new String(bytes, "GBK");
                        }
                        Publisher.openQueryEditor(content, file.getName());
                        valkyrie.core.repository.RecentRepository.add(
                                new valkyrie.core.repository.RecentRepository.RecentEntry(
                                        valkyrie.core.repository.RecentRepository.RecentType.QUERY_FILE,
                                        file.getName(), file.getAbsolutePath(), System.currentTimeMillis()));
                } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "打开文件失败: " + e.getMessage()).show();
                }
        }

        private void refreshRecentMenu(Menu recentMenu)
        {
                recentMenu.getItems().clear();
                var recents = valkyrie.core.repository.RecentRepository.getAll();
                if (recents.isEmpty()) {
                        MenuItem empty = new MenuItem("(空)");
                        empty.setDisable(true);
                        recentMenu.getItems().add(empty);
                        return;
                }
                for (var entry : recents) {
                        String typeMark = switch (entry.getType()) {
                                case QUERY_FILE -> "[Q]";
                                case TABLE_TAB -> "[T]";
                                case CONNECTION -> "[C]";
                        };
                        MenuItem item = new MenuItem(typeMark + " " + entry.getLabel());
                        item.setOnAction(e -> {
                                if (entry.getType() == valkyrie.core.repository.RecentRepository.RecentType.QUERY_FILE) {
                                        try {
                                                byte[] bytes = Files.readAllBytes(java.nio.file.Path.of(entry.getPath()));
                                                String content = new String(bytes, "UTF-8");
                                                Publisher.openQueryEditor(content, entry.getLabel());
                                        } catch (Exception ex) {
                                                new Alert(Alert.AlertType.WARNING, "文件不存在: " + entry.getPath()).show();
                                                valkyrie.core.repository.RecentRepository.remove(entry.getPath());
                                        }
                                }
                        });
                        recentMenu.getItems().add(item);
                }
                recentMenu.getItems().add(new SeparatorMenuItem());
                MenuItem clearItem = new MenuItem("清空最近记录");
                clearItem.setOnAction(e -> valkyrie.core.repository.RecentRepository.clear());
                recentMenu.getItems().add(clearItem);
        }

        private void importConnections()
        {
                FileChooser fc = new FileChooser();
                fc.setTitle("导入连接");
                fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Valkyrie 连接文件", "*.vkc.json"),
                        new FileChooser.ExtensionFilter("Navicat 连接文件", "*.ncx"),
                        new FileChooser.ExtensionFilter("所有文件", "*.*")
                );
                File file = fc.showOpenDialog(null);
                if (file == null) return;

                try {
                        java.util.List<valkyrie.core.model.DiskSavedConnection> imported;
                        if (file.getName().endsWith(".ncx")) {
                                imported = valkyrie.core.repository.ConnectionTransfer.importFromNcx(file.toPath());
                        } else {
                                imported = valkyrie.core.repository.ConnectionTransfer.importFromVkc(file.toPath());
                        }

                        for (var conn : imported) {
                                String content = valkyrie.core.utils.JSONUtils.toJSONString(conn);
                                valkyrie.core.repository.ConnectionRepository.saveConnection(conn.getName(), content);
                        }

                        valkyrie.app.event.bus.EventBus.publish(new valkyrie.app.event.RefreshConnectionEvent());

                        String msg = "成功导入 " + imported.size() + " 个连接";
                        if (file.getName().endsWith(".ncx"))
                                msg += "\n注意: Navicat 密码未导入，请手工补录";
                        new Alert(Alert.AlertType.INFORMATION, msg).show();
                } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "导入失败: " + e.getMessage()).show();
                }
        }

        private void exportConnections()
        {
                var connections = valkyrie.core.repository.ConnectionRepository.loadConnections();
                if (connections.isEmpty()) {
                        new Alert(Alert.AlertType.WARNING, "没有可导出的连接").show();
                        return;
                }

                FileChooser fc = new FileChooser();
                fc.setTitle("导出连接");
                fc.setInitialFileName("connections.vkc.json");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Valkyrie 连接文件", "*.vkc.json"));
                File file = fc.showSaveDialog(null);
                if (file == null) return;

                try {
                        String json = valkyrie.core.repository.ConnectionTransfer.exportToJson(connections, false);
                        Files.writeString(file.toPath(), json);
                        new Alert(Alert.AlertType.INFORMATION, "已导出 " + connections.size() + " 个连接").show();
                } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "导出失败: " + e.getMessage()).show();
                }
        }

        // ===== 编辑 =====

        private Menu buildEditMenu()
        {
                Menu menu = new Menu("编辑");
                menu.getItems().addAll(
                        new MenuItem("复制"),
                        new MenuItem("粘贴"),
                        new MenuItem("撤销"),
                        new MenuItem("重做"),
                        new SeparatorMenuItem(),
                        new MenuItem("查找")
                );
                return menu;
        }

        // ===== 查看 =====

        private Menu buildViewMenu()
        {
                Menu menu = new Menu("查看");

                // 主题子菜单
                Menu themeMenu = new Menu("主题");
                ToggleGroup themeGroup = new ToggleGroup();

                RadioMenuItem lightTheme = new RadioMenuItem("亮色");
                lightTheme.setToggleGroup(themeGroup);
                lightTheme.setSelected(true);
                lightTheme.setOnAction(e -> ThemeManager.switchTheme(ThemeManager.Theme.LIGHT));

                RadioMenuItem darkTheme = new RadioMenuItem("暗色");
                darkTheme.setToggleGroup(themeGroup);
                darkTheme.setOnAction(e -> ThemeManager.switchTheme(ThemeManager.Theme.DARK));

                themeMenu.getItems().addAll(lightTheme, darkTheme);

                // 信息窗格
                CheckMenuItem infoPaneItem = new CheckMenuItem("信息窗格");
                infoPaneItem.setSelected(true);

                // 显示隐藏的项目
                CheckMenuItem showSystemItem = new CheckMenuItem("显示隐藏的项目");

                // ER 图表
                MenuItem erItem = new MenuItem("ER 图表...");

                menu.getItems().addAll(
                        themeMenu,
                        new SeparatorMenuItem(),
                        infoPaneItem,
                        showSystemItem,
                        new SeparatorMenuItem(),
                        erItem
                );

                return menu;
        }

        // ===== 代码 =====

        private Menu buildCodeMenu()
        {
                Menu menu = new Menu("代码");
                menu.getItems().addAll(
                        new MenuItem("格式化 SQL"),
                        new MenuItem("注释/取消注释")
                );
                return menu;
        }

        // ===== 运行 =====

        private Menu buildRunMenu()
        {
                Menu menu = new Menu("运行");
                MenuItem executeItem = new MenuItem("执行 SQL");
                executeItem.setAccelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCodeCombination.SHORTCUT_DOWN));
                menu.getItems().addAll(executeItem);
                return menu;
        }

        // ===== 收藏夹 =====

        private Menu buildFavoritesMenu()
        {
                Menu menu = new Menu("收藏夹");

                MenuItem addItem = new MenuItem("添加到收藏夹");

                menu.getItems().add(addItem);
                menu.getItems().add(new SeparatorMenuItem());

                // Slots 1~9
                for (int i = 1; i <= 9; i++) {
                        MenuItem slotItem = new MenuItem(i + "  (空)");
                        slotItem.setAccelerator(new KeyCodeCombination(
                                KeyCode.valueOf("DIGIT" + i), KeyCodeCombination.SHORTCUT_DOWN));
                        final int slot = i;
                        slotItem.setOnAction(e -> activateFavoriteSlot(slot));
                        menu.getItems().add(slotItem);
                }

                menu.getItems().add(new SeparatorMenuItem());

                // 清除收藏夹
                Menu clearMenu = new Menu("清除收藏夹");
                MenuItem clearAll = new MenuItem("清除全部");
                clearAll.setOnAction(e -> {
                        valkyrie.core.repository.FavoriteRepository.clearAll();
                });
                clearMenu.getItems().add(clearAll);

                menu.getItems().add(clearMenu);

                // Update slot labels on showing
                menu.setOnShowing(e -> {
                        for (int i = 0; i < 9; i++) {
                                var fav = valkyrie.core.repository.FavoriteRepository.getBySlot(i + 1);
                                MenuItem item = menu.getItems().get(2 + i);
                                if (item != null)
                                        item.setText((i + 1) + "  " + (fav != null ? fav.getConnection() + "/" + fav.getObjectName() : "(空)"));
                        }
                });

                return menu;
        }

        private void activateFavoriteSlot(int slot)
        {
                var fav = valkyrie.core.repository.FavoriteRepository.getBySlot(slot);
                if (fav == null) return;
                // Would open the favorite object based on type
        }

        // ===== 工具 =====

        private Menu buildToolsMenu()
        {
                Menu menu = new Menu("工具");

                // 跨库同步
                MenuItem syncItem = new MenuItem("跨库同步...");
                syncItem.setOnAction(e -> new SyncWizardDialog().show());

                // 数据传输
                MenuItem transferItem = new MenuItem("数据传输...");
                transferItem.setOnAction(e -> new DataExportDialog().show());

                // 数据生成
                MenuItem dataGenItem = new MenuItem("数据生成...");
                dataGenItem.setOnAction(e -> new DataGenDialog().show());

                // 命令列界面
                MenuItem consoleItem = new MenuItem("命令列界面...");
                consoleItem.setAccelerator(new KeyCodeCombination(KeyCode.F6));
                consoleItem.setOnAction(e -> {
                        // Open SQL Console as a new Tab
                        // For now, show in a new window
                        var pane = new SqlConsolePane();
                        var stage = new javafx.stage.Stage();
                        stage.setTitle("SQL Console");
                        stage.setScene(new javafx.scene.Scene(pane, 800, 500));
                        stage.show();
                });

                // 服务器监控
                Menu monitorMenu = new Menu("服务器监控");

                // 在数据库或模式中查找
                MenuItem findItem = new MenuItem("在数据库或模式中查找...");
                findItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
                findItem.setOnAction(e -> new FindInSchemaDialog().show());

                // 历史日志
                MenuItem historyItem = new MenuItem("历史日志...");
                historyItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCodeCombination.SHORTCUT_DOWN));
                historyItem.setOnAction(e -> {
                        var pane = new HistoryLogPane();
                        var stage = new javafx.stage.Stage();
                        stage.setTitle("历史日志");
                        stage.setScene(new javafx.scene.Scene(pane, 900, 500));
                        stage.show();
                });

                // 选项
                MenuItem optionsItem = new MenuItem("选项...");
                optionsItem.setOnAction(e -> new PreferencesDialog().show());

                menu.getItems().addAll(
                        syncItem,
                        transferItem,
                        dataGenItem,
                        new SeparatorMenuItem(),
                        consoleItem,
                        monitorMenu,
                        findItem,
                        historyItem,
                        new SeparatorMenuItem(),
                        optionsItem
                );

                return menu;
        }

        // ===== 窗口 =====

        private Menu buildWindowMenu()
        {
                Menu menu = new Menu("窗口");

                MenuItem closeTabItem = new MenuItem("关闭选项卡");
                MenuItem closeOthersItem = new MenuItem("关闭其它选项卡");
                MenuItem closeAllItem = new MenuItem("关闭全部选项卡");

                menu.getItems().addAll(
                        closeTabItem,
                        closeOthersItem,
                        closeAllItem,
                        new SeparatorMenuItem()
                );

                return menu;
        }

        // ===== 帮助 =====

        private Menu buildHelpMenu()
        {
                Menu menu = new Menu("帮助");
                MenuItem aboutItem = new MenuItem("关于");
                menu.getItems().add(aboutItem);
                return menu;
        }
}
