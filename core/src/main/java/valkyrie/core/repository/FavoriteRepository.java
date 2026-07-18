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
}
