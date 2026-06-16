package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig

internal object SingleClickBlockHook {

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        if (!AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: disabled by config, skipping hook registration")
            return  // 用户关闭：不注册任何 hook，零 YukiHookAPI 拦截开销
        }

        hookSingleClickBlock(
            targetClass = "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback",
            label = "NormalAodSingleClick"
        )
        hookSingleClickBlock(
            targetClass = "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback",
            label = "PanoramicAodSingleClick"
        )
        hookSingleClickBlock(
            targetClass = "com.oplus.systemui.aod.scene.AodSceneViewHolder\$AodSceneGestureCallback",
            label = "SceneAodSingleClick"
        )
        hookSingleClickBlock(
            targetClass = "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback",
            label = "WakeUpControllerSingleClick"
        )
    }

    /** 通用单击唤醒屏蔽 Hook。通过 [targetClass] 定位不同 AOD 场景的回调。 */
    private fun YukiBaseHooker.hookSingleClickBlock(targetClass: String, label: String) {
        runCatching {
            targetClass
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "isSupportGesture"
                    parameters(Int::class)
                }.hook {
                    before {
                        // 热路径：仅 int 对比，无 IPC 无 volatile 无反射
                        val gesture = args(0).any() as? Int ?: return@before
                        if (gesture == GESTURE_SINGLE_CLICK) {
                            result = false
                            if (BuildConfig.DEBUG) {
                                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label blocked (gesture=$gesture)")
                            }
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label not available, ${it.message}")
            }
        }
    }

    private const val GESTURE_SINGLE_CLICK = 16
}
