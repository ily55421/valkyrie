package valkyrie.app;

import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenQueryEditorPaneEvent;

/**
 * @author Luo Tiansheng
 * @since 2026/6/3
 */
public class Publisher
{
        public static void openQueryEditor()
        {
                EventBus.publish(new OpenQueryEditorPaneEvent(null));
        }

        /**
         * 打开查询编辑器（带内容，用于打开外部文件）
         */
        public static void openQueryEditor(String content, String suggestedName)
        {
                EventBus.publish(new OpenQueryEditorPaneEvent(null, content, suggestedName));
        }
}
