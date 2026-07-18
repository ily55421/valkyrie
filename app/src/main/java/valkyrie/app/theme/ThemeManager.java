package valkyrie.app.theme;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.core.repository.SettingsRepository;
import valkyrie.utils.Captor;

/**
 * 主题管理器
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class ThemeManager
{
        private static final Logger LOG = LoggerFactory.getLogger(ThemeManager.class);

        public enum Theme
        {
                LIGHT, DARK
        }

        private static Theme current = Theme.LIGHT;
        private static Scene activeScene;

        private ThemeManager()
        {
        }

        public static void init(Scene scene)
        {
                activeScene = scene;
                // Load saved preference
                String saved = SettingsRepository.get("theme", Theme.LIGHT.name());
                current = Captor.icall(() -> Theme.valueOf(saved)) != null
                        ? Theme.valueOf(saved) : Theme.LIGHT;
                apply(current);
        }

        public static void switchTheme(Theme theme)
        {
                if (activeScene == null || theme == current)
                        return;
                String oldUrl = themeCssUrl(current);
                if (oldUrl != null)
                        activeScene.getStylesheets().remove(oldUrl);
                current = theme;
                apply(theme);
                SettingsRepository.set("theme", theme.name());
                LOG.info("Theme switched to {}", theme);
        }

        private static void apply(Theme theme)
        {
                String url = themeCssUrl(theme);
                if (url != null && !activeScene.getStylesheets().contains(url))
                        activeScene.getStylesheets().add(0, url);
        }

        private static String themeCssUrl(Theme theme)
        {
                String path = "/css/theme/vk-theme-" + theme.name().toLowerCase() + ".css";
                var url = ThemeManager.class.getResource(path);
                return url != null ? url.toExternalForm() : null;
        }

        public static Theme getCurrent()
        {
                return current;
        }
}
