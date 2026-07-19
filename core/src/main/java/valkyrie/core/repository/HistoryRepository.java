package valkyrie.core.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import valkyrie.core.Users;
import valkyrie.core.utils.JSONUtils;
import valkyrie.utils.Captor;
import valkyrie.utils.io.UFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL 执行历史日志仓库（JSONL）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class HistoryRepository
{
        private static final String HISTORY_DIR = "history";
        private static final int MAX_PER_FILE = 100_000;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class HistoryEntry
        {
                private String time;
                private String connection;
                private String catalog;
                private String sql;
                private long durationMs;
                private long rows;
                private String status;
                private String errorMsg;
        }

        /**
         * 记录一条历史
         */
        public static void record(String sql, long cost, String status, String errorMsg,
                                   String connection, String catalog)
        {
                Captor.icall(() -> {
                        HistoryEntry entry = new HistoryEntry(
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                connection, catalog, sql, cost, 0, status, errorMsg);
                        String line = JSONUtils.toJSONString(entry);
                        appendToFile(getCurrentFile(), line);
                });
        }

        /**
         * 查询历史（过滤）
         */
        public static List<HistoryEntry> query(String keyword, String connectionFilter, String statusFilter)
        {
                List<HistoryEntry> result = new ArrayList<>();
                Captor.icall(() -> {
                        UFile dir = new UFile(Users.baseDir, HISTORY_DIR);
                        if (!dir.exists()) return;
                        UFile[] files = dir.listFiles();
                        if (files == null) return;
                        for (UFile file : files) {
                                if (!file.getName().endsWith(".jsonl")) continue;
                                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                for (String line : content.split("\n")) {
                                        if (line.isBlank()) continue;
                                        HistoryEntry entry = JSONUtils.toJavaObject(line, HistoryEntry.class);
                                        if (entry == null) continue;
                                        if (keyword != null && !keyword.isBlank()
                                                && (entry.getSql() == null || !entry.getSql().toLowerCase().contains(keyword.toLowerCase())))
                                                continue;
                                        if (connectionFilter != null && !connectionFilter.isBlank()
                                                && !connectionFilter.equals(entry.getConnection()))
                                                continue;
                                        if (statusFilter != null && !statusFilter.isBlank()
                                                && !statusFilter.equals(entry.getStatus()))
                                                continue;
                                        result.add(entry);
                                }
                        }
                });
                return result;
        }

        /**
         * 清空所有历史
         */
        public static void clearAll()
        {
                Captor.icall(() -> {
                        UFile dir = new UFile(Users.baseDir, HISTORY_DIR);
                        if (dir.exists()) {
                                UFile[] files = dir.listFiles();
                                if (files != null) {
                                        for (UFile f : files) f.forceDelete();
                                }
                        }
                });
        }

        private static UFile getCurrentFile()
        {
                LocalDate now = LocalDate.now();
                int week = now.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                String fileName = String.format("history-%d-W%d.jsonl", now.getYear(), week);
                UFile dir = new UFile(Users.baseDir, HISTORY_DIR);
                dir.mkdirs();
                return new UFile(dir, fileName);
        }

        private static void appendToFile(UFile file, String line)
        {
                Captor.icall(() -> {
                        java.nio.file.Files.write(file.toPath(),
                                (line + "\n").getBytes(StandardCharsets.UTF_8),
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND);
                });
        }
}
