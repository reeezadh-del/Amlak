package com.example.ui.components

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(value: Long): String {
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(value)
        return toPersianDigits(formatted.replace(",", "،"))
    }

    fun formatPriceToman(priceToman: Long): String {
        if (priceToman <= 0L) return "توافقی"
        val billions = priceToman / 1_000_000_000L
        val remainingMillions = (priceToman % 1_000_000_000L) / 1_000_000L
        return when {
            billions > 0 && remainingMillions > 0 ->
                "${formatNumber(billions)} میلیارد و ${formatNumber(remainingMillions)} میلیون تومان"
            billions > 0 ->
                "${formatNumber(billions)} میلیارد تومان"
            priceToman >= 1_000_000L ->
                "${formatNumber(priceToman / 1_000_000L)} میلیون تومان"
            else ->
                "${formatNumber(priceToman)} تومان"
        }
    }

    fun formatPricePerMeter(priceToman: Long, areaSqm: Long): String {
        if (priceToman <= 0L || areaSqm <= 0L) return ""
        val perMeter = priceToman / areaSqm
        return if (perMeter >= 1_000_000L) {
            "${formatNumber(perMeter / 1_000_000L)} میلیون / متر"
        } else {
            "${formatNumber(perMeter)} تومان / متر"
        }
    }

    fun formatRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "هم‌اکنون"
        val diffSeconds = ((System.currentTimeMillis() - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 60L -> "لحظاتی پیش"
            diffSeconds < 3600L -> "${toPersianDigits((diffSeconds / 60L).toString())} دقیقه پیش"
            diffSeconds < 86400L -> "${toPersianDigits((diffSeconds / 3600L).toString())} ساعت پیش"
            else -> "${toPersianDigits((diffSeconds / 86400L).toString())} روز پیش"
        }
    }
}
