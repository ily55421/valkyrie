package valkyrie.driver.api;

/**
 * 外键关系信息
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public record ForeignKeyInfo(
        String fkTable,
        String fkColumn,
        String pkTable,
        String pkColumn
) {}
