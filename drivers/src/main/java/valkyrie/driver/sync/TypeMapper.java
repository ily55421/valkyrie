package valkyrie.driver.sync;

import valkyrie.driver.api.Column;
import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.type.LogicalType;
import valkyrie.driver.sync.model.ColumnDefinition;

/**
 * 类型映射器
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class TypeMapper
{
        /**
         * 列映射结果
         */
        public record ColumnMapping(
                ColumnDefinition source,
                String targetNativeType,
                boolean degraded,
                String warning
        )
        {
        }

        private TypeMapper()
        {
        }

        /**
         * 源列定义 → 目标库列映射
         */
        public static ColumnMapping map(ColumnDefinition source, Dialect sourceDialect, Dialect targetDialect)
        {
                LogicalType logical = source.logicalType();
                Integer size = source.size();
                Integer scale = source.scale();
                String warning = null;
                boolean degraded = false;

                // Length conversion rate
                double rate = targetDialect.lengthRate(sourceDialect);
                if (size != null && rate != 1.0) {
                        size = (int) Math.ceil(size * rate);
                }

                // Check varchar degradation threshold
                if (logical == LogicalType.STRING && size != null) {
                        int threshold = targetDialect.varcharDegradeThreshold();
                        if (threshold > 0 && size > threshold) {
                                logical = targetDialect.varcharDegradeTarget();
                                degraded = true;
                                warning = "VARCHAR(" + size + ") exceeds threshold " + threshold + ", degraded to " + logical;
                                size = null;
                        }
                }

                // UNKNOWN type warning
                if (logical == LogicalType.UNKNOWN) {
                        warning = "Unknown type: " + source.sourceNativeType() + ", fallback to VARCHAR(255)";
                }

                String targetNativeType = targetDialect.toNativeType(logical, size, scale);

                return new ColumnMapping(source, targetNativeType, degraded, warning);
        }

        /**
         * 从 Driver Column 转换为 ColumnDefinition
         */
        public static ColumnDefinition toColumnDefinition(Column column, Dialect dialect)
        {
                String nativeType = column.getType();
                LogicalType logical = dialect.toLogicalType(nativeType);

                Integer size = column.getSize();
                Integer scale = column.getDecimalDigits();

                return new ColumnDefinition(
                        column.getName(),
                        logical,
                        nativeType,
                        size,
                        scale,
                        column.isNotNull(),
                        column.isAutoIncrement(),
                        column.getDefaultValue(),
                        column.getComment()
                );
        }

        /**
         * 判断是否为日期时间类型（双判机制，对策 K2）
         */
        public static boolean isDateTime(int jdbcType, String nativeTypeName)
        {
                if (jdbcType == java.sql.Types.DATE
                        || jdbcType == java.sql.Types.TIME
                        || jdbcType == java.sql.Types.TIMESTAMP
                        || jdbcType == java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                        return true;
                if (nativeTypeName == null) return false;
                String n = nativeTypeName.toUpperCase();
                return n.contains("DATE") || n.contains("TIME");
        }
}
