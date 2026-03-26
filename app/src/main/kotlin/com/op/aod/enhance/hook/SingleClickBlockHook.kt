package com.op.aod.enhance.hook

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass

internal object SingleClickBlockHook {

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        hookNormalAodSingleClickBlock()
        hookPanoramicAodSingleClickBlock()
    }

    private fun YukiBaseHooker.hookNormalAodSingleClickBlock() {
        NORMAL_AOD_SINGLE_CLICK_CALLBACK
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "isSupportGesture"
                parameters(Int::class)
            }.hook {
                before {
                    val gesture = args(0).any() as? Int ?: return@before
                    val cfg = AodConfigReader.read()
                    if (cfg.blockSingleClick && gesture == GESTURE_SINGLE_CLICK) {
                        result = false
                    }
                }
            }
    }

    private fun YukiBaseHooker.hookPanoramicAodSingleClickBlock() {
        PANORAMIC_AOD_SINGLE_CLICK_CALLBACK
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "isSupportGesture"
                parameters(Int::class)
            }.hook {
                before {
                    val gesture = args(0).any() as? Int ?: return@before
                    val cfg = AodConfigReader.read()
                    if (cfg.blockSingleClick && gesture == GESTURE_SINGLE_CLICK) {
                        result = false
                    }
                }
            }
    }

    private const val GESTURE_SINGLE_CLICK = 16
    private const val NORMAL_AOD_SINGLE_CLICK_CALLBACK =
        "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback"
    private const val PANORAMIC_AOD_SINGLE_CLICK_CALLBACK =
        "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback"

}
