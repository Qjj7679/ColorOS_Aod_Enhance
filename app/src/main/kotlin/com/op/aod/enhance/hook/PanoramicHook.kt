package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import android.content.Context
import java.lang.reflect.Field

internal object PanoramicHook {

    private var fieldAllDay: Field? = null
    private var fieldAllDayByPanel: Field? = null

    fun YukiBaseHooker.hookPanoramicAllDaySupport() {
        SMOOTH_TRANSITION_CONTROLLER_COMPANION
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "getInstance"
            }.hook {
                after {
                    val cfg = AodConfigReader.read(currentAppContext)
                    if (!cfg.enablePanoramic) return@after
                    val instance = result<Any>() ?: return@after
                    if (fieldAllDay == null || fieldAllDayByPanel == null) {
                        runCatching {
                            fieldAllDay = instance::class.java.getDeclaredField("isSupportPanoramicAllDay").apply { isAccessible = true }
                        }
                        runCatching {
                            fieldAllDayByPanel = instance::class.java.getDeclaredField("isSupportPanoramicAllDayByPanelFeature").apply { isAccessible = true }
                        }
                    }
                    runCatching { fieldAllDay?.setBoolean(instance, true) }
                    runCatching { fieldAllDayByPanel?.setBoolean(instance, true) }
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_PANORAMIC_HOOK: Set panoramic all day support to true (enabled=${cfg.enablePanoramic})")
                    }
                }
            }
    }

    private const val SMOOTH_TRANSITION_CONTROLLER_COMPANION = "com.oplus.systemui.aod.display.SmoothTransitionController\$Companion"

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}
