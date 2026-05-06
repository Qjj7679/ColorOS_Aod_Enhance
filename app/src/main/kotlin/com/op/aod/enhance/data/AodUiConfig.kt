package com.op.aod.enhance.data

/**
 * UI 侧配置数据类。
 * 镜像关系：hook/AodConfig.kt 持有相同字段的 Hook 侧镜像。
 * 新增/修改字段时需同步更新：AodConfig（hook 侧）、AodConfigContract。
 */
data class AodUiConfig(
    val initDark: Int = AodConfigContract.DEFAULT_INIT_DARK,
    val initBright: Int = AodConfigContract.DEFAULT_INIT_BRIGHT,
    val runningMultiplier: Float = AodConfigContract.DEFAULT_RUNNING_MULTIPLIER,
    val enablePanoramic: Boolean = AodConfigContract.DEFAULT_ENABLE_PANORAMIC,
    val enableSettingsSupport: Boolean = AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT,
    val blockSingleClick: Boolean = AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK,
)
