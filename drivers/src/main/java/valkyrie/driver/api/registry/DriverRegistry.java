package valkyrie.driver.api.registry;

import lombok.Getter;
import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.DbType;
import valkyrie.driver.api.Driver;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 驱动注册表（取代硬编码 switch）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class DriverRegistry
{
        /**
         * 驱动描述符
         */
        public record DriverDescriptor(
                DbType type,
                @Getter String displayName,
                @Getter String icon,
                @Getter String driverClass,
                @Getter String urlTemplate,
                @Getter int defaultPort,
                @Getter String testSql,
                @Getter boolean jdbc,
                BiFunction<ConnectionConfig, Object, Driver> driverFactory
        )
        {
                public Driver createDriver(ConnectionConfig config, Object dialect)
                {
                        return driverFactory.apply(config, dialect);
                }
        }

        private static final Map<DbType, DriverDescriptor> REGISTRY = new ConcurrentHashMap<>();

        private DriverRegistry()
        {
        }

        public static void register(DriverDescriptor descriptor)
        {
                REGISTRY.put(descriptor.type(), descriptor);
        }

        public static DriverDescriptor descriptor(DbType type)
        {
                return REGISTRY.get(type);
        }

        public static Collection<DriverDescriptor> all()
        {
                return REGISTRY.values();
        }

        /**
         * 根据模板构建 JDBC URL
         */
        public static String buildUrl(ConnectionConfig config)
        {
                // If jdbcUrl is explicitly provided, use it
                if (config.getJdbcUrl() != null && !config.getJdbcUrl().isBlank())
                        return config.getJdbcUrl();

                DriverDescriptor desc = descriptor(config.getType());
                if (desc == null) return null;

                String template = desc.urlTemplate();
                if (template == null) return null;

                String url = template;
                if (config.getHost() != null)
                        url = url.replace("{host}", config.getHost());
                if (config.getPort() != null)
                        url = url.replace("{port}", config.getPort());
                if (config.getDefaultDatabase() != null)
                        url = url.replace("{database}", config.getDefaultDatabase());
                if (config.getPath() != null)
                        url = url.replace("{path}", config.getPath());
                if (config.getTimezone() != null)
                        url = url.replace("{timezone}", config.getTimezone());

                // Remove trailing ? if no params
                if (url.endsWith("?") || url.endsWith("&"))
                        url = url.substring(0, url.length() - 1);

                return url;
        }

        /**
         * 创建 Driver 实例
         */
        public static Driver create(ConnectionConfig config)
        {
                DriverDescriptor desc = descriptor(config.getType());
                if (desc == null)
                        throw new IllegalArgumentException("No driver registered for type: " + config.getType());
                return desc.createDriver(config, null);
        }
}
