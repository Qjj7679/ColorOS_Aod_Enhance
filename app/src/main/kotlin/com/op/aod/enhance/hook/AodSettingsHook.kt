package com.op.aod.enhance.hook

import android.content.Context
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig

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
                    val resultValue = if (cfg.enableSettingsSupport) 1 else 0
                    result = resultValue
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_SETTINGS_HOOK: getKeyAodAllDaySupportSettings -> $resultValue (enable=${cfg.enableSettingsSupport})")
                    }
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
