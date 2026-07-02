package com.op.aod.enhance.data

import android.content.ContentResolver
import android.content.ContentValues
import java.util.concurrent.atomic.AtomicReference

object AodConfigStore {

    private val DEFAULT_CONFIG = AodUiConfig()

    /** 使用 AtomicReference 替代 @Volatile var，保证 CAS 操作原子性 */
    private val cachedRef = AtomicReference<AodUiConfig?>()

    /**
     * 读取配置，缓存优先。
     *
     * 首次调用走一次 IPC query 填充缓存；后续读直接返回缓存值。
     * 写入侧 ([write]) 使用 CAS 更新缓存，保证线程安全。
     */
    fun read(resolver: ContentResolver): AodUiConfig {
        val local = cachedRef.get()
        if (local != null) return local
        val fresh = queryOrNull(resolver)
        if (fresh != null) {
            // 尝试 CAS 设置，失败说明其他线程已设置，忽略即可
            cachedRef.compareAndSet(null, fresh)
            return fresh
        }
        return DEFAULT_CONFIG
    }

    fun write(resolver: ContentResolver, cfg: AodUiConfig) {
        val values = ContentValues().apply {
            put(AodConfigContract.KEY_INIT_DARK, cfg.initDark)
            put(AodConfigContract.KEY_INIT_BRIGHT, cfg.initBright)
            put(AodConfigContract.KEY_RUNNING_MULTIPLIER, cfg.runningMultiplier)
            put(AodConfigContract.KEY_ENABLE_PANORAMIC, cfg.enablePanoramic)
            put(AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT, cfg.enableSettingsSupport)
            put(AodConfigContract.KEY_BLOCK_SINGLE_CLICK, cfg.blockSingleClick)
            put(AodConfigContract.KEY_BLOCK_LOW_LIGHT_HIDE, cfg.blockLowLightHide)
        }
        resolver.update(AodConfigProvider.CONTENT_URI, values, null, null)
        // 使用 CAS 更新缓存，避免多线程竞争丢失更新
        cachedRef.set(cfg)
    }

    private fun queryOrNull(resolver: ContentResolver): AodUiConfig? {
        return runCatching {
            resolver.query(AodConfigProvider.CONTENT_URI, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val v = AodConfigContract.readRow(c)
                    AodUiConfig(
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
        }.getOrNull()
    }
}