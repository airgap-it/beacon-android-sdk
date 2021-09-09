package it.airgap.beaconsdk.core.internal.message.beacon

import beaconMessages
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.failWith
import it.airgap.beaconsdk.core.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mockChainRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

internal class VersionedBeaconMessageTest {

    private val notSupported = listOf(
        AcknowledgeBeaconResponse::class to "1",
    )

    @Before
    fun setup() {
        mockChainRegistry()
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `proper version is created from BeaconMessage`() {
        versionsWithClasses.forEach { (version, versionedClass) ->
            beaconMessages(version = version).forEach { beaconMessage ->
                val messageWithVersion = Pair(beaconMessage, version)
                messageWithVersion.ifSupported {
                    val versioned = VersionedBeaconMessage.from("senderId", beaconMessage)

                    assertTrue(
                        versionedClass.isInstance(versioned),
                        "Expected versioned message for version \"${version}\" to be an instance of ${versionedClass.simpleName}"
                    )
                }

                messageWithVersion.ifNotSupported {
                    assertFails { VersionedBeaconMessage.from("senderId", beaconMessage) }
                }
            }
        }
    }

    @Test
    fun `is deserialized from JSON to proper version`() {
        versionsWithClasses.forEach { (version, versionedClass) ->
            beaconMessages(version = version).forEach { beaconMessage ->
                val messageWithVersion = Pair(beaconMessage, version)

                messageWithVersion.ifSupported {
                    val json = createJson(version, beaconMessage)
                    val deserialized = Json.decodeFromString<VersionedBeaconMessage>(json)

                    assertTrue(
                        versionedClass.isInstance(deserialized),
                        "Expected deserialized message for version \"${version}\" to be an instance of ${versionedClass.simpleName}"
                    )
                }
            }
        }
    }

    @Test
    fun `serializes to JSON based on version`() {
        beaconMessages().forEach { beaconMessage ->
            versionsWithClasses.forEach {
                val messageWithVersion = Pair(beaconMessage, it.first)

                messageWithVersion.ifSupported {
                    val (versioned, expected) = createVersionedJsonPair(it.first, beaconMessage)
                    val serialized = Json.encodeToString(versioned)

                    assertEquals(expected, serialized)
                }
            }
        }
    }

    private val versionsWithClasses: List<Pair<String, KClass<out VersionedBeaconMessage>>> =
        listOf(
            "1" to V1BeaconMessage::class,
            "1.0" to V1BeaconMessage::class,
            "1.0.0" to V1BeaconMessage::class,
            "1.abc" to V1BeaconMessage::class,
            "2" to V2BeaconMessage::class,
            "2.0" to V2BeaconMessage::class,
            "2.0.0" to V2BeaconMessage::class,
            "2.abc" to V2BeaconMessage::class,
            "3" to V2BeaconMessage::class,
        )

    private fun createJson(version: String, beaconMessage: BeaconMessage): String =
        when (version.major) {
            "1" -> Json.encodeToString(beaconMessage.versioned<V1BeaconMessage>(version))
            "2" -> Json.encodeToString(beaconMessage.versioned<V2BeaconMessage>(version))
            else -> Json.encodeToString(beaconMessage.versioned<V2BeaconMessage>(version))
        }

    private fun createVersionedJsonPair(version: String, beaconMessage: BeaconMessage): Pair<VersionedBeaconMessage, String> =
        when (version.major) {
            "1" -> {
                val versioned = beaconMessage.versioned<V1BeaconMessage>(version)
                val json = Json.encodeToString(versioned)

                Pair(versioned, json)
            }

            "2" -> {
                val versioned = beaconMessage.versioned<V2BeaconMessage>(version)
                val json = Json.encodeToString(versioned)

                Pair(versioned, json)
            }

            else -> {
                val versioned = beaconMessage.versioned<V2BeaconMessage>(version)
                val json = Json.encodeToString(versioned)

                Pair(versioned, json)
            }
        }

    private inline fun <reified T : VersionedBeaconMessage> BeaconMessage.versioned(version: String): T =
        when (T::class) {
            V1BeaconMessage::class -> V1BeaconMessage.from("senderId", this) as T
            V2BeaconMessage::class -> V2BeaconMessage.from( "senderId", this) as T
            else -> failWith("Unknown class")
        }

    private val String.major: String
        get() = substringBefore('.')

    private val Pair<BeaconMessage, String>.isSupported: Boolean
        get() = !notSupported.contains(Pair(first::class, second.major))

    private inline fun Pair<BeaconMessage, String>.ifSupported(block: () -> Unit) {
        if (isSupported) { block() }
    }

    private inline fun Pair<BeaconMessage, String>.ifNotSupported(block: () -> Unit) {
        if (!isSupported) { block() }
    }
}