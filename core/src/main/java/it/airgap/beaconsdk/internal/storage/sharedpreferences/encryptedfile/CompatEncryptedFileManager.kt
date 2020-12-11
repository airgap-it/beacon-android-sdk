package it.airgap.beaconsdk.internal.storage.sharedpreferences.encryptedfile

import android.content.Context
import android.security.KeyPairGeneratorSpec
import it.airgap.beaconsdk.internal.utils.asHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

@Suppress("DEPRECATION")
internal class CompatEncryptedFileManager(private val context: Context, private val keyStore: KeyStore) : EncryptedFileManager {

    private var _salt: ByteArray? = null
    private suspend fun salt(): ByteArray =
        _salt ?: initSalt()

    override suspend fun read(fileName: String, keyAlias: String): ByteArray? {
        val fileNameHash = hashForKey(fileName)
        val file = file(fileNameHash.asHexString().asString(), keyAlias)
        if (!file.exists()) return null

        val fsCipher = cipher(Cipher.DECRYPT_MODE, keyAlias) ?: return null
        val fsCipherInputStream = CipherInputStream(file.inputStream(), fsCipher)

        val encryptionSecret = encryptionSecret(fileNameHash)
        val secretKeySpec = SecretKeySpec(encryptionSecret, 0, encryptionSecret.size, "AES")
        val secretKeyCipher = Cipher.getInstance(SECRET_KEY_CIPHER_ALGORITHM)

        secretKeyCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(ivForKey(fileName)))

        val secretCipherInputStream = CipherInputStream(fsCipherInputStream, secretKeyCipher)

        return withContext(Dispatchers.IO) {
            async {
                secretCipherInputStream.use { it.readBytes() }
            }
        }.await()
    }

    override suspend fun write(fileName: String, keyAlias: String, data: ByteArray) {
        val fileNameHash = hashForKey(fileName)
        val file = file(fileNameHash.asHexString().asString(), keyAlias)
        val fsCipher = cipher(Cipher.ENCRYPT_MODE, keyAlias) ?: return
        val fsCipherOutputStream = CipherOutputStream(file.outputStream(), fsCipher)

        val encryptionSecret = encryptionSecret(fileNameHash)
        val secretKeySpec = SecretKeySpec(encryptionSecret, 0, encryptionSecret.size, "AES")
        val secretKeyCipher = Cipher.getInstance(SECRET_KEY_CIPHER_ALGORITHM)

        secretKeyCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(ivForKey(fileName)))

        withContext(Dispatchers.IO) {
            launch {
                CipherOutputStream(fsCipherOutputStream, secretKeyCipher).use {
                    it.write(data)
                    it.flush()
                }
            }
        }
    }

    private fun file(name: String, keyAlias: String): File =
        File(context.filesDir, name).apply {
            deleteIfNoKey(keyAlias)
        }

    private fun cipher(mode: Int, keyAlias: String): Cipher? {
        val key = key(keyAlias, mode)
        return Cipher.getInstance(FILESYSTEM_CIPHER_ALGORITHM).apply { init(mode, key) }
    }

    private fun key(alias: String, mode: Int): Key? {
        val entry = getOrCreateKeyEntry(alias) ?: return null
        return when (mode) {
            Cipher.DECRYPT_MODE -> entry.privateKey
            Cipher.ENCRYPT_MODE -> entry.certificate.publicKey
            else -> null
        }
    }

    @Throws(IOException::class)
    private suspend fun initSalt(): ByteArray {
        val file = File(context.filesDir, FILE_SALT)
        return withContext(Dispatchers.IO) {
            async {
                if (file.exists()) return@async file.inputStream().use { it.readBytes() }
                val salt = ByteArray(SIZE_KEY / 8).also { SecureRandom().nextBytes(it) }
                file.outputStream().use {
                    it.write(salt)
                    it.flush()
                }

                salt
            }
        }.await()
    }

    private fun getOrCreateKeyEntry(alias: String): KeyStore.PrivateKeyEntry? {
        keyStore.load(null)

        if (!keyStore.containsAlias(alias)) {
            val parameterSpec = KeyPairGeneratorSpec.Builder(context).apply {
                setAlias(alias)
                setSubject(X500Principal("CN=$alias, OU=${context.packageName}"))
                setSerialNumber(BigInteger.ONE)
                setStartDate(Calendar.getInstance().time)
                setEndDate(Calendar.getInstance().time)
            }.build()

            val keyGenerator = KeyPairGenerator.getInstance("RSA", keyStore.type).apply { initialize(parameterSpec) }
            keyGenerator.genKeyPair()
        }

        return keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
    }

    private fun encryptionSecret(key: ByteArray): ByteArray {
        return MessageDigest.getInstance(DIGEST_ALGORITHM).digest(key)
    }

    private suspend fun ivForKey(key: String): ByteArray {
        return hashForKey(key).sliceArray(IntRange(0, 15))
    }

    private suspend fun hashForKey(key: String): ByteArray {
        return MessageDigest.getInstance(DIGEST_ALGORITHM).digest(salt() + key.toByteArray())
    }

    private fun File.deleteIfNoKey(alias: String): Boolean {
        if (!keyStore.containsAlias(alias)) {
            return delete()
        }

        return false
    }

    companion object {
        private const val DIGEST_ALGORITHM = "SHA-256"

        private const val FILESYSTEM_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding"
        private const val SECRET_KEY_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"

        private const val SIZE_KEY = 256
        private const val FILE_SALT = "salt"
    }
}