package com.op.aod.enhance.data

import android.database.Cursor
import kotlin.concurrent.Volatile

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
    const val KEY_BLOCK_LOW_LIGHT_HIDE = "block_low_light_hide"

    // 默认值
    const val DEFAULT_INIT_DARK = 80
    const val DEFAULT_INIT_BRIGHT = 160
    const val DEFAULT_RUNNING_MULTIPLIER = 1.6f
    const val DEFAULT_ENABLE_PANORAMIC = true
    const val DEFAULT_ENABLE_SETTINGS_SUPPORT = true
    const val DEFAULT_BLOCK_SINGLE_CLICK = true
    const val DEFAULT_BLOCK_LOW_LIGHT_HIDE = true

    /** 列索引缓存（延迟初始化，只在首次 readRow 时查询一次）。@Volatile 保证多线程可见性。 */
    @Volatile
    private var columnIndices: IntArray? = null

    /**
     * 获取缓存的列索引，如果尚未初始化则先查询并缓存。
     * 避免每次 readRow 都调用 getColumnIndexOrThrow。
     */
    private fun getCachedColumnIndices(c: Cursor): IntArray {
        columnIndices?.let { return it }
        val indices = intArrayOf(
            c.getColumnIndexOrThrow(KEY_INIT_DARK),
            c.getColumnIndexOrThrow(KEY_INIT_BRIGHT),
            c.getColumnIndexOrThrow(KEY_RUNNING_MULTIPLIER),
            c.getColumnIndexOrThrow(KEY_ENABLE_PANORAMIC),
            c.getColumnIndexOrThrow(KEY_ENABLE_SETTINGS_SUPPORT),
            c.getColumnIndexOrThrow(KEY_BLOCK_SINGLE_CLICK),
            c.getColumnIndexOrThrow(KEY_BLOCK_LOW_LIGHT_HIDE),
        )
        columnIndices = indices
        return indices
    }

    /**
     * 从 [Cursor] 当前行读取所有配置列的原始值。
     *
     * 由 UI 侧 ([AodConfigStore]) 和 Hook 侧 ([com.op.aod.enhance.hook.AodConfigReader]) 共用，
     * 新增/修改字段时只需改动此处和对应的数据类。
     */
    fun readRow(c: Cursor): ConfigValues {
        val indices = getCachedColumnIndices(c)
        return ConfigValues(
            initDark = c.getInt(indices[0]),
            initBright = c.getInt(indices[1]),
            runningMultiplier = c.getFloat(indices[2]),
            enablePanoramic = c.getInt(indices[3]) == 1,
            enableSettingsSupport = c.getInt(indices[4]) == 1,
            blockSingleClick = c.getInt(indices[5]) == 1,
            blockLowLightHide = c.getInt(indices[6]) == 1,
        )
    }

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
        val blockLowLightHide: Boolean,
    )
}
