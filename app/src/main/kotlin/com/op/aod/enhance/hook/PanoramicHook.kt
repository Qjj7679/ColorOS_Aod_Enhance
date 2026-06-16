package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import java.lang.reflect.Field

internal object PanoramicHook {

    @Volatile
    private var fieldAllDay: Field? = null
    @Volatile
    private var fieldAllDayByPanel: Field? = null

    /** 反射是否已尝试过（无论成功与否），避免重复反射。 */
    @Volatile
    private var reflectionAttempted = false

    /** 是否已完成写入。true 后热路径直接跳过。 */
    @Volatile
    private var writeDone = false

    fun YukiBaseHooker.hookPanoramicAllDaySupport() {
        if (!AodConfigReader.read(MainHook.hostAppContext).enablePanoramic) {
            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_PANORAMIC_HOOK: disabled by config, skipping hook registration")
            return  // 用户关闭：不注册 hook，零 YukiHookAPI 拦截开销
        }

        SMOOTH_TRANSITION_CONTROLLER_COMPANION
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "getInstance"
            }.hook {
                after {
                    // 首次写入完成后直接跳过，零开销
                    if (writeDone) return@after

                    val instance = result<Any>() ?: return@after

                    // 反射获取字段（仅首次尝试，成功后缓存 Field 对象复用）
                    if (!reflectionAttempted) {
                        runCatching {
                            fieldAllDay = instance::class.java.getDeclaredField("isSupportPanoramicAllDay").apply { isAccessible = true }
                        }
                        runCatching {
                            fieldAllDayByPanel = instance::class.java.getDeclaredField("isSupportPanoramicAllDayByPanelFeature").apply { isAccessible = true }
                        }
                        reflectionAttempted = true
                    }

                    if (fieldAllDay == null || fieldAllDayByPanel == null) {
                        writeDone = true  // 字段不存在，后续不再尝试
                        return@after
                    }

                    // 硬编码 true（已确认配置为开启才会注册此 hook）
                    runCatching { fieldAllDay?.setBoolean(instance, true) }
                    runCatching { fieldAllDayByPanel?.setBoolean(instance, true) }
                    writeDone = true
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_PANORAMIC_HOOK: Set panoramic all day support to true")
                    }
                }
            }
    }

    private const val SMOOTH_TRANSITION_CONTROLLER_COMPANION = "com.oplus.systemui.aod.display.SmoothTransitionController\$Companion"
}
