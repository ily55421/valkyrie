package valkyrie.driver.mysql;

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
 * MySQL 方言
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public class MySQLDialect implements Dialect
{
        private static final Set<String> KEYWORDS = Set.of(
                "ADD", "ALL", "ALTER", "AND", "AS", "ASC", "BETWEEN", "BY", "CASE",
                "CHECK", "COLUMN", "CREATE", "DATABASE", "DEFAULT", "DELETE", "DESC",
                "DISTINCT", "DROP", "ELSE", "END", "EXISTS", "FOREIGN", "FROM", "FULL",
                "GROUP", "HAVING", "IN", "INDEX", "INNER", "INSERT", "INTO", "IS", "JOIN",
                "KEY", "LEFT", "LIKE", "LIMIT", "NOT", "NULL", "ON", "OR", "ORDER",
                "OUTER", "PRIMARY", "REFERENCES", "RIGHT", "SELECT", "SET", "TABLE",
                "THEN", "TO", "UNION", "UNIQUE", "UPDATE", "VALUES", "VIEW", "WHEN",
                "WHERE", "WITH", "KEYS", "STATUS", "AUTO_INCREMENT", "ENGINE", "COMMENT"
        );

        @Override
        public String quote(String identifier)
        {
                if (identifier.startsWith("`") && identifier.endsWith("`"))
                        return identifier;
                return "`" + identifier + "`";
        }

        @Override
        public String removeQuote(String identifier)
        {
                if (identifier.startsWith("`")) {
                        identifier = strcut(identifier, 1, 0);
                        identifier = strcut(identifier, 0, -1);
                        return identifier;
                }
                return identifier;
        }

        @Override
        public Set<String> keywords() { return KEYWORDS; }

        @Override
        public int maxIndexKeyBytes() { return 3072; }

        @Override
        public int maxRowBytes() { return 65535; }

        @Override
        public int varcharDegradeThreshold() { return 3000; }

        @Override
        public LogicalType toLogicalType(String nativeType)
        {
                if (nativeType == null) return LogicalType.UNKNOWN;
                String t = nativeType.toUpperCase(Locale.ROOT).split("\\(")[0].trim();
                return switch (t) {
                        case "VARCHAR", "CHAR", "ENUM", "SET" -> LogicalType.STRING;
                        case "TEXT", "TINYTEXT", "MEDIUMTEXT" -> LogicalType.TEXT;
                        case "LONGTEXT" -> LogicalType.LONG_TEXT;
                        case "INT", "INTEGER", "TINYINT", "SMALLINT", "MEDIUMINT" -> {
                                // TINYINT(1) → BOOLEAN
                                if (t.equals("TINYINT") && nativeType.contains("(1)"))
                                        yield LogicalType.BOOLEAN;
                                yield LogicalType.INT;
                        }
                        case "BIGINT" -> LogicalType.BIGINT;
                        case "DECIMAL", "NUMERIC" -> LogicalType.DECIMAL;
                        case "FLOAT" -> LogicalType.FLOAT;
                        case "DOUBLE", "DOUBLE PRECISION", "REAL" -> LogicalType.DOUBLE;
                        case "DATE" -> LogicalType.DATE;
                        case "TIME" -> LogicalType.TIME;
                        case "DATETIME", "TIMESTAMP" -> LogicalType.DATETIME;
                        case "BIT", "BOOL", "BOOLEAN" -> LogicalType.BOOLEAN;
                        case "VARBINARY", "BINARY" -> LogicalType.BINARY;
                        case "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB" -> LogicalType.LONG_BINARY;
                        case "JSON" -> LogicalType.JSON;
                        default -> LogicalType.UNKNOWN;
                };
        }

        @Override
        public String toNativeType(LogicalType logical, Integer size, Integer scale)
        {
                return switch (logical) {
                        case STRING -> {
                                if (size != null && size > varcharDegradeThreshold())
                                        yield "TEXT";
                                yield size != null ? "VARCHAR(" + size + ")" : "VARCHAR(255)";
                        }
                        case TEXT -> "TEXT";
                        case LONG_TEXT -> "LONGTEXT";
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
                        case DATETIME -> "DATETIME";
                        case BOOLEAN -> "TINYINT(1)";
                        case BINARY -> size != null ? "VARBINARY(" + size + ")" : "VARBINARY(255)";
                        case LONG_BINARY -> "LONGBLOB";
                        case JSON -> "JSON";
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

                // Primary key
                if (table.primaryKeys() != null && !table.primaryKeys().isEmpty()) {
                        colDefs.add("PRIMARY KEY (" + String.join(", ",
                                table.primaryKeys().stream().map(this::quoteIfKeyword).toList()) + ")");
                }

                sb.append(String.join(",\n", colDefs));
                sb.append("\n)");

                // Comment
                if (table.comment() != null && !table.comment().isBlank()) {
                        sb.append(" COMMENT='").append(table.comment().replace("'", "''")).append("'");
                }

                sql.add(sb.toString());

                // Indexes
                if (table.indexes() != null) {
                        for (IndexDefinition idx : table.indexes()) {
                                // Skip indexes that are equal to primary key
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
                if (col.autoIncrement()) sb.append(" AUTO_INCREMENT");

                // Default value (not for LOB types)
                if (col.defaultValue() != null && !col.logicalType().isLobType()) {
                        sb.append(" DEFAULT ").append(col.defaultValue());
                }

                // Comment
                if (col.comment() != null && !col.comment().isBlank()) {
                        sb.append(" COMMENT '").append(col.comment().replace("'", "''")).append("'");
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
                return List.of("ALTER TABLE " + quoteIfKeyword(table) +
                        " MODIFY COLUMN " + buildColumnDef(column));
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

        @Override
        public String batchUrlParam() { return "rewriteBatchedStatements=true"; }
}
