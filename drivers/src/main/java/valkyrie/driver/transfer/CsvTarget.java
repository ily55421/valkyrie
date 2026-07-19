package valkyrie.driver.transfer;

import valkyrie.driver.api.Column;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CSV 导出目标
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class CsvTarget implements DataTarget
{
        private final Path file;
        private final Charset charset;
        private final char delimiter;
        private final boolean includeHeader;
        private BufferedWriter writer;
        private List<Column> columns;

        public CsvTarget(Path file, String charset, char delimiter, boolean includeHeader)
        {
                this.file = file;
                this.charset = Charset.forName(charset);
                this.delimiter = delimiter;
                this.includeHeader = includeHeader;
        }

        @Override
        public void open(List<Column> columns) throws IOException
        {
                this.columns = columns;
                writer = Files.newBufferedWriter(file, charset);
                if (includeHeader) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < columns.size(); i++) {
                                if (i > 0) sb.append(delimiter);
                                sb.append(escape(columns.get(i).getName()));
                        }
                        writer.write(sb.toString());
                        writer.newLine();
                }
        }

        @Override
        public void writeRow(Object[] row) throws IOException
        {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                        if (i > 0) sb.append(delimiter);
                        sb.append(escape(row[i] == null ? "" : row[i].toString()));
                }
                writer.write(sb.toString());
                writer.newLine();
        }

        private String escape(String value)
        {
                if (value == null) return "";
                boolean needQuote = value.indexOf(delimiter) >= 0
                        || value.contains("\n") || value.contains("\r") || value.contains("\"");
                if (needQuote) {
                        return "\"" + value.replace("\"", "\"\"") + "\"";
                }
                return value;
        }

        @Override
        public void close() throws IOException
        {
                if (writer != null) writer.close();
        }
}
