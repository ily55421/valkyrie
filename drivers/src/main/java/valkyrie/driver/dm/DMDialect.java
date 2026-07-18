package valkyrie.driver.dm;

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
 * 达梦数据库方言
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public class DMDialect implements Dialect
{
        private static final Set<String> KEYWORDS = Set.of(
                "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "BETWEEN", "BY", "CASE",
                "CHECK", "CLUSTER", "COLUMN", "COMMENT", "CONSTRAINT", "CREATE", "DATABASE",
                "DEFAULT", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "END", "EXISTS",
                "FOREIGN", "FROM", "FULL", "GROUP", "HAVING", "IN", "INDEX", "INNER",
                "INSERT", "INTO", "IS", "JOIN", "KEY", "LEFT", "LIKE", "LIMIT", "NOT",
                "NULL", "ON", "OR", "ORDER", "OUTER", "PRIMARY", "REFERENCES", "RIGHT",
                "SELECT", "SET", "TABLE", "THEN", "TO", "UNION", "UNIQUE", "UPDATE",
                "VALUES", "VIEW", "WHEN", "WHERE", "WITH", "IDENTITY", "AUTO_INCREMENT",
                "COMMENT", "BIT", "CLOB", "BLOB", "VARCHAR2", "NUMBER", "TIMESTAMP"
        );

        @Override
        public String normalize(String sql)
        {
                sql = sql.replaceAll("NOT\\s+CLUSTER\\s+PRIMARY\\s+KEY", "PRIMARY KEY");
                sql = sql.replaceAll("(?i)\\s+ENCRYPT\\s+WITH\\s+AES256_CBC\\s+AUTO\\s+BY\\s+WRAPPED\\s+'[^']*'", "");
                sql = sql.replaceAll("STORAGE\\s*\\([^)]*\\)", "");
                sql = sql.replaceAll("\\((\\d+)\\s+CHAR\\)", "");
                return sql;
        }

        @Override
        public String quote(String identifier)
        {
                if (identifier.startsWith("\"") && identifier.endsWith("\""))
                        return identifier;
                return "\"" + identifier + "\"";
        }

        @Override
        public String removeQuote(String identifier)
        {
                if (identifier.startsWith("\"")) {
                        identifier = strcut(identifier, 1, 0);
                        identifier = strcut(identifier, 0, -1);
                        return identifier;
                }
                return identifier;
        }

        @Override
        public Set<String> keywords() { return KEYWORDS; }

        @Override
        public int varcharDegradeThreshold() { return 2000; }

        @Override
        public LogicalType toLogicalType(String nativeType)
        {
                if (nativeType == null) return LogicalType.UNKNOWN;
                String t = nativeType.toUpperCase(Locale.ROOT).split("\\(")[0].trim();
                return switch (t) {
                        case "VARCHAR", "VARCHAR2", "CHAR", "CHARACTER" -> LogicalType.STRING;
                        case "TEXT", "CLOB", "LONGVARCHAR", "LONG" -> LogicalType.TEXT;
                        case "INT", "INTEGER", "SMALLINT", "TINYINT" -> LogicalType.INT;
                        case "BIGINT" -> LogicalType.BIGINT;
                        case "DECIMAL", "NUMERIC", "NUMBER" -> LogicalType.DECIMAL;
                        case "FLOAT" -> LogicalType.FLOAT;
                        case "DOUBLE", "DOUBLE PRECISION", "REAL" -> LogicalType.DOUBLE;
                        case "DATE" -> LogicalType.DATE;
                        case "TIME" -> LogicalType.TIME;
                        case "TIMESTAMP", "DATETIME", "TIMESTAMP WITH TIME ZONE" -> LogicalType.DATETIME;
                        case "BIT", "BOOL", "BOOLEAN" -> LogicalType.BOOLEAN;
                        case "VARBINARY", "BINARY" -> LogicalType.BINARY;
                        case "BLOB", "IMAGE", "LONGVARBINARY" -> LogicalType.LONG_BINARY;
                        case "JSON" -> LogicalType.JSON;
                        default -> LogicalType.UNKNOWN;
                };
        }

        @Override
        public String toNativeType(LogicalType logical, Integer size, Integer scale)
        {
                return switch (logical) {
                        case STRING -> {
                                if (size != null && size > varcharDegradeThreshold()) yield "CLOB";
                                yield size != null ? "VARCHAR2(" + size + ")" : "VARCHAR2(255)";
                        }
                        case TEXT, LONG_TEXT -> "CLOB";
                        case INT -> "INT";
                        case BIGINT -> "BIGINT";
                        case DECIMAL -> {
                                if (size != null && scale != null) yield "DECIMAL(" + size + "," + scale + ")";
                                if (size != null) yield "DECIMAL(" + size + ")";
                                yield "DECIMAL";
                        }
                        case FLOAT -> "FLOAT";
                        case DOUBLE -> "DOUBLE";
                        case DATE -> "DATE";
                        case TIME -> "TIME";
                        case DATETIME -> "TIMESTAMP";
                        case BOOLEAN -> "BIT";
                        case BINARY -> size != null ? "VARBINARY(" + size + ")" : "VARBINARY(255)";
                        case LONG_BINARY -> "BLOB";
                        case JSON -> "TEXT";
                        case UNKNOWN -> "VARCHAR2(255)";
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

                // Comments
                if (table.comment() != null && !table.comment().isBlank()) {
                        sql.add("COMMENT ON TABLE " + quoteIfKeyword(table.name()) +
                                " IS '" + table.comment().replace("'", "''") + "'");
                }
                for (ColumnDefinition col : table.columns()) {
                        if (col.comment() != null && !col.comment().isBlank()) {
                                sql.add("COMMENT ON COLUMN " + quoteIfKeyword(table.name()) + "." +
                                        quoteIfKeyword(col.name()) + " IS '" +
                                        col.comment().replace("'", "''") + "'");
                        }
                }

                // Indexes
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
                if (col.autoIncrement()) sb.append(" IDENTITY(1,1)");

                if (col.defaultValue() != null && !col.logicalType().isLobType()) {
                        sb.append(" DEFAULT ").append(col.defaultValue());
                }

                return sb.toString();
        }

        @Override
        public List<String> addColumnSql(String table, ColumnDefinition column)
        {
                List<String> sql = new ArrayList<>();
                sql.add("ALTER TABLE " + quoteIfKeyword(table) + " ADD " + buildColumnDef(column));
                if (column.comment() != null && !column.comment().isBlank()) {
                        sql.add("COMMENT ON COLUMN " + quoteIfKeyword(table) + "." +
                                quoteIfKeyword(column.name()) + " IS '" +
                                column.comment().replace("'", "''") + "'");
                }
                return sql;
        }

        @Override
        public List<String> modifyColumnSql(String table, ColumnDefinition column)
        {
                List<String> sql = new ArrayList<>();
                String colName = quoteIfKeyword(column.name());
                String type = toNativeType(column.logicalType(), column.size(), column.scale());
                sql.add("ALTER TABLE " + quoteIfKeyword(table) + " MODIFY " + colName + " " + type);
                if (column.notNull()) {
                        sql.add("ALTER TABLE " + quoteIfKeyword(table) + " MODIFY " + colName + " NOT NULL");
                }
                return sql;
        }

        @Override
        public List<String> primaryKeySql(String table, List<String> pkColumns)
        {
                return List.of("ALTER TABLE " + quoteIfKeyword(table) +
                        " ADD PRIMARY KEY (" + String.join(", ",
                        pkColumns.stream().map(this::quoteIfKeyword).toList()) + ")");
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
