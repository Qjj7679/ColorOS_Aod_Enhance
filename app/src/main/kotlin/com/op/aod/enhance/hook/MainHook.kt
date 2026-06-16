package com.op.aod.enhance.hook

import android.content.Context
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.op.aod.enhance.BuildConfig
import com.op.aod.enhance.hook.AodSettingsHook.hookAodAllDaySupportSettings
import com.op.aod.enhance.hook.BrightnessHook.hookInitBrightnessFix
import com.op.aod.enhance.hook.BrightnessHook.hookRunningBrightnessBoost
import com.op.aod.enhance.hook.PanoramicHook.hookPanoramicAllDaySupport
import com.op.aod.enhance.hook.SingleClickBlockHook.hookSingleClickWakeUpBlock

/**
 * OP AOD Enhance - 主入口
 *
 * 仅负责按包名分发到各功能 Hook，
 * 并为所有 Hook 提供统一的 [hostAppContext] 缓存。
 */
object MainHook : YukiBaseHooker() {

    /**
     * 当前宿主进程的 Application Context，仅初始化一次。
     *
     * 所有 Hook 应通过 [hostAppContext] 获取 Context，而非自行反射获取，
     * 以避免每帧重复反射调用造成的对象分配和 CPU 开销。
     */
    val hostAppContext: Context? by lazy {
        runCatching {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            Class.forName("android.app.AppGlobals")
                .getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull().also {
            if (it == null && BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "WARN: hostAppContext is null after both fallback paths")
            }
        }
    }

    override fun onHook() {
        loadApp(name = SYSTEM_UI) {
            runCatching { hookInitBrightnessFix() }
            runCatching { hookRunningBrightnessBoost() }
            runCatching { hookPanoramicAllDaySupport() }
            runCatching { hookSingleClickWakeUpBlock() }
        }
        loadApp(name = OPLUS_AOD) {
            runCatching { hookAodAllDaySupportSettings() }
        }
    }

    private const val SYSTEM_UI = "com.android.systemui"
    private const val OPLUS_AOD = "com.oplus.aod"

}