package com.op.aod.enhance.hook

import android.content.Context
import android.net.Uri

internal data class AodConfig(
    val initDark: Int = 80,
    val initBright: Int = 160,
    val runningMultiplier: Float = 1.6f,
    val enablePanoramic: Boolean = true,
    val enableSettingsSupport: Boolean = true,
    val blockSingleClick: Boolean = true,
)

internal object AodConfigReader {
    private val uri: Uri = Uri.parse("content://com.op.aod.enhance.config/aod_config")

    @Volatile
    private var cached: AodConfig = AodConfig()

    @Volatile
    private var loaded: Boolean = false

    fun read(context: Context?): AodConfig {
        if (loaded) return cached
        if (context == null) return cached
        cached = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    AodConfig(
                        initDark = c.getString(0)?.toIntOrNull() ?: 80,
                        initBright = c.getString(1)?.toIntOrNull() ?: 160,
                        runningMultiplier = c.getString(2)?.toFloatOrNull() ?: 1.6f,
                        enablePanoramic = c.getString(3)?.toBooleanStrictOrNull() ?: true,
                        enableSettingsSupport = c.getString(4)?.toBooleanStrictOrNull() ?: true,
                        blockSingleClick = c.getString(5)?.toBooleanStrictOrNull() ?: true,
                    )
                } else AodConfig()
            } ?: AodConfig()
        }.getOrDefault(AodConfig())
        loaded = true
        return cached
    }

    fun read(): AodConfig = read(currentAppContext)

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}
