package com.op.aod.enhance.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.op.aod.enhance.hook.AodSettingsHook.hookAodAllDaySupportSettings
import com.op.aod.enhance.hook.BrightnessHook.hookInitBrightnessFix
import com.op.aod.enhance.hook.BrightnessHook.hookRunningBrightnessBoost
import com.op.aod.enhance.hook.PanoramicHook.hookPanoramicAllDaySupport
import com.op.aod.enhance.hook.SingleClickBlockHook.hookSingleClickWakeUpBlock

/**
 * OP AOD Enhance - 主入口
 *
 * 仅负责按包名分发到各功能 Hook。
 */
object MainHook : YukiBaseHooker() {

    override fun onHook() {
        loadApp(name = SYSTEM_UI) {
            val cfg = AodConfigReader.read()
            hookInitBrightnessFix()
            if (cfg.runningMultiplier != 1.0f) {
                hookRunningBrightnessBoost()
            }
            if (cfg.enablePanoramic) {
                hookPanoramicAllDaySupport()
            }
            if (cfg.blockSingleClick) {
                hookSingleClickWakeUpBlock()
            }
        }
        loadApp(name = OPLUS_AOD) {
            val cfg = AodConfigReader.read()
            if (cfg.enableSettingsSupport) {
                hookAodAllDaySupportSettings()
            }
        }
    }

    private const val SYSTEM_UI = "com.android.systemui"
    private const val OPLUS_AOD = "com.oplus.aod"

}
