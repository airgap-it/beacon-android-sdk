package it.airgap.beaconsdk.internal.storage.sharedpreferences.encryptedfile

internal interface EncryptedFileManager {
    @Throws(Exception::class)
    suspend fun read(fileName: String, keyAlias: String): ByteArray?

    @Throws(Exception::class)
    suspend fun write(fileName: String, keyAlias: String, data: ByteArray)
}