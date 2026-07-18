# app 模块

## 模块概述

`app` 模块是应用的 UI 层，基于 JavaFX 实现，包含所有界面组件、事件处理、用户交互逻辑。

## 包结构

```
app/src/main/java/valkyrie/app/
├── Application.java        # JavaFX Application 主类
├── layout/                 # 布局组件
│   ├── MainLayout.java
│   └── ContainerLayout.java
├── workbench/              # 工作台
│   ├── Workbench.java
│   ├── QueryEditor.java
│   └── ScriptEditor.java
├── explorer/               # 导航树（对象浏览器）
│   ├── ObjectExplorerPane.java
│   └── node/
│       ├── UIConnectionNode.java
│       ├── UICatalogNode.java
│       ├── UITableNode.java
│       └── UIQueryDynamicNode.java
├── pane/                   # 功能面板
│   ├── QueryResultDataPane.java
│   ├── TableDataPane.java
│   ├── TableDesignerPane.java
│   ├── TableOverviewPane.java
│   ├── DataGridViewPane.java
│   └── ExecuteLoggerPane.java
├── dialog/                 # 对话框
│   ├── CreateOrEditConnectionDialog.java
│   ├── SaveScriptDialog.java
│   ├── ConnectionGeneralPane.java
│   └── ConnectionAdvancedPane.java
├── widgets/                # 自定义UI控件
│   ├── VkTabPane.java
│   ├── VkTableView.java
│   ├── VkComboBox.java
│   ├── VkContextMenu.java
│   ├── VkStatusBar.java
│   └── VkDialogHelper.java
├── event/bus/              # 事件总线
│   ├── EventBus.java
│   ├── Event.java
│   └── EventListener.java
├── menu/                   # 菜单栏
│   ├── AppMenuBar.java
│   └── ConnectionMenuBuilder.java
└── model/                  # UI状态模型
    ├── ConnectionPropertyModel.java
    └── UIExplorerStatus.java
```

## 核心类

### Application

**路径**: `app/src/main/java/valkyrie/app/Application.java`

JavaFX 应用主类，继承 `javafx.application.Application`。

**职责**:
- 设置主题（AtlantaFX CupertinoLight）
- 加载全局 CSS 样式
- 创建主窗口和 MainLayout
- 设置 Dock 图标（macOS）
- 窗口标题：`VALKYRIE v1.6.0`

### MainLayout

主布局类，组织整体界面结构：

```
┌─────────────────────────────────────────────┐
│  MenuBar (AppMenuBar)                        │
├─────────────────────────────────────────────┤
│  ToolBar                                     │
├──────────┬──────────────────────────────────┤
│          │                                  │
│ Object   │  Workbench (TabPane)             │
│ Explorer │                                  │
│ (Tree)   │                                  │
│          │                                  │
├──────────┴──────────────────────────────────┤
│  StatusBar (VkStatusBar)                     │
└─────────────────────────────────────────────┘
```

### EventBus

线程安全的事件总线，实现发布-订阅模式：

```java
public class EventBus {
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventListener>> listeners;
    
    public void publish(Event event);
    public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener);
    public <T extends Event> void unsubscribe(Class<T> type, EventListener<T> listener);
}
```

### Workbench

工作台管理类，负责标签页的创建、切换、关闭：
- 管理 QueryEditor、TableDataPane、TableDesignerPane 等标签页
- 支持标签页拖动排序
- 支持标签页关闭联动

### QueryEditor

SQL 查询编辑器，基于 MonacoFX 封装：
- SQL 语法高亮
- 自动补全
- 查询执行/取消
- 结果展示
- 脚本保存

### ObjectExplorerPane

左侧导航树面板，展示数据库对象层级：
- 连接节点 → Catalog/Schema → 表 → 列
- 查询脚本节点
- 右键菜单（新建连接、刷新、删除等）
- 双击打开表/查询

## 自定义控件 (widgets/)

所有自定义控件统一使用 `Vk` 前缀：

| 控件 | 基类 | 功能 |
|------|------|------|
| VkTabPane | TabPane | 自定义标签页面板 |
| VkTableView | TableView | 自定义数据表格 |
| VkComboBox | ComboBox | 自定义下拉框 |
| VkContextMenu | ContextMenu | 带动画的右键菜单 |
| VkStatusBar | HBox | 全局状态栏 |
| VkDialogHelper | - | 对话框工具类 |

## 异步执行

所有 SQL 操作在独立线程池执行，防止阻塞 UI：

```java
private final ExecutorService sqlExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "sql-executor");
    t.setDaemon(true);
    return t;
});
```

执行流程：
1. 用户触发 SQL 执行
2. 任务提交到 sqlExecutor
3. SQL 执行完成后通过 `Platform.runLater()` 更新 UI

## CSS 样式

应用使用 AtlantaFX CupertinoLight 主题，并添加自定义样式：
- 自定义控件样式统一管理
- 表格奇偶行背景色
- 树节点选中样式
- 右键菜单圆角、模糊效果
