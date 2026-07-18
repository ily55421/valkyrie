# launcher 模块

## 模块概述

`launcher` 模块是整个应用的启动入口模块，负责启动 JavaFX 应用程序。

## 文件结构

```
launcher/
├── pom.xml
└── src/main/java/valkyrie/
    └── Launcher.java    # 主入口类
```

## 核心类

### Launcher

**路径**: `launcher/src/main/java/valkyrie/Launcher.java`

应用主入口类，包含 `main()` 方法。

```java
public class Launcher {
    public static void main(String[] args) {
        Application.launch(valkyrie.app.Application.class, args);
    }
}
```

**职责**:
- 定义 JVM 入口点
- 委托给 `valkyrie.app.Application` 启动 JavaFX 应用

## Maven 配置

`launcher/pom.xml` 配置要点：

- 依赖 `app` 模块
- 使用 `maven-shade-plugin` 打包可执行 fat-jar
- Main-Class 设置为 `valkyrie.Launcher`

### Shade 插件配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>valkyrie.Launcher</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 构建产物

构建后生成可执行 JAR：
```
launcher/target/valkyrie-launcher-v1.0.0-arch.1.jar
```

运行方式：
```bash
java -jar valkyrie-launcher-v1.0.0-arch.1.jar
```
