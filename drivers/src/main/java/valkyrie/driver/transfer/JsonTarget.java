package valkyrie.driver.transfer;

import valkyrie.driver.api.Column;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * JSON 导出目标
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class JsonTarget implements DataTarget
{
        private final Path file;
        private final Charset charset;
        private final boolean prettyPrint;
        private BufferedWriter writer;
        private List<Column> columns;
        private boolean firstRow = true;

        public JsonTarget(Path file, String charset, boolean prettyPrint)
        {
                this.file = file;
                this.charset = Charset.forName(charset);
                this.prettyPrint = prettyPrint;
        }

        @Override
        public void open(List<Column> columns) throws IOException
        {
                this.columns = columns;
                writer = Files.newBufferedWriter(file, charset);
                writer.write("[");
        }

        @Override
        public void writeRow(Object[] row) throws IOException
        {
                if (!firstRow) writer.write(",");
                if (prettyPrint) writer.newLine();
                writer.write("{");
                for (int i = 0; i < columns.size(); i++) {
                        if (i > 0) writer.write(",");
                        writer.write("\"" + escapeJson(columns.get(i).getName()) + "\":");
                        if (row[i] == null) {
                                writer.write("null");
                        } else if (row[i] instanceof Number) {
                                writer.write(row[i].toString());
                        } else {
                                writer.write("\"" + escapeJson(row[i].toString()) + "\"");
                        }
                }
                writer.write("}");
                firstRow = false;
        }

        @Override
        public void close() throws IOException
        {
                if (writer != null) {
                        writer.write("]");
                        writer.close();
                }
        }

        private String escapeJson(String s)
        {
                return s.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }
}
