package valkyrie.app.menu;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import valkyrie.app.Publisher;
import valkyrie.app.sync.SyncWizardDialog;
import valkyrie.app.theme.ThemeManager;
import valkyrie.utils.system.OS;

/**
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

                // 文件菜单
                Menu fileMenu = new Menu("文件");

                MenuItem newQueryItem = new MenuItem("新建查询");
                newQueryItem.setOnAction(e -> Publisher.openQueryEditor());

                MenuItem importItem = new MenuItem("导入");
                MenuItem exportItem = new MenuItem("导出");

                MenuItem exitItem = new MenuItem("退出");
                fileMenu.getItems().addAll(
                        ConnectionMenuBuilder.buildMenu(),
                        newQueryItem,
                        new SeparatorMenuItem(),
                        importItem,
                        exportItem,
                        new SeparatorMenuItem(),
                        exitItem);

                // 编辑菜单
                Menu editMenu = new Menu("编辑");
                MenuItem copyItem = new MenuItem("复制");
                MenuItem pasteItem = new MenuItem("粘贴");
                editMenu.getItems().addAll(copyItem, pasteItem);

                // 代码菜单
                Menu codeMenu = new Menu("代码");

                // 运行菜单
                Menu runMenu = new Menu("运行");

                // 视图菜单
                Menu viewMenu = new Menu("视图");
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
                viewMenu.getItems().add(themeMenu);

                // 工具菜单
                Menu toolsMenu = new Menu("工具");
                MenuItem syncItem = new MenuItem("跨库同步");
                syncItem.setOnAction(e -> new SyncWizardDialog().show());
                toolsMenu.getItems().add(syncItem);

                // 帮助菜单
                Menu helpMenu = new Menu("帮助");
                MenuItem aboutItem = new MenuItem("关于");
                helpMenu.getItems().add(aboutItem);

                getMenus().addAll(
                        fileMenu,
                        editMenu,
                        codeMenu,
                        runMenu,
                        viewMenu,
                        toolsMenu,
                        helpMenu
                );
        }
}
