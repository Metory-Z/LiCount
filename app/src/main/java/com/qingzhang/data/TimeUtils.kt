package com.qingzhang.data

import java.util.Calendar

/** 时间与统计相关工具。 */
object TimeUtils {

    /** 本月起止（毫秒）：[start, end)。 */
    fun currentMonthRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    /** 把时间戳归一到当天零点（毫秒）。 */
    fun dayStart(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * 计算连续打卡天数。days 是「有记录的天的零点时间戳」集合。
     * 规则：从今天（或昨天，若今天还没记）往前连续推，断一天即止。
     */
    fun streakDays(days: Set<Long>, now: Long = System.currentTimeMillis()): Int {
        if (days.isEmpty()) return 0
        val today = dayStart(now)
        // 今天没记，从昨天开始算；今天记了，从今天开始
        var cursor = if (days.contains(today)) today else today - 86_400_000L
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor -= 86_400_000L
        }
        return streak
    }
}
