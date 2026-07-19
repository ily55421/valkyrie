package valkyrie.driver.datagen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 数据生成规则枚举
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public enum DataGenRule
{
        SEQUENCE,
        RANDOM_INT,
        RANDOM_DECIMAL,
        RANDOM_STRING,
        PATTERN,
        ENUM,
        DICT,
        DATE_RANGE,
        CURRENT,
        REFERENCE,
        NULL_RATIO;

        /** 内置字典类型 */
        public enum DictType {
                CN_NAME, EN_NAME, EMAIL, PHONE, UUID, URL, CITY, COMPANY
        }

        /** 字典数据 */
        public static final Map<DictType, List<String>> DICT_DATA = new EnumMap<>(DictType.class);

        static {
                DICT_DATA.put(DictType.CN_NAME, List.of("张伟", "李娜", "王芳", "刘洋", "陈静", "杨光", "赵磊", "黄丽", "周杰", "吴敏"));
                DICT_DATA.put(DictType.EN_NAME, List.of("John Smith", "Jane Doe", "Bob Johnson", "Alice Brown", "Charlie Wilson"));
                DICT_DATA.put(DictType.EMAIL, List.of("user1@example.com", "test@test.com", "admin@demo.org"));
                DICT_DATA.put(DictType.PHONE, List.of("13800138000", "13900139000", "15012345678"));
                DICT_DATA.put(DictType.UUID, List.of("AUTO"));
                DICT_DATA.put(DictType.URL, List.of("https://example.com", "https://test.org/page", "http://demo.net"));
                DICT_DATA.put(DictType.CITY, List.of("北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "南京"));
                DICT_DATA.put(DictType.COMPANY, List.of("华夏科技有限公司", "东方科技股份有限公司", "创新集团有限公司"));
        }

        private static final Random RANDOM = new Random();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RuleConfig
        {
                private DataGenRule rule;
                private Map<String, String> params;
                private DictType dictType;
        }

        /**
         * 生成单个值
         */
        public static Object generate(RuleConfig config, int rowIndex)
        {
                if (config == null) return null;

                // NULL_RATIO handling
                if (config.getRule() == NULL_RATIO) {
                        String pct = config.getParams() != null ? config.getParams().get("percent") : "10";
                        int percent = Integer.parseInt(pct != null ? pct : "10");
                        if (RANDOM.nextInt(100) < percent) return null;
                        // Delegate to inner rule
                        String delegateRule = config.getParams() != null ? config.getParams().get("delegate") : null;
                        if (delegateRule != null) {
                                RuleConfig inner = new RuleConfig(
                                        DataGenRule.valueOf(delegateRule), config.getParams(), config.getDictType());
                                return generate(inner, rowIndex);
                        }
                        return null;
                }

                Map<String, String> params = config.getParams() != null ? config.getParams() : new HashMap<>();

                return switch (config.getRule()) {
                        case SEQUENCE -> {
                                long start = Long.parseLong(params.getOrDefault("start", "1"));
                                long step = Long.parseLong(params.getOrDefault("step", "1"));
                                yield start + (long) rowIndex * step;
                        }
                        case RANDOM_INT -> {
                                int min = Integer.parseInt(params.getOrDefault("min", "0"));
                                int max = Integer.parseInt(params.getOrDefault("max", "10000"));
                                yield min + RANDOM.nextInt(max - min + 1);
                        }
                        case RANDOM_DECIMAL -> {
                                double min = Double.parseDouble(params.getOrDefault("min", "0"));
                                double max = Double.parseDouble(params.getOrDefault("max", "10000"));
                                int scale = Integer.parseInt(params.getOrDefault("scale", "2"));
                                double val = min + RANDOM.nextDouble() * (max - min);
                                yield Math.round(val * Math.pow(10, scale)) / Math.pow(10, scale);
                        }
                        case RANDOM_STRING -> {
                                int len = Integer.parseInt(params.getOrDefault("length", "10"));
                                String chars = params.getOrDefault("charset", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < len; i++)
                                        sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
                                yield sb.toString();
                        }
                        case PATTERN -> params.getOrDefault("template", "PREFIX_" + rowIndex);
                        case ENUM -> {
                                String[] values = params.getOrDefault("values", "A,B,C").split(",");
                                yield values[RANDOM.nextInt(values.length)];
                        }
                        case DICT -> {
                                if (config.getDictType() == DictType.UUID)
                                        yield java.util.UUID.randomUUID().toString();
                                List<String> data = DICT_DATA.get(config.getDictType());
                                if (data == null || data.isEmpty()) yield "";
                                yield data.get(RANDOM.nextInt(data.size()));
                        }
                        case DATE_RANGE -> {
                                String from = params.getOrDefault("from", "2020-01-01");
                                String to = params.getOrDefault("to", "2026-12-31");
                                yield from; // Simplified: return from date
                        }
                        case CURRENT -> java.time.LocalDateTime.now().toString();
                        case REFERENCE -> "REF_" + rowIndex; // Simplified
                        default -> null;
                };
        }
}
