# Valkyrie 本地脚本

## 脚本说明

### `build.ps1` - 一键编译打包

```powershell
# 编译打包（跳过测试）
.\local\build.ps1

# 编译打包并启动
.\local\build.ps1 -Run

# 编译打包（含测试）
.\local\build.ps1 -SkipTests:$false
```

### `run.ps1` - 一键启动

```powershell
# 启动应用（jar 不存在时自动编译）
.\local\run.ps1
```

## 环境要求

- JDK 21（路径：`D:\dev\Java\jdk-21.0.10_windows-x64_bin\jdk-21.0.10`）
- Maven 3.8+

## 产物位置

- 可执行 jar：`launcher/target/launcher-v1.0.0-arch.1.jar`（约 95 MB）
