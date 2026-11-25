package com.licount.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** 金额解析必须精确到分，不能有任何浮点截断。 */
class MoneyInputTest {

    @Test
    fun decimalsAreExact() {
        assertEquals(29L, MoneyInput.parseYuanToFen("0.29"))
        assertEquals(115L, MoneyInput.parseYuanToFen("1.15"))
        assertEquals(1999L, MoneyInput.parseYuanToFen("19.99"))
        assertEquals(255L, MoneyInput.parseYuanToFen("2.55"))
        assertEquals(1L, MoneyInput.parseYuanToFen("0.01"))
        assertEquals(823L, MoneyInput.parseYuanToFen("8.23"))
    }

    @Test
    fun integersAndTrailingDot() {
        assertEquals(1200L, MoneyInput.parseYuanToFen("12"))
        assertEquals(1230L, MoneyInput.parseYuanToFen("12.3"))
        assertEquals(0L, MoneyInput.parseYuanToFen("0"))
        assertEquals(0L, MoneyInput.parseYuanToFen("0."))
        assertEquals(0L, MoneyInput.parseYuanToFen(""))
    }

    @Test
    fun invalidInputNeverProducesHugeValues() {
        assertEquals(0L, MoneyInput.parseYuanToFen("Infinity"))
        assertEquals(0L, MoneyInput.parseYuanToFen("1.2.3"))
        assertEquals(0L, MoneyInput.parseYuanToFen("999999999999999999999"))
        assertEquals(0L, MoneyInput.parseYuanToFen("-5"))
    }

    @Test
    fun sanitizeLimitsLengthAndDecimals() {
        assertEquals("1.23", MoneyInput.sanitize("1.23456"))
        assertEquals("1234567", MoneyInput.sanitize("12345678"))
        assertEquals("0", MoneyInput.sanitize("000"))
        assertEquals("12.34", MoneyInput.sanitize("12.3.4"))
        assertEquals("99", MoneyInput.sanitize("9a9"))
    }
}
