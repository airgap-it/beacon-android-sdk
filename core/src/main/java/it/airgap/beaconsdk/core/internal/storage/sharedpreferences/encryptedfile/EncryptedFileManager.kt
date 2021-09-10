package it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile

public interface EncryptedFileManager {
    @Throws(Exception::class)
    suspend fun read(fileName: String, keyAlias: String): ByteArray?

    @Throws(Exception::class)
    suspend fun write(fileName: String, keyAlias: String, data: ByteArray)
}