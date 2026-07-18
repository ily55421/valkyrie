package valkyrie.driver.api;

import valkyrie.driver.api.type.LogicalType;
import valkyrie.driver.sync.model.ColumnDefinition;
import valkyrie.driver.sync.model.IndexDefinition;
import valkyrie.driver.sync.model.TableDefinition;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 数据库方言接口。
 * <p>
 * 用于封装特定数据库的 SQL 语法差异，为上层的 SQL 构建和执行提供统一适配。
 * 不同数据库（MySQL、PostgreSQL、Oracle 等）应提供各自的 {@code Dialect} 实现，
 * 以生成符合该数据库语法的 SQL 语句。
 * <p>
 * <b>职责范围：</b>
 * <ul>
 *   <li>生成数据库特定的 DDL 语句（如 {@code SHOW CREATE TABLE}）</li>
 *   <li>生成分页查询的 {@code LIMIT} 或 {@code ROWNUM} 子句</li>
 *   <li>转义标识符（表名、列名等）以防止 SQL 注入或处理保留字</li>
 *   <li>类型映射（物理类型 ↔ 逻辑类型）</li>
 *   <li>DDL 生成（建表/加列/改列/主键/索引）</li>
 *   <li>DML 批量语句生成</li>
 * </ul>
 * <p>
 * <b>实现注意事项：</b>
 * <ul>
 *   <li>实现类应为无状态（stateless）且线程安全</li>
 *   <li>每个数据库产品应有唯一对应的方言实现</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public interface Dialect
{
        // ===== 现有（保持不变）=====

        default String limit(String sql, int off, int size)
        {
                return sql + " LIMIT " + size + " OFFSET " + off;
        }

        default String paginate(String sql, long offset, int limit)
        {
                String trimmed = sql.strip();
                if (trimmed.endsWith(";"))
                        trimmed = trimmed.substring(0, trimmed.length() - 1);
                return limit(trimmed, (int) offset, limit);
        }

        default String normalize(String sql) { return sql; }

        String quote(String identifier);

        String removeQuote(String identifier);

        // ===== 组 1：关键字与标识符 =====

        /**
         * 方言保留关键字集（大写）
         */
        default Set<String> keywords() { return Set.of(); }

        /**
         * 仅当标识符是关键字时才加引号
         */
        default String quoteIfKeyword(String identifier)
        {
                if (identifier == null || identifier.isBlank())
                        return identifier;
                return keywords().contains(identifier.toUpperCase(Locale.ROOT))
                        ? quote(identifier) : identifier;
        }

        // ===== 组 2：物理限制钩子 =====

        /** 单索引键最大字节数，无限制返回 -1 */
        default int maxIndexKeyBytes() { return -1; }

        /** 单行最大字节数，无限制返回 -1 */
        default int maxRowBytes() { return -1; }

        /** VARCHAR 最大长度阈值，超过应降级为大文本，无限制 -1 */
        default int varcharDegradeThreshold() { return -1; }

        /** VARCHAR 超阈值后的降级目标类型 */
        default LogicalType varcharDegradeTarget() { return LogicalType.TEXT; }

        // ===== 组 3：类型映射（逻辑中枢）=====

        /**
         * 本库物理类型名 → 统一逻辑类型（含 size/scale 解析）
         */
        LogicalType toLogicalType(String nativeType);

        /**
         * 统一逻辑类型 + 长度/精度 → 本库物理类型表达式
         */
        String toNativeType(LogicalType logical, Integer size, Integer scale);

        /**
         * 长度语义换算比率
         */
        default double lengthRate(Dialect source) { return 1.0; }

        // ===== 组 4：DDL 生成（纯文本，不执行）=====

        default List<String> createTableSql(TableDefinition table)
        {
                throw new UnsupportedOperationException("createTableSql not implemented");
        }

        default List<String> addColumnSql(String table, ColumnDefinition column)
        {
                throw new UnsupportedOperationException("addColumnSql not implemented");
        }

        default List<String> modifyColumnSql(String table, ColumnDefinition column)
        {
                throw new UnsupportedOperationException("modifyColumnSql not implemented");
        }

        default List<String> primaryKeySql(String table, List<String> pkColumns)
        {
                throw new UnsupportedOperationException("primaryKeySql not implemented");
        }

        default List<String> createIndexSql(String table, IndexDefinition index)
        {
                throw new UnsupportedOperationException("createIndexSql not implemented");
        }

        // ===== 组 5：数据同步支持 =====

        default String insertSql(String table, List<String> columns)
        {
                StringBuilder sb = new StringBuilder("INSERT INTO ");
                sb.append(quoteIfKeyword(table)).append(" (");
                sb.append(String.join(", ", columns.stream().map(this::quoteIfKeyword).toList()));
                sb.append(") VALUES (");
                sb.append(String.join(", ", columns.stream().map(c -> "?").toList()));
                sb.append(")");
                return sb.toString();
        }

        default String updateSql(String table, List<String> columns, List<String> pkColumns)
        {
                StringBuilder sb = new StringBuilder("UPDATE ");
                sb.append(quoteIfKeyword(table)).append(" SET ");
                sb.append(String.join(", ", columns.stream()
                        .filter(c -> !pkColumns.contains(c))
                        .map(c -> quoteIfKeyword(c) + " = ?").toList()));
                sb.append(" WHERE ");
                sb.append(String.join(" AND ", pkColumns.stream()
                        .map(c -> quoteIfKeyword(c) + " = ?").toList()));
                return sb.toString();
        }

        /** JDBC URL 是否支持批量重写参数 */
        default String batchUrlParam() { return null; }
}
