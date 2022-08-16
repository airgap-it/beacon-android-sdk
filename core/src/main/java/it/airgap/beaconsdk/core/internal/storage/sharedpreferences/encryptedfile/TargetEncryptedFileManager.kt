package it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

@RequiresApi(Build.VERSION_CODES.M)
internal class TargetEncryptedFileManager(private val context: Context, private val keyStore: KeyStore) : EncryptedFileManager {

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun read(fileName: String, keyAlias: String): ByteArray? {
        val file = file(fileName, keyAlias)
        if (!file.exists()) return null

        return withContext(Dispatchers.IO) {
            async {
                val masterKey = getOrCreateKey(keyAlias)
                val encryptedFile = EncryptedFile.Builder(
                    file,
                    context,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()

                encryptedFile.openFileInput().use { it.readBytes() }
            }
        }.await()
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun write(fileName: String, keyAlias: String, data: ByteArray) {
        withContext(Dispatchers.IO) {
            launch {
                val masterKey = getOrCreateKey(keyAlias)
                val encryptedFile = EncryptedFile.Builder(
                    file(fileName, keyAlias),
                    context,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()

                encryptedFile.openFileOutput().use { it.write(data) }
            }
        }
    }

    private fun file(name: String, keyAlias: String): File =
        File(context.filesDir, name).apply {
            deleteIfNoKey(keyAlias)
        }

    private fun getOrCreateKey(alias: String): String {
        val parameterSpec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).apply {
            setKeySize(SIZE_KEY)
            setDigests(KeyProperties.DIGEST_SHA512)
            setUserAuthenticationRequired(false)
            setRandomizedEncryptionRequired(true)
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        }.build()

        return MasterKeys.getOrCreate(parameterSpec)
    }

    private fun File.deleteIfNoKey(alias: String): Boolean {
        if (!keyStore.containsAlias(alias)) {
            return delete()
        }

        return false
    }

    companion object {
        private const val SIZE_KEY = 256
    }
}