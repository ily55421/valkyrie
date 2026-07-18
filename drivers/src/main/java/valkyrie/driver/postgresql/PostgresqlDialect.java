package valkyrie.driver.postgresql;

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
 * PostgreSQL 方言
 *
 * @author Luo Tiansheng
 * @since 2026/6/04
 */
public class PostgresqlDialect implements Dialect
{
        private static final Set<String> KEYWORDS = Set.of(
                "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "BETWEEN", "BY", "CASE",
                "CHECK", "COLUMN", "CONSTRAINT", "CREATE", "DATABASE", "DEFAULT", "DELETE", "DESC",
                "DISTINCT", "DROP", "ELSE", "END", "EXISTS", "FOREIGN", "FROM", "FULL",
                "GROUP", "HAVING", "IN", "INDEX", "INNER", "INSERT", "INTO", "IS", "JOIN",
                "LEFT", "LIKE", "LIMIT", "NOT", "NULL", "ON", "OR", "ORDER",
                "OUTER", "PRIMARY", "REFERENCES", "RIGHT", "SELECT", "SET", "TABLE",
                "THEN", "TO", "UNION", "UNIQUE", "UPDATE", "VALUES", "VIEW", "WHEN",
                "WHERE", "WITH", "SERIAL", "BIGSERIAL", "TEXT", "VARCHAR", "BOOLEAN",
                "INTEGER", "BIGINT", "NUMERIC", "REAL", "DOUBLE", "PRECISION", "TIMESTAMP"
        );

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
        public int varcharDegradeThreshold() { return 10485760; }

        @Override
        public LogicalType toLogicalType(String nativeType)
        {
                if (nativeType == null) return LogicalType.UNKNOWN;
                String t = nativeType.toUpperCase(Locale.ROOT).split("\\(")[0].trim();
                return switch (t) {
                        case "VARCHAR", "CHAR", "CHARACTER", "VARYING" -> LogicalType.STRING;
                        case "TEXT" -> LogicalType.TEXT;
                        case "INT", "INTEGER", "SMALLINT", "SERIAL" -> LogicalType.INT;
                        case "BIGINT", "BIGSERIAL" -> LogicalType.BIGINT;
                        case "DECIMAL", "NUMERIC" -> LogicalType.DECIMAL;
                        case "REAL", "FLOAT4" -> LogicalType.FLOAT;
                        case "DOUBLE", "FLOAT8" -> LogicalType.DOUBLE;
                        case "DATE" -> LogicalType.DATE;
                        case "TIME" -> LogicalType.TIME;
                        case "TIMESTAMP", "TIMESTAMPTZ" -> LogicalType.DATETIME;
                        case "BOOL", "BOOLEAN" -> LogicalType.BOOLEAN;
                        case "BYTEA", "BLOB" -> LogicalType.LONG_BINARY;
                        case "JSON", "JSONB" -> LogicalType.JSON;
                        case "CLOB" -> LogicalType.LONG_TEXT;
                        default -> LogicalType.UNKNOWN;
                };
        }

        @Override
        public String toNativeType(LogicalType logical, Integer size, Integer scale)
        {
                return switch (logical) {
                        case STRING -> size != null ? "VARCHAR(" + size + ")" : "VARCHAR(255)";
                        case TEXT, LONG_TEXT -> "TEXT";
                        case INT -> "INTEGER";
                        case BIGINT -> "BIGINT";
                        case DECIMAL -> {
                                if (size != null && scale != null) yield "NUMERIC(" + size + "," + scale + ")";
                                if (size != null) yield "NUMERIC(" + size + ")";
                                yield "NUMERIC";
                        }
                        case FLOAT -> "REAL";
                        case DOUBLE -> "DOUBLE PRECISION";
                        case DATE -> "DATE";
                        case TIME -> "TIME";
                        case DATETIME -> "TIMESTAMP";
                        case BOOLEAN -> "BOOLEAN";
                        case BINARY, LONG_BINARY -> "BYTEA";
                        case JSON -> "JSONB";
                        case UNKNOWN -> "VARCHAR(255)";
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

                // Comments (separate statements in PG)
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

                String nativeType = toNativeType(col.logicalType(), col.size(), col.scale());
                sb.append(nativeType);

                if (col.notNull()) sb.append(" NOT NULL");

                if (col.defaultValue() != null && !col.logicalType().isLobType()) {
                        sb.append(" DEFAULT ").append(col.defaultValue());
                }

                return sb.toString();
        }

        @Override
        public List<String> addColumnSql(String table, ColumnDefinition column)
        {
                List<String> sql = new ArrayList<>();
                sql.add("ALTER TABLE " + quoteIfKeyword(table) + " ADD COLUMN " + buildColumnDef(column));
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
                sql.add("ALTER TABLE " + quoteIfKeyword(table) + " ALTER COLUMN " + colName + " TYPE " + type);
                if (column.notNull()) {
                        sql.add("ALTER TABLE " + quoteIfKeyword(table) + " ALTER COLUMN " + colName + " SET NOT NULL");
                } else {
                        sql.add("ALTER TABLE " + quoteIfKeyword(table) + " ALTER COLUMN " + colName + " DROP NOT NULL");
                }
                if (column.defaultValue() != null && !column.logicalType().isLobType()) {
                        sql.add("ALTER TABLE " + quoteIfKeyword(table) + " ALTER COLUMN " + colName +
                                " SET DEFAULT " + column.defaultValue());
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
