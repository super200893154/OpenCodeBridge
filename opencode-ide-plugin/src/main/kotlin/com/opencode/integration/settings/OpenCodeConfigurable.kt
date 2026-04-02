package com.opencode.integration.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.opencode.integration.client.OpenCodeClient
import javax.swing.JComponent

/**
 * OpenCode 配置面板
 */
class OpenCodeConfigurable : Configurable {

    private lateinit var panel: DialogPanel
    private val settings = OpenCodeSettings.getInstance()

    private val serverHostField = JBTextField(settings.serverHost)
    private val serverPortField = JBTextField(settings.serverPort.toString())
    private val serverUsernameField = JBTextField(settings.serverUsername)
    private val serverPasswordField = JBPasswordField()
    private val autoSubmitCheckBox = JBCheckBox("自动提交提示", settings.autoSubmit)
    private val showConfirmationCheckBox = JBCheckBox("发送前确认", settings.showConfirmation)
    private val includeFileContextCheckBox = JBCheckBox("包含文件上下文", settings.includeFileContext)
    private val checkWorkspaceDirCheckBox = JBCheckBox("检查工作空间目录", settings.checkWorkspaceDir)

    override fun getDisplayName(): String = "OpenCode Integration"

    override fun createComponent(): JComponent {
        panel = panel {
            group("服务器设置") {
                row("主机地址：") {
                    cell(serverHostField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("OpenCode 服务器的主机地址")
                }
                row("端口：") {
                    cell(serverPortField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("OpenCode 服务器的端口号")
                }
                row("用户名：") {
                    cell(serverUsernameField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("服务器认证用户名（如果需要）")
                }
                row("密码：") {
                    cell(serverPasswordField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("服务器认证密码（如果需要）")
                }
            }

            group("行为设置") {
                row {
                    cell(autoSubmitCheckBox)
                        .comment("发送后自动提交提示")
                }
                row {
                    cell(showConfirmationCheckBox)
                        .comment("发送前显示确认对话框")
                }
                row {
                    cell(includeFileContextCheckBox)
                        .comment("在提示中包含文件名和语言信息")
                }
                row {
                    cell(checkWorkspaceDirCheckBox)
                        .comment("发送前检查 IDE 项目目录是否在 OpenCode 工作空间目录下")
                }
            }

            group("提示模板") {
                row("可用模板：") {
                    comment("发送对话框中提供以下预定义模板：")
                }
                row {
                    comment("• 自定义提示... - 用户自定义提示内容")
                }
                row {
                    comment("• 代码解释 - 解释代码功能和实现逻辑")
                }
                row {
                    comment("• 修复问题 - 找出代码问题并提供修复方案")
                }
                row {
                    comment("• 重构代码 - 重构代码提高质量和可读性")
                }
                row {
                    comment("• 性能优化 - 优化代码性能和效率")
                }
                row {
                    comment("• 代码审查 - 代码审查并指出潜在问题")
                }
                row {
                    comment("• 生成测试 - 为代码编写单元测试")
                }
                row("") {
                    comment("可用变量：{code}, {language}, {filename}, {filepath}, {start_line}, {end_line}")
                }
            }

            group("测试连接") {
                row {
                    button("测试连接") {
                        testConnection()
                    }
                }
            }
        }

        return panel
    }

    override fun isModified(): Boolean {
        return serverHostField.text != settings.serverHost ||
                serverPortField.text != settings.serverPort.toString() ||
                serverUsernameField.text != settings.serverUsername ||
                String(serverPasswordField.password) != settings.serverPassword ||
                autoSubmitCheckBox.isSelected != settings.autoSubmit ||
                showConfirmationCheckBox.isSelected != settings.showConfirmation ||
                includeFileContextCheckBox.isSelected != settings.includeFileContext ||
                checkWorkspaceDirCheckBox.isSelected != settings.checkWorkspaceDir
    }

    override fun apply() {
        settings.updateServerHost(serverHostField.text.trim())
        settings.updateServerPort(serverPortField.text.trim().toIntOrNull() ?: 4096)
        settings.updateServerUsername(serverUsernameField.text.trim())
        settings.updateServerPassword(String(serverPasswordField.password))
        settings.updateAutoSubmit(autoSubmitCheckBox.isSelected)
        settings.updateShowConfirmation(showConfirmationCheckBox.isSelected)
        settings.updateIncludeFileContext(includeFileContextCheckBox.isSelected)
        settings.updateCheckWorkspaceDir(checkWorkspaceDirCheckBox.isSelected)
    }

    override fun reset() {
        serverHostField.text = settings.serverHost
        serverPortField.text = settings.serverPort.toString()
        serverUsernameField.text = settings.serverUsername
        serverPasswordField.text = settings.serverPassword
        autoSubmitCheckBox.isSelected = settings.autoSubmit
        showConfirmationCheckBox.isSelected = settings.showConfirmation
        includeFileContextCheckBox.isSelected = settings.includeFileContext
        checkWorkspaceDirCheckBox.isSelected = settings.checkWorkspaceDir
    }

    private fun testConnection() {
        apply()

        // 使用 Task.Backgroundable 在后台执行测试,但确保弹窗在主线程立即显示
        object : com.intellij.openapi.progress.Task.Backgroundable(
            null,
            "正在测试连接...",
            true,
            com.intellij.openapi.progress.PerformInBackgroundOption.ALWAYS_BACKGROUND
        ) {
            private var result: Result<Boolean>? = null

            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                val client = OpenCodeClient()
                result = client.checkHealth()
            }

            override fun onSuccess() {
                // 在EDT线程显示结果
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    result?.let { testResult ->
                        testResult.fold(
                            onSuccess = { isHealthy ->
                                if (isHealthy) {
                                    com.intellij.openapi.ui.Messages.showMessageDialog(
                                        "连接成功!\n服务器: ${settings.getServerUrl()}",
                                        "测试连接",
                                        com.intellij.openapi.ui.Messages.getInformationIcon()
                                    )
                                } else {
                                    com.intellij.openapi.ui.Messages.showMessageDialog(
                                        "服务器响应异常\n服务器: ${settings.getServerUrl()}",
                                        "测试连接失败",
                                        com.intellij.openapi.ui.Messages.getWarningIcon()
                                    )
                                }
                            },
                            onFailure = { error ->
                                com.intellij.openapi.ui.Messages.showMessageDialog(
                                    "连接失败\n原因: ${error.message}\n服务器: ${settings.getServerUrl()}",
                                    "测试连接失败",
                                    com.intellij.openapi.ui.Messages.getErrorIcon()
                                )
                            }
                        )
                    }
                }
            }
        }.queue()
    }
}
