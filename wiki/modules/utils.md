# utils 模块

## 模块概述

`utils` 模块是通用工具类库，提供项目中各模块共用的工具方法，不依赖其他业务模块。

## 主要工具类

### 字符串工具

| 类 | 功能 |
|----|------|
| `StringUtils` | 字符串判空、拼接、转换等 |
| `SqlFormatter` | SQL 格式化（基于 vertical-blank/sql-formatter） |
| `CaseConverter` | 命名风格转换（驼峰/下划线/短横线） |

### 集合工具

| 类 | 功能 |
|----|------|
| `CollectionUtils` | 集合判空、转换、分组等 |
| `ListUtils` | 列表操作 |
| `MapUtils` | Map 操作 |

### 文件/IO 工具

| 类 | 功能 |
|----|------|
| `FileUtils` | 文件读写、复制、删除 |
| `IOUtils` | 流操作 |
| `PathUtils` | 路径处理 |

### 加密/编码工具

| 类 | 功能 |
|----|------|
| `CryptoUtils` | AES/DES 加解密 |
| `HashUtils` | MD5/SHA 哈希 |
| `Base64Utils` | Base64 编码/解码 |

### 日期/时间工具

| 类 | 功能 |
|----|------|
| `DateUtils` | 日期格式化、解析、计算 |
| `StopWatch` | 计时器（用于性能统计） |

### 系统/平台工具

| 类 | 功能 |
|----|------|
| `OSUtils` | 操作系统检测（Windows/macOS/Linux） |
| `DesktopUtils` | 桌面操作（打开文件、打开目录、打开浏览器） |
| `ClipboardUtils` | 剪贴板操作 |

### 功能工具

| 类 | 功能 |
|----|------|
| `ExcelUtils` | Excel 读写（基于 Apache POI） |
| `JsonUtils` | JSON 处理（基于 Jackson/Fastjson） |
| `AssertUtils` | 断言工具 |
| `Validator` | 数据验证 |
| `IdGenerator` | ID 生成器（UUID、雪花ID等） |

## 关键工具类示例

### OSUtils

```java
public class OSUtils {
    public static boolean isWindows();
    public static boolean isMac();
    public static boolean isLinux();
    public static String getOSName();
}
```

### DesktopUtils

```java
public class DesktopUtils {
    public static void openFile(Path file);
    public static void openDirectory(Path dir);
    public static void openInBrowser(String url);
}
```

### StopWatch

```java
public class StopWatch {
    public void start();
    public void stop();
    public long getElapsedMillis();
    public String getElapsedFormatted();  // 格式化为 "123ms" 或 "1.2s"
}
```

## 依赖

utils 模块依赖：
- Apache Commons Lang（可能）
- Apache POI（Excel处理）
- Jackson / Fastjson（JSON处理）
- SLF4J（日志）
- Lombok
