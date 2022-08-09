package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile.CompatEncryptedFileManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile.EncryptedFileManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile.TargetEncryptedFileManager
import it.airgap.beaconsdk.core.internal.utils.sdkAtLeast
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.SecureStorage
import java.security.KeyStore
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SharedPreferencesSecureStorage(
    sharedPreferences: SharedPreferences,
    private val encryptedFileManager: EncryptedFileManager,
    beaconScope: BeaconScope = BeaconScope.Global,
) : SecureStorage, SharedPreferencesBaseStorage(beaconScope, sharedPreferences) {

    private var masterKeyAlias: String?
        get() = sharedPreferences.getString(Key.MasterKeyAlias.scoped(), null)
        set(value) {
            value?.let { sharedPreferences.putString(Key.MasterKeyAlias.scoped(), it) }
        }

    override suspend fun getSdkSecretSeed(): String? {
        val keyAlias = masterKeyAlias ?: return null
        val secretSeed = encryptedFileManager.read(FileKey.SdkSecretKey.scoped(), keyAlias)

        return if (secretSeed?.isEmpty() == true) null else secretSeed?.decodeToString()
    }

    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        val keyAlias = masterKeyAlias ?: UUID.randomUUID().toString().also { masterKeyAlias = it }
        encryptedFileManager.write(FileKey.SdkSecretKey.scoped(), keyAlias, sdkSecretSeed.encodeToByteArray())
    }

    private enum class Key(override val value: String) : SharedPreferencesBaseStorage.Key {
        MasterKeyAlias("masterKeyAlias"),
    }

    private enum class FileKey(override val value: String) : SharedPreferencesBaseStorage.Key {
        SdkSecretKey("sdk_seed"),
    }

    override fun scoped(beaconScope: BeaconScope): SecureStorage =
        if (beaconScope == this.beaconScope) this
        else SharedPreferencesSecureStorage(sharedPreferences, encryptedFileManager, beaconScope)

    public companion object {
        internal const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SharedPreferencesSecureStorage(context: Context): SharedPreferencesSecureStorage {
    val keyStore = KeyStore.getInstance(SharedPreferencesSecureStorage.ANDROID_KEY_STORE).apply { load(null) }
    val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)
    val encryptedFileManager =
        if (sdkAtLeast(Build.VERSION_CODES.M)) TargetEncryptedFileManager(context, keyStore)
        else CompatEncryptedFileManager(context, keyStore)

    return SharedPreferencesSecureStorage(sharedPreferences, encryptedFileManager)
}