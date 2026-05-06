package com.op.aod.enhance.data

/**
 * 配置契约：定义 ContentProvider 的列索引、键名和常量。
 *
 * 该文件是 Provider、UI 侧 (AodUiConfig) 和 Hook 侧 (AodConfig) 之间的唯一数据来源，
 * 添加/修改字段时必须同步更新三处：
 * - AodUiConfig.kt (UI 侧)
 * - AodConfig.kt (Hook 侧)
 * - AodConfigContract.kt (列定义)
 */
object AodConfigContract {

    // 列索引 —— 在 MatrixCursor 和读取方之间共享
    const val COL_INIT_DARK = 0
    const val COL_INIT_BRIGHT = 1
    const val COL_RUNNING_MULTIPLIER = 2
    const val COL_ENABLE_PANORAMIC = 3
    const val COL_ENABLE_SETTINGS_SUPPORT = 4
    const val COL_BLOCK_SINGLE_CLICK = 5

    const val COL_COUNT = 6

    // SharedPreferences 键名
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
}
