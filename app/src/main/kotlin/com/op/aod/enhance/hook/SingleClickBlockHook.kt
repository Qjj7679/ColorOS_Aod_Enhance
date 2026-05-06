package com.op.aod.enhance.hook

import android.content.Context
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.log.YLog
import com.op.aod.enhance.BuildConfig

internal object SingleClickBlockHook {

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        hookSafely("NormalAodSingleClick") { hookNormalAodSingleClickBlock() }
        hookSafely("PanoramicAodSingleClick") { hookPanoramicAodSingleClickBlock() }
        hookSafely("SceneAodSingleClick") { hookSceneAodSingleClickBlock() }
        hookSafely("WakeUpControllerSingleClick") { hookWakeUpControllerSingleClickBlock() }
    }

    private inline fun hookSafely(tag: String, block: () -> Unit) {
        runCatching { block() }.onFailure {
            if (BuildConfig.DEBUG) {
                YLog.debug("AOD_SINGLE_CLICK_BLOCK: skip $tag, ${it.message}")
            }
        }
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
                    val cfg = AodConfigReader.read(currentAppContext)
                    if (cfg.blockSingleClick && gesture == GESTURE_SINGLE_CLICK) {
                        result = false
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Normal AOD single click blocked (gesture=$gesture)")
                        }
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
                    val cfg = AodConfigReader.read(currentAppContext)
                    if (cfg.blockSingleClick && gesture == GESTURE_SINGLE_CLICK) {
                        result = false
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Panoramic AOD single click blocked (gesture=$gesture)")
                        }
                    }
                }
            }
    }

    private fun YukiBaseHooker.hookSceneAodSingleClickBlock() {
        SCENE_AOD_SINGLE_CLICK_CALLBACK
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "isSupportGesture"
                parameters(Int::class)
            }.hook {
                before {
                    val gesture = args(0).any() as? Int ?: return@before
                    val cfg = AodConfigReader.read(currentAppContext)
                    if (cfg.blockSingleClick && gesture == GESTURE_SINGLE_CLICK) {
                        result = false
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Scene AOD single click blocked (gesture=$gesture)")
                        }
                    }
                }
            }
    }

    private fun YukiBaseHooker.hookWakeUpControllerSingleClickBlock() {
        WAKEUP_CONTROLLER_SINGLE_CLICK_CALLBACK
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "isSupportGesture"
                parameters(Int::class)
            }.hook {
                before {
                    val gesture = args(0).any() as? Int ?: return@before
                    val cfg = AodConfigReader.read(currentAppContext)
                    if (cfg.blockSingleClick && gesture == GESTURE_SINGLE_CLICK) {
                        result = false
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: WakeUpController single click blocked (gesture=$gesture)")
                        }
                    }
                }
            }
    }

    private const val GESTURE_SINGLE_CLICK = 16
    private const val NORMAL_AOD_SINGLE_CLICK_CALLBACK =
        "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback"
    private const val PANORAMIC_AOD_SINGLE_CLICK_CALLBACK =
        "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback"
    private const val SCENE_AOD_SINGLE_CLICK_CALLBACK =
        "com.oplus.systemui.aod.scene.AodSceneViewHolder\$AodSceneGestureCallback"
    private const val WAKEUP_CONTROLLER_SINGLE_CLICK_CALLBACK =
        "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback"

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()

}
