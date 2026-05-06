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
            AodConfigContract.KEY_INIT_DARK,
            AodConfigContract.KEY_INIT_BRIGHT,
            AodConfigContract.KEY_RUNNING_MULTIPLIER,
            AodConfigContract.KEY_ENABLE_PANORAMIC,
            AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT,
            AodConfigContract.KEY_BLOCK_SINGLE_CLICK,
        ))
        cursor.addRow(arrayOf(
            p.getInt(AodConfigContract.KEY_INIT_DARK, AodConfigContract.DEFAULT_INIT_DARK).toString(),
            p.getInt(AodConfigContract.KEY_INIT_BRIGHT, AodConfigContract.DEFAULT_INIT_BRIGHT).toString(),
            p.getFloat(AodConfigContract.KEY_RUNNING_MULTIPLIER, AodConfigContract.DEFAULT_RUNNING_MULTIPLIER).toString(),
            p.getBoolean(AodConfigContract.KEY_ENABLE_PANORAMIC, AodConfigContract.DEFAULT_ENABLE_PANORAMIC).toString(),
            p.getBoolean(AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT, AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT).toString(),
            p.getBoolean(AodConfigContract.KEY_BLOCK_SINGLE_CLICK, AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK).toString(),
        ))
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (matcher.match(uri) != 1) return 0
        val p = prefs() ?: return 0
        val e = p.edit()
        values?.let {
            if (it.containsKey(AodConfigContract.KEY_INIT_DARK))
                e.putInt(AodConfigContract.KEY_INIT_DARK, it.getAsInteger(AodConfigContract.KEY_INIT_DARK))
            if (it.containsKey(AodConfigContract.KEY_INIT_BRIGHT))
                e.putInt(AodConfigContract.KEY_INIT_BRIGHT, it.getAsInteger(AodConfigContract.KEY_INIT_BRIGHT))
            if (it.containsKey(AodConfigContract.KEY_RUNNING_MULTIPLIER))
                e.putFloat(AodConfigContract.KEY_RUNNING_MULTIPLIER, it.getAsFloat(AodConfigContract.KEY_RUNNING_MULTIPLIER))
            if (it.containsKey(AodConfigContract.KEY_ENABLE_PANORAMIC))
                e.putBoolean(AodConfigContract.KEY_ENABLE_PANORAMIC, it.getAsBoolean(AodConfigContract.KEY_ENABLE_PANORAMIC))
            if (it.containsKey(AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT))
                e.putBoolean(AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT, it.getAsBoolean(AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT))
            if (it.containsKey(AodConfigContract.KEY_BLOCK_SINGLE_CLICK))
                e.putBoolean(AodConfigContract.KEY_BLOCK_SINGLE_CLICK, it.getAsBoolean(AodConfigContract.KEY_BLOCK_SINGLE_CLICK))
        }
        e.apply()
        context?.contentResolver?.notifyChange(uri, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = if (matcher.match(uri) == 1) "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_CONFIG" else null
}
