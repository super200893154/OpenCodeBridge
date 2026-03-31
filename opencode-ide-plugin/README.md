# OpenCode IntelliJ 插件

一款将 IntelliJ IDEA 中的代码片段发送到 OpenCode TUI 对话框的插件。

## 功能特性

- ✅ 右键菜单快速发送选中的代码到 OpenCode
- ✅ 支持自定义 OpenCode 服务器地址和端口
- ✅ 支持服务器认证（用户名/密码）
- ✅ 可配置发送前确认
- ✅ 可配置自动提交
- ✅ 可配置是否包含文件上下文（文件名、语言等）
- ✅ 自定义提示模板
- ✅ 键盘快捷键支持（Ctrl+Shift+O）
- ✅ 进度显示和错误处理
- ✅ 工作空间目录检查（防止发送到错误的工作空间）
- ✅ 测试连接功能（快速验证服务器配置）
- ✅ 在 OpenCode TUI 中显示通知

## 安装

### 从源码构建

1. 克隆仓库：
```bash
git clone <repository-url>
cd opencode-ide-plugin
```

2. 构建插件：
```bash
./gradlew buildPlugin
```

3. 生成的插件位于 `build/distributions/opencode-ide-plugin-1.0.0.zip`

### 在 IntelliJ IDEA 中安装

1. 打开 IntelliJ IDEA
2. 进入 `Settings` → `Plugins`
3. 点击齿轮图标 → `Install Plugin from Disk...`
4. 选择构建好的 zip 文件
5. 重启 IDE

## 使用方法

### 1. 启动 OpenCode 服务器

在终端中启动 OpenCode 服务器：

```bash
# 基本启动（推荐）
opencode serve --port 4096

# 或者同时启动 TUI 界面和服务器
opencode --port 4096

# 带 Web 界面启动
opencode web --port 4096
```

**重要提示：**
- 请使用 `opencode serve` 而不是 `opencode server`（注意是 `serve` 不是 `server`）
- 必须显式指定 `--port 4096`，否则不会监听4096端口
- 启动后可以使用 `netstat -ano | findstr :4096`（Windows）或 `lsof -i :4096`（macOS/Linux）验证端口是否在监听
- 如需配置用户名和密码，请在插件设置中填写，OpenCode 服务器本身不需要额外配置

### 2. 配置插件

1. 打开 `Settings` → `Tools` → `OpenCode Integration`
2. 配置以下参数：
   - **服务器地址**：OpenCode 服务器的主机地址（默认：localhost）
   - **端口**：OpenCode 服务器的端口（默认：4096）
   - **用户名**：认证用户名（如果需要）
   - **密码**：认证密码（如果需要）
   - **自动提交**：发送后自动提交提示
   - **发送前确认**：发送前显示确认对话框
   - **包含文件上下文**：在提示中包含文件名和语言信息
   - **检查工作空间目录**：发送前检查 IDE 项目目录是否在 OpenCode 工作空间目录下
   - **自定义模板**：自定义发送内容的模板
   - **测试连接**：测试与 OpenCode 服务器的连接

### 3. 发送代码到 OpenCode

有两种方式：

#### 方法 1：右键菜单
1. 在编辑器中选中要发送的代码
2. 右键点击 → 选择 `发送到 OpenCode`
3. 确认发送（如果启用了确认选项）

#### 方法 2：键盘快捷键
1. 在编辑器中选中要发送的代码
2. 按 `Ctrl+Shift+O`

## 配置说明

### 提示模板

提示模板支持以下变量：
- `{code}` - 选中的代码内容
- `{language}` - 文件的编程语言
- `{filename}` - 文件名
- `{filepath}` - 文件完整路径

默认模板：
```
请分析这段 {language} 代码（来自 {filename}）：
```{language}
{code}
```
```

### 示例模板

**代码审查模板：**
```
请审查这段 {language} 代码（来自 {filename}）：
```{language}
{code}
```
指出可能的问题和改进建议。
```

**代码解释模板：**
```
请解释这段 {language} 代码的功能（来自 {filename}）：
```{language}
{code}
```
```

**代码优化模板：**
```
请优化这段 {language} 代码（来自 {filename}）：
```{language}
{code}
```
提供性能优化建议。
```

## API 参考

插件使用 OpenCode 的 HTTP API：

- `POST /tui/append-prompt` - 追加文本到提示框
- `POST /tui/submit-prompt` - 提交当前提示
- `POST /tui/clear-prompt` - 清空提示
- `POST /tui/show-toast` - 显示通知
- `GET /global/health` - 健康检查
- `GET /path` - 获取当前工作空间目录

完整的 API 文档请访问 OpenCode 官方文档：
https://opencode.ai/docs/zh-cn/server/

## 开发

### 项目结构

```
opencode-ide-plugin/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/opencode/integration/
│       │       ├── OpenCodePlugin.kt          # 插件主类
│       │       ├── actions/
│       │       │   ├── SendToOpenCodeAction.kt  # 发送代码 Action
│       │       │   └── OpenCodeSettingsAction.kt # 设置 Action
│       │       ├── client/
│       │       │   └── OpenCodeClient.kt       # OpenCode API 客户端
│       │       ├── icons/
│       │       │   └── OpenCodeIcons.kt        # 图标定义
│       │       └── settings/
│       │           ├── OpenCodeSettings.kt     # 配置数据类
│       │           └── OpenCodeConfigurable.kt # 配置面板
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml                  # 插件配置文件
│           └── icons/
│               └── opencode.svg                # 插件图标
├── build.gradle.kts                             # 构建配置
├── settings.gradle.kts                          # Gradle 设置
└── README.md                                    # 项目文档
```

### 构建命令

```bash
# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test

# 在开发环境中运行插件
./gradlew runIde
```

## 故障排除

### 无法连接到 OpenCode 服务器

1. 确认 OpenCode 服务器已正确启动：
   ```bash
   # PowerShell (Windows)
   opencode serve --port 4096

   # 验证端口是否监听
   netstat -ano | findstr :4096
   ```

2. 确认使用了正确的命令：
   - ✅ 正确：`opencode serve --port 4096`
   - ❌ 错误：`opencode server --port 4096`（注意是 `serve` 不是 `server`）

3. 检查服务器地址和端口配置是否正确（默认：localhost:4096）

4. 确认防火墙没有阻止连接

5. 查看插件通知中的错误信息

6. 如果提示 "Failed to connect"，确认：
   - 服务器进程正在运行
   - 端口4096没有被其他程序占用
   - 使用 `opencode --help` 查看帮助信息

### 发送失败

1. 检查网络连接
2. 确认 OpenCode 服务器正常运行
3. 查看插件通知中的详细错误信息
4. 如果配置了认证，确认用户名和密码正确

### 插件未显示在右键菜单

1. 确认已选中代码文本
2. 重启 IDE
3. 检查插件是否已启用：`Settings` → `Plugins`

### 工作空间目录警告

如果在发送代码时看到"工作空间目录不匹配"的警告：

1. 确认 IDE 项目目录与 OpenCode 工作空间目录一致
2. 如果确实需要发送到不同的工作空间，可以禁用"检查工作空间目录"选项
3. 建议保持此选项启用，以避免上下文混乱

## 依赖项

- IntelliJ Platform SDK
- Kotlin 1.9.20
- OkHttp 4.12.0
- Gson 2.10.1

## 兼容性

- IntelliJ IDEA 2023.2+
- 其他 JetBrains IDE（基于 IntelliJ 平台）

## 许可证

[待定]

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- OpenCode 官网：https://opencode.ai
- 文档：https://opencode.ai/docs/zh-cn/

## 更新日志

### 1.0.0 (2024-XX-XX)

- 初始版本发布
- 支持发送选中代码到 OpenCode
- 支持配置服务器地址和端口
- 支持认证
- 支持自定义提示模板
- 支持自动提交
- 支持发送前确认
