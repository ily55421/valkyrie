package valkyrie.core.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import valkyrie.core.Users;
import valkyrie.core.utils.JSONUtils;
import valkyrie.utils.Captor;
import valkyrie.utils.io.UFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 最近使用记录仓库（LRU）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class RecentRepository
{
        private static final UFile RECENTS_FILE = new UFile(Users.baseDir, "recents.json");
        private static final int MAX_SIZE = 20;
        private static List<RecentEntry> cache = new ArrayList<>();

        static { load(); }

        public enum RecentType { QUERY_FILE, TABLE_TAB, CONNECTION }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RecentEntry
        {
                private RecentType type;
                private String label;
                private String path;
                private long timestamp;
        }

        private static void load()
        {
                Captor.icall(() -> {
                        if (RECENTS_FILE.exists()) {
                                byte[] bytes = java.nio.file.Files.readAllBytes(RECENTS_FILE.toPath());
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                List<RecentEntry> loaded = JSONUtils.toJavaList(content, RecentEntry.class);
                                if (loaded != null) cache = new ArrayList<>(loaded);
                        }
                });
        }

        private static void save()
        {
                Captor.icall(() -> {
                        RECENTS_FILE.getParentFile().mkdirs();
                        String content = JSONUtils.toJSONString(cache);
                        java.nio.file.Files.write(RECENTS_FILE.toPath(),
                                content.getBytes(StandardCharsets.UTF_8));
                });
        }

        public static void add(RecentEntry entry)
        {
                // Remove existing with same path
                cache.removeIf(e -> e.getPath() != null && e.getPath().equals(entry.getPath()));
                // Add to front (most recent)
                cache.add(0, entry);
                // Trim to max size
                while (cache.size() > MAX_SIZE)
                        cache.remove(cache.size() - 1);
                save();
        }

        public static List<RecentEntry> getAll()
        {
                return new ArrayList<>(cache);
        }

        public static void remove(String path)
        {
                cache.removeIf(e -> e.getPath() != null && e.getPath().equals(path));
                save();
        }

        public static void clear()
        {
                cache.clear();
                save();
        }
}
