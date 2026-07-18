package valkyrie.app.dialog.connection;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.app.pane.PropertyGridPane;
import valkyrie.core.model.EnvTag;
import valkyrie.driver.api.DbType;

import static valkyrie.utils.string.StrStaticImports.strhas;

/**
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
class ConnectionGeneralPane extends PropertyGridPane
{
        private final ConnectionPropertyModel info;

        private final TextField name = new TextField();
        private final TextField host = new TextField();
        private final TextField port = new TextField();
        private final TextField db = new TextField();
        private final TextField username = new TextField();
        private final TextField sqlitePath = new TextField();
        private final PasswordField password = new PasswordField();
        private final CheckBox savePassword = new CheckBox("保存密码");
        private final ComboBox<String> envSelector = new ComboBox<>();

        public ConnectionGeneralPane(ConnectionPropertyModel info)
        {
                super();
                this.info = info;
                bindModelProperty();
                setupPaneLayout();

                host.textProperty().addListener((observable, oldValue, newValue) -> {
                        if (strhas(newValue, ":")) {
                                String[] ret = newValue.split(":");
                                host.textProperty().set(ret[0]);
                                port.textProperty().set(ret[1]);
                        }
                });
        }

        private void bindModelProperty()
        {
                name.textProperty().bindBidirectional(info.nameProperty());
                host.textProperty().bindBidirectional(info.hostProperty());
                port.textProperty().bindBidirectional(info.portProperty());
                db.textProperty().bindBidirectional(info.dbProperty());
                password.textProperty().bindBidirectional(info.passwordProperty());
                username.textProperty().bindBidirectional(info.usernameProperty());
                sqlitePath.textProperty().bindBidirectional(info.sqlitePathProperty());
                savePassword.selectedProperty().bindBidirectional(info.savePasswordProperty());

                // Environment selector
                envSelector.getItems().addAll("生产", "测试", "开发", "本地");
                envSelector.getSelectionModel().select(EnvTag.of(info.getEnvTag()).ordinal());
                envSelector.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
                        String[] tags = {"PRODUCTION", "TEST", "DEV", "LOCAL"};
                        info.setEnvTag(tags[idx.intValue()]);
                });
        }

        private void setupPaneLayout()
        {
                addRow("连接名称", name);
                addRow("环境", envSelector);
                addRow(null, new Label()); /* separator */

                if (info.getDbType() != DbType.sqlite) {
                        addRow("主机地址", host);
                        addRow("端口号", port);

                        if (info.getDbType() == DbType.postgresql)
                                addRow("初始数据库", db);

                        addRow("用户名", username);
                        addRow("密码", password);
                        addRow(null, new Label()); /* separator */
                        addRow(null, savePassword);
                } else {
                        addRow("数据库文件路径", sqlitePath);
                }
        }

}
