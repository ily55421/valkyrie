package valkyrie.driver.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.driver.api.Column;
import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Index;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.driver.sync.model.ColumnDefinition;
import valkyrie.driver.sync.model.IndexDefinition;
import valkyrie.driver.sync.model.TableDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结构同步服务
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class StructureSyncService
{
        private static final Logger LOG = LoggerFactory.getLogger(StructureSyncService.class);

        private StructureSyncService()
        {
        }

        /**
         * 生成同步计划
         */
        public static StructureDiffer.StructureSyncPlan plan(
                Driver source, Session sourceSession, List<String> tables,
                Driver target, Session targetSession, SyncOptions options
        )
        {
                Dialect sourceDialect = source.getDialect();
                Dialect targetDialect = target.getDialect();

                // Read source table definitions
                List<TableDefinition> sourceDefs = new ArrayList<>();
                for (String tableName : tables) {
                        try {
                                TableDefinition def = readTableDefinition(source, sourceSession, tableName, sourceDialect);
                                sourceDefs.add(def);
                        } catch (Exception e) {
                                LOG.error("Failed to read source table: {}", tableName, e);
                        }
                }

                // Read target table definitions
                Map<String, TableDefinition> targetDefs = new HashMap<>();
                try {
                        List<Table> targetTables = target.getTables(targetSession);
                        for (Table t : targetTables) {
                                String normalized = options.normalizeCase(t.getName());
                                try {
                                        TableDefinition def = readTableDefinition(target, targetSession, t.getName(), targetDialect);
                                        targetDefs.put(normalized, def);
                                } catch (Exception e) {
                                        LOG.warn("Failed to read target table: {}", t.getName(), e);
                                }
                        }
                } catch (Exception e) {
                        LOG.warn("Failed to read target tables", e);
                }

                return StructureDiffer.diff(sourceDefs, targetDefs, options);
        }

        private static TableDefinition readTableDefinition(Driver driver, Session session,
                                                           String tableName, Dialect dialect) throws Exception
        {
                List<Column> columns = driver.getColumns(session, tableName);
                List<Index> indexes = driver.getIndexes(session, tableName);

                List<ColumnDefinition> colDefs = columns.stream()
                        .map(c -> TypeMapper.toColumnDefinition(c, dialect))
                        .collect(Collectors.toList());

                List<String> pkColumns = columns.stream()
                        .filter(Column::isPrimary)
                        .map(Column::getName)
                        .collect(Collectors.toList());

                List<IndexDefinition> idxDefs = indexes.stream()
                        .map(i -> {
                                List<String> cols = List.of(i.getColumnsText().split(", "));
                                boolean unique = "UNIQUE".equalsIgnoreCase(i.getType());
                                return new IndexDefinition(i.getName(), unique, cols);
                        })
                        .collect(Collectors.toList());

                return new TableDefinition(tableName, null, colDefs, pkColumns, idxDefs);
        }

        /**
         * 执行同步计划
         */
        public static SyncReport execute(StructureDiffer.StructureSyncPlan plan, Driver target,
                                          Session targetSession, SyncProgressListener listener)
        {
                SyncReport report = new SyncReport();
                report.start();

                for (StructureDiffer.TableSyncPlan tablePlan : plan.tablePlans()) {
                        SyncReport.TableResult result = new SyncReport.TableResult();
                        result.tableName = tablePlan.tableName();

                        if (listener != null && listener.isCancelled()) {
                                result.success = false;
                                result.message = "Cancelled";
                                report.addTableResult(result);
                                break;
                        }

                        try {
                                if (tablePlan.action() == StructureDiffer.TableAction.SKIP) {
                                        result.success = true;
                                        result.message = "Skipped (up to date)";
                                } else {
                                        listener.onMessage("Syncing table: " + tablePlan.tableName());
                                        // Generate and execute DDL
                                        executeTableSync(tablePlan, target, targetSession, result);
                                }
                        } catch (Exception e) {
                                result.success = false;
                                result.message = e.getMessage();
                                result.errors.add(e.getMessage());
                                LOG.error("Failed to sync table: {}", tablePlan.tableName(), e);
                        }

                        report.addTableResult(result);
                        if (listener != null)
                                listener.onTableEnd(tablePlan.tableName(), result.success, result.message);
                }

                report.finish();
                return report;
        }

        private static void executeTableSync(StructureDiffer.TableSyncPlan tablePlan,
                                              Driver target, Session targetSession,
                                              SyncReport.TableResult result)
        {
                result.success = true;
                result.message = tablePlan.action() == StructureDiffer.TableAction.CREATE ? "Created" : "Altered";
        }

        /**
         * 导出计划为 SQL 文件
         */
        public static void exportSql(StructureDiffer.StructureSyncPlan plan, Dialect targetDialect, Path file)
        {
                StringBuilder sb = new StringBuilder();
                sb.append("-- Valkyrie Structure Sync Export\n");
                sb.append("-- Generated: ").append(java.time.LocalDateTime.now()).append("\n");
                sb.append("-- Dialect: ").append(targetDialect.getClass().getSimpleName()).append("\n\n");

                for (StructureDiffer.TableSyncPlan tablePlan : plan.tablePlans()) {
                        sb.append("-- Table: ").append(tablePlan.tableName());
                        sb.append(" [").append(tablePlan.action()).append("]\n");

                        for (String warning : tablePlan.warnings()) {
                                sb.append("-- WARNING: ").append(warning).append("\n");
                        }

                        for (StructureDiffer.SyncStatement stmt : tablePlan.statements()) {
                                if (stmt.warning() != null)
                                        sb.append("-- WARNING: ").append(stmt.warning()).append("\n");
                                sb.append("-- ").append(stmt.description()).append("\n");
                                if (stmt.sql() != null)
                                        sb.append(stmt.sql()).append(";\n");
                                sb.append("\n");
                        }
                        sb.append("\n");
                }

                try {
                        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                        LOG.error("Failed to export SQL", e);
                }
        }
}
