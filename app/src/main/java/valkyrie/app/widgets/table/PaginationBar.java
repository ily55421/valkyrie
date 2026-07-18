package valkyrie.app.widgets.table;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * 分页栏组件
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class PaginationBar extends HBox
{
        private final Label infoLabel = new Label();
        private final Button prevBtn = new Button("上一页");
        private final Button nextBtn = new Button("下一页");
        private final TextField pageField = new TextField();
        private final ComboBox<Integer> pageSizeCombo = new ComboBox<>();
        private int currentPage = 1;
        private long totalRows = 0;
        private int pageSize = 500;
        private Consumer<Integer> onPageChange;

        public PaginationBar()
        {
                super(8);
                setAlignment(Pos.CENTER_LEFT);
                setStyle("-fx-padding: 4 8; -fx-background-color: -vk-bg-alt; -fx-border-color: -vk-border transparent transparent transparent;");

                pageSizeCombo.getItems().addAll(100, 500, 1000);
                pageSizeCombo.setValue(pageSize);
                pageSizeCombo.setPrefWidth(100);
                pageSizeCombo.setOnAction(e -> {
                        pageSize = pageSizeCombo.getValue();
                        currentPage = 1;
                        firePageChange();
                });

                pageField.setPrefWidth(50);
                pageField.setOnAction(e -> {
                        try {
                                int p = Integer.parseInt(pageField.getText());
                                if (p >= 1 && p <= getTotalPages()) {
                                        currentPage = p;
                                        firePageChange();
                                }
                        } catch (NumberFormatException ignored) {
                        }
                        pageField.setText(String.valueOf(currentPage));
                });

                prevBtn.setOnAction(e -> {
                        if (currentPage > 1) {
                                currentPage--;
                                firePageChange();
                        }
                });

                nextBtn.setOnAction(e -> {
                        if (currentPage < getTotalPages()) {
                                currentPage++;
                                firePageChange();
                        }
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                getChildren().addAll(
                        prevBtn,
                        new Label("第"),
                        pageField,
                        new Label("页 / 共"),
                        new Label() {
                                @Override
                                public String toString()
                                {
                                        return String.valueOf(getTotalPages());
                                }
                        },
                        new Label("页"),
                        nextBtn,
                        spacer,
                        new Label("每页:"),
                        pageSizeCombo,
                        infoLabel
                );

                updateInfo();
        }

        private int getTotalPages()
        {
                return (int) Math.max(1, Math.ceil((double) totalRows / pageSize));
        }

        private void firePageChange()
        {
                updateInfo();
                if (onPageChange != null)
                        onPageChange.accept((currentPage - 1) * pageSize);
        }

        private void updateInfo()
        {
                long start = totalRows == 0 ? 0 : (long) (currentPage - 1) * pageSize + 1;
                long end = Math.min((long) currentPage * pageSize, totalRows);
                infoLabel.setText(String.format("Showing %d~%d of %d", start, end, totalRows));
                pageField.setText(String.valueOf(currentPage));
                prevBtn.setDisable(currentPage <= 1);
                nextBtn.setDisable(currentPage >= getTotalPages());
        }

        public void setTotalRows(long totalRows)
        {
                this.totalRows = totalRows;
                this.currentPage = 1;
                updateInfo();
        }

        public void setOnPageChange(Consumer<Integer> onPageChange)
        {
                this.onPageChange = onPageChange;
        }

        public int getPageSize()
        {
                return pageSize;
        }

        public int getCurrentOffset()
        {
                return (currentPage - 1) * pageSize;
        }
}
