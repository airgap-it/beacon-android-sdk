package it.airgap.beaconsdk.internal.message.beacon

import beaconMessages
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.internal.utils.failWith
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class VersionedBeaconMessageTest {

    @Test
    fun `proper version is created from BeaconMessage`() {
        beaconMessages().forEach { beaconMessage ->
            versionsWithClasses.forEach {
                val versioned = VersionedBeaconMessage.fromBeaconMessage(it.first, "senderId", beaconMessage)

                assertTrue(
                    it.second.isInstance(versioned),
                    "Expected versioned message for version \"${it.first}\" to be an instance of ${it.second.simpleName}"
                )
            }
        }
    }

    @Test
    fun `is deserialized from JSON to proper version`() {
        beaconMessages().forEach { beaconMessage ->
            versionsWithClasses.forEach {
                val json = createJson(it.first, beaconMessage)
                val deserialized = Json.decodeFromString<VersionedBeaconMessage>(json)

                assertTrue(
                    it.second.isInstance(deserialized),
                    "Expected deserialized message for version \"${it.first}\" to be an instance of ${it.second.simpleName}"
                )
            }
        }
    }

    @Test
    fun `serializes to JSON based on version`() {
        beaconMessages().forEach { beaconMessage ->
            versionsWithClasses.forEach {
                val (versioned, expected) = createVersionedJsonPair(it.first, beaconMessage)
                val serialized = Json.encodeToString(versioned)

                assertEquals(expected, serialized)
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
            V1BeaconMessage::class -> V1BeaconMessage.fromBeaconMessage(version, "senderId", this) as T
            V2BeaconMessage::class -> V2BeaconMessage.fromBeaconMessage(version, "senderId", this) as T
            else -> failWith("Unknown class")
        }

    private val String.major: String
        get() = substringBefore('.')
}