# richtextfx 模块

## 模块概述

`richtextfx` 模块是一个内嵌的富文本编辑组件库，基于开源的 RichTextFX 项目定制，提供支持样式化文本的 JavaFX 区域控件。

## 文件结构

```
richtextfx/
├── pom.xml
└── src/main/java/org/fxmisc/richtext/
    ├── (114 个 Java 文件)
    ├── css/                    # 样式文件（6个CSS）
    └── resources/
        └── fxml/               # FXML文件
```

## 主要功能

RichTextFX 提供了一个支持富文本编辑的 JavaFX 控件：

- **StyledTextArea**: 可样式化文本区域基类
- **InlineCssTextArea**: 使用内联CSS样式的文本区域
- **StyleClassedTextArea**: 使用样式类的文本区域
- **CodeArea**: 代码编辑器区域（支持行号、语法高亮）

## 核心特性

1. **多样式支持**：可以为任意文本范围设置不同样式
2. **虚拟滚动**：高效处理大文档
3. **撤销/重做**：内置撤销管理器
4. **键盘导航**：完整的键盘导航支持
5. **选择操作**：丰富的文本选择API
6. **剪贴板操作**：复制/剪切/粘贴

## 在项目中的用途

本项目中 richtextfx 主要用于：
- 执行日志显示（`ExecuteLoggerPane`）
- 支持高亮显示不同级别的日志信息
- 支持错误信息的特殊样式显示

## 关键类

| 类 | 功能 |
|----|------|
| `StyledTextArea` | 通用样式化文本区域 |
| `CodeArea` | 代码编辑区域，带行号 |
| `StyleSpans` | 样式范围集合 |
| `UndoManager` | 撤销/重做管理器 |
| `VirtualFlow` | 虚拟滚动容器 |

## 依赖

richtextfx 是相对独立的模块，主要依赖：
- JavaFX Controls
- ReactFX（事件流处理）
- Flowless（虚拟滚动）
