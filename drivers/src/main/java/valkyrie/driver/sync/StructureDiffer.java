package valkyrie.driver.sync;

import valkyrie.driver.api.type.LogicalType;
import valkyrie.driver.sync.model.ColumnDefinition;
import valkyrie.driver.sync.model.IndexDefinition;
import valkyrie.driver.sync.model.TableDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构差异比对器
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public final class StructureDiffer
{
        public enum TableAction { CREATE, ALTER, SKIP }
        public enum ColumnAction { ADD, MODIFY, DROP }

        /**
         * 同步语句
         */
        public record SyncStatement(
                String sql,
                String description,
                boolean risk,
                String warning
        )
        {
        }

        /**
         * 表同步计划
         */
        public record TableSyncPlan(
                String tableName,
                TableAction action,
                List<SyncStatement> statements,
                List<String> warnings
        )
        {
        }

        /**
         * 完整同步计划
         */
        public record StructureSyncPlan(
                List<TableSyncPlan> tablePlans
        )
        {
        }

        private StructureDiffer()
        {
        }

        /**
         * 比对源与目标结构差异
         */
        public static StructureSyncPlan diff(
                List<TableDefinition> sourceTables,
                Map<String, TableDefinition> targetTables,
                SyncOptions options
        )
        {
                List<TableSyncPlan> plans = new ArrayList<>();

                for (TableDefinition source : sourceTables) {
                        String normalizedName = options.normalizeCase(source.name());
                        TableDefinition target = targetTables.get(normalizedName);

                        if (target == null) {
                                // New table - CREATE
                                plans.add(new TableSyncPlan(source.name(), TableAction.CREATE,
                                        List.of(), List.of("Will create new table")));
                        } else {
                                // Existing table - ALTER or SKIP
                                List<SyncStatement> stmts = new ArrayList<>();
                                List<String> warnings = new ArrayList<>();
                                diffColumns(source, target, options, stmts, warnings);
                                diffIndexes(source, target, options, stmts, warnings);

                                TableAction action = stmts.isEmpty() ? TableAction.SKIP : TableAction.ALTER;
                                plans.add(new TableSyncPlan(source.name(), action, stmts, warnings));
                        }
                }

                return new StructureSyncPlan(plans);
        }

        private static void diffColumns(TableDefinition source, TableDefinition target,
                                        SyncOptions options, List<SyncStatement> stmts, List<String> warnings)
        {
                Map<String, ColumnDefinition> targetCols = new HashMap<>();
                for (ColumnDefinition col : target.columns()) {
                        targetCols.put(options.normalizeCase(col.name()), col);
                }

                for (ColumnDefinition sourceCol : source.columns()) {
                        String colName = options.normalizeCase(sourceCol.name());
                        ColumnDefinition targetCol = targetCols.get(colName);

                        if (targetCol == null) {
                                // Add column
                                stmts.add(new SyncStatement(
                                        null,
                                        "ADD COLUMN " + sourceCol.name(),
                                        false,
                                        null
                                ));
                        } else {
                                // Check for modification
                                boolean typeChanged = sourceCol.logicalType() != targetCol.logicalType();
                                boolean sizeChanged = !safeEquals(sourceCol.size(), targetCol.size());
                                boolean scaleChanged = !safeEquals(sourceCol.scale(), targetCol.scale());
                                boolean nullChanged = sourceCol.notNull() != targetCol.notNull();

                                if (typeChanged || sizeChanged || scaleChanged || nullChanged) {
                                        StringBuilder desc = new StringBuilder("MODIFY COLUMN ");
                                        desc.append(sourceCol.name());
                                        if (typeChanged) desc.append(" (type change)");
                                        if (sizeChanged) desc.append(" (size change)");
                                        stmts.add(new SyncStatement(null, desc.toString(), false, null));
                                }
                        }
                }

                // DROP columns (only mark, don't generate SQL in v1.1)
                Map<String, ColumnDefinition> sourceCols = new HashMap<>();
                for (ColumnDefinition col : source.columns()) {
                        sourceCols.put(options.normalizeCase(col.name()), col);
                }
                for (ColumnDefinition targetCol : target.columns()) {
                        String colName = options.normalizeCase(targetCol.name());
                        if (!sourceCols.containsKey(colName)) {
                                warnings.add("Column " + targetCol.name() + " exists in target but not in source (will NOT be dropped)");
                        }
                }
        }

        private static void diffIndexes(TableDefinition source, TableDefinition target,
                                        SyncOptions options, List<SyncStatement> stmts, List<String> warnings)
        {
                Map<String, IndexDefinition> targetIdx = new HashMap<>();
                if (target.indexes() != null) {
                        for (IndexDefinition idx : target.indexes()) {
                                targetIdx.put(options.normalizeCase(idx.name()), idx);
                        }
                }

                if (source.indexes() != null) {
                        for (IndexDefinition sourceIdx : source.indexes()) {
                                String idxName = options.normalizeCase(sourceIdx.name());
                                if (!targetIdx.containsKey(idxName)) {
                                        stmts.add(new SyncStatement(
                                                null,
                                                "ADD INDEX " + sourceIdx.name(),
                                                false,
                                                null
                                        ));
                                }
                        }
                }
        }

        private static boolean safeEquals(Object a, Object b)
        {
                if (a == null && b == null) return true;
                if (a == null || b == null) return false;
                return a.equals(b);
        }
}
