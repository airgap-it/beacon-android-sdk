package it.airgap.beaconsdk.transport.p2p.matrix.internal

import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.data.HexString
import it.airgap.beaconsdk.core.internal.utils.*

internal class P2pMatrixSecurity(private val app: BeaconApplication, private val crypto: Crypto) {
    private val keyPair: KeyPair get() = app.keyPair

    private val serverSessionKeyPair: MutableMap<HexString, SessionKeyPair> = mutableMapOf()
    private val clientSessionKeyPair: MutableMap<HexString, SessionKeyPair> = mutableMapOf()

    fun userId(): Result<String> =
        crypto.hashKey(keyPair.publicKey).map { it.toHexString().asString() }

    fun password(): Result<String> =
        runCatching {
            val publicKeyHex = keyPair.publicKey
                .toHexString()
                .asString()

            val loginDigest = crypto.hash(
                "login:${currentTimestamp() / 1000 / (5 * 60)}",
                32
            ).getOrThrow()

            val signature = crypto.signMessageDetached(loginDigest, keyPair.privateKey)
                .getOrThrow()
                .toHexString()
                .asString()

            "ed:$signature:$publicKeyHex"
        }

    fun deviceId(): Result<String> =
        Result.success(keyPair.publicKey.toHexString().asString())

    fun encryptPairingPayload(publicKey: ByteArray, pairingPayload: String): Result<ByteArray> =
        crypto.encryptMessageWithPublicKey(
            pairingPayload,
            publicKey,
        )

    fun decryptPairingPayload(pairingPayload: ByteArray): Result<String> =
        crypto.decryptMessageWithKeyPair(
            pairingPayload,
            keyPair.publicKey,
            keyPair.privateKey,
        ).map { it.decodeToString() }

    fun encrypt(publicKey: ByteArray, message: String): Result<String> =
        getOrCreateClientSessionKeyPair(publicKey, keyPair.privateKey)
            .flatMap { crypto.encryptMessageWithSharedKey(message, it.tx) }
            .map { it.toHexString().asString() }

    fun decrypt(publicKey: ByteArray, message: String): Result<String> =
        getOrCreateServerSessionKeyPair(publicKey, keyPair.privateKey)
            .flatMap {
                if (message.isHex()) crypto.decryptMessageWithSharedKey(message.asHexString(), it.rx)
                else crypto.decryptMessageWithSharedKey(message, it.rx)
            }
            .map { it.decodeToString() }

    private fun getOrCreateClientSessionKeyPair(publicKey: ByteArray, privateKey: ByteArray): Result<SessionKeyPair> =
        clientSessionKeyPair.getOrCreate(publicKey) {
            crypto.createClientSessionKeyPair(publicKey, privateKey)
        }

    private fun getOrCreateServerSessionKeyPair(publicKey: ByteArray, privateKey: ByteArray): Result<SessionKeyPair> =
        serverSessionKeyPair.getOrCreate(publicKey) {
            crypto.createServerSessionKeyPair(publicKey, privateKey)
        }

    private fun MutableMap<HexString, SessionKeyPair>.getOrCreate(
        publicKey: ByteArray,
        default: () -> Result<SessionKeyPair>,
    ): Result<SessionKeyPair> =
        if (containsKey(publicKey)) Result.success(getValue(publicKey))
        else default().onSuccess { put(publicKey, it) }

    private fun <V> MutableMap<HexString, V>.getValue(key: ByteArray): V = getValue(key.toHexString())
    private fun <V> MutableMap<HexString, V>.put(key: ByteArray, value: V): V? = put(key.toHexString(), value)
    private fun <V> MutableMap<HexString, V>.containsKey(key: ByteArray) = containsKey(key.toHexString())
}