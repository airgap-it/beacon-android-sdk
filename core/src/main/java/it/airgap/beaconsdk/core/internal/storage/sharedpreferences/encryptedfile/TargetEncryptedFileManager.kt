package it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.M)
internal fun TargetEncryptedFileManager(context: Context, keyStore: KeyStore): EncryptedFileManager =
    TargetEncryptedFileManagerWithFallback(
        BaseTargetEncryptedFileManager(context, keyStore),
        LegacyTargetEncryptedFileReader(context, keyStore)
    )

@RequiresApi(Build.VERSION_CODES.M)
private class TargetEncryptedFileManagerWithFallback(
    private val baseManager: BaseTargetEncryptedFileManager,
    private val legacyReader: LegacyTargetEncryptedFileReader,
) : EncryptedFileManager, EncryptedFileWriter by baseManager {

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun read(fileName: String, keyAlias: String): ByteArray? =
        try {
            baseManager.read(fileName, keyAlias)
        } catch (e: AEADBadTagException) {
            legacyReader.read(fileName, keyAlias)
        }
}

@RequiresApi(Build.VERSION_CODES.M)
private class BaseTargetEncryptedFileManager(private val context: Context, private val keyStore: KeyStore) : EncryptedFileManager {

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun read(fileName: String, keyAlias: String): ByteArray? {
        val file = file(fileName, keyAlias)
        if (!file.exists()) return null

        return withContext(Dispatchers.IO) {
            file.inputStream().use { fis ->
                val iv = ByteArray(SIZE_IV).also { fis.read(it) }
                val encryptedData = fis.readBytes()

                val cipher = Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, getOrCreateKey(keyAlias), GCMParameterSpec(SIZE_TAG * 8, iv))
                    updateAAD(fileName.toByteArray(charset = Charsets.UTF_8))
                }

                cipher.doFinal(encryptedData)
            }
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun write(fileName: String, keyAlias: String, data: ByteArray) {
        val file = file(fileName, keyAlias)
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey(keyAlias))
            updateAAD(fileName.toByteArray(charset = Charsets.UTF_8))
        }

        return withContext(Dispatchers.IO) {
            file.outputStream().use { fos ->
                fos.write(cipher.iv)
                fos.write(cipher.doFinal(data))
            }
        }
    }

    private fun file(name: String, keyAlias: String): File =
        File(context.filesDir, name).apply {
            deleteIfNoKey(keyAlias)
        }

    private fun getOrCreateKey(alias: String): SecretKey {
        if (!keyStore.containsAlias(alias)) {
            val parameterSpec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).apply {
                setKeySize(SIZE_KEY)
                setDigests(KeyProperties.DIGEST_SHA512)
                setUserAuthenticationRequired(false)
                setRandomizedEncryptionRequired(true)
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            }.build()

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStore.type).apply { init(parameterSpec) }
            keyGenerator.generateKey()
        }

        return keyStore.getKey(alias, null) as SecretKey
    }

    private fun File.deleteIfNoKey(alias: String): Boolean {
        if (!keyStore.containsAlias(alias)) {
            return delete()
        }

        return false
    }

    companion object {
        private const val SIZE_KEY = 256
        private const val SIZE_IV = 12
        private const val SIZE_TAG = 16

        private const val ALGORITHM = "AES/GCM/NoPadding"
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private class LegacyTargetEncryptedFileReader(private val context: Context, private val keyStore: KeyStore) : EncryptedFileReader {

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun read(fileName: String, keyAlias: String): ByteArray? {
        val file = file(fileName, keyAlias)
        if (!file.exists()) return null

        return withContext(Dispatchers.IO) {
            val masterKey = getOrCreateKey(keyAlias)
            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileInput().use { it.readBytes() }
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