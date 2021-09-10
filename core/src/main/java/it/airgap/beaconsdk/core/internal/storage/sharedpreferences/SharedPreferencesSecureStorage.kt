package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.SecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile.CompatEncryptedFileManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile.EncryptedFileManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile.TargetEncryptedFileManager
import it.airgap.beaconsdk.core.internal.utils.sdkAtLeast
import java.security.KeyStore
import java.util.*

// TODO: improve error handling
public class SharedPreferencesSecureStorage(
    private val sharedPreferences: SharedPreferences,
    private val encryptedFileManager: EncryptedFileManager,
) : SecureStorage {

    private var masterKeyAlias: String?
        get() = sharedPreferences.getString(KEY_MASTER_KEY_ALIAS, null)
        set(value) {
            value?.let { sharedPreferences.putString(KEY_MASTER_KEY_ALIAS, it) }
        }

    override suspend fun getSdkSecretSeed(): String? {
        val keyAlias = masterKeyAlias ?: return null
        val secretSeed = encryptedFileManager.read(FILE_SDK_SECRET_KEY, keyAlias)

        return if (secretSeed?.isEmpty() == true) null else secretSeed?.decodeToString()
    }

    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        val keyAlias = masterKeyAlias ?: UUID.randomUUID().toString().also { masterKeyAlias = it }
        encryptedFileManager.write(FILE_SDK_SECRET_KEY, keyAlias, sdkSecretSeed.encodeToByteArray())
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"

        private const val KEY_MASTER_KEY_ALIAS = "masterKeyAlias"
        private const val FILE_SDK_SECRET_KEY = "sdk_seed"

        fun create(context: Context): SharedPreferencesSecureStorage {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)
            val encryptedFileManager =
                if (sdkAtLeast(Build.VERSION_CODES.M)) TargetEncryptedFileManager(context, keyStore)
                else CompatEncryptedFileManager(context, keyStore)

            return SharedPreferencesSecureStorage(sharedPreferences, encryptedFileManager)
        }
    }
}