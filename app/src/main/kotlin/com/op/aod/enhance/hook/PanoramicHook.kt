package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig

internal object PanoramicHook {

    private val FIELD_NAMES = listOf(
        "isSupportPanoramicAllDay",
        "isSupportPanoramicAllDayByPanelFeature",
        "isSupportPanoramicByPanelFeature",
        "isSupportPanoramic"
    )
    private const val SMOOTH_TRANSITION_CONTROLLER = "com.oplus.systemui.aod.display.SmoothTransitionController"

    fun YukiBaseHooker.hookPanoramicAllDaySupport() {
        val clazz = runCatching {
            SMOOTH_TRANSITION_CONTROLLER.toClass(appClassLoader).resolve()
        }.getOrNull() ?: return

        if (BuildConfig.DEBUG) {
            Log.d("AOD_Enhance", "AOD_PANORAMIC_HOOK: Registered")
        }

        // 一次性字段修正逻辑
        fun applyPanoramicSupport(instance: Any) {
            val cfg = AodConfigReader.read(MainHook.hostAppContext)
            if (!cfg.enablePanoramic) return

            val realClass = instance::class.java
            for (name in FIELD_NAMES) {
                runCatching {
                    val f = realClass.getDeclaredField(name)
                    f.isAccessible = true
                    f.setBoolean(instance, true)
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_PANORAMIC_HOOK: Applied panoramic support")
            }
        }

        // Hook 1: initSmoothTransitionState — 初始化时修正（一次性）
        runCatching {
            clazz.firstMethod { name = "initSmoothTransitionState" }
        }.getOrNull()?.hook {
            after {
                applyPanoramicSupport(instance<Any>())
            }
        }

        // Hook 2: setPanoramicSupportedByRemote — 远程调用时修正
        runCatching {
            clazz.firstMethod { name = "setPanoramicSupportedByRemote" }
        }.getOrNull()?.hook {
            after {
                applyPanoramicSupport(instance<Any>())
            }
        }
    }
}