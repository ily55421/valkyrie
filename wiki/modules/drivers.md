# drivers 模块

## 模块概述

`drivers` 模块是数据库驱动抽象层，定义了统一的数据库访问接口，并为每种数据库提供具体实现。

## 包结构

```
drivers/src/main/java/valkyrie/driver/
├── api/                          # 核心API定义
│   ├── Driver.java               # 驱动抽象基类
│   ├── Dialect.java              # 数据库方言接口
│   ├── DbType.java               # 数据库类型枚举
│   ├── DriverFactory.java        # 驱动工厂
│   ├── Session.java              # 数据库会话
│   ├── ConnectionConfig.java     # 连接配置接口
│   ├── VkDataSource.java         # 数据源接口
│   ├── VkDataSourceFactory.java  # 数据源工厂接口
│   ├── PooledDataSource.java     # HikariCP连接池实现
│   ├── ConnectionProxy.java      # Connection代理
│   ├── StatementProxy.java       # Statement代理
│   ├── ProductMetaData.java      # 产品元数据
│   ├── model/                    # 数据模型
│   │   ├── Column.java
│   │   ├── Table.java
│   │   ├── Index.java
│   │   ├── DataGrid.java
│   │   ├── QueryResult.java
│   │   ├── GridRow.java
│   │   ├── Catalog.java
│   │   └── Sealable.java
│   ├── sql/                      # SQL执行相关
│   │   ├── SQL.java
│   │   ├── SQLParsedStatement.java
│   │   ├── SQLCommandType.java
│   │   ├── SQLExecutor.java
│   │   ├── SQLExecuteCallback.java
│   │   └── SQLExecuteHook.java
│   └── node/                     # 节点层次结构
│       ├── DBNode.java
│       ├── DBNodeKind.java
│       ├── DBNodePath.java
│       └── DBMetadataProvider.java
├── dialect/                      # 方言实现
│   ├── MySQLDialect.java
│   ├── PostgreSQLDialect.java
│   ├── SQLiteDialect.java
│   └── DmDialect.java
├── mysql/                        # MySQL驱动实现
│   └── MySQLDriver.java
├── postgresql/                   # PostgreSQL驱动实现
│   └── PostgreSQLDriver.java
├── sqlite/                       # SQLite驱动实现
│   └── SQLiteDriver.java
├── dm/                           # 达梦驱动实现
│   └── DmDriver.java
└── redis/                        # Redis驱动实现
    └── RedisDriver.java
```

## 核心 API

### Driver（抽象基类）

所有数据库驱动的基类，定义统一的数据库操作接口。

```java
public abstract class Driver implements SQLExecutor {
    
    // 基本信息
    public abstract DbType getType();
    public abstract Dialect createDialect();
    
    // 元数据查询
    public abstract List<Catalog> getCatalogs(Session session);
    public abstract List<String> getSchemas(Session session, String catalog);
    public abstract List<Table> getTables(Session session, String catalog, String schema);
    public abstract List<Column> getColumns(Session session, String catalog, String schema, String table);
    public abstract List<Index> getIndexes(Session session, String catalog, String schema, String table);
    
    // DDL 操作
    public abstract String showCreateTable(Session session, String table);
    public abstract void dropTable(Session session, String table);
    public abstract void addPrimaryKey(Session session, String table, List<String> columns);
    public abstract void dropPrimaryKey(Session session, String table);
    public abstract void alterChange(Session session, String table, Column column);
    public abstract void dropColumns(Session session, String table, List<String> columns);
    public abstract void alterIndexKeys(Session session, String table, Index index);
    public abstract void dropIndexKeys(Session session, String table, String indexName);
    
    // 自动补全
    public abstract List<String> getSuggestions(Session session, String prefix);
    
    // Hook 注册
    public void addHook(SQLExecuteHook hook);
    
    // 取消查询
    public void cancel(String jobId);
}
```

### Dialect（方言接口）

封装不同数据库的 SQL 语法差异。

```java
public interface Dialect {
    String limit(String sql, long offset, long size);  // 分页
    String normalize(String sql);                       // SQL规范化
    String quote(String identifier);                    // 标识符转义
    String removeQuote(String identifier);              // 去除引号
}
```

### DbType（数据库类型枚举）

```java
public enum DbType {
    MYSQL("mysql", "MySQL", "mysql.png", "com.mysql.cj.jdbc.Driver", true),
    POSTGRESQL("postgresql", "PostgreSQL", "postgresql.png", "org.postgresql.Driver", true),
    SQLITE("sqlite", "SQLite", "sqlite.png", "org.sqlite.JDBC", true),
    DM("dm", "达梦", "dm.png", "dm.jdbc.driver.DmDriver", true),
    REDIS("redis", "Redis", "redis.png", null, false);
    
    public abstract Driver createDriver(VkDataSource dataSource);
}
```

## 数据模型

### Column（列元数据）

继承 `Sealable`，支持完整性校验。

| 属性 | 类型 | 说明 |
|------|------|------|
| label | String | 显示标签 |
| name | String | 列名 |
| index | int | 列索引 |
| type | String | 数据类型 |
| notNull | boolean | 是否非空 |
| primary | boolean | 是否主键 |
| autoIncrement | boolean | 是否自增 |
| defaultValue | String | 默认值 |
| comment | String | 列注释 |
| originalName | String | 原始列名（用于修改检测） |

### Table（表元数据）

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 表名 |
| createTime | String | 创建时间 |
| updateTime | String | 更新时间 |
| engine | String | 存储引擎 |
| size | long | 表大小(KB) |
| rows | long | 行数 |
| comment | String | 表注释 |

### Index（索引元数据）

继承 `Sealable`。

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 索引名 |
| columnsText | String | 索引列文本 |
| type | String | 索引类型 |
| visible | boolean | 是否可见 |
| originalName | String | 原始索引名 |
| originalVisible | boolean | 原始可见状态 |

### DataGrid / QueryResult（数据网格）

查询结果集模型，支持可编辑结果集：

```java
public class DataGrid {
    List<Column> columns;           // 列定义
    List<GridRow> rows;             // 数据行
    List<String> pks;               // 主键列
    boolean editable;               // 是否可编辑
    boolean addable;                // 是否可新增
    
    // 操作方法
    public void reload(ResultSet rs);
    public void addEmptyRow();
    public void remove(int rowIndex);
    public void update(int row, int column, Object value);
    public List<String> generateDeleteSQL();
    public List<String> generateUpdateSQL();
}
```

## SQL 执行

### SQL 类

使用 JSqlParser 解析多条 SQL 语句：

```java
public class SQL implements Iterable<SQLParsedStatement> {
    public SQL(String sql);
    public List<SQLParsedStatement> getStatements();
    public String format();  // SQL格式化
}
```

### SQLParsedStatement

解析后的单条 SQL 语句：

| 属性 | 说明 |
|------|------|
| statement | 原始 Statement 对象 |
| commandType | SQL 命令类型（EXECUTE/EXECUTE_UPDATE/EXECUTE_QUERY） |
| tableName | 涉及的单表名（如能提取） |

### 代理机制

通过 `ConnectionProxy` 和 `StatementProxy` 实现：
- SQL 执行 Hook 注入
- SQL 日志记录
- 执行耗时统计
- 查询取消支持

```java
// StatementProxy 拦截示例
public ResultSet executeQuery(String sql) throws SQLException {
    hook.beforeExecute(sql);
    long start = System.currentTimeMillis();
    try {
        ResultSet rs = delegate.executeQuery(sql);
        hook.afterExecute(sql, System.currentTimeMillis() - start, null);
        return rs;
    } catch (SQLException e) {
        hook.afterExecute(sql, System.currentTimeMillis() - start, e);
        throw e;
    }
}
```

## 连接池配置

`PooledDataSource` 使用 HikariCP，默认配置：

| 参数 | 值 |
|------|-----|
| maximumPoolSize | 16 |
| minimumIdle | 1 |
| connectionTimeout | 30000ms |
| idleTimeout | 600000ms |
| maxLifetime | 1800000ms |

## 支持的数据库

| 数据库 | 驱动类 | Dialect | 元数据支持 |
|--------|--------|---------|-----------|
| MySQL | `com.mysql.cj.jdbc.Driver` | MySQLDialect | ✅ |
| PostgreSQL | `org.postgresql.Driver` | PostgreSQLDialect | ✅ |
| SQLite | `org.sqlite.JDBC` | SQLiteDialect | ✅ |
| 达梦 DM | `dm.jdbc.driver.DmDriver` | DmDialect | ✅ |
| Redis | Jedis 直连 | - | 部分支持 |
