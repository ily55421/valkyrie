package valkyrie.core.repository;

import valkyrie.core.model.DiskSavedConnection;
import valkyrie.core.utils.JSONUtils;
import valkyrie.utils.Captor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 连接导入导出
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class ConnectionTransfer
{
        public record ConflictResult(List<DiskSavedConnection> conflicts, List<DiskSavedConnection> newOnes) {}

        /**
         * 导出为 JSON
         */
        public static String exportToJson(List<DiskSavedConnection> connections, boolean includePassword)
        {
                List<DiskSavedConnection> export = new ArrayList<>();
                for (DiskSavedConnection conn : connections) {
                        DiskSavedConnection copy = new DiskSavedConnection();
                        copy.setName(conn.getName());
                        copy.setType(conn.getType());
                        copy.setSqlitePath(conn.getSqlitePath());
                        copy.setHost(conn.getHost());
                        copy.setPort(conn.getPort());
                        copy.setDb(conn.getDb());
                        copy.setUsername(conn.getUsername());
                        copy.setJdbcUrl(conn.getJdbcUrl());
                        copy.setTimezone(conn.getTimezone());
                        copy.setUseSSL(conn.getUseSSL());
                        copy.setTinyint1isBit(conn.getTinyint1isBit());
                        copy.setEnvTag(conn.getEnvTag());
                        if (includePassword) {
                                copy.setPassword(conn.getPassword());
                                copy.setSavePassword(conn.getSavePassword());
                        } else {
                                copy.setPassword("");
                                copy.setSavePassword(false);
                        }
                        export.add(copy);
                }
                return JSONUtils.toJSONString(export);
        }

        /**
         * 从 .vkc.json 导入
         */
        public static List<DiskSavedConnection> importFromVkc(Path file)
        {
                return Captor.icall(() -> {
                        byte[] bytes = java.nio.file.Files.readAllBytes(file);
                        String content = new String(bytes, StandardCharsets.UTF_8);
                        return JSONUtils.toJavaList(content, DiskSavedConnection.class);
                });
        }

        /**
         * 从 Navicat .ncx 导入（XML 解析）
         */
        public static List<DiskSavedConnection> importFromNcx(Path file)
        {
                return Captor.icall(() -> {
                        List<DiskSavedConnection> result = new ArrayList<>();
                        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                        var doc = builder.parse(file.toFile());
                        var connNodes = doc.getElementsByTagName("Connection");
                        for (int i = 0; i < connNodes.getLength(); i++) {
                                var node = connNodes.item(i);
                                var attrs = node.getAttributes();
                                DiskSavedConnection conn = new DiskSavedConnection();
                                conn.setName(sanitizeFileName(getAttr(attrs, "ConnectionName", "unnamed")));
                                conn.setType("mysql"); // NCX 无类型字段，默认 MySQL
                                conn.setHost(getAttr(attrs, "Host", "127.0.0.1"));
                                conn.setPort(getAttr(attrs, "Port", "3306"));
                                conn.setDb(getAttr(attrs, "Database", ""));
                                conn.setUsername(getAttr(attrs, "UserName", "root"));
                                conn.setPassword(""); // NCX 密码加密，置空
                                conn.setSavePassword(false);
                                result.add(conn);
                        }
                        return result;
                });
        }

        /**
         * 冲突检测
         */
        public static ConflictResult checkConflict(List<DiskSavedConnection> existing, List<DiskSavedConnection> imported)
        {
                Set<String> existingNames = existing.stream()
                        .map(DiskSavedConnection::getName)
                        .collect(Collectors.toSet());
                List<DiskSavedConnection> conflicts = new ArrayList<>();
                List<DiskSavedConnection> newOnes = new ArrayList<>();
                for (DiskSavedConnection conn : imported) {
                        if (existingNames.contains(conn.getName()))
                                conflicts.add(conn);
                        else
                                newOnes.add(conn);
                }
                return new ConflictResult(conflicts, newOnes);
        }

        private static String getAttr(org.w3c.dom.NamedNodeMap attrs, String name, String def)
        {
                var item = attrs.getNamedItem(name);
                return item != null ? item.getNodeValue() : def;
        }

        private static String sanitizeFileName(String name)
        {
                if (name == null) return "unnamed";
                return name.replaceAll("[/\\\\:*?\"<>|]", "_");
        }
}
