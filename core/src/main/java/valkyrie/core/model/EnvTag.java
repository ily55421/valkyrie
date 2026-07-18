package valkyrie.core.model;

/**
 * 环境标签
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public enum EnvTag
{
        PRODUCTION, TEST, DEV, LOCAL;

        /**
         * 从字符串解析，兼容存量配置（null/空值返回 LOCAL）
         */
        public static EnvTag of(String value)
        {
                if (value == null || value.isBlank())
                        return LOCAL;
                try {
                        return valueOf(value.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                        return LOCAL;
                }
        }

        /**
         * 获取对应的 CSS 样式类名
         */
        public String cssClass()
        {
                return "env-" + name().toLowerCase();
        }
}
