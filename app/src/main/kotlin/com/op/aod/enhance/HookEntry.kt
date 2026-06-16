package com.op.aod.enhance

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.YukiHookAPI
import com.op.aod.enhance.hook.MainHook

@InjectYukiHookWithXposed(
    sourcePath = "src/main",
    modulePackageName = "com.op.aod.enhance",
    entryClassName = "HookEntryXposed"
)
object HookEntry : IYukiHookXposedInit {

    override fun onInit() {
        // Configure YukiHookAPI for logging
        YukiHookAPI.configs {
            debugLog {
                tag = "AOD_Enhance"
                isEnable = BuildConfig.DEBUG
                isRecord = false
                // Show basic log elements
                elements(TAG, PRIORITY)
            }
            // Enable debug mode to see logs
            isDebug = BuildConfig.DEBUG
        }
    }

    override fun onHook() = encase(MainHook)

}