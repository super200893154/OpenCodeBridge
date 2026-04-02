# AGENTS.md — OpenCode IntelliJ Plugin 开发指南

## 项目概述

IntelliJ IDEA 插件，将 IDE 中选中的代码片段发送到 OpenCode TUI 对话框。

- **语言：** Kotlin 1.9.20，目标 JVM 17
- **构建系统：** Gradle 8.5（Kotlin DSL）
- **插件框架：** IntelliJ Platform SDK（`org.jetbrains.intellij` v1.17.0）
- **目标 IDE：** IntelliJ IDEA 2023.2+（build 232 至 261.*）
- **依赖：** OkHttp 4.12.0、Gson 2.10.1

## 目录结构

```
opencode-ide-plugin/
├── build.gradle.kts              # Gradle 构建配置
├── settings.gradle.kts           # Gradle 设置
├── gradle.properties             # JVM 参数
└── src/main/
    ├── kotlin/com/opencode/integration/
    │   ├── OpenCodePlugin.kt           # 插件主类、常量定义
    │   ├── actions/
    │   │   ├── SendToOpenCodeAction.kt # 发送代码 Action
    │   │   └── OpenCodeSettingsAction.kt
    │   ├── client/
    │   │   └── OpenCodeClient.kt       # HTTP API 客户端
    │   ├── icons/
    │   │   └── OpenCodeIcons.kt        # 图标加载
    │   └── settings/
    │       ├── OpenCodeSettings.kt     # 持久化配置
    │       └── OpenCodeConfigurable.kt # 设置 UI 面板
    └── resources/
        ├── META-INF/plugin.xml         # 插件描述
        └── icons/opencode.svg
```

## 构建/测试命令

所有命令在 `opencode-ide-plugin/` 目录下执行：

| 命令 | 说明 |
|------|------|
| `./gradlew buildPlugin` | 构建插件，输出 `build/distributions/*.zip` |
| `./gradlew runIde` | 在沙箱 IDE 中运行插件（开发调试） |
| `./gradlew test` | 运行测试（当前无测试代码） |
| `./gradlew build` | 标准构建 |
| `./gradlew clean` | 清理构建输出 |

**运行单个测试：** `./gradlew test --tests "com.opencode.integration.YourTestClass.testName"`

**JVM 参数：** 最大堆内存 2GB（`-Xmx2048M`），Gradle 使用腾讯云镜像加速。

## 代码风格约定

### 命名约定

| 类型 | 风格 | 示例 |
|------|------|------|
| 包名 | 反向域名，全小写 | `com.opencode.integration.actions` |
| 类名 | PascalCase | `SendToOpenCodeAction` |
| 函数/变量 | camelCase | `appendPrompt`、`serverHost` |
| 常量 | UPPER_SNAKE_CASE | `PLUGIN_ID`、`JSON_MEDIA_TYPE` |
| 私有成员 | camelCase，无下划线前缀 | `client`、`settings` |

### 导入约定

- 按字母顺序排列（IDE 默认行为）
- 不使用通配符导入（OkHttp DSL 的 `import okhttp3.*` 是例外）
- 使用 `Companion.toXxx` 扩展导入，如 `okhttp3.MediaType.Companion.toMediaType`

### Kotlin 惯用法

- 使用 `data class` 定义数据状态
- 使用 `Result<T>` 进行错误处理，而非抛出异常
- 使用扩展函数封装复用逻辑（如 `Request.Builder.addHeaders()`）
- 使用 `companion object` 定义静态常量和工厂方法
- 使用 `lateinit var` 延迟初始化
- 使用字符串模板：`"${settings.getServerUrl()}/tui/append-prompt"`
- 使用 Elvis 运算符 `?:` 提供默认值
- 使用 `isNullOrBlank()` 进行空值检查
- 使用 `let`、`fold` 等 Kotlin 标准库函数

### 代码组织

- 每个文件一个主类
- 类成员顺序：属性 → 构造函数/init → 公开方法 → 私有方法 → companion object
- 使用 `override fun` 明确标记重写方法
- 子包按功能划分：`actions`、`client`、`icons`、`settings`

### 注释风格

- KDoc：`/** ... */` 用于类和方法文档
- 行内注释：`//` 用于代码说明
- 注释语言以中文为主

## 错误处理

- API 客户端统一返回 `Result<T>`，调用方通过 `isFailure`/`getOrNull()` 处理
- 网络错误（`IOException`）在客户端层捕获，不向上抛出
- Action 层根据异常类型提供友好的用户提示（连接失败、超时等）
- 使用 `ProgressManager` + `Task.Backgroundable` 执行后台任务，避免阻塞 UI

## 注意事项

- 当前项目**无测试代码**（`src/test/` 不存在），新增功能应补充测试
- 当前项目**无 linting/格式化配置**（无 ktlint、detekt、editorconfig）
- 无 CI/CD 配置
- 插件签名和发布通过环境变量控制（`CERTIFICATE_CHAIN`、`PRIVATE_KEY`、`PUBLISH_TOKEN`）
