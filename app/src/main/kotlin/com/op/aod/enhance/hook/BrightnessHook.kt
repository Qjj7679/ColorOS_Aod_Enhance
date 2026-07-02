package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import kotlin.math.roundToInt

/**
 * AOD 亮度修正 Hook。
 *
 * 负责两部分功能：
 * 1. 初始亮度修正 — Hook setBrightnessBeforeDozing()，在 AOD 进入时设置合理的初始亮度
 * 2. 运行时亮度倍率 — 对运行中的 AOD 亮度应用用户配置的倍率
 *
 * 关于双重叠加问题：
 * BaseDisplayUtil.setDozeScreenBrightness(float, int) 内部最终会调用
 * OplusDozeServiceExImpl.setDozeScreenBrightness(int)。
 * 如果两者都被 Hook 修改倍率，亮度值会被平方计算（如 1.6 × 1.6 = 2.56）。
 *
 * 解决方案：只对 BaseDisplayUtil 的入口做倍率修改（before 阶段修改参数），
 * 不再对 OplusDozeServiceExImpl.setDozeScreenBrightness(int) 做倍率 Hook。
 * setBrightnessForFallbackStrategy(int) 仍保留 Hook，因为它是独立的调用路径，
 * 不经过 BaseDisplayUtil。
 */
internal object BrightnessHook {

    fun YukiBaseHooker.hookInitBrightnessFix() {
        OPLUS_DOZE_SERVICE_EX_IMPL
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "setBrightnessBeforeDozing"
                emptyParameters()
            }.hook {
                after {
                    val originalResult = result<Int>() ?: return@after
                    // -1 表示非全景 AOD，上游不会使用此返回值，不应替换
                    if (originalResult == -1) return@after
                    val cfg = AodConfigReader.read(MainHook.hostAppContext)
                    val target = if (originalResult < INIT_DARK_THRESHOLD) cfg.initDark else cfg.initBright
                    result = target
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_INIT_FIX: 原始=$originalResult -> 修正为=$target")
                    }
                }
            }
    }

    fun YukiBaseHooker.hookRunningBrightnessBoost() {
        runCatching {
            // BaseDisplayUtil 入口：统一修改 nit + brightness 参数
            // 这是运行时亮度调整的主入口，内部会调用
            // OplusDozeServiceExImpl.setDozeScreenBrightness(int)
            // 所以不再单独 Hook 那个方法，避免双重叠加
            hookBaseDisplayBrightnessBoost()
            // setBrightnessForFallbackStrategy 是独立的降级路径，
            // 不经过 BaseDisplayUtil，需要单独 Hook
            runCatching { hookFallbackBrightnessBoost() }
        }
    }

    /**
     * Hook BaseDisplayUtil.setDozeScreenBrightness(float, int)
     *
     * 这是运行时亮度调整的主入口。在 before 阶段修改 nit 和 brightness 参数，
     * BaseDisplayUtil 内部会将修改后的值传递给 OplusDozeServiceExImpl，
     * 因此不需要再 Hook OplusDozeServiceExImpl.setDozeScreenBrightness(int)，
     * 避免亮度被乘以倍率两次（双重叠加）。
     */
    private fun YukiBaseHooker.hookBaseDisplayBrightnessBoost() {
        BASE_DISPLAY_UTIL
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "setDozeScreenBrightness"
            }.hook {
                before {
                    val originalNit = args(0).any() as? Float ?: return@before
                    val originalBrightness = args(1).any() as? Int ?: return@before

                    val cfg = AodConfigReader.read(MainHook.hostAppContext)
                    val multiplier = cfg.runningMultiplier
                    if (multiplier == 1.0f) return@before

                    val boostedNit = originalNit * multiplier
                    val boostedBrightness = (originalBrightness * multiplier).roundToInt()
                    val clampedBrightness = boostedBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)

                    args(0).set(boostedNit)
                    args(1).set(clampedBrightness)
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_RUNNING_BOOST(BaseDisplay): $originalBrightness -> $clampedBrightness")
                    }
                }
            }
    }

    /**
     * Hook OplusDozeServiceExImpl.setBrightnessForFallbackStrategy(int)
     *
     * 这是独立的降级亮度路径，不经过 BaseDisplayUtil，
     * 因此需要单独应用倍率。旧版 ColorOS 可能存在拼写为
     * setBrightness4FallbackStrategy 的版本，一并尝试 Hook。
     */
    private fun YukiBaseHooker.hookFallbackBrightnessBoost() {
        val fallbackMethodNames = listOf(
            "setBrightnessForFallbackStrategy",
            "setBrightness4FallbackStrategy" // 旧版 ColorOS 兼容
        )
        for (methodName in fallbackMethodNames) {
            runCatching {
                OPLUS_DOZE_SERVICE_EX_IMPL
                    .toClass(appClassLoader)
                    .resolve()
                    .firstMethod {
                        name = methodName
                    }.hook {
                        before {
                            val originalBrightness = args(0).any() as? Int ?: return@before
                            val cfg = AodConfigReader.read(MainHook.hostAppContext)
                            val multiplier = cfg.runningMultiplier
                            if (multiplier == 1.0f) return@before

                            val boostedBrightness = (originalBrightness * multiplier).roundToInt()
                            val clampedBrightness = boostedBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)

                            args(0).set(clampedBrightness)
                            if (BuildConfig.DEBUG) {
                                Log.d("AOD_Enhance", "AOD_RUNNING_BOOST($methodName): $originalBrightness -> $clampedBrightness")
                            }
                        }
                    }
            }
        }
    }

    private const val OPLUS_DOZE_SERVICE_EX_IMPL = "com.oplus.systemui.aod.OplusDozeServiceExImpl"
    private const val BASE_DISPLAY_UTIL = "com.oplus.systemui.aod.display.BaseDisplayUtil"

    private const val INIT_DARK_THRESHOLD = 40
    private const val MIN_BRIGHTNESS = 0
    private const val MAX_BRIGHTNESS = 255

}
