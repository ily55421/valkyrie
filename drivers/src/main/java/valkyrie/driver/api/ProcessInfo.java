package valkyrie.driver.api;

/**
 * 服务器进程信息
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public record ProcessInfo(
        long id,
        String user,
        String host,
        String db,
        String command,
        long time,
        String state,
        String info
) {}
