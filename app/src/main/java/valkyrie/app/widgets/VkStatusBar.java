package valkyrie.app.widgets;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.event.ConnectedSuccessEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * 状态栏
 *
 * @author Luo Tiansheng
 * @since 2026/5/18
 */
public class VkStatusBar extends BorderPane implements EventListener
{
        private static final Logger LOG = LoggerFactory.getLogger(VkStatusBar.class);
        private static final VkStatusBar INSTANCE = new VkStatusBar();

        private final Label connectionLabel = new Label();
        private final Circle statusDot = new Circle(4);
        private final Label rowsLabel = new Label();
        private final Label timeLabel = new Label();
        private final Label messageLabel = new Label();
        private final Label encodingLabel = new Label("UTF-8");
        private final Label txModeLabel = new Label("Auto Commit");
        private final Label memoryLabel = new Label();

        private VkStatusBar()
        {
                getStyleClass().add("vk-status-bar");

                statusDot.getStyleClass().add("status-dot");
                statusDot.getStyleClass().add("disconnected");

                // Left: connection status + rows + time
                HBox leftBox = new HBox(8);
                leftBox.getChildren().addAll(
                        statusDot,
                        connectionLabel,
                        createSeparator(),
                        rowsLabel,
                        createSeparator(),
                        timeLabel
                );

                // Center: message (compatible with updateMessage)
                messageLabel.setText("(STARTING)");

                // Right: encoding + tx mode + memory
                HBox rightBox = new HBox(8);
                rightBox.getChildren().addAll(
                        encodingLabel,
                        createSeparator(),
                        txModeLabel,
                        createSeparator(),
                        memoryLabel
                );

                setLeft(leftBox);
                setCenter(messageLabel);
                setRight(rightBox);

                // Subscribe to connection events
                EventBus.subscribe(this, ConnectedSuccessEvent.class);

                // Memory monitor: update every 5 seconds
                startMemoryMonitor();
        }

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof ConnectedSuccessEvent e) {
                        String name = e.getConnectionNode().getLabel();
                        updateConnection(name, true);
                }
        }

        private Region createSeparator()
        {
                Region sep = new Region();
                sep.setPrefWidth(1);
                sep.setStyle("-fx-background-color: -vk-border;");
                sep.setMaxHeight(16);
                return sep;
        }

        private void startMemoryMonitor()
        {
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                Timeline timeline = new Timeline();
                timeline.getKeyFrames().add(new javafx.animation.KeyFrame(Duration.seconds(5), e -> {
                        long used = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
                        long max = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
                        Platform.runLater(() -> memoryLabel.setText(String.format("Mem: %d/%dMB", used, max)));
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
        }

        public static VkStatusBar getInstance()
        {
                return INSTANCE;
        }

        /**
         * 兼容旧 API：更新消息区
         */
        public void updateMessage(String message)
        {
                String frameMessage = message.replaceAll("\n", "");

                if (frameMessage.length() > 128)
                        frameMessage = frameMessage.substring(0, 128) + "...";

                messageLabel.setText(frameMessage);
        }

        /**
         * 更新连接状态
         */
        public void updateConnection(String name, boolean connected)
        {
                Platform.runLater(() -> {
                        connectionLabel.setText(name != null ? name : "");
                        if (connected) {
                                statusDot.getStyleClass().remove("disconnected");
                        } else {
                                if (!statusDot.getStyleClass().contains("disconnected"))
                                        statusDot.getStyleClass().add("disconnected");
                        }
                });
        }

        /**
         * 更新查询结果信息
         */
        public void updateQueryResult(long rows, long timeMs)
        {
                Platform.runLater(() -> {
                        rowsLabel.setText(String.format("Rows: %d", rows));
                        timeLabel.setText(String.format("Time: %dms", timeMs));
                });
        }
}
