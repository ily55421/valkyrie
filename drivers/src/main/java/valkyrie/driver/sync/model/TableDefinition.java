package valkyrie.driver.sync.model;

import java.util.List;

/**
 * 表结构定义（同步用的规范化中间模型）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public record TableDefinition(
        String name,
        String comment,
        List<ColumnDefinition> columns,
        List<String> primaryKeys,
        List<IndexDefinition> indexes
)
{
}
