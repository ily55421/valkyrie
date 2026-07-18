# VALKYRIE DB Wiki 知识库

欢迎使用 VALKYRIE DB 知识库！VALKYRIE DB 是一款基于 JavaFX 的跨平台多数据库管理桌面客户端工具。

## 快速导航

### 项目概述
- [项目简介](./01-项目简介.md) - 项目定位、核心特性、技术栈
- [架构设计](./02-架构设计.md) - 模块结构、依赖关系、设计理念

### 开发指南
- [环境搭建](./03-环境搭建.md) - 开发环境配置、构建运行
- [代码规范](./04-代码规范.md) - 提交规范、编码约定
- [模块开发指南](./05-模块开发指南.md) - 各模块开发说明

### 核心模块
- [launcher 启动模块](./modules/launcher.md) - 应用入口
- [app UI模块](./modules/app.md) - JavaFX界面层
- [core 核心模块](./modules/core.md) - 数据持久化与配置
- [drivers 驱动模块](./modules/drivers.md) - 数据库驱动抽象层
- [utils 工具模块](./modules/utils.md) - 通用工具库
- [monacofx 编辑器模块](./modules/monacofx.md) - Monaco编辑器封装
- [richtextfx 富文本模块](./modules/richtextfx.md) - 富文本组件
- [blueprint 蓝图模块](./modules/blueprint.md) - 工作流封装
- [workflow-editor 工作流编辑器](./modules/workflow-editor.md) - 前端工作流编辑器

### 功能文档
- [数据库连接管理](./features/connection.md)
- [SQL编辑器](./features/sql-editor.md)
- [数据浏览与编辑](./features/data-grid.md)
- [表结构设计](./features/table-designer.md)
- [数据导入导出](./features/import-export.md)

### 版本历史
- [v1.6.0 更新日志](./changelog/v1.6.0.md)
- [v1.0.0-arch.1 更新日志](./changelog/v1.0.0-arch.1.md)

## 支持的数据库

| 数据库 | 版本要求 | 支持状态 |
|--------|----------|----------|
| MySQL | 8.x | ✅ 完整支持 |
| PostgreSQL | 42.x | ✅ 完整支持 |
| SQLite | 3.x | ✅ 完整支持 |
| 达梦数据库 (DM) | 8.x | ✅ 完整支持 |
| Redis | 7.x | ✅ 基础支持 |

## 技术栈一览

- **语言**: Java 21 + TypeScript 6
- **UI框架**: JavaFX 21 (OpenJFX) + AtlantaFX 主题
- **前端框架**: React 19 + Vite 8 + ReactFlow 11
- **构建工具**: Maven 3.9.x
- **连接池**: HikariCP 5.1
- **SQL解析**: JSqlParser 5.3
- **编辑器**: Monaco Editor
