package com.op.aod.enhance.data

import android.database.Cursor

/**
 * 配置契约：定义 ContentProvider 的键名和常量。
 *
 * 该文件是 Provider、UI 侧 (AodUiConfig) 和 Hook 侧 (AodConfig) 之间的唯一数据来源，
 * 添加/修改字段时必须同步更新三处：
 * - AodUiConfig.kt (UI 侧)
 * - AodConfig.kt (Hook 侧)
 * - AodConfigContract.kt (键名定义)
 *
 * 注意：列索引已废弃，两侧统一使用 [getColumnIndex] 按列名查找。
 */
object AodConfigContract {

    // SharedPreferences 键名（同时作为 Cursor 列名）
    const val KEY_INIT_DARK = "init_brightness_dark"
    const val KEY_INIT_BRIGHT = "init_brightness_bright"
    const val KEY_RUNNING_MULTIPLIER = "running_brightness_multiplier"
    const val KEY_ENABLE_PANORAMIC = "enable_panoramic"
    const val KEY_ENABLE_SETTINGS_SUPPORT = "enable_settings_support"
    const val KEY_BLOCK_SINGLE_CLICK = "block_single_click"

    // 默认值
    const val DEFAULT_INIT_DARK = 80
    const val DEFAULT_INIT_BRIGHT = 160
    const val DEFAULT_RUNNING_MULTIPLIER = 1.6f
    const val DEFAULT_ENABLE_PANORAMIC = true
    const val DEFAULT_ENABLE_SETTINGS_SUPPORT = true
    const val DEFAULT_BLOCK_SINGLE_CLICK = true

    /**
     * 从 [Cursor] 当前行读取所有配置列的原始值。
     *
     * 由 UI 侧 ([AodConfigStore]) 和 Hook 侧 ([com.op.aod.enhance.hook.AodConfigReader]) 共用，
     * 新增/修改字段时只需改动此处和对应的数据类。
     */
    fun readRow(c: Cursor): ConfigValues = ConfigValues(
        initDark = c.getInt(c.getColumnIndexOrThrow(KEY_INIT_DARK)),
        initBright = c.getInt(c.getColumnIndexOrThrow(KEY_INIT_BRIGHT)),
        runningMultiplier = c.getFloat(c.getColumnIndexOrThrow(KEY_RUNNING_MULTIPLIER)),
        enablePanoramic = c.getInt(c.getColumnIndexOrThrow(KEY_ENABLE_PANORAMIC)) == 1,
        enableSettingsSupport = c.getInt(c.getColumnIndexOrThrow(KEY_ENABLE_SETTINGS_SUPPORT)) == 1,
        blockSingleClick = c.getInt(c.getColumnIndexOrThrow(KEY_BLOCK_SINGLE_CLICK)) == 1,
    )

    /**
     * Cursor 原始值快照，避免两处重复实现相同的列解析。
     */
    data class ConfigValues(
        val initDark: Int,
        val initBright: Int,
        val runningMultiplier: Float,
        val enablePanoramic: Boolean,
        val enableSettingsSupport: Boolean,
        val blockSingleClick: Boolean,
    )
}
