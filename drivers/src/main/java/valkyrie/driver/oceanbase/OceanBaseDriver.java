package valkyrie.driver.oceanbase;

import valkyrie.driver.api.*;
import valkyrie.driver.mysql.MySQLDialect;

/**
 * OceanBase 驱动（MySQL 兼容模式）
 * OceanBase 使用 MySQL 协议，复用 MySQL 方言和驱动逻辑
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class OceanBaseDriver extends valkyrie.driver.mysql.MySQLDriver
{
        public OceanBaseDriver(VkDataSource dataSource)
        {
                super(dataSource);
        }

        @Override
        public DbType getType()
        {
                return DbType.oceanbase;
        }

        @Override
        protected Dialect createDialect()
        {
                return new MySQLDialect(); // OceanBase MySQL 模式复用 MySQL 方言
        }
}
