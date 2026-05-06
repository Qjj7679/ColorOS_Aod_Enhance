package com.op.aod.enhance.data

import android.content.ContentResolver
import android.content.ContentValues

object AodConfigStore {

    @Volatile
    private var cached: AodUiConfig? = null

    /**
     * 读取配置，优先从 Provider 获取最新值，不可用时返回缓存或默认值。
     */
    fun read(resolver: ContentResolver): AodUiConfig {
        val fresh = queryOrNull(resolver)
        if (fresh != null) {
            cached = fresh
            return fresh
        }
        return cached ?: AodUiConfig()
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
                    AodUiConfig(
                        initDark = c.getString(AodConfigContract.COL_INIT_DARK)?.toIntOrNull()
                            ?: AodConfigContract.DEFAULT_INIT_DARK,
                        initBright = c.getString(AodConfigContract.COL_INIT_BRIGHT)?.toIntOrNull()
                            ?: AodConfigContract.DEFAULT_INIT_BRIGHT,
                        runningMultiplier = c.getString(AodConfigContract.COL_RUNNING_MULTIPLIER)?.toFloatOrNull()
                            ?: AodConfigContract.DEFAULT_RUNNING_MULTIPLIER,
                        enablePanoramic = c.getString(AodConfigContract.COL_ENABLE_PANORAMIC)?.toBooleanStrictOrNull()
                            ?: AodConfigContract.DEFAULT_ENABLE_PANORAMIC,
                        enableSettingsSupport = c.getString(AodConfigContract.COL_ENABLE_SETTINGS_SUPPORT)?.toBooleanStrictOrNull()
                            ?: AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT,
                        blockSingleClick = c.getString(AodConfigContract.COL_BLOCK_SINGLE_CLICK)?.toBooleanStrictOrNull()
                            ?: AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK,
                    )
                } else null
            }
        }.getOrNull()
    }
}
