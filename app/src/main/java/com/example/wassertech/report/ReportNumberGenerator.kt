package com.example.wassertech.report

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object ReportNumberGenerator {
    private const val PREFS_NAME = "report_number_prefs"
    private const val KEY_LAST_NUMBER = "last_report_number"
    private const val KEY_LAST_MONTH_YEAR = "last_month_year"
    private const val STARTING_NUMBER = 1157  // Начальный номер 01157
    
    /**
     * Генерирует номер отчета в формате АXXXXX/mmyy
     * Номер увеличивается последовательно, формат месяца/года обновляется каждый месяц
     */
    fun generateReportNumber(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val currentMonthYear = SimpleDateFormat("MMyy", Locale.US).format(calendar.time)
        
        val lastMonthYear = prefs.getString(KEY_LAST_MONTH_YEAR, null)
        
        // Если месяц изменился, сбрасываем номер на начальный
        val currentNumber = if (lastMonthYear != currentMonthYear) {
            prefs.edit()
                .putInt(KEY_LAST_NUMBER, STARTING_NUMBER)
                .putString(KEY_LAST_MONTH_YEAR, currentMonthYear)
                .apply()
            STARTING_NUMBER
        } else {
            // Проверяем, был ли уже сохранен номер
            val hasSavedNumber = prefs.contains(KEY_LAST_NUMBER)
            val lastNumber = if (hasSavedNumber) {
                prefs.getInt(KEY_LAST_NUMBER, STARTING_NUMBER)
            } else {
                STARTING_NUMBER - 1  // При первом использовании в этом месяце начинаем с STARTING_NUMBER
            }
            val nextNumber = lastNumber + 1
            prefs.edit()
                .putInt(KEY_LAST_NUMBER, nextNumber)
                .apply()
            nextNumber
        }
        
        // Форматируем номер с ведущими нулями (5 символов)
        val formattedNumber = String.format("%05d", currentNumber)
        return "А$formattedNumber/$currentMonthYear"
    }
}

