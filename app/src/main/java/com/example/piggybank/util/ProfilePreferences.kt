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

    fun getMonthlyBudgetLimit(): Long {
        migrateLegacyDefaultBudgetIfNeeded()
        return prefs.getLong(KEY_MONTHLY_BUDGET_LIMIT, DEFAULT_MONTHLY_BUDGET_LIMIT).coerceAtLeast(0L)
    }

    fun setMonthlyBudgetLimit(limit: Long) {
        prefs.edit()
            .putLong(KEY_MONTHLY_BUDGET_LIMIT, limit.coerceAtLeast(0L))
            .putBoolean(KEY_LEGACY_BUDGET_MIGRATED, true)
            .apply()
    }

    private fun migrateLegacyDefaultBudgetIfNeeded() {
        if (prefs.getBoolean(KEY_LEGACY_BUDGET_MIGRATED, false)) return

        val editor = prefs.edit().putBoolean(KEY_LEGACY_BUDGET_MIGRATED, true)
        if (prefs.contains(KEY_MONTHLY_BUDGET_LIMIT) &&
            prefs.getLong(KEY_MONTHLY_BUDGET_LIMIT, DEFAULT_MONTHLY_BUDGET_LIMIT) == LEGACY_DEFAULT_MONTHLY_BUDGET_LIMIT
        ) {
            editor.putLong(KEY_MONTHLY_BUDGET_LIMIT, DEFAULT_MONTHLY_BUDGET_LIMIT)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "piggybank_profile_prefs"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_MONTHLY_BUDGET_LIMIT = "monthly_budget_limit"
        private const val KEY_LEGACY_BUDGET_MIGRATED = "monthly_budget_default_zero_migrated"
        private const val LEGACY_DEFAULT_MONTHLY_BUDGET_LIMIT = 10_000_000L
        const val DEFAULT_MONTHLY_BUDGET_LIMIT = 0L
        const val DEFAULT_DISPLAY_NAME = "Pengguna"
    }
}
