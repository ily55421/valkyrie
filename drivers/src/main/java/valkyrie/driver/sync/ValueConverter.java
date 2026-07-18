package valkyrie.driver.sync;

import valkyrie.driver.api.type.LogicalType;
import valkyrie.driver.api.Dialect;

import java.sql.Clob;
import java.sql.Blob;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 值转换器
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class ValueConverter
{
        private ValueConverter()
        {
        }

        /**
         * 源 JDBC 值 → 目标 setObject 值
         */
        public static Object convert(Object value, LogicalType sourceType, LogicalType targetType,
                                      Dialect source, Dialect target)
        {
                if (value == null) return null;

                try {
                        // CLOB → String
                        if (value instanceof Clob clob) {
                                return clob.getSubString(1, (int) clob.length());
                        }

                        // BLOB → byte[]
                        if (value instanceof Blob blob) {
                                return blob.getBytes(1, (int) blob.length());
                        }

                        // DateTime types → LocalDateTime bridge
                        if (sourceType.isDateTimeFamily()) {
                                return convertDateTime(value);
                        }

                        // Boolean handling
                        if (targetType == LogicalType.BOOLEAN) {
                                if (value instanceof Number n) return n.intValue() != 0;
                                if (value instanceof Boolean b) return b;
                                return "1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString());
                        }

                        // Numeric precision
                        if (targetType.isNumeric()) {
                                return convertNumeric(value, targetType);
                        }

                        // String conversion fallback
                        return value.toString();
                } catch (Exception e) {
                        // Fallback to string representation
                        return value.toString();
                }
        }

        private static Object convertDateTime(Object value)
        {
                if (value instanceof java.sql.Timestamp ts) {
                        return ts.toLocalDateTime();
                }
                if (value instanceof java.sql.Date d) {
                        return d.toLocalDate();
                }
                if (value instanceof java.sql.Time t) {
                        return t.toLocalTime();
                }
                if (value instanceof java.util.Date d) {
                        return LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
                }
                // Zero date fallback
                String s = value.toString();
                if (s.startsWith("0000-00-00")) return null;
                return value;
        }

        private static Object convertNumeric(Object value, LogicalType targetType)
        {
                if (value instanceof Number n) {
                        return switch (targetType) {
                                case INT -> n.intValue();
                                case BIGINT -> n.longValue();
                                case FLOAT -> n.floatValue();
                                case DOUBLE -> n.doubleValue();
                                case DECIMAL -> n;  // Keep BigDecimal
                                default -> n;
                        };
                }
                try {
                        String s = value.toString().trim();
                        return switch (targetType) {
                                case INT -> Integer.parseInt(s);
                                case BIGINT -> Long.parseLong(s);
                                case FLOAT -> Float.parseFloat(s);
                                case DOUBLE -> Double.parseDouble(s);
                                default -> value;
                        };
                } catch (NumberFormatException e) {
                        return value;
                }
        }

        /**
         * 转换为 SQL 字面量（用于导出）
         */
        public static String toLiteral(Object value, LogicalType type)
        {
                if (value == null) return "NULL";

                return switch (type) {
                        case STRING, TEXT, LONG_TEXT, JSON, DATE, TIME, DATETIME ->
                                "'" + value.toString().replace("'", "''") + "'";
                        case BOOLEAN -> value instanceof Boolean b ? (b ? "1" : "0") : value.toString();
                        case BINARY, LONG_BINARY -> "X'" + bytesToHex((byte[]) value) + "'";
                        default -> value.toString();
                };
        }

        private static String bytesToHex(byte[] bytes)
        {
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) sb.append(String.format("%02X", b));
                return sb.toString();
        }
}
