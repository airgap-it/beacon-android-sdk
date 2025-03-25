package it.airgap.beaconsdk.core.internal.storage.sharedpreferences.encryptedfile

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.sdkAtLeast
import java.security.KeyStore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EncryptedFileReader {
    @Throws(Exception::class)
    public suspend fun read(fileName: String, keyAlias: String): ByteArray?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EncryptedFileWriter {
    @Throws(Exception::class)
    public suspend fun write(fileName: String, keyAlias: String, data: ByteArray)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EncryptedFileManager : EncryptedFileReader, EncryptedFileWriter

public fun EncryptedFileManager(context: Context, keyStore: KeyStore): EncryptedFileManager =
    if (sdkAtLeast(Build.VERSION_CODES.M)) TargetEncryptedFileManager(context, keyStore)
    else CompatEncryptedFileManager(context, keyStore)