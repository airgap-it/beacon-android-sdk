package it.airgap.beaconsdk.blockchain.substrate.internal.creator

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.toHexString
import kotlinx.coroutines.runBlocking
import mockTime
import org.junit.Before
import org.junit.Test
import permissionSubstrateRequest
import permissionSubstrateResponse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class SubstrateCreatorTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    private lateinit var creator: SubstrateCreator

    private val currentTimeMillis: Long = 1

    private val version: String = "2"
    private val senderId: String = "00"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockTime(currentTimeMillis)

        every { identifierCreator.accountId(any(), any()) } answers { Result.success(firstArg()) }
        every { identifierCreator.senderId(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        creator = SubstrateCreator(
            DataSubstrateCreator(storageManager, identifierCreator),
            V1BeaconMessageSubstrateCreator(),
            V2BeaconMessageSubstrateCreator(),
            V3BeaconMessageSubstrateCreator(),
        )
    }

    @Test
    fun `extracts permission`() {
        val id = "id"

        val appMetadata = SubstrateAppMetadata(senderId, "mockApp")
        val permissionRequest = permissionSubstrateRequest(id = id, version = version, senderId = senderId)
        val permissionResponse = permissionSubstrateResponse(id = id, version = version)

        runBlocking {
            storageManager.setAppMetadata(listOf(appMetadata))
            val permission = creator.data.extractPermission(permissionRequest, permissionResponse).getOrThrow()

            val expected = permissionResponse.accounts.map {
                SubstratePermission(
                    it.address,
                    appMetadata.senderId,
                    currentTimeMillis,
                    appMetadata,
                    permissionResponse.scopes,
                    it
                )
            }

            assertEquals(expected, permission)
        }
    }


    @Test
    fun `fails to extract permission when app metadata not found`() {
        val id = "id"

        val permissionRequest = permissionSubstrateRequest(id = id, version = version)
        val permissionResponse = permissionSubstrateResponse(id = id, version = version)

        runBlocking {
            storageManager.setAppMetadata(emptyList())
        }

        assertFailsWith<IllegalStateException> {
            runBlocking { creator.data.extractPermission(permissionRequest, permissionResponse).getOrThrow() }
        }
    }
}