package com.op.aod.enhance.data

import android.content.ContentResolver
import android.content.ContentValues

object AodConfigStore {

    private val DEFAULT_CONFIG = AodUiConfig()

    @Volatile
    private var cached: AodUiConfig? = null

    /**
     * 读取配置，缓存优先。
     *
     * 首次调用走一次 IPC query 填充缓存；后续读直接返回缓存值。
     * 写入侧 ([write]) 同步更新缓存，保证读写一致性。
     */
    fun read(resolver: ContentResolver): AodUiConfig {
        val local = cached
        if (local != null) return local
        val fresh = queryOrNull(resolver)
        if (fresh != null) {
            cached = fresh
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
        }
        resolver.update(AodConfigProvider.CONTENT_URI, values, null, null)
        cached = cfg
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
                    )
                } else null
            }
        }.getOrNull()
    }
}