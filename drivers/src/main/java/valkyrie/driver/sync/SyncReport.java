package valkyrie.driver.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步报告
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class SyncReport
{
        private final List<TableResult> tableResults = new ArrayList<>();
        private long totalRows = 0;
        private long successRows = 0;
        private long failedRows = 0;
        private long startTime;
        private long endTime;

        public void start()
        {
                startTime = System.currentTimeMillis();
        }

        public void finish()
        {
                endTime = System.currentTimeMillis();
        }

        public void addTableResult(TableResult result)
        {
                tableResults.add(result);
                totalRows += result.totalRows;
                successRows += result.successRows;
                failedRows += result.failedRows;
        }

        public List<TableResult> getTableResults() { return tableResults; }
        public long getTotalRows() { return totalRows; }
        public long getSuccessRows() { return successRows; }
        public long getFailedRows() { return failedRows; }
        public long getDurationMs() { return endTime - startTime; }

        public static class TableResult
        {
                public String tableName;
                public boolean success;
                public String message;
                public long totalRows;
                public long successRows;
                public long failedRows;
                public List<String> errors = new ArrayList<>();
                public List<String> warnings = new ArrayList<>();
        }
}
