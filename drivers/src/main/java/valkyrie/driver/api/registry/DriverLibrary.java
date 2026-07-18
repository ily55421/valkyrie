package valkyrie.driver.api.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 运行时 JDBC 驱动加载
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class DriverLibrary
{
        private static final Logger LOG = LoggerFactory.getLogger(DriverLibrary.class);
        private static final List<ExternalDriverDef> loaded = new ArrayList<>();

        private DriverLibrary()
        {
        }

        /**
         * 外部驱动定义
         */
        public record ExternalDriverDef(
                String name,
                List<String> jarPaths,
                String driverClass,
                String urlTemplate,
                int defaultPort
        )
        {
        }

        /**
         * 从 jar 加载 JDBC 驱动并注册
         */
        public static void load(ExternalDriverDef def) throws Exception
        {
                // Check if already loaded
                for (ExternalDriverDef existing : loaded) {
                        if (existing.driverClass().equals(def.driverClass())) {
                                LOG.info("Driver already loaded: {}", def.driverClass());
                                return;
                        }
                }

                URL[] urls = new URL[def.jarPaths().size()];
                for (int i = 0; i < def.jarPaths().size(); i++) {
                        urls[i] = new File(def.jarPaths().get(i)).toURI().toURL();
                }

                URLClassLoader classLoader = new URLClassLoader(urls, DriverLibrary.class.getClassLoader());
                Class<?> driverCls = Class.forName(def.driverClass(), true, classLoader);
                Driver driver = (Driver) driverCls.getDeclaredConstructor().newInstance();

                // Register via shim to make visible to DriverManager
                DriverShim shim = new DriverShim(driver);
                DriverManager.registerDriver(shim);

                loaded.add(def);
                LOG.info("Loaded external driver: {} from {}", def.name(), def.jarPaths());
        }

        public static List<ExternalDriverDef> listLoaded()
        {
                return new ArrayList<>(loaded);
        }
}
