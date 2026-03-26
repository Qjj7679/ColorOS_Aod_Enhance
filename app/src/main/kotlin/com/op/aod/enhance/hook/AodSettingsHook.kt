package com.op.aod.enhance.hook

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass

internal object AodSettingsHook {

    fun YukiBaseHooker.hookAodAllDaySupportSettings() {
        SETTINGS_UTILS
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "getKeyAodAllDaySupportSettings"
                parameters(Context::class, Int::class)
            }.hook {
                after {
                    val cfg = AodConfigReader.read(currentAppContext)
                    result = if (cfg.enableSettingsSupport) 1 else 0
                }
            }
    }

    private const val SETTINGS_UTILS = "com.oplus.aod.util.SettingsUtils"

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull()

}
