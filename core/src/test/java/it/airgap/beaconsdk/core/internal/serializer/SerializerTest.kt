package it.airgap.beaconsdk.core.internal.serializer

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import it.airgap.beaconsdk.core.internal.serializer.provider.SerializerProvider
import kotlinx.serialization.SerializationException
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

internal class SerializerTest {

    @MockK
    private lateinit var serializerProvider: SerializerProvider

    private lateinit var serializer: Serializer

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        serializer = Serializer(serializerProvider)
    }

    @Test
    fun `serializes message`() {
        every { serializerProvider.serialize<MockMessage>(any(), any()) } answers { firstArg<MockMessage>().serialize() }

        val message = MockMessage("message")
        val serialized = serializer.serialize(message)

        assertTrue(serialized.isSuccess, "Expected serialized result to be a success")
        verify { serializerProvider.serialize(message, MockMessage::class) }

        confirmVerified(serializerProvider)
    }

    @Test
    fun `returns failure result if serialization failed`() {
        every { serializerProvider.serialize(any(), any()) } throws SerializationException()

        val serialized = serializer.serialize("")

        assertTrue(serialized.isFailure, "Expected serialized result to be a failure")
    }

    @Test
    fun `deserializes message`() {
        every { serializerProvider.deserialize<MockMessage>(any(), any()) } answers { MockMessage.deserialize(firstArg()) }

        val serialized = "message"
        val deserialized = serializer.deserialize<MockMessage>(serialized)

        assertTrue(deserialized.isSuccess, "Expected deserialized result to be a success")
        verify { serializerProvider.deserialize(serialized, MockMessage::class) }

        confirmVerified(serializerProvider)
    }

    @Test
    fun `returns failure result if deserialization failed`() {
        every { serializerProvider.deserialize<Any>(any(), any()) } throws SerializationException()

        val deserialized = serializer.deserialize<Any>("")

        assertTrue(deserialized.isFailure, "Expected serialized result to be a failure")
    }

    private data class MockMessage(val value: String) {
        fun serialize(): String = value

        companion object {
            fun deserialize(string: String): MockMessage = MockMessage(string)
        }
    }
}