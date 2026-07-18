package valkyrie.driver.sync.model;

import java.util.List;

/**
 * 索引定义
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public record IndexDefinition(
        String name,
        boolean unique,
        List<String> columns
)
{
}
