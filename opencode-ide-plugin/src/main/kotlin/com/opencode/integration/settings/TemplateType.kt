package com.opencode.integration.settings

/**
 * 预定义模板类型
 */
enum class TemplateType(
    val displayName: String,
    val contextHint: String
) {
    CUSTOM("自定义提示...", ""),
    EXPLAIN("代码解释", "请详细解释这段代码的功能和实现逻辑"),
    FIX("修复问题", "请找出这段代码中的问题并提供修复方案"),
    REFACTOR("重构代码", "请重构这段代码，提高代码质量和可读性"),
    OPTIMIZE("性能优化", "请优化这段代码的性能和效率"),
    REVIEW("代码审查", "请对这段代码进行代码审查，指出潜在问题和改进建议"),
    TEST("生成测试", "请为这段代码编写单元测试");

    override fun toString(): String = displayName
}
