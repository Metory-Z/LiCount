package com.licount.data

/**
 * 金额输入：全程用字符串与 Long 运算，绝不经过 Double。
 *
 * 之前的 `(yuan.toDouble() * 100).toLong()` 会踩浮点尾差：0.29 算成 28.999… 再截断成 28 分，
 * 1.15 算成 114 分、19.99 算成 1998 分。记账必须精确到分，首页键盘与编辑/补记框统一走这里。
 */
object MoneyInput {

    /** 整数部分最多 7 位（¥9,999,999.99），保证 *100 后仍远离 Long 上限。 */
    const val MAX_INTEGER_DIGITS = 7
    const val MAX_DECIMAL_DIGITS = 2

    /**
     * 清洗输入框内容：只保留数字与第一个小数点，限制整数/小数位数，去掉多余前导零。
     * 首页数字键盘有等价的逐键规则，这里用于可自由粘贴的 OutlinedTextField。
     */
    fun sanitize(raw: String): String {
        val firstDot = raw.indexOf('.')
        val cleaned = if (firstDot < 0) raw
        else raw.substring(0, firstDot + 1) + raw.substring(firstDot + 1).replace(".", "")
        val digitsOnly = cleaned.filter { it.isDigit() || it == '.' }
        val dot = digitsOnly.indexOf('.')
        val intPart = (if (dot < 0) digitsOnly else digitsOnly.substring(0, dot))
            .take(MAX_INTEGER_DIGITS)
        val fracPart = if (dot < 0) "" else digitsOnly.substring(dot + 1)
            .filter { it.isDigit() }
            .take(MAX_DECIMAL_DIGITS)
        val normalized = if (intPart.isEmpty()) "" else intPart.trimStart('0').ifEmpty { "0" }
        return if (dot < 0) normalized else "$normalized.$fracPart"
    }

    /** 把「元」字符串精确转成「分」；非法、多个小数点或超出范围一律返回 0（保存按钮会禁用）。 */
    fun parseYuanToFen(raw: String): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0L
        val dot = s.indexOf('.')
        if (dot >= 0 && s.indexOf('.', dot + 1) >= 0) return 0L
        val intPart = if (dot < 0) s else s.substring(0, dot)
        val fracPart = if (dot < 0) "" else s.substring(dot + 1)
        if (intPart.any { !it.isDigit() } || fracPart.any { !it.isDigit() }) return 0L
        if (intPart.length > MAX_INTEGER_DIGITS) return 0L
        val yuan = intPart.ifEmpty { "0" }.toLongOrNull() ?: return 0L
        val cents = when {
            fracPart.isEmpty() -> 0L
            fracPart.length == 1 -> (fracPart[0] - '0') * 10L
            else -> (fracPart[0] - '0') * 10L + (fracPart[1] - '0')
        }
        // 多余的第三位小数由 sanitize 拦掉；即便直接调用，也只按前两位计，绝不溢出
        return yuan * 100L + cents
    }
}
