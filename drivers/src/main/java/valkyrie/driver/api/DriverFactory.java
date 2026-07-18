package valkyrie.driver.api;

import valkyrie.driver.api.registry.BuiltinDrivers;
import valkyrie.driver.api.registry.DriverRegistry;

/**
 * @author Luo Tiansheng
 * @since 2026/4/20
 */
public class DriverFactory
{
        static {
                BuiltinDrivers.ensureRegistered();
        }

        public static VkDataSource createDataSource(ConnectionConfig config)
        {
                return switch (config.getType()) {
                        case mysql, postgresql, dm, sqlite -> new PooledDataSource(config);
                        case redis -> new valkyrie.driver.redis.RedisDataSource(config);
                };
        }

        public static Driver create(ConnectionConfig config)
        {
                return DriverRegistry.create(config);
        }
}
