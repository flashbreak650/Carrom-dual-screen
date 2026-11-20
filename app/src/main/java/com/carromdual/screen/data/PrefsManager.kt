package com.carromdual.screen.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var screen1Package: String
        get() = prefs.getString(KEY_SCREEN1_PKG, DEFAULT_CARROM_PACKAGE) ?: DEFAULT_CARROM_PACKAGE
        set(value) = prefs.edit().putString(KEY_SCREEN1_PKG, value).apply()

    var screen1UserSerial: Long
        get() = prefs.getLong(KEY_SCREEN1_USER, 0L)
        set(value) = prefs.edit().putLong(KEY_SCREEN1_USER, value).apply()

    var screen2Package: String
        get() = prefs.getString(KEY_SCREEN2_PKG, DEFAULT_CARROM_PACKAGE) ?: DEFAULT_CARROM_PACKAGE
        set(value) = prefs.edit().putString(KEY_SCREEN2_PKG, value).apply()

    var screen2UserSerial: Long
        get() = prefs.getLong(KEY_SCREEN2_USER, -1L)
        set(value) = prefs.edit().putLong(KEY_SCREEN2_USER, value).apply()

    var launchMode: String
        get() = prefs.getString(KEY_LAUNCH_MODE, MODE_SPLIT) ?: MODE_SPLIT
        set(value) = prefs.edit().putString(KEY_LAUNCH_MODE, value).apply()

    var splitLayoutPreset: String
        get() = prefs.getString(KEY_SPLIT_PRESET, PRESET_TOP_BOTTOM) ?: PRESET_TOP_BOTTOM
        set(value) = prefs.edit().putString(KEY_SPLIT_PRESET, value).apply()

    var floatingWidthDp: Int
        get() = prefs.getInt(KEY_FLOAT_WIDTH_DP, 256)
        set(value) = prefs.edit().putInt(KEY_FLOAT_WIDTH_DP, value.coerceIn(210, 380)).apply()

    var floatingHeightDp: Int
        get() = prefs.getInt(KEY_FLOAT_HEIGHT_DP, 236)
        set(value) = prefs.edit().putInt(KEY_FLOAT_HEIGHT_DP, value.coerceIn(180, 460)).apply()

    var floatingOpacityPercent: Int
        get() = prefs.getInt(KEY_FLOAT_OPACITY, 94)
        set(value) = prefs.edit().putInt(KEY_FLOAT_OPACITY, value.coerceIn(45, 100)).apply()

    var floatingSnapToEdge: Boolean
        get() = prefs.getBoolean(KEY_FLOAT_SNAP, true)
        set(value) = prefs.edit().putBoolean(KEY_FLOAT_SNAP, value).apply()

    var floatingStartMinimized: Boolean
        get() = prefs.getBoolean(KEY_FLOAT_START_MIN, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOAT_START_MIN, value).apply()

    var floatingPosX: Int
        get() = prefs.getInt(KEY_FLOAT_POS_X, 24)
        set(value) = prefs.edit().putInt(KEY_FLOAT_POS_X, value).apply()

    var floatingPosY: Int
        get() = prefs.getInt(KEY_FLOAT_POS_Y, 180)
        set(value) = prefs.edit().putInt(KEY_FLOAT_POS_Y, value).apply()

    fun resetWindowDefaults() {
        prefs.edit()
            .putString(KEY_LAUNCH_MODE, MODE_SPLIT)
            .putString(KEY_SPLIT_PRESET, PRESET_TOP_BOTTOM)
            .putInt(KEY_FLOAT_WIDTH_DP, 256)
            .putInt(KEY_FLOAT_HEIGHT_DP, 236)
            .putInt(KEY_FLOAT_OPACITY, 94)
            .putBoolean(KEY_FLOAT_SNAP, true)
            .putBoolean(KEY_FLOAT_START_MIN, false)
            .putInt(KEY_FLOAT_POS_X, 24)
            .putInt(KEY_FLOAT_POS_Y, 180)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "carrom_dual_screen_prefs"
        const val DEFAULT_CARROM_PACKAGE = "com.miniclip.carrom"
        const val MODE_SPLIT = "split"
        const val MODE_FREEFORM = "freeform"
        const val PRESET_TOP_BOTTOM = "top_bottom"
        const val PRESET_LEFT_RIGHT = "left_right"

        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_SCREEN1_PKG = "screen1_pkg"
        private const val KEY_SCREEN1_USER = "screen1_user_serial"
        private const val KEY_SCREEN2_PKG = "screen2_pkg"
        private const val KEY_SCREEN2_USER = "screen2_user_serial"
        private const val KEY_LAUNCH_MODE = "launch_mode"
        private const val KEY_SPLIT_PRESET = "split_preset"
        private const val KEY_FLOAT_WIDTH_DP = "float_width_dp"
        private const val KEY_FLOAT_HEIGHT_DP = "float_height_dp"
        private const val KEY_FLOAT_OPACITY = "float_opacity"
        private const val KEY_FLOAT_SNAP = "float_snap"
        private const val KEY_FLOAT_START_MIN = "float_start_min"
        private const val KEY_FLOAT_POS_X = "float_pos_x"
        private const val KEY_FLOAT_POS_Y = "float_pos_y"
    }
}
