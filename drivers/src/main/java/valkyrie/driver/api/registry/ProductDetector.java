package valkyrie.driver.api.registry;

import valkyrie.driver.api.DbType;

import java.util.Locale;
import java.util.Optional;

/**
 * 产品名探测器：按 JDBC 产品名细化类型识别
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class ProductDetector
{
        private ProductDetector()
        {
        }

        /**
         * 按 JDBC 产品名细化类型识别（仅提示，不自动切换方言）
         */
        public static Optional<DbType> refine(DbType declared, String productName)
        {
                if (productName == null) return Optional.empty();
                String lower = productName.toLowerCase(Locale.ROOT);

                // PostgreSQL 系国产库识别
                if (declared == DbType.postgresql) {
                        if (lower.contains("gauss") || lower.contains("gaussdb"))
                                return Optional.of(DbType.postgresql); // 仍用 PG 方言，仅提示
                        if (lower.contains("kingbase") || lower.contains("kingbasees"))
                                return Optional.of(DbType.postgresql);
                }

                // MySQL 系识别
                if (declared == DbType.mysql) {
                        if (lower.contains("mariadb"))
                                return Optional.of(DbType.mysql);
                }

                return Optional.empty();
        }

        /**
         * 获取展示用产品名（含变体提示）
         */
        public static String displayName(DbType declared, String productName, String version)
        {
                String base = declared.getAlias();
                if (productName != null && !productName.isBlank()) {
                        String lower = productName.toLowerCase(Locale.ROOT);
                        if (lower.contains("gauss")) return base + " (GaussDB)";
                        if (lower.contains("kingbase")) return base + " (KingbaseES)";
                        if (lower.contains("mariadb")) return base + " (MariaDB)";
                }
                if (version != null && !version.isBlank()) {
                        return base + " " + version;
                }
                return base;
        }
}
