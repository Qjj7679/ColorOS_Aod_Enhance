package com.op.aod.enhance.hook

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.op.aod.enhance.data.AodConfigContract

/**
 * Hook 侧配置镜像，与 [com.op.aod.enhance.data.AodUiConfig] 保持字段同步。
 * 新增/修改字段时需同步更新：AodUiConfig, AodConfigContract。
 */
internal data class AodConfig(
    val initDark: Int = AodConfigContract.DEFAULT_INIT_DARK,
    val initBright: Int = AodConfigContract.DEFAULT_INIT_BRIGHT,
    val runningMultiplier: Float = AodConfigContract.DEFAULT_RUNNING_MULTIPLIER,
    val enablePanoramic: Boolean = AodConfigContract.DEFAULT_ENABLE_PANORAMIC,
    val enableSettingsSupport: Boolean = AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT,
    val blockSingleClick: Boolean = AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK,
)

internal object AodConfigReader {
    private val uri: Uri = Uri.parse("content://com.op.aod.enhance.config/aod_config")

    @Volatile
    private var cached: AodConfig? = null

    @Volatile
    private var observerRegistered: Boolean = false

    private var observer: ContentObserver? = null

    fun startObserve(context: Context?) {
        if (context == null || observerRegistered) return
        observerRegistered = true
        cached = readFromProvider(context)
        val handler = Handler(Looper.getMainLooper())
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                cached = readFromProvider(context)
            }
        }.also {
            context.contentResolver.registerContentObserver(uri, true, it)
        }
    }

    fun read(context: Context?): AodConfig {
        if (context == null) return AodConfig()
        val local = cached
        if (local != null) return local
        val fresh = readFromProvider(context)
        cached = fresh
        return fresh
    }

    fun read(): AodConfig = read(currentAppContext)

    fun currentContext(): Context? = currentAppContext

    private fun readFromProvider(context: Context): AodConfig {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    AodConfig(
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
                } else AodConfig()
            } ?: AodConfig()
        }.getOrDefault(AodConfig())
    }

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}
