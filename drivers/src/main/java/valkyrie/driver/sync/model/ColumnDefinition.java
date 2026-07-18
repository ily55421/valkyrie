package valkyrie.driver.sync.model;

import valkyrie.driver.api.type.LogicalType;

/**
 * 列定义（同步用）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public record ColumnDefinition(
        String name,
        LogicalType logicalType,
        String sourceNativeType,
        Integer size,
        Integer scale,
        boolean notNull,
        boolean autoIncrement,
        String defaultValue,
        String comment
)
{
}
