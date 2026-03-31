package com.op.aod.enhance.hook

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper

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
