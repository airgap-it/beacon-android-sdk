package it.airgap.beaconsdk.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import it.airgap.beaconsdk.internal.BeaconApp

class BeaconInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let {
            BeaconApp.init(it)
            Log.i(TAG, "BeaconApp initialized")
        } ?: run {
            Log.i(TAG, "BeaconApp could not be initialized")
        }


        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        private const val TAG = "BeaconInitProvider"
    }
}