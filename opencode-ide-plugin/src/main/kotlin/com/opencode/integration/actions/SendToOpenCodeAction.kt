package com.opencode.integration.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.opencode.integration.OpenCodePlugin
import com.opencode.integration.client.OpenCodeClient
import com.opencode.integration.settings.OpenCodeSettings
import com.opencode.integration.settings.TemplateType

/**
 * 发送选中的代码到 OpenCode 的 Action
 */
class SendToOpenCodeAction : AnAction() {

    private val settings = OpenCodeSettings.getInstance()
    private val client = OpenCodeClient()

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE)

        // 获取选中的文本
        val selectedText = editor.selectionModel.selectedText
        if (selectedText.isNullOrBlank()) {
            showNotification(project, "请先选中要发送的代码", NotificationType.WARNING)
            return
        }

        // 获取文件信息
        val fileName = file?.name ?: "unknown"
        val filePath = file?.virtualFile?.path ?: ""
        val language = file?.language?.displayName ?: "text"

        // 获取选中代码的行号信息
        val selectionModel = editor.selectionModel
        val startPos = selectionModel.selectionStartPosition
        val endPos = selectionModel.selectionEndPosition
        val startLine = (startPos?.line ?: 0) + 1  // 转换为 1-based
        // 如果结束位置在行首(column=0)，说明选中内容实际到上一行末尾
        val endLine = if ((endPos?.column ?: 0) == 0 && (endPos?.line ?: 0) > (startPos?.line ?: 0)) {
            endPos!!.line  // 0-based 已经是上一行，直接 +1
        } else {
            (endPos?.line ?: 0) + 1
        }

        // 显示对话框
        val dialog = SendToOpenCodeDialog(
            project = project,
            selectedCode = selectedText,
            fileName = fileName,
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            language = language
        )

        if (dialog.showAndGet()) {
            // 用户点击了"发送"按钮
            val codeToSend = dialog.buildPrompt()

            // 检查工作空间目录是否匹配
            if (settings.checkWorkspaceDir) {
                val workspaceWarning = checkWorkspaceDirectory(project, filePath)
                if (workspaceWarning != null) {
                    val result = Messages.showYesNoDialog(
                        project,
                        workspaceWarning,
                        "工作空间目录警告",
                        Messages.getWarningIcon()
                    )
                    if (result != Messages.YES) {
                        return
                    }
                }
            }

            // 执行发送
            sendToOpenCode(codeToSend, project)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor?.selectionModel?.hasSelection() == true
    }

    /**
     * 构造提示内容（已废弃，现在由 SendToOpenCodeDialog 处理）
     */
    @Deprecated("使用 SendToOpenCodeDialog.buildPrompt() 代替")
    private fun buildPrompt(code: String, language: String, fileName: String, filePath: String, startLine: Int, endLine: Int): String {
        // 计算 OpenCode 工作空间目录
        val openCodeWorkspaceResult = client.getWorkspaceDir()
        val openCodeWorkspaceDir = openCodeWorkspaceResult.getOrNull()

        val fileReference = if (openCodeWorkspaceDir.isNullOrBlank()) {
            // 如果无法获取工作空间目录，使用文件名
            "@$fileName"
        } else {
            // 计算相对于工作空间的路径
            val relativePath = getRelativePath(filePath, openCodeWorkspaceDir)
            "@$relativePath"
        }

        // 添加行号
        val fileReferenceWithLines = if (startLine == endLine) {
            "$fileReference#L$startLine"
        } else {
            "$fileReference#L$startLine-$endLine"
        }

        // 构造发送内容 - 只发送文件引用
        val template = fileReferenceWithLines

        // 调试输出
        println("=== OpenCode Debug ===")
        println("FileName: $fileName")
        println("FilePath: $filePath")
        println("OpenCodeWorkspace: $openCodeWorkspaceDir")
        println("FileReference: $fileReferenceWithLines")
        println("StartLine: $startLine, EndLine: $endLine")
        println("Language: $language")
        println("====================")

        return template
    }

    /**
     * 获取相对于 OpenCode 工作空间的文件路径
     */
    private fun getRelativePath(filePath: String, workspaceDir: String): String {
        // 规范化路径
        val normalizedFilePath = normalizePath(filePath)
        val normalizedWorkspaceDir = normalizePath(workspaceDir)

        // 计算相对路径
        return if (normalizedFilePath.startsWith(normalizedWorkspaceDir)) {
            val relativePath = normalizedFilePath.substring(normalizedWorkspaceDir.length).trimStart('/')
            relativePath
        } else {
            // 如果文件不在工作空间内，使用文件名
            val fileName = filePath.substringAfterLast('/')
            fileName
        }
    }

    /**
     * 检查工作空间目录是否匹配
     * @return 如果目录不匹配，返回警告消息；否则返回 null
     */
    private fun checkWorkspaceDirectory(project: Project, filePath: String): String? {
        // 获取 IDE 项目根目录
        val ideProjectDir = project.basePath
        if (ideProjectDir == null || filePath.isEmpty()) {
            return null
        }

        // 获取 OpenCode 工作空间目录
        val openCodeWorkspaceResult = client.getWorkspaceDir()
        val openCodeWorkspaceDir = openCodeWorkspaceResult.getOrNull()

        // 如果无法获取 OpenCode 工作空间目录，跳过检查
        if (openCodeWorkspaceDir.isNullOrBlank()) {
            return null
        }

        // 规范化路径（处理不同操作系统的路径分隔符）
        val normalizedIdeDir = normalizePath(ideProjectDir)
        val normalizedOpenCodeDir = normalizePath(openCodeWorkspaceDir)

        // 检查 IDE 项目目录是否在 OpenCode 工作空间目录下
        val isMatch = isSubDirectory(normalizedIdeDir, normalizedOpenCodeDir)

        return if (!isMatch) {
            """警告：IDE 工作空间目录与 OpenCode 工作空间目录不匹配！

IDE 项目目录：$normalizedIdeDir
OpenCode 工作空间：$normalizedOpenCodeDir

您确定要继续发送代码吗？发送到错误的工作空间可能会导致上下文混乱。"""
        } else {
            null
        }
    }

    /**
     * 规范化路径，统一使用 / 作为分隔符
     */
    private fun normalizePath(path: String): String {
        return path.replace("\\", "/").trimEnd('/')
    }

    /**
     * 检查目录是否是另一个目录的子目录或相同目录
     * @param child 子目录路径
     * @param parent 父目录路径
     * @return 如果 child 是 parent 的子目录或相同目录，返回 true
     */
    private fun isSubDirectory(child: String, parent: String): Boolean {
        val childPath = if (child.endsWith("/")) child else "$child/"
        val parentPath = if (parent.endsWith("/")) parent else "$parent/"

        // 如果两个路径完全相同，返回 true
        if (childPath.equals(parentPath, ignoreCase = true)) {
            return true
        }

        // 检查 child 是否是 parent 的子目录
        return childPath.startsWith(parentPath, ignoreCase = true)
    }

    /**
     * 发送到 OpenCode
     */
    private fun sendToOpenCode(content: String, project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "发送到 OpenCode", true) {
            private var success = false
            private var error: Throwable? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在连接 OpenCode 服务器..."
                indicator.isIndeterminate = false

                try {
                    indicator.fraction = 0.1

                    // 追加文本
                    indicator.text = "正在发送代码..."
                    val appendResult = client.appendPrompt(content)
                    indicator.fraction = 0.5

                    if (appendResult.isFailure) {
                        error = appendResult.exceptionOrNull()
                        return
                    }

                    // 自动提交（如果配置了）
                    if (settings.autoSubmit) {
                        indicator.text = "正在提交提示..."
                        val submitResult = client.submitPrompt()
                        indicator.fraction = 0.8

                        if (submitResult.isFailure) {
                            error = submitResult.exceptionOrNull()
                            return
                        }
                    }

                    indicator.fraction = 1.0
                    success = true

                    // 在 OpenCode 中显示通知
                    client.showToast(
                        title = "来自 IDE",
                        message = "已从 ${project.name} 发送代码",
                        variant = "info"
                    )

                } catch (e: Exception) {
                    error = e
                }
            }

            override fun onSuccess() {
                ApplicationManager.getApplication().invokeLater {
                    if (success) {
                        showNotification(
                            project,
                            "已成功发送到 OpenCode",
                            NotificationType.INFORMATION
                        )
                    } else if (error != null) {
                        showErrorNotification(project, error!!)
                    }
                }
            }

            override fun onThrowable(error: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    showErrorNotification(project, error)
                }
            }
        })
    }

    /**
     * 显示通知
     */
    private fun showNotification(project: Project, message: String, type: NotificationType) {
        val notification = Notification(
            OpenCodePlugin.NOTIFICATION_GROUP,
            OpenCodePlugin.PLUGIN_ID,
            message,
            type
        )
        Notifications.Bus.notify(notification, project)
    }

    /**
     * 显示错误通知
     */
    private fun showErrorNotification(project: Project, error: Throwable) {
        val message = when (error) {
            is java.net.ConnectException -> "无法连接到 OpenCode 服务器（${settings.getServerUrl()}）。请确保 OpenCode 已启动。"
            is java.net.SocketTimeoutException -> "连接 OpenCode 服务器超时。请检查网络连接。"
            else -> "发送失败：${error.message}"
        }

        val notification = Notification(
            OpenCodePlugin.NOTIFICATION_GROUP,
            OpenCodePlugin.PLUGIN_ID,
            message,
            NotificationType.ERROR
        )
        Notifications.Bus.notify(notification, project)
    }
}
