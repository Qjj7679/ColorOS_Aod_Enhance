package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * AOD 单击唤醒屏蔽 Hook。
 *
 * 屏蔽 AOD 场景下的单击误触唤醒，仅允许双击唤醒。
 *
 * 覆盖两条触摸事件分发路径：
 *
 * - 路径 A（黑屏手势服务上报）：3 个 onClick 回调
 *   （NormalAod / PanoramicAod / WakeUpController）各自维护独立的
 *   [lastBlockedTime]，用 350ms 双击计时判据避免误放行。
 *
 * - 路径 B（AOD 视图触摸事件）：OplusDoubleClickSleep.OnDoubleClickListener
 *   .onSingleTapConfirmed() — GestureDetector 已确认"不是双击"后的回调，
 *   直接拦截即可，无需双击计时。
 */
internal object SingleClickBlockHook {

    /** 双击间隔阈值（ms）。 */
    private const val DOUBLE_CLICK_THRESHOLD = 350L

    private const val DOUBLE_CLICK_LISTENER =
        "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep\$OnDoubleClickListener"

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        if (!AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: disabled by config")
            return
        }

        // 路径 A：黑屏手势服务上报的 3 个 onClick 回调
        val targets = arrayOf(
            "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback" to "NormalAod",
            "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback" to "PanoramicAod",
            "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback" to "WakeUpController",
        )

        for ((cls, label) in targets) {
            registerClickHook(cls, label)
        }

        // 路径 B：AOD 视图触摸事件经由 GestureDetector 判定的单击确认
        hookDoubleClickSleepSingleTap()
    }

    /**
     * Hook OplusDoubleClickSleep.OnDoubleClickListener.onSingleTapConfirmed(MotionEvent)
     *
     * 覆盖路径 B：AOD 视图触摸事件分发链。
     *
     * 触摸事件从 AodBlackLayout/AodRootLayout.onTouchEvent() 进入
     * OplusDoubleClickSleep.onTouchEvent()，由 GestureDetector 判定后回调：
     * - 双击 → onDoubleTap() → wakeUp / goToSleep（**不 Hook**，确保双击正常）
     * - 单击确认 → onSingleTapConfirmed() → processPanoramicWakeup() → wakeUp
     *
     * onSingleTapConfirmed 是 GestureDetector 在确认"这不是双击"之后才调用的，
     * 因此直接 result = false 即可，无需像 onClick Hook 那样做双击计时。
     */
    private fun YukiBaseHooker.hookDoubleClickSleepSingleTap() {
        runCatching {
            DOUBLE_CLICK_LISTENER
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "onSingleTapConfirmed" }
                .hook {
                    before {
                        val cfg = AodConfigReader.read(MainHook.hostAppContext)
                        if (cfg.blockSingleClick) {
                            result = false
                            if (BuildConfig.DEBUG) {
                                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: DoubleClickListener blocked (view touch path)")
                            }
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: DoubleClickListener onSingleTapConfirmed not available, ${it.message}")
            }
        }
    }

    /**
     * 为单个目标类注册 onClick 拦截 Hook。
     *
     * 每个目标拥有独立的 [lastBlockedTime]，避免不同手势区域之间
     * 的单击/双击状态互相干扰（如 A 区域单击后，B 区域在 350ms 内
     * 被触发不会错误放行）。
     */
    private fun YukiBaseHooker.registerClickHook(targetClass: String, label: String) {
        val lastBlockedTime = AtomicLong(0L)
        runCatching {
            targetClass
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "onClick" }
                .hook {
                    before {
                        val now = System.currentTimeMillis()
                        val prev = lastBlockedTime.get()

                        if (prev != 0L && now - prev < DOUBLE_CLICK_THRESHOLD) {
                            // 放行：快速第二次点击（双击）
                            lastBlockedTime.set(0L)
                            if (BuildConfig.DEBUG) {
                                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label allowed (double-click)")
                            }
                            return@before
                        }

                        // 拦截：首次单击或慢速重试
                        lastBlockedTime.set(now)
                        result = null
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label blocked")
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label onClick not available, ${it.message}")
            }
        }
    }
}
