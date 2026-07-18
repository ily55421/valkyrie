package valkyrie.driver.sync;

/**
 * 同步进度监听器
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public interface SyncProgressListener
{
        void onTableStart(String table, long totalRows);

        void onRows(String table, long doneRows);

        void onTableEnd(String table, boolean success, String message);

        void onMessage(String message);

        boolean isCancelled();
}
