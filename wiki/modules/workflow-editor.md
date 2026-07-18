# workflow-editor 模块

## 模块概述

`workflow-editor` 是基于 React + TypeScript + ReactFlow 构建的前端工作流编辑器，用于可视化编辑数据处理流程/ETL工作流。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | React | 19.2.6 |
| 语言 | TypeScript | 6.0.2 |
| 构建工具 | Vite | 8.0.12 |
| 流程图库 | ReactFlow | 11.11.4 |
| Lint | ESLint | 10.3.0 |

## 文件结构

```
workflow-editor/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── eslint.config.js
├── src/
│   ├── main.tsx              # 入口
│   ├── App.tsx               # 根组件
│   ├── components/           # React组件
│   │   ├── FlowCanvas.tsx    # 流程画布
│   │   ├── NodePanel.tsx     # 节点面板
│   │   ├── PropertyPanel.tsx # 属性面板
│   │   └── Toolbar.tsx       # 工具栏
│   ├── nodes/                # 自定义节点
│   │   ├── SourceNode.tsx    # 数据源节点
│   │   ├── TransformNode.tsx # 转换节点
│   │   └── SinkNode.tsx      # 目标节点
│   ├── types/                # TypeScript类型定义
│   │   └── index.ts
│   └── utils/                # 工具函数
├── public/                   # 静态资源
└── styles/                   # 样式文件（CSS）
```

## 核心功能

### 1. 流程画布 (FlowCanvas)
- 基于 ReactFlow 的无限画布
- 支持缩放、平移
- 节点拖拽放置
- 连线绘制

### 2. 节点类型
- **数据源节点 (SourceNode)**: 代表数据来源（数据库表、文件等）
- **转换节点 (TransformNode)**: 数据转换操作（过滤、映射、聚合等）
- **目标节点 (SinkNode)**: 数据输出目标

### 3. 节点面板 (NodePanel)
- 展示可用节点类型列表
- 拖拽节点到画布

### 4. 属性面板 (PropertyPanel)
- 选中节点时显示节点属性
- 编辑节点配置参数

### 5. 工具栏 (Toolbar)
- 保存工作流
- 运行工作流
- 导出/导入
- 撤销/重做

## 开发指南

### 安装依赖

```bash
cd workflow-editor
npm install
```

### 开发模式

```bash
npm run dev
```

启动 Vite 开发服务器，支持热更新。

### 构建生产版本

```bash
npm run build
```

构建产物输出到 `blueprint/` 模块的资源目录。

### Lint 检查

```bash
npm run lint
```

## 与 Java 层通信

通过 `window.javaBridge` 对象与 JavaFX WebView 通信：

```typescript
// TypeScript 调用 Java 方法
declare global {
    interface Window {
        javaBridge?: {
            saveWorkflow(json: string): void;
            loadWorkflow(): string;
            onNodeSelect(nodeId: string): void;
        };
    }
}

// 保存工作流
window.javaBridge?.saveWorkflow(JSON.stringify(workflowData));
```

## 数据格式

工作流数据使用 JSON 格式：

```json
{
  "nodes": [
    {
      "id": "node-1",
      "type": "source",
      "position": { "x": 100, "y": 100 },
      "data": {
        "label": "MySQL数据源",
        "config": {
          "connectionId": "conn-001",
          "table": "users"
        }
      }
    }
  ],
  "edges": [
    {
      "id": "edge-1",
      "source": "node-1",
      "target": "node-2"
    }
  ]
}
```
