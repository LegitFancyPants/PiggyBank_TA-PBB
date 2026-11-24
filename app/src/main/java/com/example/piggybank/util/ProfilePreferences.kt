package com.example.piggybank.util

import android.content.Context

class ProfilePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayName(): String =
        prefs.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME)
            ?.trim()
            ?.ifBlank { DEFAULT_DISPLAY_NAME }
            ?: DEFAULT_DISPLAY_NAME

    fun setDisplayName(name: String) {
        val cleaned = name.trim().ifBlank { DEFAULT_DISPLAY_NAME }
        prefs.edit().putString(KEY_DISPLAY_NAME, cleaned).apply()
    }

    fun getMonthlyBudgetLimit(): Long =
        prefs.getLong(KEY_MONTHLY_BUDGET_LIMIT, 0L).coerceAtLeast(0L)

    fun setMonthlyBudgetLimit(limit: Long) {
        prefs.edit().putLong(KEY_MONTHLY_BUDGET_LIMIT, limit.coerceAtLeast(0L)).apply()
    }

    companion object {
        private const val PREFS_NAME = "piggybank_profile_prefs"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_MONTHLY_BUDGET_LIMIT = "monthly_budget_limit"
        const val DEFAULT_DISPLAY_NAME = "Pengguna"
    }
}
