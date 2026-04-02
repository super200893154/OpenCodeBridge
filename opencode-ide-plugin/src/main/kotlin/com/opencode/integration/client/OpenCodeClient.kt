package com.opencode.integration.client

import com.google.gson.Gson
import com.opencode.integration.settings.OpenCodeSettings
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenCode API 客户端
 */
class OpenCodeClient {

    private val settings = OpenCodeSettings.getInstance()
    private val client: OkHttpClient
    private val gson = Gson()

    init {
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 追加提示文本到 OpenCode TUI
     */
    fun appendPrompt(text: String): Result<Boolean> {
        return try {
            val url = "${settings.getServerUrl()}/tui/append-prompt"
            val requestBody = mapOf("text" to text)
            val json = gson.toJson(requestBody)

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(json.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val result = response.body?.string()?.toBoolean() ?: false
                Result.success(result)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 提交当前提示
     */
    fun submitPrompt(): Result<Boolean> {
        return try {
            val url = "${settings.getServerUrl()}/tui/submit-prompt"

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(EMPTY_REQUEST)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val result = response.body?.string()?.toBoolean() ?: false
                Result.success(result)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清空提示
     */
    fun clearPrompt(): Result<Boolean> {
        return try {
            val url = "${settings.getServerUrl()}/tui/clear-prompt"

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(EMPTY_REQUEST)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val result = response.body?.string()?.toBoolean() ?: false
                Result.success(result)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 显示 Toast 通知
     */
    fun showToast(title: String?, message: String, variant: String = "info"): Result<Boolean> {
        return try {
            val url = "${settings.getServerUrl()}/tui/show-toast"
            val requestBody = mutableMapOf<String, Any>(
                "message" to message,
                "variant" to variant
            )
            title?.let { requestBody["title"] = it }

            val json = gson.toJson(requestBody)

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(json.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val result = response.body?.string()?.toBoolean() ?: false
                Result.success(result)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查服务器健康状态
     */
    fun checkHealth(): Result<Boolean> {
        return try {
            val url = "${settings.getServerUrl()}/global/health"

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                val isHealthy = gson.fromJson(body, Map::class.java)["healthy"] as? Boolean ?: false
                Result.success(isHealthy)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取 OpenCode 当前的工作空间目录
     * 使用 GET /path API，优先使用 directory 字段（当前工作目录），如果不存在则使用 worktree 字段
     */
    fun getWorkspaceDir(): Result<String?> {
        return try {
            val url = "${settings.getServerUrl()}/path"

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                // 响应格式：{ state: string, config: string, worktree: string, directory: string }
                val pathInfo = gson.fromJson(body, Map::class.java)

                // 优先使用 directory（当前工作目录），如果不存在则使用 worktree
                val workspaceDir = (pathInfo["directory"] as? String)
                    ?: (pathInfo["worktree"] as? String)

                Result.success(workspaceDir)
            } else {
                // 如果 API 不存在，返回 null 而不是错误
                Result.success(null)
            }
        } catch (e: IOException) {
            // 网络错误时也返回 null，避免阻断主要功能
            Result.success(null)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    /**
     * 添加认证头
     */
    private fun Request.Builder.addHeaders(): Request.Builder {
        val username = settings.serverUsername
        val password = settings.serverPassword

        if (username.isNotEmpty() || password.isNotEmpty()) {
            val credentials = "$username:$password"
            val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
            this.addHeader("Authorization", "Basic $encoded")
        }

        return this.addHeader("Content-Type", "application/json")
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val EMPTY_REQUEST = "".toRequestBody(JSON_MEDIA_TYPE)
    }
}
