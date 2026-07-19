package valkyrie.app.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.theme.ThemeManager;
import valkyrie.core.repository.SettingsRepository;

/**
 * 选项对话框
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class PreferencesDialog extends Stage
{
        public PreferencesDialog()
        {
                initOwner(Application.primaryStage);
                initModality(Modality.WINDOW_MODAL);
                setTitle("选项");
                setWidth(600);
                setHeight(480);

                TabPane tabPane = new TabPane();
                tabPane.getTabs().addAll(
                        createGeneralTab(),
                        createEditorTab(),
                        createGridTab(),
                        createSyncTab(),
                        createShortcutTab()
                );

                Button save = new Button("确定");
                save.setOnAction(e -> { save(); close(); });
                Button cancel = new Button("取消");
                cancel.setOnAction(e -> close());

                HBox buttons = new HBox(10, cancel, save);
                buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                buttons.setPadding(new Insets(10));

                BorderPane root = new BorderPane();
                root.setCenter(tabPane);
                root.setBottom(buttons);
                setScene(new Scene(root));
        }

        private final ComboBox<String> themeCombo = new ComboBox<>();

        private Tab createGeneralTab()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                themeCombo.getItems().addAll("亮色", "暗色");
                String current = SettingsRepository.get("theme", "LIGHT");
                themeCombo.setValue("DARK".equals(current) ? "暗色" : "亮色");

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("主题:"), 0, 0);
                grid.add(themeCombo, 1, 0);

                box.getChildren().add(grid);
                return new Tab("常规", box);
        }

        private final Spinner<Integer> fontSizeSpinner = new Spinner<>(8, 32, 14);
        private final Spinner<Integer> tabWidthSpinner = new Spinner<>(2, 8, 4);

        private Tab createEditorTab()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                fontSizeSpinner.getValueFactory().setValue(
                        Integer.parseInt(SettingsRepository.get("editor.fontSize", "14")));
                tabWidthSpinner.getValueFactory().setValue(
                        Integer.parseInt(SettingsRepository.get("editor.tabWidth", "4")));

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("字体大小:"), 0, 0);
                grid.add(fontSizeSpinner, 1, 0);
                grid.add(new Label("Tab 宽度:"), 0, 1);
                grid.add(tabWidthSpinner, 1, 1);

                box.getChildren().add(grid);
                return new Tab("编辑器", box);
        }

        private final ComboBox<String> pageSizeCombo = new ComboBox<>();
        private final TextField nullDisplayField = new TextField("(NULL)");

        private Tab createGridTab()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                pageSizeCombo.getItems().addAll("100", "500", "1000");
                pageSizeCombo.setValue(SettingsRepository.get("grid.pageSize", "100"));
                nullDisplayField.setText(SettingsRepository.get("grid.nullDisplay", "(NULL)"));

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("默认页大小:"), 0, 0);
                grid.add(pageSizeCombo, 1, 0);
                grid.add(new Label("NULL 显示:"), 0, 1);
                grid.add(nullDisplayField, 1, 1);

                box.getChildren().add(grid);
                return new Tab("数据网格", box);
        }

        private final Spinner<Integer> batchSizeSpinner = new Spinner<>(100, 10000, 1000);

        private Tab createSyncTab()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                batchSizeSpinner.getValueFactory().setValue(
                        Integer.parseInt(SettingsRepository.get("sync.batchSize", "1000")));

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("批量大小:"), 0, 0);
                grid.add(batchSizeSpinner, 1, 0);

                box.getChildren().add(grid);
                return new Tab("同步", box);
        }

        private Tab createShortcutTab()
        {
                VBox box = new VBox(10);
                box.setPadding(new Insets(20));

                String[][] shortcuts = {
                        {"F6", "命令列界面"},
                        {"Ctrl+L", "历史日志"},
                        {"Ctrl+Shift+F", "全局查找"},
                        {"Ctrl+1~9", "收藏夹槽位"},
                        {"Ctrl+N", "新建查询"},
                        {"Ctrl+Enter", "执行 SQL"},
                        {"Ctrl+S", "保存查询"}
                };

                ListView<String> list = new ListView<>();
                for (String[] s : shortcuts)
                        list.getItems().add(s[0] + "\t" + s[1]);

                box.getChildren().addAll(new Label("快捷键清单:"), list);
                return new Tab("快捷键", box);
        }

        private void save()
        {
                SettingsRepository.set("theme", "暗色".equals(themeCombo.getValue()) ? "DARK" : "LIGHT");
                ThemeManager.switchTheme("暗色".equals(themeCombo.getValue())
                        ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT);
                SettingsRepository.set("editor.fontSize", String.valueOf(fontSizeSpinner.getValue()));
                SettingsRepository.set("editor.tabWidth", String.valueOf(tabWidthSpinner.getValue()));
                SettingsRepository.set("grid.pageSize", pageSizeCombo.getValue());
                SettingsRepository.set("grid.nullDisplay", nullDisplayField.getText());
                SettingsRepository.set("sync.batchSize", String.valueOf(batchSizeSpinner.getValue()));
        }
}
