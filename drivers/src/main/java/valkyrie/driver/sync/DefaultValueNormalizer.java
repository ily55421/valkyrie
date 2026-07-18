package valkyrie.driver.sync;

import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.type.LogicalType;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 默认值归一化器（对策 K1/K2/K3）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class DefaultValueNormalizer
{
        private static final Pattern CURRENT_TIMESTAMP_PATTERN = Pattern.compile(
                "(?i)^(current_timestamp|now|sysdate|localtimestamp|getdate)\\s*\\(.*\\)\\s*$"
        );

        private static final Pattern ZERO_DATE_PATTERN = Pattern.compile(
                "^0{2,4}[-/]0{1,2}([-/]0{1,2})?.*$"
        );

        private DefaultValueNormalizer()
        {
        }

        /**
         * 将源库默认值表达式归一化为目标库可执行的默认值子句片段。
         *
         * @return "DEFAULT xxx" 片段；无需默认值时返回 null
         */
        public static String normalize(String sourceDefault, LogicalType logical,
                                       Dialect source, Dialect target)
        {
                if (sourceDefault == null) return null;
                String v = sourceDefault.trim();
                if (v.isEmpty()) return null;

                // 1. 去 PG 风格 cast 后缀：'xxx'::character varying
                int castIdx = v.lastIndexOf("::");
                if (castIdx > 0) v = v.substring(0, castIdx).trim();

                // 2. 去包裹单引号后二次判空（对策 K3）
                String unquoted = stripSingleQuotes(v);
                if (unquoted.isBlank()) {
                        return logical == LogicalType.STRING ? "DEFAULT ''" : null;
                }

                // 3. 日期时间函数归一（对策 K1）
                if (logical.isDateTimeFamily() && isCurrentTimestampExpr(unquoted)) {
                        return "DEFAULT CURRENT_TIMESTAMP";
                }

                // 4. 零日期/非法日期（对策 K2）
                if (logical.isDateTimeFamily() && isZeroDate(unquoted)) {
                        return "DEFAULT NULL";
                }

                // 5. 数值列去引号
                if (logical.isNumeric()) {
                        String numVal = unquoted.trim();
                        if (numVal.isEmpty()) return null;
                        // 验证是否为合法数值
                        try {
                                Double.parseDouble(numVal);
                                return "DEFAULT " + numVal;
                        } catch (NumberFormatException e) {
                                return null;
                        }
                }

                // 6. BOOLEAN 处理
                if (logical == LogicalType.BOOLEAN) {
                        String bv = unquoted.toLowerCase(Locale.ROOT);
                        if ("1".equals(bv) || "true".equals(bv) || "yes".equals(bv))
                                return "DEFAULT 1";
                        if ("0".equals(bv) || "false".equals(bv) || "no".equals(bv))
                                return "DEFAULT 0";
                        return null;
                }

                // 7. 字符串列保留引号并转义
                if (logical == LogicalType.STRING) {
                        String escaped = unquoted.replace("'", "''");
                        return "DEFAULT '" + escaped + "'";
                }

                // 8. LOB/JSON 类型不生成 default
                if (logical.isLobType()) {
                        return null;
                }

                // 9. 其他类型：尝试原样返回（去引号）
                return "DEFAULT " + unquoted;
        }

        private static String stripSingleQuotes(String v)
        {
                if (v.startsWith("'") && v.endsWith("'") && v.length() >= 2) {
                        return v.substring(1, v.length() - 1);
                }
                return v;
        }

        static boolean isCurrentTimestampExpr(String v)
        {
                if (v == null) return false;
                String lower = v.toLowerCase(Locale.ROOT).trim();
                return CURRENT_TIMESTAMP_PATTERN.matcher(lower).matches()
                        || "current_timestamp".equals(lower)
                        || "now()".equals(lower)
                        || "sysdate".equals(lower)
                        || "getdate()".equals(lower);
        }

        static boolean isZeroDate(String v)
        {
                if (v == null) return false;
                return ZERO_DATE_PATTERN.matcher(v.trim()).matches()
                        || "0000-00-00".equals(v.trim())
                        || "0000-00-00 00:00:00".equals(v.trim())
                        || "00/00/0000".equals(v.trim());
        }
}
