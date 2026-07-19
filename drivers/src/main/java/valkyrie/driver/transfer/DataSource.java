package valkyrie.driver.transfer;

import java.util.List;

/**
 * 数据源接口（导入）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public interface DataSource extends AutoCloseable
{
        List<DataTarget.ColumnSpec> columns() throws Exception;
        Object[] readRow() throws Exception;
        @Override void close() throws Exception;
}
