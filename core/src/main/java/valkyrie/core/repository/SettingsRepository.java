package valkyrie.core.repository;

import valkyrie.core.Users;
import valkyrie.core.utils.JSONUtils;
import valkyrie.utils.Captor;
import valkyrie.utils.io.UFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 设置持久化（用户目录 settings.json）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class SettingsRepository
{
        private static final UFile SETTINGS_FILE = new UFile(Users.baseDir, "settings.json");
        private static final Map<String, Object> cache = new HashMap<>();

        static {
                load();
        }

        private static void load()
        {
                Captor.icall(() -> {
                        if (SETTINGS_FILE.exists()) {
                                byte[] bytes = java.nio.file.Files.readAllBytes(SETTINGS_FILE.toPath());
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                Map<String, Object> loaded = JSONUtils.toJavaObject(content, Map.class);
                                if (loaded != null)
                                        cache.putAll(loaded);
                        }
                });
        }

        private static void save()
        {
                Captor.icall(() -> {
                        SETTINGS_FILE.getParentFile().mkdirs();
                        String content = JSONUtils.toJSONString(cache);
                        java.nio.file.Files.write(SETTINGS_FILE.toPath(),
                                content.getBytes(StandardCharsets.UTF_8));
                });
        }

        public static String get(String key, String defaultValue)
        {
                Object v = cache.get(key);
                return v != null ? v.toString() : defaultValue;
        }

        public static void set(String key, String value)
        {
                cache.put(key, value);
                save();
        }
}
