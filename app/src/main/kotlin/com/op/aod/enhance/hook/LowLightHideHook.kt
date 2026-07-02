package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig

/**
 * 低光环境 AOD 保持显示 Hook。
 *
 * ColorOS 在以下场景自动关闭 AOD：
 * 1. 极暗环境（lux ≤ 0.1）→ 立即关闭，调用 hideAodByDarkLight(1)
 * 2. 夜间低光（lux ≤ 5.0 + 夜间超时）→ 延迟关闭，调用 hideAodByDarkLight(0)
 *    两条路径均在 AodUpdateManager$2.onSensorChanged() → hideAodByDarkLight() 中触发，
 *    最终调用 setIsHideBySpecialRule(true) 标记 AOD 为"特殊规则隐藏"。
 * 3. AOD 显示超过 4 小时超时 → needDisplayAodInSpecialRule() 返回 false，
 *    同时调用 setIsHideBySpecialRule(true)。
 *
 * 关键汇合点：isDisplayModeAllowUpdateClock() 会调用 needDisplayAodInSpecialRule()，
 * 而 needDisplayAodInSpecialRule() 的返回值决定了 AOD 是否继续显示。
 * 如果 mIsHideBySpecialRule == true，needDisplayAodInSpecialRule() 直接返回 false。
 *
 * 本 Hook 采用三重拦截策略（按拦截层级从高到低）：
 *
 * - Hook 1（最优先）: needDisplayAodInSpecialRule → 强制返回 true
 *   这是两条隐藏路径的汇合判断点，直接绕过所有"特殊规则隐藏"检查。
 *
 * - Hook 2: setIsHideBySpecialRule → 拦截 true 参数改为 false
 *   防止 mIsHideBySpecialRule 标记被设置，作为 Hook 1 的补充。
 *
 * - Hook 3: hideAodByDarkLight → 吞掉低光隐藏方法本身
 *   阻止 hideAodByDarkLight 内部的 setIsHideBySpecialRule(true)、
 *   resetAodAlwaysDisplayTime()、setAodNearState(6) 执行，
 *   同时保留 onSensorChanged 中的传感器反注册和日志逻辑。
 *   由于 Hook 1 和 2 已经能兜底，此 Hook 是可选增强。
 */
internal object LowLightHideHook {

    private const val TAG = "AOD_Enhance"

    private const val AOD_UPDATE_MANAGER =
        "com.oplus.systemui.aod.aodclock.off.AodUpdateManager"
    private const val SENSOR_CALLBACK =
        "com.oplus.systemui.aod.aodclock.off.AodUpdateManager\$2"

    fun YukiBaseHooker.hookLowLightAodHide() {
        hookNeedDisplayAodInSpecialRule()
        hookSetIsHideBySpecialRule()
        hookHideAodByDarkLight()
    }

    /**
     * Hook 1（核心）: 拦截 AodUpdateManager.needDisplayAodInSpecialRule()
     *
     * 这是两条隐藏路径的汇合判断点：
     * - 低光隐藏路径：hideAodByDarkLight → setIsHideBySpecialRule(true) →
     *   needDisplayAodInSpecialRule() 检查 mIsHideBySpecialRule 返回 false
     * - 4 小时超时路径：needDisplayAodInSpecialRule() 检查 mCurrentDisplayTime
     *   超过 aodDayDuring 时直接调用 setIsHideBySpecialRule(true) 并返回 false
     *
     * 强制返回 true 后，即使 mIsHideBySpecialRule 被设为 true 或
     * 显示时间超过 4 小时，AOD 仍会继续显示。
     *
     * 此 Hook 是最可靠的拦截点，因为它位于判断链的末端，
     * 不依赖于阻止前面的任何 setter 调用。
     */
    private fun YukiBaseHooker.hookNeedDisplayAodInSpecialRule() {
        runCatching {
            AOD_UPDATE_MANAGER
                .toClass(appClassLoader).resolve()
                .firstMethod { name = "needDisplayAodInSpecialRule" }
                .hook {
                    after {
                        val cfg = AodConfigReader.read(MainHook.hostAppContext)
                        if (cfg.blockLowLightHide) {
                            result = true
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "LowLightHide: forced needDisplayAodInSpecialRule = true")
                            }
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "LowLightHide: needDisplayAodInSpecialRule hook failed, ${it.message}")
            }
        }
    }

    /**
     * Hook 2: 拦截 AodUpdateManager.setIsHideBySpecialRule(boolean)
     *
     * 只阻止参数为 true 的调用（低光隐藏 + 4 小时超时隐藏），放行参数为 false 的调用。
     *
     * setIsHideBySpecialRule(false) 在以下正常恢复场景中被调用，不应被阻止：
     * - AodClockLayout.showAodClock() — AOD 正常显示恢复
     * - AodClockLayout.startShowAod() — AOD 开始显示
     * - BaseAodClockLayoutController.onHide() — AOD 完整重置流程
     *
     * 此 Hook 作为 Hook 1 的补充：即使 needDisplayAodInSpecialRule 的 Hook 因故失效，
     * 也能通过阻止标记被设置来保持 AOD 显示。
     */
    private fun YukiBaseHooker.hookSetIsHideBySpecialRule() {
        runCatching {
            AOD_UPDATE_MANAGER
                .toClass(appClassLoader).resolve()
                .firstMethod {
                    name = "setIsHideBySpecialRule"
                    paramCount = 1
                }
                .hook {
                    before {
                        val cfg = AodConfigReader.read(MainHook.hostAppContext)
                        if (cfg.blockLowLightHide) {
                            val originalValue = args(0).any() as? Boolean ?: return@before
                            if (originalValue) {
                                args(0).set(false)
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "LowLightHide: forced setIsHideBySpecialRule(false), original was true")
                                }
                            }
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "LowLightHide: setIsHideBySpecialRule hook failed, ${it.message}")
            }
        }
    }

    /**
     * Hook 3（可选增强）: 拦截 AodUpdateManager$2.hideAodByDarkLight(int)
     *
     * 直接拦截低光隐藏的核心方法，阻止其内部的全部副作用：
     * - setIsHideBySpecialRule(true)
     * - resetAodAlwaysDisplayTime()
     * - setAodNearState(6)（通知系统 AOD 进入近场状态 6）
     * - 统计上报
     *
     * 与旧方案（吞掉整个 onSensorChanged）不同，此方案仅阻止 hideAodByDarkLight 本身，
     * onSensorChanged 中的传感器反注册（unregisterListener）和日志记录仍正常执行，
     * 避免传感器监听器无法移除导致持续上报和 CPU 浪费。
     *
     * 由于 Hook 1 和 2 已经能兜底，此 Hook 是可选的增强层。
     * 如果此 Hook 因匿名内部类加载顺序等原因注册失败，不影响整体功能。
     */
    private fun YukiBaseHooker.hookHideAodByDarkLight() {
        runCatching {
            SENSOR_CALLBACK
                .toClass(appClassLoader).resolve()
                .firstMethod { name = "hideAodByDarkLight" }
                .hook {
                    before {
                        val cfg = AodConfigReader.read(MainHook.hostAppContext)
                        if (cfg.blockLowLightHide) {
                            result = null
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "LowLightHide: blocked hideAodByDarkLight")
                            }
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "LowLightHide: hideAodByDarkLight hook failed (non-critical), ${it.message}")
            }
        }
    }
}
