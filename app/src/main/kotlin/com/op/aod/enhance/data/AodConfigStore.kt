package com.op.aod.enhance.data

import android.content.ContentResolver
import android.content.ContentValues

object AodConfigStore {

    fun read(resolver: ContentResolver): AodUiConfig {
        return runCatching {
            resolver.query(AodConfigProvider.CONTENT_URI, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    AodUiConfig(
                        initDark = c.getString(0)?.toIntOrNull() ?: 80,
                        initBright = c.getString(1)?.toIntOrNull() ?: 160,
                        runningMultiplier = c.getString(2)?.toFloatOrNull() ?: 1.6f,
                        enablePanoramic = c.getString(3)?.toBooleanStrictOrNull() ?: true,
                        enableSettingsSupport = c.getString(4)?.toBooleanStrictOrNull() ?: true,
                        blockSingleClick = c.getString(5)?.toBooleanStrictOrNull() ?: true,
                    )
                } else AodUiConfig()
            } ?: AodUiConfig()
        }.getOrDefault(AodUiConfig())
    }

    fun write(resolver: ContentResolver, cfg: AodUiConfig) {
        val values = ContentValues().apply {
            put("init_brightness_dark", cfg.initDark)
            put("init_brightness_bright", cfg.initBright)
            put("running_brightness_multiplier", cfg.runningMultiplier)
            put("enable_panoramic", cfg.enablePanoramic)
            put("enable_settings_support", cfg.enableSettingsSupport)
            put("block_single_click", cfg.blockSingleClick)
        }
        resolver.update(AodConfigProvider.CONTENT_URI, values, null, null)
    }
}
