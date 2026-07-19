package valkyrie.app.history;

import valkyrie.core.repository.HistoryRepository;
import valkyrie.driver.api.SQLExecuteHook;

/**
 * SQL 执行历史钩子
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class HistoryHook implements SQLExecuteHook
{
        private final String connectionName;
        private final String catalog;

        public HistoryHook(String connectionName, String catalog)
        {
                this.connectionName = connectionName != null ? connectionName : "";
                this.catalog = catalog != null ? catalog : "";
        }

        @Override
        public void afterExecute(String sql, long cost)
        {
                HistoryRepository.record(sql, cost, "OK", null, connectionName, catalog);
        }

        @Override
        public void onError(String sql, Throwable e)
        {
                HistoryRepository.record(sql, 0, "ERROR", e.getMessage(), connectionName, catalog);
        }
}
