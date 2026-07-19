package valkyrie.driver.transfer;

import valkyrie.driver.api.Column;
import valkyrie.driver.api.type.LogicalType;

import java.util.List;

/**
 * 数据目标接口（导出）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public interface DataTarget extends AutoCloseable
{
        void open(List<Column> columns) throws Exception;
        void writeRow(Object[] row) throws Exception;
        @Override void close() throws Exception;

        /**
         * 列规格（用于文件源推断）
         */
        record ColumnSpec(String name, LogicalType inferredType) {}
}
