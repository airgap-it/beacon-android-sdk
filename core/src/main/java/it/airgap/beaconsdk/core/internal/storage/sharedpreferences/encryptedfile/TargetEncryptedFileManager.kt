package it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

@RequiresApi(Build.VERSION_CODES.M)
internal class TargetEncryptedFileManager(private val context: Context) : EncryptedFileManager {

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun read(fileName: String, keyAlias: String): ByteArray? {
        val file = file(fileName, keyAlias)
        if (!file.exists()) return null

        return coroutineScope {
            async(Dispatchers.IO) {
                file.readBytes()
            }
        }.await()
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    override suspend fun write(fileName: String, keyAlias: String, data: ByteArray) {
        coroutineScope {
            launch(Dispatchers.IO) {
                file(fileName, keyAlias).writeBytes(data)
            }
        }
    }

    private fun file(name: String, keyAlias: String): File =
        File(context.filesDir, name).apply {
            deleteIfNoKey(keyAlias)
        }

    private fun File.deleteIfNoKey(alias: String): Boolean {
        return false
    }

    companion object {
        private const val SIZE_KEY = 256
    }
}