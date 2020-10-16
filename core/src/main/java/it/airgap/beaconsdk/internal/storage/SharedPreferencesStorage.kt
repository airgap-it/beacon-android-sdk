package it.airgap.beaconsdk.internal.storage

import android.content.Context
import android.content.SharedPreferences
import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class SharedPreferencesStorage(private val sharedPreferences: SharedPreferences) : BeaconStorage {
    override suspend fun getP2pPeers(): List<P2pPairingRequest> =
        sharedPreferences.getSerializable(KEY_P2P_PEERS, emptyList())

    override suspend fun setP2pPeers(p2pPeers: List<P2pPairingRequest>) {
        sharedPreferences.putSerializable(KEY_P2P_PEERS, p2pPeers)
    }

    override suspend fun getAccounts(): List<AccountInfo> =
        sharedPreferences.getSerializable(KEY_ACCOUNTS, emptyList())

    override suspend fun setAccounts(accounts: List<AccountInfo>) {
        sharedPreferences.putSerializable(KEY_ACCOUNTS, accounts)
    }

    override suspend fun getActiveAccountIdentifier(): String? =
        sharedPreferences.getString(KEY_ACTIVE_ACCOUNT_IDENTIFIER, null)

    override suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String) {
        sharedPreferences.putSerializable(KEY_ACTIVE_ACCOUNT_IDENTIFIER, activeAccountIdentifier)
    }

    override suspend fun getAppsMetadata(): List<AppMetadata> =
        sharedPreferences.getSerializable(KEY_APPS_METADATA, emptyList())

    override suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>) {
        sharedPreferences.putSerializable(KEY_APPS_METADATA, appsMetadata)
    }

    override suspend fun getPermissions(): List<PermissionInfo> =
        sharedPreferences.getSerializable(KEY_PERMISSIONS, emptyList())

    override suspend fun setPermissions(permissions: List<PermissionInfo>) {
        sharedPreferences.putSerializable(KEY_PERMISSIONS, permissions)
    }

    override suspend fun getSdkSecretSeed(): String? =
        sharedPreferences.getString(KEY_SDK_SECRET_SEED, null)

    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        sharedPreferences.putString(KEY_SDK_SECRET_SEED, sdkSecretSeed)
    }

    override suspend fun getSdkVersion(): String? =
        sharedPreferences.getString(KEY_SDK_VERSION, null)

    override suspend fun setSdkVersion(sdkVersion: String) {
        sharedPreferences.putString(KEY_SDK_VERSION, sdkVersion)
    }

    private inline fun <reified T> SharedPreferences.getSerializable(key: String, default: T): T {
        val encoded = getString(key, null)

        return encoded?.let { Json.decodeFromString(encoded) } ?: default
    }

    private fun SharedPreferences.putString(key: String, value: String) {
        edit().putString(key, value).apply()
    }

    private inline fun <reified T> SharedPreferences.putSerializable(key: String, value: T) {
        val encoded = Json.encodeToString(value)
        putString(key, encoded)
    }

    companion object {
        private const val STORAGE_NAME = "beaconsdk"

        const val KEY_P2P_PEERS = "p2pPeers"

        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACTIVE_ACCOUNT_IDENTIFIER = "activeAccountIdentifier"

        const val KEY_APPS_METADATA = "appsMetadata"
        const val KEY_PERMISSIONS = "permissions"

        const val KEY_SDK_SECRET_SEED = "sdkSecretSeed"
        const val KEY_SDK_VERSION = "sdkVersion"

        fun create(context: Context): SharedPreferencesStorage {
            val sharedPreferences = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)

            return SharedPreferencesStorage(sharedPreferences)
        }
    }
}