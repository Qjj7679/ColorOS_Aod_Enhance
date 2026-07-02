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
import com.op.aod.enhance.hook.LowLightHideHook.hookLowLightAodHide

/**
 * OP AOD Enhance - 主入口
 *
 * 仅负责按包名分发到各功能 Hook，
 * 并为所有 Hook 提供统一的 [hostAppContext]。
 */
object MainHook : YukiBaseHooker() {

    /**
     * 当前宿主进程的 Application Context。
     *
     * 使用可重试的获取机制而非 lazy 缓存，避免进程启动早期
     * `ActivityThread.currentApplication()` 返回 null 时
     * null 被永久缓存的问题。
     *
     * 一旦成功获取非 null 值，后续调用直接返回缓存值（零开销）。
     * 所有 Hook 应通过 [hostAppContext] 获取 Context。
     */
    val hostAppContext: Context?
        get() {
            _cachedContext?.let { return it }
            val ctx = fetchContext()
            if (ctx != null) _cachedContext = ctx
            return ctx
        }

    @Volatile
    private var _cachedContext: Context? = null

    /** 反射获取 Application Context，双重降级策略。 */
    private fun fetchContext(): Context? {
        return runCatching {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            Class.forName("android.app.AppGlobals")
                .getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
    }

    override fun onHook() {
        loadApp(name = SYSTEM_UI) {
            runCatching { hookInitBrightnessFix() }
            runCatching { hookRunningBrightnessBoost() }
            runCatching { hookPanoramicAllDaySupport() }
            runCatching { hookSingleClickWakeUpBlock() }
            runCatching { hookLowLightAodHide() }
        }
        loadApp(name = OPLUS_AOD) {
            runCatching { hookAodAllDaySupportSettings() }
        }
    }

    private const val SYSTEM_UI = "com.android.systemui"
    private const val OPLUS_AOD = "com.oplus.aod"

}