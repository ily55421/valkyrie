package valkyrie.driver.api.registry;

import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.DbType;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.PooledDataSource;
import valkyrie.driver.api.VkDataSource;
import valkyrie.driver.dm.DMDriver;
import valkyrie.driver.mysql.MySQLDriver;
import valkyrie.driver.oceanbase.OceanBaseDriver;
import valkyrie.driver.postgresql.PostgresqlDriver;
import valkyrie.driver.redis.RedisDataSource;
import valkyrie.driver.redis.RedisDriver;
import valkyrie.driver.sqlite.SQLiteDriver;

/**
 * 内置驱动注册
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class BuiltinDrivers
{
        private static boolean initialized = false;

        private BuiltinDrivers()
        {
        }

        public static synchronized void ensureRegistered()
        {
                if (initialized) return;

                // MySQL
                DriverRegistry.register(new DriverRegistry.DriverDescriptor(
                        DbType.mysql,
                        "MySQL",
                        "mysql",
                        "com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone={timezone}",
                        3306,
                        "SELECT 1",
                        true,
                        (config, dialect) -> new MySQLDriver(createDataSource(config))
                ));

                // PostgreSQL
                DriverRegistry.register(new DriverRegistry.DriverDescriptor(
                        DbType.postgresql,
                        "PostgreSQL",
                        "postgresql",
                        "org.postgresql.Driver",
                        "jdbc:postgresql://{host}:{port}/{database}",
                        5432,
                        "SELECT 1",
                        true,
                        (config, dialect) -> new PostgresqlDriver(createDataSource(config))
                ));

                // SQLite
                DriverRegistry.register(new DriverRegistry.DriverDescriptor(
                        DbType.sqlite,
                        "SQLite",
                        "sqlite",
                        "org.sqlite.JDBC",
                        "jdbc:sqlite:{path}",
                        0,
                        "SELECT 1",
                        true,
                        (config, dialect) -> new SQLiteDriver(createDataSource(config))
                ));

                // DM
                DriverRegistry.register(new DriverRegistry.DriverDescriptor(
                        DbType.dm,
                        "达梦数据库",
                        "dm2",
                        "dm.jdbc.driver.DmDriver",
                        "jdbc:dm://{host}:{port}",
                        5236,
                        "SELECT 1",
                        true,
                        (config, dialect) -> new DMDriver(createDataSource(config))
                ));

                // Redis
                DriverRegistry.register(new DriverRegistry.DriverDescriptor(
                        DbType.redis,
                        "Redis",
                        "redis",
                        null,
                        "redis://{host}:{port}/{database}",
                        6379,
                        "PING",
                        false,
                        (config, dialect) -> new RedisDriver(new RedisDataSource(config))
                ));

                // OceanBase (MySQL 兼容模式)
                DriverRegistry.register(new DriverRegistry.DriverDescriptor(
                        DbType.oceanbase,
                        "OceanBase",
                        "oceanbase",
                        "com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone={timezone}",
                        2883,
                        "SELECT 1 FROM DUAL",
                        true,
                        (config, dialect) -> new OceanBaseDriver(createDataSource(config))
                ));

                initialized = true;
        }

        private static VkDataSource createDataSource(ConnectionConfig config)
        {
                return new PooledDataSource(config);
        }
}
