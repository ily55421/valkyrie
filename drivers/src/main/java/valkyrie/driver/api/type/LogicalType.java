package valkyrie.driver.api.type;

/**
 * 统一逻辑类型中枢（跨库类型映射的中间表示）
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public enum LogicalType
{
        STRING,
        TEXT,
        LONG_TEXT,
        INT,
        BIGINT,
        DECIMAL,
        FLOAT,
        DOUBLE,
        DATE,
        TIME,
        DATETIME,
        BOOLEAN,
        BINARY,
        LONG_BINARY,
        JSON,
        UNKNOWN;

        /**
         * 判断是否为日期时间类型族
         */
        public boolean isDateTimeFamily()
        {
                return this == DATE || this == TIME || this == DATETIME;
        }

        /**
         * 判断是否为数值类型
         */
        public boolean isNumeric()
        {
                return this == INT || this == BIGINT || this == DECIMAL || this == FLOAT || this == DOUBLE;
        }

        /**
         * 判断是否为大对象类型（不支持默认值）
         */
        public boolean isLobType()
        {
                return this == TEXT || this == LONG_TEXT || this == LONG_BINARY || this == JSON;
        }
}
