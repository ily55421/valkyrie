package valkyrie.driver.transfer;

import valkyrie.driver.api.Column;
import valkyrie.driver.api.Dialect;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SQL INSERT 导出目标
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class SqlInsertTarget implements DataTarget
{
        private final Path file;
        private final String tableName;
        private final Dialect dialect;
        private final Charset charset;
        private BufferedWriter writer;
        private List<Column> columns;
        private String insertTemplate;

        public SqlInsertTarget(Path file, String tableName, String charset, Dialect dialect)
        {
                this.file = file;
                this.tableName = tableName;
                this.dialect = dialect;
                this.charset = Charset.forName(charset);
        }

        @Override
        public void open(List<Column> columns) throws IOException
        {
                this.columns = columns;
                writer = Files.newBufferedWriter(file, charset);
                List<String> colNames = columns.stream().map(Column::getName).toList();
                insertTemplate = dialect.insertSql(tableName, colNames);
                writer.write("-- Valkyrie SQL Export");
                writer.newLine();
                writer.write("-- Table: " + tableName);
                writer.newLine();
                writer.newLine();
        }

        @Override
        public void writeRow(Object[] row) throws IOException
        {
                StringBuilder sb = new StringBuilder(insertTemplate);
                for (int i = 0; i < row.length; i++) {
                        String placeholder = "?";
                        int idx = sb.indexOf(placeholder, sb.indexOf("VALUES"));
                        if (idx < 0) break;
                        String literal;
                        if (row[i] == null) {
                                literal = "NULL";
                        } else if (row[i] instanceof Number) {
                                literal = row[i].toString();
                        } else {
                                literal = "'" + row[i].toString().replace("'", "''") + "'";
                        }
                        sb.replace(idx, idx + 1, literal);
                }
                sb.append(";");
                writer.write(sb.toString());
                writer.newLine();
        }

        @Override
        public void close() throws IOException
        {
                if (writer != null) writer.close();
        }
}
