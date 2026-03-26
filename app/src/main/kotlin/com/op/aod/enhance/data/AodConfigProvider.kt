package com.op.aod.enhance.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class AodConfigProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.op.aod.enhance.config"
        private const val PATH_CONFIG = "aod_config"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CONFIG")

        private const val PREFS_NAME = "aod_config"
        private const val KEY_INIT_BRIGHTNESS_DARK = "init_brightness_dark"
        private const val KEY_INIT_BRIGHTNESS_BRIGHT = "init_brightness_bright"
        private const val KEY_RUNNING_BRIGHTNESS_MULTIPLIER = "running_brightness_multiplier"
        private const val KEY_ENABLE_PANORAMIC = "enable_panoramic"
        private const val KEY_ENABLE_SETTINGS_SUPPORT = "enable_settings_support"
        private const val KEY_BLOCK_SINGLE_CLICK = "block_single_click"

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_CONFIG, 1)
        }
    }

    override fun onCreate(): Boolean = true

    private fun prefs() = context?.getSharedPreferences(PREFS_NAME, 0)

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != 1) return null
        val p = prefs() ?: return null
        val cursor = MatrixCursor(arrayOf(
            KEY_INIT_BRIGHTNESS_DARK,
            KEY_INIT_BRIGHTNESS_BRIGHT,
            KEY_RUNNING_BRIGHTNESS_MULTIPLIER,
            KEY_ENABLE_PANORAMIC,
            KEY_ENABLE_SETTINGS_SUPPORT,
            KEY_BLOCK_SINGLE_CLICK
        ))
        cursor.addRow(arrayOf(
            p.getInt(KEY_INIT_BRIGHTNESS_DARK, 80).toString(),
            p.getInt(KEY_INIT_BRIGHTNESS_BRIGHT, 160).toString(),
            p.getFloat(KEY_RUNNING_BRIGHTNESS_MULTIPLIER, 1.6f).toString(),
            p.getBoolean(KEY_ENABLE_PANORAMIC, true).toString(),
            p.getBoolean(KEY_ENABLE_SETTINGS_SUPPORT, true).toString(),
            p.getBoolean(KEY_BLOCK_SINGLE_CLICK, true).toString()
        ))
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (matcher.match(uri) != 1) return 0
        val p = prefs() ?: return 0
        val e = p.edit()
        values?.let {
            if (it.containsKey(KEY_INIT_BRIGHTNESS_DARK)) e.putInt(KEY_INIT_BRIGHTNESS_DARK, it.getAsInteger(KEY_INIT_BRIGHTNESS_DARK))
            if (it.containsKey(KEY_INIT_BRIGHTNESS_BRIGHT)) e.putInt(KEY_INIT_BRIGHTNESS_BRIGHT, it.getAsInteger(KEY_INIT_BRIGHTNESS_BRIGHT))
            if (it.containsKey(KEY_RUNNING_BRIGHTNESS_MULTIPLIER)) e.putFloat(KEY_RUNNING_BRIGHTNESS_MULTIPLIER, it.getAsFloat(KEY_RUNNING_BRIGHTNESS_MULTIPLIER))
            if (it.containsKey(KEY_ENABLE_PANORAMIC)) e.putBoolean(KEY_ENABLE_PANORAMIC, it.getAsBoolean(KEY_ENABLE_PANORAMIC))
            if (it.containsKey(KEY_ENABLE_SETTINGS_SUPPORT)) e.putBoolean(KEY_ENABLE_SETTINGS_SUPPORT, it.getAsBoolean(KEY_ENABLE_SETTINGS_SUPPORT))
            if (it.containsKey(KEY_BLOCK_SINGLE_CLICK)) e.putBoolean(KEY_BLOCK_SINGLE_CLICK, it.getAsBoolean(KEY_BLOCK_SINGLE_CLICK))
        }
        e.apply()
        context?.contentResolver?.notifyChange(uri, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = if (matcher.match(uri) == 1) "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_CONFIG" else null
}
