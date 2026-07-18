package valkyrie.app.widgets.table;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import valkyrie.driver.api.Column;
import valkyrie.driver.api.GridRow;

import java.util.List;

/**
 * 表格列宽自适应工具
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class ColumnAutoFit
{
        private static final double PADDING = 24;
        private static final double MIN_WIDTH = 60;
        private static final double MAX_WIDTH = 400;
        private static final double CHAR_WIDTH = 7.5;
        private static final int SAMPLE_ROWS = 200;

        private ColumnAutoFit()
        {
        }

        /**
         * 自动调整所有列宽
         */
        public static void fitColumns(TableView<GridRow> tableView, List<Column> columns)
        {
                if (columns == null || tableView == null)
                        return;

                var items = tableView.getItems();
                int rowCount = Math.min(items.size(), SAMPLE_ROWS);

                for (int colIdx = 0; colIdx < columns.size() && colIdx < tableView.getColumns().size(); colIdx++) {
                        TableColumn<GridRow, ?> col = tableView.getColumns().get(colIdx);
                        Column meta = columns.get(colIdx);

                        double maxWidth = estimateWidth(meta.getName());

                        // Also consider type name
                        if (meta.getType() != null)
                                maxWidth = Math.max(maxWidth, estimateWidth(meta.getName() + " " + meta.getType()));

                        // Sample data rows
                        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                                Object cell = items.get(rowIdx).get(colIdx);
                                if (cell != null) {
                                        maxWidth = Math.max(maxWidth, estimateWidth(cell.toString()));
                                }
                        }

                        double finalWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, maxWidth + PADDING));
                        col.setPrefWidth(finalWidth);
                }
        }

        private static double estimateWidth(String text)
        {
                if (text == null)
                        return 0;
                return text.length() * CHAR_WIDTH;
        }
}
