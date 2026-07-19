package valkyrie.app.utils;

import javafx.scene.control.TreeItem;
import valkyrie.app.explorer.UICatalogNode;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.DriverFactory;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.sql.SQL;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库工具类
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class DbUtils
{
        /**
         * 从 Driver 加载数据库列表（多种方式尝试）
         */
        public static List<String> loadCatalogs(UIConnectionNode connNode)
        {
                List<String> catalogs = new ArrayList<>();
                if (connNode == null) return catalogs;

                // Auto-connect if not connected
                if (!connNode.isConnect()) {
                        try {
                                connNode.connect();
                                // Wait a bit for connection to establish
                                Thread.sleep(500);
                        } catch (Exception e) {
                                return catalogs;
                        }
                }

                Driver driver = connNode.getDriver();
                if (driver == null) return catalogs;

                // Method 1: JDBC getCatalogs
                try {
                        catalogs = driver.getCatalogs();
                } catch (Exception e) { /* ignore */ }

                // Method 2: SHOW DATABASES (MySQL)
                if (catalogs.isEmpty()) {
                        try {
                                var result = driver.execute(Session.ofCatalog(""), new SQL("SHOW DATABASES"));
                                for (var row : result.getRows()) {
                                        Object val = row.get(0);
                                        if (val != null) catalogs.add(val.toString());
                                }
                        } catch (Exception e) { /* ignore */ }
                }

                // Method 3: PostgreSQL pg_database
                if (catalogs.isEmpty()) {
                        try {
                                var result = driver.execute(Session.ofCatalog(""),
                                        new SQL("SELECT datname FROM pg_database WHERE datistemplate = false"));
                                for (var row : result.getRows()) {
                                        Object val = row.get(0);
                                        if (val != null) catalogs.add(val.toString());
                                }
                        } catch (Exception e) { /* ignore */ }
                }

                // Method 4: SQLite PRAGMA database_list
                if (catalogs.isEmpty()) {
                        try {
                                var result = driver.execute(Session.ofCatalog(""), new SQL("PRAGMA database_list"));
                                for (var row : result.getRows()) {
                                        if (row.size() > 1) {
                                                Object val = row.get(1);
                                                if (val != null) catalogs.add(val.toString());
                                        }
                                }
                        } catch (Exception e) { /* ignore */ }
                }

                // Method 5: Fallback to tree children
                if (catalogs.isEmpty()) {
                        for (TreeItem<String> child : connNode.getChildren()) {
                                if (child instanceof UICatalogNode cat) {
                                        catalogs.add(cat.getLabel());
                                }
                        }
                }

                // Method 6: Try schemas
                if (catalogs.isEmpty()) {
                        try { catalogs.addAll(driver.getSchemas()); } catch (Exception ignored) {}
                }

                return catalogs;
        }
}
