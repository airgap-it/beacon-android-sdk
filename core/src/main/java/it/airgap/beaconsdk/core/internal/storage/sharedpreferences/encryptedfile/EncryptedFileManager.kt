package it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EncryptedFileManager {
    @Throws(Exception::class)
    public suspend fun read(fileName: String, keyAlias: String): ByteArray?

    @Throws(Exception::class)
    public suspend fun write(fileName: String, keyAlias: String, data: ByteArray)
}