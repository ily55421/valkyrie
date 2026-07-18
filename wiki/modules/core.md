# core 模块

## 模块概述

`core` 模块是应用的核心层，负责数据持久化、用户配置管理、核心数据模型定义。该模块不依赖任何 UI 组件。

## 文件结构

```
core/src/main/java/valkyrie/core/
├── Users.java                          # 用户数据目录管理
├── model/
│   ├── ConnectionProfile.java          # 连接配置模型
│   ├── DiskSavedConnection.java        # 磁盘持久化连接信息
│   ├── QueryFile.java                  # 查询脚本文件模型
│   └── ScriptFile.java                 # 脚本文件模型
├── repository/
│   ├── ConnectionRepository.java       # 连接信息 CRUD
│   ├── QueryFileRepository.java        # 查询脚本仓库
│   └── ScriptFileRepository.java       # 脚本文件仓库
├── utils/
│   ├── JSONUtils.java                  # JSON 序列化工具
│   └── FileUtils.java                  # 文件工具
└── exception/
    └── CoreException.java              # 核心异常
```

## 核心类

### Users

用户数据目录管理类，负责管理应用数据存储路径。

**数据存储位置**: `~/.valkyries/C/`

```java
public class Users {
    public static Path getConfigDir();       // 获取配置目录
    public static Path getConnectionsFile(); // 获取连接配置文件路径
    public static Path getQueryDir();        // 获取查询脚本目录
    public static Path getScriptDir();       // 获取脚本目录
}
```

### ConnectionProfile

连接配置模型，存储数据库连接的所有参数。

```java
@Data
public class ConnectionProfile {
    private String id;              // 唯一ID
    private String name;            // 连接名称
    private DbType type;            // 数据库类型
    private String host;            // 主机地址
    private int port;               // 端口
    private String database;        // 数据库名
    private String username;        // 用户名
    private String password;        // 密码
    private String jdbcUrl;         // 自定义JDBC URL（可选）
    private Map<String, String> properties; // 额外连接属性
}
```

### DiskSavedConnection

磁盘持久化的连接信息，与 ConnectionProfile 类似但用于 JSON 序列化。

### ConnectionRepository

连接信息仓库，提供 CRUD 操作，数据以 JSON 格式存储在文件中。

```java
public class ConnectionRepository {
    public List<ConnectionProfile> findAll();           // 查询所有连接
    public Optional<ConnectionProfile> findById(String id); // 根据ID查询
    public void save(ConnectionProfile profile);        // 保存连接（新增或更新）
    public void deleteById(String id);                  // 删除连接
}
```

**存储格式**: `~/.valkyries/C/connections.json`

### QueryFileRepository / ScriptFileRepository

查询脚本和脚本文件仓库，管理保存的 SQL 脚本文件。

### JSONUtils

JSON 序列化工具类，基于 Jackson 封装。

```java
public class JSONUtils {
    private static final ObjectMapper MAPPER;
    
    public static String toJson(Object obj);                    // 对象转JSON
    public static <T> T fromJson(String json, Class<T> clazz); // JSON转对象
    public static <T> T fromJson(Path path, Class<T> clazz);   // 从文件读取
    public static void toJson(Path path, Object obj);          // 写入文件
}
```

### CoreException

核心模块异常类，用于包装业务异常：

```java
public class CoreException extends RuntimeException {
    public CoreException(String message);
    public CoreException(String message, Throwable cause);
}
```

## 数据存储结构

用户目录结构：

```
~/.valkyries/
└── C/
    ├── connections.json       # 连接配置列表
    ├── queries/               # 查询脚本目录
    │   ├── query-001.json
    │   └── query-002.json
    └── scripts/               # 脚本目录
        └── script-001.json
```

### connections.json 格式示例

```json
[
  {
    "id": "conn-001",
    "name": "本地MySQL",
    "type": "MYSQL",
    "host": "localhost",
    "port": 3306,
    "database": "test",
    "username": "root",
    "password": "******",
    "jdbcUrl": null,
    "properties": {}
  }
]
```

## 依赖关系

core 模块依赖：
- `drivers` 模块（DbType 等）
- `utils` 模块（工具类）
- Jackson（JSON 序列化）
- Lombok
