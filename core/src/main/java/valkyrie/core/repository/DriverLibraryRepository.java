package valkyrie.core.repository;

import valkyrie.core.Users;
import valkyrie.core.utils.JSONUtils;
import valkyrie.driver.api.registry.DriverLibrary;
import valkyrie.utils.Captor;
import valkyrie.utils.io.UFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 外部驱动库持久化
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class DriverLibraryRepository
{
        private static final UFile DRIVERS_FILE = new UFile(Users.baseDir, "drivers.json");

        public static List<DriverLibrary.ExternalDriverDef> loadAll()
        {
                List<DriverLibrary.ExternalDriverDef> result = new ArrayList<>();
                Captor.icall(() -> {
                        if (DRIVERS_FILE.exists()) {
                                byte[] bytes = java.nio.file.Files.readAllBytes(DRIVERS_FILE.toPath());
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                List<DriverLibrary.ExternalDriverDef> loaded =
                                        JSONUtils.toJavaList(content, DriverLibrary.ExternalDriverDef.class);
                                if (loaded != null)
                                        result.addAll(loaded);
                        }
                });
                return result;
        }

        public static void save(List<DriverLibrary.ExternalDriverDef> drivers)
        {
                Captor.icall(() -> {
                        DRIVERS_FILE.getParentFile().mkdirs();
                        String content = JSONUtils.toJSONString(drivers);
                        java.nio.file.Files.write(DRIVERS_FILE.toPath(),
                                content.getBytes(StandardCharsets.UTF_8));
                });
        }

        public static void loadOnStartup()
        {
                List<DriverLibrary.ExternalDriverDef> drivers = loadAll();
                for (DriverLibrary.ExternalDriverDef def : drivers) {
                        Captor.icall(() -> DriverLibrary.load(def));
                }
        }
}
