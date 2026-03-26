package com.op.aod.enhance

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.highcapable.yukihookapi.hook.factory.encase
import com.op.aod.enhance.hook.MainHook

@InjectYukiHookWithXposed(
    sourcePath = "src/main",
    modulePackageName = "com.op.aod.enhance",
    entryClassName = "HookEntryXposed"
)
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase(MainHook)
}
