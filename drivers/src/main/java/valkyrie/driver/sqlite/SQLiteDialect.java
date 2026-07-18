package valkyrie.driver.sqlite;

import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.type.LogicalType;
import valkyrie.driver.sync.model.ColumnDefinition;
import valkyrie.driver.sync.model.IndexDefinition;
import valkyrie.driver.sync.model.TableDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static valkyrie.utils.string.StrStaticImports.strcut;

/**
 * SQLite 方言（弱类型亲和性）
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public class SQLiteDialect implements Dialect
{
        private static final Set<String> KEYWORDS = Set.of(
                "ADD", "ALL", "ALTER", "AND", "AS", "AUTOINCREMENT", "BETWEEN", "BY", "CASE",
                "CHECK", "COLLATE", "COLUMN", "COMMIT", "CONSTRAINT", "CREATE", "CROSS",
                "DATABASE", "DEFAULT", "DEFERRABLE", "DELETE", "DESC", "DISTINCT", "DROP",
                "ELSE", "END", "ESCAPE", "EXCEPT", "EXISTS", "FOREIGN", "FROM", "FULL",
                "GROUP", "HAVING", "IN", "INDEX", "INNER", "INSERT", "INTERSECT", "INTO",
                "IS", "ISNULL", "JOIN", "LEFT", "LIKE", "LIMIT", "NOT", "NOTNULL", "NULL",
                "ON", "OR", "ORDER", "OUTER", "PRIMARY", "REFERENCES", "RIGHT", "ROLLBACK",
                "SELECT", "SET", "TABLE", "THEN", "TO", "TRANSACTION", "UNION", "UNIQUE",
                "UPDATE", "USING", "VALUES", "VIEW", "WHEN", "WHERE", "WITH"
        );

        @Override
        public String quote(String identifier) {
                if ((identifier.startsWith("\"") && identifier.endsWith("\"")) ||
                    (identifier.startsWith("`") && identifier.endsWith("`"))) {
                        return identifier;
                }
                return "\"" + identifier + "\"";
        }

        @Override
        public String removeQuote(String identifier)
        {
                if (identifier.startsWith("`") || identifier.startsWith("\"")) {
                        identifier = strcut(identifier, 1, 0);
                        identifier = strcut(identifier, 0, -1);
                        return identifier;
                }
                return identifier;
        }

        @Override
        public Set<String> keywords() { return KEYWORDS; }

        @Override
        public LogicalType toLogicalType(String nativeType)
        {
                if (nativeType == null) return LogicalType.UNKNOWN;
                String t = nativeType.toUpperCase(Locale.ROOT).split("\\(")[0].trim();
                return switch (t) {
                        case "TEXT", "VARCHAR", "CHAR", "CLOB", "CHARACTER" -> LogicalType.TEXT;
                        case "INTEGER", "INT", "BIGINT", "SMALLINT", "TINYINT", "MEDIUMINT" -> LogicalType.BIGINT;
                        case "REAL", "FLOAT", "DOUBLE", "NUMERIC", "DECIMAL" -> LogicalType.DOUBLE;
                        case "BLOB", "BINARY", "VARBINARY" -> LogicalType.LONG_BINARY;
                        case "BOOLEAN", "BOOL" -> LogicalType.BOOLEAN;
                        case "DATE", "DATETIME", "TIMESTAMP" -> LogicalType.DATETIME;
                        case "JSON" -> LogicalType.JSON;
                        default -> LogicalType.TEXT; // SQLite 弱类型，默认 TEXT
                };
        }

        @Override
        public String toNativeType(LogicalType logical, Integer size, Integer scale)
        {
                // SQLite 弱类型，按亲和性归并
                return switch (logical) {
                        case STRING, TEXT, LONG_TEXT, JSON -> "TEXT";
                        case INT, BIGINT, BOOLEAN -> "INTEGER";
                        case DECIMAL, FLOAT, DOUBLE -> "REAL";
                        case DATE, TIME, DATETIME -> "TEXT";
                        case BINARY, LONG_BINARY -> "BLOB";
                        case UNKNOWN -> "TEXT";
                };
        }

        @Override
        public List<String> createTableSql(TableDefinition table)
        {
                List<String> sql = new ArrayList<>();
                StringBuilder sb = new StringBuilder("CREATE TABLE ");
                sb.append(quoteIfKeyword(table.name())).append(" (\n");

                List<String> colDefs = new ArrayList<>();
                for (ColumnDefinition col : table.columns()) {
                        colDefs.add(buildColumnDef(col));
                }

                if (table.primaryKeys() != null && !table.primaryKeys().isEmpty()) {
                        colDefs.add("PRIMARY KEY (" + String.join(", ",
                                table.primaryKeys().stream().map(this::quoteIfKeyword).toList()) + ")");
                }

                sb.append(String.join(",\n", colDefs));
                sb.append("\n)");
                sql.add(sb.toString());

                if (table.indexes() != null) {
                        for (IndexDefinition idx : table.indexes()) {
                                if (table.primaryKeys() != null &&
                                        idx.columns().size() == table.primaryKeys().size() &&
                                        idx.columns().containsAll(table.primaryKeys()))
                                        continue;
                                createIndexSql(table.name(), idx).forEach(sql::add);
                        }
                }

                return sql;
        }

        private String buildColumnDef(ColumnDefinition col)
        {
                StringBuilder sb = new StringBuilder();
                sb.append(quoteIfKeyword(col.name())).append(" ");
                sb.append(toNativeType(col.logicalType(), col.size(), col.scale()));

                if (col.notNull()) sb.append(" NOT NULL");
                if (col.autoIncrement()) sb.append(" AUTOINCREMENT");

                if (col.defaultValue() != null && !col.logicalType().isLobType()) {
                        sb.append(" DEFAULT ").append(col.defaultValue());
                }

                return sb.toString();
        }

        @Override
        public List<String> addColumnSql(String table, ColumnDefinition column)
        {
                return List.of("ALTER TABLE " + quoteIfKeyword(table) +
                        " ADD COLUMN " + buildColumnDef(column));
        }

        @Override
        public List<String> modifyColumnSql(String table, ColumnDefinition column)
        {
                // SQLite 不直接支持 MODIFY COLUMN，需重建表（简化版返回占位）
                return List.of("-- SQLite requires table rebuild for column modification");
        }

        @Override
        public List<String> primaryKeySql(String table, List<String> pkColumns)
        {
                return List.of("-- SQLite primary keys defined at CREATE TABLE time");
        }

        @Override
        public List<String> createIndexSql(String table, IndexDefinition index)
        {
                StringBuilder sb = new StringBuilder("CREATE ");
                if (index.unique()) sb.append("UNIQUE ");
                sb.append("INDEX ").append(quoteIfKeyword(index.name()));
                sb.append(" ON ").append(quoteIfKeyword(table));
                sb.append(" (").append(String.join(", ",
                        index.columns().stream().map(this::quoteIfKeyword).toList())).append(")");
                return List.of(sb.toString());
        }
}
