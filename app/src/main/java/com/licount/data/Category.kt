package com.licount.data

/**
 * 预置消费分类。emoji 用作分类图标，零资源、开箱即用。
 * 收入不是分类，而是独立的「收支类型」，由记账页的 支出/收入 切换控制。
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
            Category("entertainment", "娱乐", "🎬"),
            Category("housing", "居住", "🏠"),
            Category("medical", "医疗", "💊"),
            Category("education", "学习", "📚"),
            Category("fund", "基金", "📈"),
            Category("other", "其他", "📦")
        )

        fun byKey(key: String): Category =
            presets.firstOrNull { it.key == key } ?: Category(key, key, "📦")
    }
}
