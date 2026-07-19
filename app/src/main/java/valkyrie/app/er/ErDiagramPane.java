package valkyrie.app.er;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import valkyrie.driver.api.Column;
import valkyrie.driver.api.ForeignKeyInfo;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;

import java.util.*;

/**
 * ER 图表面板
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class ErDiagramPane extends ScrollPane
{
        private final Pane canvas = new Pane();
        private final Map<String, VBox> tableNodes = new LinkedHashMap<>();

        public ErDiagramPane()
        {
                setContent(canvas);
                setPannable(true);
                canvas.setPrefSize(2000, 2000);

                // Zoom on scroll
                setOnScroll(e -> {
                        double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
                        double scale = Math.max(0.3, Math.min(3.0, canvas.getScaleX() * delta));
                        canvas.setScaleX(scale);
                        canvas.setScaleY(scale);
                });
        }

        public void render(Driver driver, Session session, List<String> tableNames)
        {
                canvas.getChildren().clear();
                tableNodes.clear();

                // Collect FK info
                Map<String, List<ForeignKeyInfo>> fkMap = new HashMap<>();

                int col = 0, row = 0;
                int maxCols = 4;
                int colSpacing = 280;
                int rowSpacing = 320;

                for (String tableName : tableNames) {
                        VBox node = createTableNode(driver, session, tableName);
                        node.setTranslateX(col * colSpacing + 20);
                        node.setTranslateY(row * rowSpacing + 20);
                        tableNodes.put(tableName, node);
                        canvas.getChildren().add(node);

                        // Get FKs
                        try {
                                List<ForeignKeyInfo> fks = driver.getImportedKeys(session, tableName);
                                if (!fks.isEmpty())
                                        fkMap.put(tableName, fks);
                        } catch (Exception ignored) {}

                        col++;
                        if (col >= maxCols) { col = 0; row++; }
                }

                // Draw FK lines
                for (Map.Entry<String, List<ForeignKeyInfo>> entry : fkMap.entrySet()) {
                        String fkTable = entry.getKey();
                        VBox fkNode = tableNodes.get(fkTable);
                        if (fkNode == null) continue;

                        for (ForeignKeyInfo fk : entry.getValue()) {
                                VBox pkNode = tableNodes.get(fk.pkTable());
                                if (pkNode == null) continue;

                                Line line = new Line();
                                line.startXProperty().bind(fkNode.translateXProperty().add(fkNode.widthProperty().divide(2)));
                                line.startYProperty().bind(fkNode.translateYProperty().add(fkNode.heightProperty().divide(2)));
                                line.endXProperty().bind(pkNode.translateXProperty().add(pkNode.widthProperty().divide(2)));
                                line.endYProperty().bind(pkNode.translateXProperty().add(pkNode.heightProperty().divide(2)));
                                line.setStroke(Color.valueOf("#3B82F6"));
                                line.setStrokeWidth(1.5);
                                line.setOpacity(0.6);
                                canvas.getChildren().add(0, line);
                        }
                }

                if (fkMap.isEmpty() && !tableNames.isEmpty()) {
                        Label hint = new Label("当前选区无外键关系");
                        hint.setStyle("-fx-text-fill: -vk-text-secondary; -fx-font-size: 14px; -fx-padding: 20;");
                        hint.setTranslateX(20);
                        hint.setTranslateY(20);
                        canvas.getChildren().add(hint);
                }
        }

        private VBox createTableNode(Driver driver, Session session, String tableName)
        {
                VBox node = new VBox(2);
                node.setPadding(new Insets(8));
                node.setStyle("-fx-background-color: -vk-surface; -fx-border-color: -vk-border; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
                node.setPrefWidth(240);

                // Table name header
                Label header = new Label(tableName);
                header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -vk-text; -fx-padding: 2 0 4 0;");
                node.getChildren().add(header);

                // Columns
                try {
                        List<Column> columns = driver.getColumns(session, tableName);
                        for (Column col : columns) {
                                String prefix = col.isPrimary() ? "* " : "  ";
                                Label colLabel = new Label(prefix + col.getName() + "  " + (col.getType() != null ? col.getType() : ""));
                                colLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -vk-text-secondary;");
                                if (col.isPrimary())
                                        colLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -vk-primary; -fx-font-weight: bold;");
                                node.getChildren().add(colLabel);
                        }
                } catch (Exception e) {
                        node.getChildren().add(new Label("(无法加载列信息)"));
                }

                // Make draggable
                final double[] dragStart = new double[2];
                node.setOnMousePressed(e -> {
                        dragStart[0] = e.getSceneX() - node.getTranslateX();
                        dragStart[1] = e.getSceneY() - node.getTranslateY();
                });
                node.setOnMouseDragged(e -> {
                        node.setTranslateX(e.getSceneX() - dragStart[0]);
                        node.setTranslateY(e.getSceneY() - dragStart[1]);
                });

                return node;
        }
}
