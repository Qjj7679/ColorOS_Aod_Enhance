package com.op.aod.enhance.data

data class AodUiConfig(
    val initDark: Int = 80,
    val initBright: Int = 160,
    val runningMultiplier: Float = 1.6f,
    val enablePanoramic: Boolean = true,
    val enableSettingsSupport: Boolean = true,
    val blockSingleClick: Boolean = true,
)
