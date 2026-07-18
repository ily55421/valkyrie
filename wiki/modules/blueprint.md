# blueprint 模块

## 模块概述

`blueprint` 模块是工作流编辑器的 JavaFX 封装层，负责将前端 React 工作流编辑器（workflow-editor）嵌入到 JavaFX WebView 中，并提供 Java 与 JavaScript 的双向通信桥接。

## 模块定位

```
┌─────────────────────────────────────────┐
│              app (UI层)                  │
│  使用 BlueprintFX 组件展示工作流编辑器    │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│           blueprint (封装层)             │
│  - JavaFX WebView 容器                   │
│  - Java ↔ JS 桥接                        │
│  - 工作流数据模型                        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      workflow-editor (前端React)         │
│  - ReactFlow 流程图                      │
│  - 节点/连线编辑                         │
│  - 属性面板                              │
└─────────────────────────────────────────┘
```

## 主要功能

1. **WebView 容器**：提供 JavaFX WebView 组件加载前端工作流编辑器
2. **双向通信**：实现 Java 与 JavaScript 的方法互调
3. **数据模型**：定义工作流、节点、连线的 Java 模型
4. **事件处理**：将前端事件（节点点击、保存等）传递到 Java 层

## 与 workflow-editor 的关系

- `workflow-editor/` 是独立的 React + TypeScript 前端项目
- 前端构建产物（HTML/JS/CSS）打包到 `blueprint/` 模块的 resources 中
- `blueprint/` 负责在 JavaFX 中加载这些资源并提供桥接

## 构建流程

1. 在 `workflow-editor/` 目录执行 `npm run build`
2. 构建产物输出到 `blueprint/src/main/resources/blueprint/`
3. Maven 构建 `blueprint` 模块时将资源打包进 JAR
