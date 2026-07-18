# monacofx 模块

## 模块概述

`monacofx` 模块是 Monaco Editor 的 JavaFX 封装，通过 WebView 嵌入 Monaco Editor（VS Code 使用的代码编辑器），提供专业的代码编辑体验。

## 文件结构

```
monacofx/
├── pom.xml
└── src/main/
    ├── java/valkyrie/monacofx/
    │   └── MonacoFX.java         # JavaFX 封装类
    └── resources/
        ├── index.html            # Monaco Editor 加载页面
        ├── css/
        │   └── monaco.css        # 样式
        └── js/
            ├── monaco-editor/    # Monaco Editor 库文件（~120个JS文件）
            ├── loader.js         # 加载器
            ├── editor.js         # 编辑器初始化
            └── bridge.js         # Java-JS 桥接
```

## 核心类

### MonacoFX

**路径**: `monacofx/src/main/java/valkyrie/monacofx/MonacoFX.java`

继承 `javafx.scene.web.WebView`，封装 Monaco Editor 的 JavaFX 组件。

```java
public class MonacoFX extends WebView {
    
    // 编辑器操作
    public String getValue();                          // 获取编辑器内容
    public void setValue(String value);                // 设置编辑器内容
    public void setLanguage(String language);          // 设置语言（sql, java等）
    public void setTheme(String theme);                // 设置主题
    public void setEditable(boolean editable);         // 设置是否可编辑
    
    // 光标/选区操作
    public void setPosition(int line, int column);     // 设置光标位置
    public String getSelectedText();                   // 获取选中文本
    
    // 编辑操作
    public void executeAction(String action);          // 执行编辑命令
    public void formatDocument();                      // 格式化文档
    
    // 自动补全
    public void setCompletions(List<CompletionItem> items);  // 设置补全项
    
    // 事件监听
    public void onContentChange(ContentChangeListener listener);
    public void onKeyPress(KeyPressListener listener);
    public void onContextMenu(ContextMenuListener listener);
}
```

## 支持的快捷键

Monaco Editor 内置 VS Code 快捷键：

| 快捷键 | 功能 |
|--------|------|
| Ctrl+C | 复制 |
| Ctrl+X | 剪切 |
| Ctrl+V | 粘贴 |
| Ctrl+D | 选中下一个匹配项 |
| Ctrl+Shift+U | 转换为大写 |
| Ctrl+Z | 撤销 |
| Ctrl+Y | 重做 |
| Ctrl+F | 查找 |
| Ctrl+H | 替换 |
| Ctrl+A | 全选 |
| Ctrl+/ | 注释/取消注释 |
| Tab | 缩进 |
| Shift+Tab | 取消缩进 |

## Java-JavaScript 桥接

通过 `JSObject` 实现 Java 与 JavaScript 的双向通信：

```java
// Java → JS
webEngine.executeScript("editor.setValue('...')");

// JS → Java
JSObject window = (JSObject) webEngine.executeScript("window");
window.setMember("javaBridge", this);
// JavaScript 中可调用 window.javaBridge.someMethod(...)
```

## 支持的语言

Monaco Editor 原生支持多种语言高亮，本项目主要使用：
- `sql` - SQL 语法高亮
- 可扩展支持其他语言

## 自定义补全

通过 `setCompletions()` 方法提供自定义自动补全项：

```java
List<CompletionItem> suggestions = List.of(
    new CompletionItem("SELECT", "Keyword", "SELECT "),
    new CompletionItem("FROM", "Keyword", "FROM "),
    new CompletionItem("table_name", "Table", "table_name")
);
monacoFX.setCompletions(suggestions);
```

补全项类型：
- `Keyword` - SQL 关键字
- `Table` - 表名
- `Column` - 列名
- `Function` - 函数
- `Snippet` - 代码片段
