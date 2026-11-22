package com.licount.data

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

    /** 在「零点时间戳」上加减天数。 */
    fun addDays(dayStart: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }

    /** 把 time 的时分秒搬到 dayStart 那一天，用于「改日期但保留原时刻」。 */
    fun withDay(time: Long, dayStart: Long): Long {
        val src = Calendar.getInstance().apply { timeInMillis = time }
        val dst = Calendar.getInstance().apply { timeInMillis = dayStart }
        dst.set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
        dst.set(Calendar.MINUTE, src.get(Calendar.MINUTE))
        dst.set(Calendar.SECOND, src.get(Calendar.SECOND))
        dst.set(Calendar.MILLISECOND, src.get(Calendar.MILLISECOND))
        return dst.timeInMillis
    }
}
