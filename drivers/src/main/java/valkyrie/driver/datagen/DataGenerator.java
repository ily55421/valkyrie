package valkyrie.driver.datagen;

import valkyrie.driver.api.Column;
import valkyrie.driver.api.type.LogicalType;

import java.util.*;
import java.util.stream.Stream;

/**
 * 数据生成器
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class DataGenerator
{
        private final List<ColumnGenConfig> configs;
        private final long targetRows;

        public DataGenerator(List<ColumnGenConfig> configs, long targetRows)
        {
                this.configs = configs;
                this.targetRows = targetRows;
        }

        public Object[] generateRow(int rowIndex)
        {
                Object[] row = new Object[configs.size()];
                for (int i = 0; i < configs.size(); i++) {
                        row[i] = DataGenRule.generate(configs.get(i).getConfig(), rowIndex);
                }
                return row;
        }

        public Stream<Object[]> stream()
        {
                return Stream.iterate(0, i -> i + 1)
                        .limit(targetRows)
                        .map(this::generateRow);
        }

        /**
         * 基于列元数据推断默认规则
         */
        public static DataGenRule.RuleConfig inferDefault(Column column)
        {
                String name = column.getName() != null ? column.getName().toLowerCase() : "";
                LogicalType logical = column.getType() != null
                        ? LogicalType.valueOf("UNKNOWN") // Would use dialect.toLogicalType
                        : LogicalType.UNKNOWN;

                // Name-based heuristics
                if (name.contains("email") || name.contains("mail"))
                        return new DataGenRule.RuleConfig(DataGenRule.DICT, Map.of(), DataGenRule.DictType.EMAIL);
                if (name.contains("phone") || name.contains("mobile") || name.contains("tel"))
                        return new DataGenRule.RuleConfig(DataGenRule.DICT, Map.of(), DataGenRule.DictType.PHONE);
                if (name.contains("name"))
                        return new DataGenRule.RuleConfig(DataGenRule.DICT, Map.of(), DataGenRule.DictType.CN_NAME);
                if (name.contains("city"))
                        return new DataGenRule.RuleConfig(DataGenRule.DICT, Map.of(), DataGenRule.DictType.CITY);
                if (name.contains("url") || name.contains("link"))
                        return new DataGenRule.RuleConfig(DataGenRule.DICT, Map.of(), DataGenRule.DictType.URL);
                if (name.contains("company"))
                        return new DataGenRule.RuleConfig(DataGenRule.DICT, Map.of(), DataGenRule.DictType.COMPANY);

                // PK auto increment
                if (column.isPrimary() && (name.contains("id") || name.contains("Id")))
                        return new DataGenRule.RuleConfig(DataGenRule.SEQUENCE,
                                Map.of("start", "1", "step", "1"), null);

                // Type-based defaults
                if (logical == LogicalType.INT || logical == LogicalType.BIGINT)
                        return new DataGenRule.RuleConfig(DataGenRule.RANDOM_INT,
                                Map.of("min", "0", "max", "10000"), null);
                if (logical == LogicalType.DECIMAL || logical == LogicalType.FLOAT || logical == LogicalType.DOUBLE)
                        return new DataGenRule.RuleConfig(DataGenRule.RANDOM_DECIMAL,
                                Map.of("min", "0", "max", "10000", "scale", "2"), null);
                if (logical == LogicalType.DATE || logical == LogicalType.DATETIME)
                        return new DataGenRule.RuleConfig(DataGenRule.DATE_RANGE,
                                Map.of("from", "2020-01-01", "to", "2026-12-31"), null);
                if (logical == LogicalType.BOOLEAN)
                        return new DataGenRule.RuleConfig(DataGenRule.ENUM,
                                Map.of("values", "true,false"), null);

                // Default: random string
                return new DataGenRule.RuleConfig(DataGenRule.RANDOM_STRING,
                        Map.of("length", "10"), null);
        }

        public static class ColumnGenConfig
        {
                private final Column column;
                private final DataGenRule.RuleConfig config;

                public ColumnGenConfig(Column column, DataGenRule.RuleConfig config)
                {
                        this.column = column;
                        this.config = config != null ? config : inferDefault(column);
                }

                public Column getColumn() { return column; }
                public DataGenRule.RuleConfig getConfig() { return config; }
        }
}
