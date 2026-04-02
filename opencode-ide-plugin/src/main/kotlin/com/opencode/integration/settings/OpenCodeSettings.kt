package com.opencode.integration.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * OpenCode 配置状态
 */
@State(
    name = "OpenCodeSettings",
    storages = [Storage("OpenCodeSettings.xml")]
)
@Service
class OpenCodeSettings : PersistentStateComponent<OpenCodeSettings.State> {

    data class State(
        var serverHost: String = "localhost",
        var serverPort: Int = 4096,
        var serverPassword: String = "",
        var serverUsername: String = "opencode",
        var autoSubmit: Boolean = false,
        var showConfirmation: Boolean = true,
        var includeFileContext: Boolean = true,
        var checkWorkspaceDir: Boolean = true,
        var customPromptTemplate: String = """代码位置：@{filename}#L{start_line}-{end_line}
文件路径：{filepath}

请分析这段 {language} 代码：
```{language}
{code}
```"""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): OpenCodeSettings {
            return ApplicationManager.getApplication().getService(OpenCodeSettings::class.java)
        }
    }

    // Getter 方法
    val serverHost: String get() = state.serverHost
    val serverPort: Int get() = state.serverPort
    val serverPassword: String get() = state.serverPassword
    val serverUsername: String get() = state.serverUsername
    val autoSubmit: Boolean get() = state.autoSubmit
    val showConfirmation: Boolean get() = state.showConfirmation
    val includeFileContext: Boolean get() = state.includeFileContext
    val checkWorkspaceDir: Boolean get() = state.checkWorkspaceDir
    val customPromptTemplate: String get() = state.customPromptTemplate

    // Setter 方法
    fun updateServerHost(host: String) {
        state.serverHost = host
    }

    fun updateServerPort(port: Int) {
        state.serverPort = port
    }

    fun updateServerPassword(password: String) {
        state.serverPassword = password
    }

    fun updateServerUsername(username: String) {
        state.serverUsername = username
    }

    fun updateAutoSubmit(autoSubmit: Boolean) {
        state.autoSubmit = autoSubmit
    }

    fun updateShowConfirmation(showConfirmation: Boolean) {
        state.showConfirmation = showConfirmation
    }

    fun updateIncludeFileContext(includeFileContext: Boolean) {
        state.includeFileContext = includeFileContext
    }

    fun updateCheckWorkspaceDir(checkWorkspaceDir: Boolean) {
        state.checkWorkspaceDir = checkWorkspaceDir
    }

    fun updateCustomPromptTemplate(template: String) {
        state.customPromptTemplate = template
    }

    fun getServerUrl(): String {
        return "http://$serverHost:$serverPort"
    }
}
