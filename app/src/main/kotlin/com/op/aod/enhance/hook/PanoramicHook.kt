package com.op.aod.enhance.hook

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
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
                    val cfg = AodConfigReader.read()
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
                }
            }
    }

    private const val SMOOTH_TRANSITION_CONTROLLER_COMPANION = "com.oplus.systemui.aod.display.SmoothTransitionController\$Companion"
}
