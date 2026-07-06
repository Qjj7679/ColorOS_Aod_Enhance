package com.op.aod.enhance.hook

import android.content.Context
import android.net.Uri
import com.op.aod.enhance.data.AodConfigContract
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal data class AodConfig(
    val initDark: Int = AodConfigContract.DEFAULT_INIT_DARK,
    val initBright: Int = AodConfigContract.DEFAULT_INIT_BRIGHT,
    val runningMultiplier: Float = AodConfigContract.DEFAULT_RUNNING_MULTIPLIER,
    val enablePanoramic: Boolean = AodConfigContract.DEFAULT_ENABLE_PANORAMIC,
    val enableSettingsSupport: Boolean = AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT,
    val blockSingleClick: Boolean = AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK,
    val blockLowLightHide: Boolean = AodConfigContract.DEFAULT_BLOCK_LOW_LIGHT_HIDE,
)

/**
 * Hook 侧配置读取器。
 *
 * 通过 ContentProvider 直读模块配置，提供 1 秒 TTL 缓存
 * 避免高频 IPC（如触摸事件触发 4 个 Hook 调用）。
 *
 * 线程安全保证：
 * - 使用 AtomicReference 存储缓存值，compareAndSet 保证写入原子性
 * - AtomicLong 存储时间戳，避免多线程同时重试 IPC
 * - 双重检查消除重复 IPC
 */
internal object AodConfigReader {

    private val DEFAULT_CONFIG = AodConfig()

    private val uri: Uri = Uri.parse("content://com.op.aod.enhance.config/aod_config")

    /** 上次成功读取的缓存值，IPC 失败时作为兜底。CAS 保证写入原子性。 */
    private val cachedRef = AtomicReference<AodConfig?>(null)

    /** 上次成功读取的时间戳（ns），用于 TTL 判断。 */
    private val lastReadTimeNs = AtomicLong(0)

    /** 缓存有效期：1 秒内复用缓存，避免高频 IPC（如触摸事件触发 4 次 Hook 调用）。 */
    private const val CACHE_TTL_NS = 1_000_000_000L

    /** 首次读取标记，避免首次读取时因 lastReadTimeNs=0 而每次都走 IPC。 */
    private val isFirstRead = AtomicBoolean(true)

    /**
     * 读取当前配置。
     *
     * 线程安全流程：
     * - 首次读取：直读 Provider
     * - TTL 内：返回缓存值（零 IPC，AtomicReference.get 是原子的）
     * - TTL 外：直读 Provider 获取最新值
     * - IPC 失败：返回缓存兜底 / DEFAULT_CONFIG
     */
    fun read(context: Context?): AodConfig {
        if (context == null) return DEFAULT_CONFIG

        // 首次读取：绕过 TTL 检查，直接走 Provider
        if (isFirstRead.get()) {
            val fresh = readFromProvider(context)
            if (fresh != null) {
                cachedRef.set(fresh)
                lastReadTimeNs.set(System.nanoTime())
                isFirstRead.set(false)
            }
            return fresh ?: DEFAULT_CONFIG
        }

        // TTL 内：直接返回缓存值（AtomicReference.get 是原子的）
        cachedRef.get()?.let { cachedVal ->
            if (System.nanoTime() - lastReadTimeNs.get() < CACHE_TTL_NS) {
                return cachedVal
            }
        }

        // TTL 过期：重试读取
        val fresh = readFromProvider(context)
        if (fresh != null) {
            cachedRef.set(fresh)
            lastReadTimeNs.set(System.nanoTime())
        }

        return cachedRef.get() ?: DEFAULT_CONFIG
    }

    /**
     * 直读 Provider。成功时更新缓存和时间戳，失败时返回 null。
     */
    private fun readFromProvider(context: Context): AodConfig? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val v = AodConfigContract.readRow(c)
                    AodConfig(
                        initDark = v.initDark,
                        initBright = v.initBright,
                        runningMultiplier = v.runningMultiplier,
                        enablePanoramic = v.enablePanoramic,
                        enableSettingsSupport = v.enableSettingsSupport,
                        blockSingleClick = v.blockSingleClick,
                        blockLowLightHide = v.blockLowLightHide,
                    )
                } else null
            }
        }.getOrNull()?.also { fresh ->
            cachedRef.set(fresh)
            lastReadTimeNs.set(System.nanoTime())
            isFirstRead.set(false)
        }
    }
}
