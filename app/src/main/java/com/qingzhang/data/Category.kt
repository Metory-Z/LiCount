package com.qingzhang.data

/**
 * 预置消费分类。emoji 用作分类图标，零资源、开箱即用。
 * 颜色用主题语义外的固定色相，保证各分类一眼可辨。
 */
data class Category(
    val key: String,
    val label: String,
    val emoji: String
) {
    companion object {
        /** 预置分类，key 稳定存储。 */
        val presets = listOf(
            Category("food", "餐饮", "🍜"),
            Category("transport", "交通", "🚇"),
            Category("shopping", "购物", "🛍️"),
            Category("entertainment", "娱乐", "🎮"),
            Category("housing", "居住", "🏠"),
            Category("medical", "医疗", "💊"),
            Category("education", "学习", "📚"),
            Category("other", "其他", "📦"),
            Category("income", "收入", "💰")
        )

        fun byKey(key: String): Category =
            presets.firstOrNull { it.key == key } ?: Category(key, key, "📦")
    }
}
