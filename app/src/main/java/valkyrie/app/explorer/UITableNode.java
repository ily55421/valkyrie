package valkyrie.app.explorer;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import valkyrie.app.Application;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Table;
import valkyrie.app.widgets.VkContextMenu;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
@SuppressWarnings("FieldCanBeLocal")
public class UITableNode extends UIExplorerNode
{
        private final Driver driver;
        private final UICatalogNode catalog;
        private final Table table;

        public UITableNode(Driver driver, UICatalogNode catalog, Table table)
        {
                super(catalog, table.getName(), "table");
                this.catalog = catalog;
                this.driver = driver;
                this.table = table;
        }

        @Override
        public VkContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem openTableItem = new MenuItem("打开表");
                openTableItem.setOnAction(event -> openDataGridBrowserPane());

                MenuItem designTableItem = new MenuItem("设计表");
                designTableItem.setOnAction(event -> openTableDesignerPane());

                MenuItem copyTableNameItem = new MenuItem("复制表名");
                copyTableNameItem.setOnAction(event -> {
                        Application.copyToClipboard(getLabel());
                });

                MenuItem copyCreateTableDLLItem = new MenuItem("复制建表语句");
                copyCreateTableDLLItem.setOnAction(event -> {
                        Application.copyToClipboard(driver.showCreateTable(catalog.getSession(), getLabel()));
                });

                MenuItem refreshTableItem = new MenuItem("刷新列表");
                refreshTableItem.setOnAction(event -> catalog.reloadTableNode());

                contextMenu.getItems().addAll(
                        openTableItem,
                        designTableItem,
                        new SeparatorMenuItem(),
                        copyTableNameItem,
                        copyCreateTableDLLItem,
                        new SeparatorMenuItem(),
                        refreshTableItem
                );

                return contextMenu;
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                openDataGridBrowserPane();
        }

        public void openDataGridBrowserPane()
        {
                // TODO: OpenTableDataPaneEvent API changed
        }

        public void openTableDesignerPane()
        {
                // TODO: OpenTableDesignerPaneEvent API changed
        }
}
