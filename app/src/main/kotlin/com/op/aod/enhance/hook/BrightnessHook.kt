package com.op.aod.enhance.hook

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import kotlin.math.roundToInt

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
                    val cfg = AodConfigReader.read()
                    val target = if (originalResult < INIT_DARK_THRESHOLD) cfg.initDark else cfg.initBright
                    result = target
                }
            }
    }

    fun YukiBaseHooker.hookRunningBrightnessBoost() {
        BASE_DISPLAY_UTIL
            .toClass(appClassLoader)
            .resolve()
            .firstMethod {
                name = "setDozeScreenBrightness"
                parameters(Float::class, Int::class)
            }.hook {
                before {
                    val originalNit = args(0).any() as? Float ?: return@before
                    val originalBrightness = args(1).any() as? Int ?: return@before

                    val cfg = AodConfigReader.read()
                    val multiplier = cfg.runningMultiplier
                    if (multiplier == 1.0f) return@before

                    val boostedNit = originalNit * multiplier
                    val boostedBrightness = (originalBrightness * multiplier).roundToInt()
                    val clampedBrightness = boostedBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)

                    args(0).set(boostedNit)
                    args(1).set(clampedBrightness)
                }
            }
    }

    private const val OPLUS_DOZE_SERVICE_EX_IMPL = "com.oplus.systemui.aod.OplusDozeServiceExImpl"
    private const val BASE_DISPLAY_UTIL = "com.oplus.systemui.aod.display.BaseDisplayUtil"

    private const val INIT_DARK_THRESHOLD = 40
    private const val MIN_BRIGHTNESS = 0
    private const val MAX_BRIGHTNESS = 255

}
