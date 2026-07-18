package valkyrie.driver.api.registry;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * JDBC Driver 代理 Shim：解决 DriverManager ClassLoader 可见性问题
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class DriverShim implements Driver
{
        private final Driver delegate;

        public DriverShim(Driver delegate)
        {
                this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException
        {
                return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException
        {
                return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException
        {
                return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion()
        {
                return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion()
        {
                return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant()
        {
                return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
                return delegate.getParentLogger();
        }
}
