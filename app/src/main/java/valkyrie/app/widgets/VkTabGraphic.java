package valkyrie.app.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import valkyrie.app.assets.Assets;

/**
 * Tab 图标组件：类型图标 + 标题 + dirty 圆点 + 关闭按钮
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class VkTabGraphic extends HBox
{
        private final Label titleLabel;
        private final Label dirtyDot;
        private boolean dirty = false;

        public VkTabGraphic(String title, String iconName)
        {
                super(4);
                setAlignment(Pos.CENTER_LEFT);

                // Type icon
                ImageView icon = Assets.use(iconName + "@1x");

                // Title
                titleLabel = new Label(title);

                // Dirty dot
                dirtyDot = new Label("●");
                dirtyDot.setStyle("-fx-text-fill: -vk-danger; -fx-font-size: 10px;");
                dirtyDot.setVisible(false);
                dirtyDot.setManaged(false);

                // Spacer
                Region spacer = new Region();
                spacer.setMinWidth(4);

                getChildren().addAll(icon, titleLabel, dirtyDot, spacer);
        }

        public void setTitle(String title)
        {
                titleLabel.setText(title);
        }

        public void setDirty(boolean dirty)
        {
                this.dirty = dirty;
                dirtyDot.setVisible(dirty);
                dirtyDot.setManaged(dirty);
        }

        public boolean isDirty()
        {
                return dirty;
        }
}
