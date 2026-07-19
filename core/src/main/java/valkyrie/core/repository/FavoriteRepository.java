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
import java.util.List;

/**
 * 收藏夹持久化
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class FavoriteRepository
{
        private static final UFile FAV_FILE = new UFile(Users.baseDir, "favorites.json");

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Favorite
        {
                private String connection;
                private List<String> path;
                private String objectName;
                private String type; // table/view/query
                private Integer slot; // 1~9, null=unassigned
        }

        private static List<Favorite> cache = new ArrayList<>();

        static {
                load();
        }

        private static void load()
        {
                Captor.icall(() -> {
                        if (FAV_FILE.exists()) {
                                byte[] bytes = java.nio.file.Files.readAllBytes(FAV_FILE.toPath());
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                List<Favorite> loaded = JSONUtils.toJavaList(content, Favorite.class);
                                if (loaded != null)
                                        cache = new ArrayList<>(loaded);
                        }
                });
        }

        private static void save()
        {
                Captor.icall(() -> {
                        FAV_FILE.getParentFile().mkdirs();
                        String content = JSONUtils.toJSONString(cache);
                        java.nio.file.Files.write(FAV_FILE.toPath(),
                                content.getBytes(StandardCharsets.UTF_8));
                });
        }

        public static List<Favorite> getAll()
        {
                return new ArrayList<>(cache);
        }

        public static List<Favorite> getByConnection(String connection)
        {
                return cache.stream()
                        .filter(f -> f.getConnection().equals(connection))
                        .toList();
        }

        public static void add(Favorite fav)
        {
                // Avoid duplicates
                boolean exists = cache.stream().anyMatch(f ->
                        f.getConnection().equals(fav.getConnection())
                                && f.getObjectName().equals(fav.getObjectName()));
                if (!exists) {
                        cache.add(fav);
                        save();
                }
        }

        public static void remove(String connection, String objectName)
        {
                cache.removeIf(f -> f.getConnection().equals(connection)
                        && f.getObjectName().equals(objectName));
                save();
        }

        public static boolean isFavorite(String connection, String objectName)
        {
                return cache.stream().anyMatch(f ->
                        f.getConnection().equals(connection)
                                && f.getObjectName().equals(objectName));
        }

        // ===== v1.2 slot 方法 =====

        public static Favorite getBySlot(int slot)
        {
                return cache.stream()
                        .filter(f -> f.getSlot() != null && f.getSlot() == slot)
                        .findFirst().orElse(null);
        }

        public static void assignSlot(String connection, String objectName, int slot)
        {
                // Clear old slot occupant
                for (Favorite f : cache) {
                        if (f.getSlot() != null && f.getSlot() == slot) {
                                f.setSlot(null);
                        }
                }
                // Assign to matching favorite
                for (Favorite f : cache) {
                        if (f.getConnection().equals(connection) && f.getObjectName().equals(objectName)) {
                                f.setSlot(slot);
                                break;
                        }
                }
                save();
        }

        public static void clearSlot(int slot)
        {
                cache.stream()
                        .filter(f -> f.getSlot() != null && f.getSlot() == slot)
                        .forEach(f -> f.setSlot(null));
                save();
        }

        public static void clearAll()
        {
                cache.clear();
                save();
        }

        public static void clearByConnection(String conn)
        {
                cache.removeIf(f -> f.getConnection().equals(conn));
                save();
        }
}
