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
 * 1. 初始亮度修正 — Hook setBrightnessBeforeDozing()，在 AOD 进入时设置合理的初始亮度
 * 2. 运行时亮度倍率 — 应用用户配置的倍率到运行中的 AOD 亮度
 *
 * 运行时亮度倍率 Hook 策略：
 *
 * 统一在 com.android.systemui.doze.DozeService.setDozeScreenBrightness(int) 上应用倍率。
 * 这是所有亮度设置路径的最终汇聚点，所有上游调用最终都透传到此处：
 *
 * - Oplus 全景 AOD：BaseDisplayUtil.setDozeScreenBrightness(float,int) →
 *   OplusDozeServiceExImpl.setDozeScreenBrightness(int) →
 *   DozeService.setDozeScreenBrightness(int)
 * - Oplus Workshop 模式：OplusWorkShopAODLightController.setAodBrightness() →
 *   getOplusDozeService().setDozeScreenBrightness(i) →
 *   OplusDozeServiceExImpl.setDozeScreenBrightness(int) →
 *   DozeService.setDozeScreenBrightness(int)
 * - Oplus Fallback 降级：brightnessSensorListenerForPanoramicAOD$1 →
 *   OplusDozeServiceExImpl.setBrightnessForFallbackStrategy(int) →
 *   DozeService.setDozeScreenBrightness(int)（直接调用，不经 setDozeScreenBrightness）
 * - AOSP 原生 sensor：DozeScreenBrightness.updateBrightnessAndReady() →
 *   DozeBrightnessHostForwarder.setDozeScreenBrightness(int) →
 *   DozeService.setDozeScreenBrightness(int)
 * - AOSP 原生 init：DozeService.onCreate() →
 *   setDozeScreenBrightness(setBrightnessBeforeDozing() 返回值) →
 *   DozeService.setDozeScreenBrightness(int)
 *
 * 之所以不再分别 Hook OplusDozeServiceExImpl 的两个方法：
 * 1. setBrightnessForFallbackStrategy 直接调用 DozeService.setDozeScreenBrightness，
 *    绕过 OplusDozeServiceExImpl.setDozeScreenBrightness，旧主 Hook 漏覆盖此路径
 * 2. AOSP 原生 DozeScreenBrightness 在带光感设备上仍会被启用，
 *    其调用同样绕过 OplusDozeServiceExImpl，旧主 Hook 也漏覆盖此路径
 * 3. 多条路径并发触发时会交替写入 DreamService，导致倍率时灵时不灵、亮度跳变
 *
 * 旧版 ColorOS（< 16.0.5）兼容降级：
 * 若 DozeService 主 Hook 因类结构变化注册失败，自动降级为
 * 旧的双 Hook 方案（OplusDozeServiceExImpl.setDozeScreenBrightness + setBrightnessForFallbackStrategy）。
 * 降级方案与主方案互斥，避免倍率叠加。
 *
 * 与 Init Hook 的协调：
 * DozeService.onCreate() 紧接 setBrightnessBeforeDozing() 的返回值调用本方法，
 * 若不协调，初始亮度会被倍率二次放大（×multiplier 一次已由 Init Hook 决定，
 * 又 ×multiplier 一次由 Running Boost）。协调方式见 [pendingInitBrightness]。
 */
internal object BrightnessHook {

    /**
     * Init Hook 刚设置的初始亮度值，用于让 Running Boost Hook 跳过紧随其后的
     * 首次 setDozeScreenBrightness 调用，避免倍率叠加。
     *
     * 时序保证：DozeService.onCreate 在主线程同步执行：
     *   setBrightnessBeforeDozing() → [Init Hook after 写入 pendingInitBrightness]
     *   → setDozeScreenBrightness(result) → [Running Boost before 读 pendingInitBrightness]
     * 同一线程相邻调用，无并发风险。@Volatile 仅为跨线程可见性保险。
     */
    @Volatile
    private var pendingInitBrightness: Int? = null

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
                    val target = (if (originalResult < INIT_DARK_THRESHOLD) cfg.initDark else cfg.initBright)
                        .coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
                    result = target
                    // 标记本次 init 输出，让紧随其后的 Running Boost 跳过倍率
                    pendingInitBrightness = target
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_INIT_FIX: 原始=$originalResult -> 修正为=$target (pending)")
                    }
                }
            }
    }

    fun YukiBaseHooker.hookRunningBrightnessBoost() {
        // 主方案：Hook DozeService.setDozeScreenBrightness(int) 最终汇聚点
        val mainHooked = runCatching { hookDozeServiceBrightnessBoost() }.isSuccess
        // 旧版 ColorOS（< 16.0.5）兼容降级：仅在主方案注册失败时启用
        if (!mainHooked) {
            runCatching { hookOplusDozeServiceBrightnessBoost() }
            runCatching { hookFallbackBrightnessBoost() }
        }
    }

    /**
     * 主方案：Hook com.android.systemui.doze.DozeService.setDozeScreenBrightness(int)
     *
     * 所有亮度路径的最终汇聚点。在此处应用倍率可同时覆盖：
     * - Oplus 全景/Workshop 主路径
     * - Oplus Fallback 降级路径
     * - AOSP 原生 sensor 路径
     * - AOSP 原生 init 路径（与 Init Hook 协调跳过）
     */
    private fun YukiBaseHooker.hookDozeServiceBrightnessBoost() {
        DOZE_SERVICE
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "setDozeScreenBrightness"
                parameters(Int::class)
            }.hook {
                before {
                    val originalBrightness = args(0).any() as? Int ?: return@before

                    // 与 Init Hook 协调：若是 Init Hook 刚输出的初始亮度，跳过倍率
                    // 避免初始亮度被二次放大
                    val pending = pendingInitBrightness
                    if (pending != null && originalBrightness == pending) {
                        pendingInitBrightness = null
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_RUNNING_BOOST: skip init brightness=$originalBrightness")
                        }
                        return@before
                    }

                    val cfg = AodConfigReader.read(MainHook.hostAppContext)
                    val multiplier = cfg.runningMultiplier
                    if (multiplier == 1.0f) return@before

                    val boostedBrightness = (originalBrightness * multiplier).roundToInt()
                    val clampedBrightness = boostedBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)

                    args(0).set(clampedBrightness)
                    if (BuildConfig.DEBUG) {
                        Log.d("AOD_Enhance", "AOD_RUNNING_BOOST(DozeService): $originalBrightness -> $clampedBrightness")
                    }
                }
            }
    }

    /**
     * 降级方案 1：Hook OplusDozeServiceExImpl.setDozeScreenBrightness(int)
     *
     * 旧版 ColorOS 兼容路径，仅在主方案注册失败时启用。
     * 覆盖范围：Oplus 全景 AOD + Workshop 模式。
     * 不覆盖：Fallback 降级路径（直接调用 DozeService）和 AOSP 原生 sensor 路径。
     */
    private fun YukiBaseHooker.hookOplusDozeServiceBrightnessBoost() {
        OPLUS_DOZE_SERVICE_EX_IMPL
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "setDozeScreenBrightness"
                parameters(Int::class)
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
                        Log.d("AOD_Enhance", "AOD_RUNNING_BOOST(OplusDozeService): $originalBrightness -> $clampedBrightness")
                    }
                }
            }
    }

    /**
     * 降级方案 2：Hook OplusDozeServiceExImpl.setBrightnessForFallbackStrategy(int)
     *
     * 独立的降级亮度路径，直接调用 DozeService.setDozeScreenBrightness(i)，
     * 不经过 OplusDozeServiceExImpl.setDozeScreenBrightness(int)，需单独应用倍率。
     * 仅在主方案注册失败时启用，与降级方案 1 互斥（不会与 Oplus 路径叠加）。
     */
    private fun YukiBaseHooker.hookFallbackBrightnessBoost() {
        OPLUS_DOZE_SERVICE_EX_IMPL
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "setBrightnessForFallbackStrategy"
                parameters(Int::class)
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
                        Log.d("AOD_Enhance", "AOD_RUNNING_BOOST(setBrightnessForFallbackStrategy): $originalBrightness -> $clampedBrightness")
                    }
                }
            }
    }

    private const val OPLUS_DOZE_SERVICE_EX_IMPL = "com.oplus.systemui.aod.OplusDozeServiceExImpl"
    private const val DOZE_SERVICE = "com.android.systemui.doze.DozeService"

    private const val INIT_DARK_THRESHOLD = 40
    private const val MIN_BRIGHTNESS = 0
    private const val MAX_BRIGHTNESS = 255

}
