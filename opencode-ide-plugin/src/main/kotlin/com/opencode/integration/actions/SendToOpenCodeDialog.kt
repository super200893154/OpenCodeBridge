package com.opencode.integration.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.UIUtil
import com.opencode.integration.settings.TemplateType
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class SendToOpenCodeDialog(
    private val project: Project,
    private val selectedCode: String,
    private val fileName: String,
    private val filePath: String,
    private val startLine: Int,
    private val endLine: Int,
    private val language: String
) : DialogWrapper(project) {

    private val templateComboBox = JComboBox<TemplateType>()
    private val contextTextArea = JBTextArea()
    private val codePreviewLabel = JBLabel()

    init {
        title = "发送到 OpenCode"
        isModal = true
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(600, 400)
        mainPanel.border = EmptyBorder(12, 12, 12, 12)

        mainPanel.add(createInfoPanel(), BorderLayout.NORTH)
        mainPanel.add(createContentPanel(), BorderLayout.CENTER)

        return mainPanel
    }

    private fun createInfoPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(0, 0, 8, 0)

        val infoText = if (startLine == endLine) {
            "$filePath (第 $startLine 行)"
        } else {
            "$filePath (第 $startLine-$endLine 行)"
        }
        val infoLabel = JBLabel(infoText)
        infoLabel.font = UIUtil.getLabelFont().deriveFont(12f)
        infoLabel.foreground = UIUtil.getContextHelpForeground()
        panel.add(infoLabel, BorderLayout.NORTH)

        val scrollPane = JScrollPane().apply {
            viewport.view = codePreviewLabel
            preferredSize = Dimension(600, 80)
            border = BorderFactory.createLineBorder(Color.GRAY)
        }

        val previewCode = if (selectedCode.length > 200) {
            selectedCode.take(200) + "\n..."
        } else {
            selectedCode
        }

        codePreviewLabel.text = "<html><pre style=\"margin: 4px; font-family: monospace; font-size: 12px;\">${escapeHtml(previewCode)}</pre></html>"
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createContentPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(8, 0, 0, 0)

        val templatePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        templatePanel.border = EmptyBorder(0, 0, 8, 0)

        templatePanel.add(JBLabel("模板："))

        templateComboBox.addItem(TemplateType.CUSTOM)
        templateComboBox.addItem(TemplateType.EXPLAIN)
        templateComboBox.addItem(TemplateType.FIX)
        templateComboBox.addItem(TemplateType.REFACTOR)
        templateComboBox.addItem(TemplateType.OPTIMIZE)
        templateComboBox.addItem(TemplateType.REVIEW)
        templateComboBox.addItem(TemplateType.TEST)

        templateComboBox.preferredSize = Dimension(200, 28)
        templateComboBox.addActionListener { onTemplateChanged() }
        templatePanel.add(templateComboBox)

        panel.add(templatePanel, BorderLayout.NORTH)

        val contextPanel = JPanel(BorderLayout())
        contextPanel.border = EmptyBorder(0, 0, 8, 0)

        val contextLabel = JBLabel("上下文（按 Enter 发送，Shift+Enter 换行）：")
        contextLabel.border = EmptyBorder(0, 0, 4, 0)
        contextPanel.add(contextLabel, BorderLayout.NORTH)

        contextTextArea.lineWrap = true
        contextTextArea.wrapStyleWord = true
        contextTextArea.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            EmptyBorder(4, 4, 4, 4)
        )
        contextTextArea.preferredSize = Dimension(600, 60)

        // 覆盖 DialogWrapper 的 Enter 键绑定，让 Shift+Enter 能正常换行
        contextTextArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("ENTER"), "none"
        )
        contextTextArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("shift ENTER"), "insert-break"
        )
        contextTextArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    doOKAction()
                }
            }
        })

        contextPanel.add(JScrollPane(contextTextArea).apply {
            preferredSize = Dimension(600, 60)
        }, BorderLayout.CENTER)

        panel.add(contextPanel, BorderLayout.CENTER)

        return panel
    }

    private fun onTemplateChanged() {
        val selected = templateComboBox.selectedItem as? TemplateType ?: return

        contextTextArea.text = when (selected) {
            TemplateType.EXPLAIN -> "请详细解释这段代码的功能和实现逻辑"
            TemplateType.FIX -> "请找出这段代码中的问题并提供修复方案"
            TemplateType.REFACTOR -> "请重构这段代码，提高代码质量和可读性"
            TemplateType.OPTIMIZE -> "请优化这段代码的性能和效率"
            TemplateType.REVIEW -> "请对这段代码进行代码审查，指出潜在问题和改进建议"
            TemplateType.TEST -> "请为这段代码编写单元测试"
            TemplateType.CUSTOM -> ""
        }

        contextTextArea.selectAll()
        contextTextArea.requestFocusInWindow()
    }

    fun buildPrompt(): String {
        val template = getSelectedTemplate()
        val userContext = getUserContext().trim()

        val lineRef = if (startLine == endLine) {
            "#L$startLine"
        } else {
            "#L$startLine-$endLine"
        }

        val fileRef = "@$fileName $lineRef"

        return if (template == TemplateType.CUSTOM && userContext.isEmpty()) {
            "$fileRef "
        } else {
            val contextText = if (userContext.isNotEmpty()) userContext else template.contextHint
            "$fileRef $contextText"
        }
    }

    private fun getSelectedTemplate(): TemplateType {
        return templateComboBox.selectedItem as? TemplateType ?: TemplateType.CUSTOM
    }

    private fun getUserContext(): String {
        return contextTextArea.text.trim()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
