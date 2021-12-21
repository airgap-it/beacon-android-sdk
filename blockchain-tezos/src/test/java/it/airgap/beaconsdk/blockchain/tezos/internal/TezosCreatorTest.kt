package it.airgap.beaconsdk.blockchain.tezos.internal

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.blockchain.tezos.mockTime
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.toHexString
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import permissionTezosRequest
import permissionTezosResponse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TezosCreatorTest {

    @MockK
    private lateinit var wallet: TezosWallet

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    private lateinit var creator: TezosCreator

    private val currentTimeMillis: Long = 1

    private val version: String = "2"
    private val senderId: String = "00"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockTime(currentTimeMillis)

        every { wallet.addressFromPublicKey(any()) } answers { Result.success("@${firstArg<String>()}") }

        every { identifierCreator.accountId(any(), any()) } answers { Result.success(firstArg()) }
        every { identifierCreator.senderId(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator)
        creator = TezosCreator(
            DataTezosCreator(wallet, storageManager, identifierCreator),
            V1BeaconMessageTezosCreator(),
            V2BeaconMessageTezosCreator(),
            V3BeaconMessageTezosCreator(),
        )
    }

    @Test
    fun `extracts permission`() {
        val id = "id"

        val appMetadata = AppMetadata(senderId, "mockApp")
        val permissionRequest = permissionTezosRequest(id = id, version = version, senderId = senderId)
        val permissionResponse = permissionTezosResponse(id = id, version = version)

        runBlocking {
            storageManager.setAppMetadata(listOf(appMetadata))
            val permission = creator.data.extractPermission(permissionRequest, permissionResponse).getOrThrow()

            val expected = TezosPermission(
                "@${permissionResponse.publicKey}",
                "@${permissionResponse.publicKey}",
                appMetadata.senderId,
                appMetadata,
                permissionResponse.publicKey,
                currentTimeMillis,
                permissionResponse.network,
                permissionResponse.scopes,
            )

            assertEquals(expected, permission)
        }
    }


    @Test
    fun `fails to extract permission when app metadata not found`() {
        val id = "id"

        val permissionRequest = permissionTezosRequest(id = id, version = version)
        val permissionResponse = permissionTezosResponse(id = id, version = version)

        runBlocking {
            storageManager.setAppMetadata(emptyList())
        }

        assertFailsWith<IllegalStateException> {
            runBlocking { creator.data.extractPermission(permissionRequest, permissionResponse).getOrThrow() }
        }
    }
}