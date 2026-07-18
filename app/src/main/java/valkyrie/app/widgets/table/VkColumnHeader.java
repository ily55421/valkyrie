package valkyrie.app.widgets.table;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import valkyrie.driver.api.Column;

/**
 * 表格列头：主键图标 + 字段名 + 类型
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class VkColumnHeader extends HBox
{
        public VkColumnHeader(Column column)
        {
                super(4);
                setAlignment(Pos.CENTER_LEFT);

                // Primary key indicator
                if (column.isPrimary()) {
                        Label keyIcon = new Label("🔑");
                        keyIcon.setStyle("-fx-font-size: 10px;");
                        getChildren().add(keyIcon);
                }

                // Column name (bold)
                Label nameLabel = new Label(column.getLabel() != null ? column.getLabel() : column.getName());
                nameLabel.setStyle("-fx-font-weight: bold;");
                getChildren().add(nameLabel);

                // Column type (secondary color, smaller)
                if (column.getType() != null) {
                        Label typeLabel = new Label(column.getType());
                        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -vk-text-secondary;");
                        getChildren().add(typeLabel);
                }
        }
}
