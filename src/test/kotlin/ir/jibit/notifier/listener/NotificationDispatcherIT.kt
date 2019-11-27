@file:Suppress("ProtectedInFinal")

package ir.jibit.notifier.listener

import com.nhaarman.mockitokotlin2.*
import io.micrometer.core.instrument.MeterRegistry
import io.nats.client.Connection
import ir.jibit.notifier.NatsExtension
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import ir.jibit.notifier.stubs.Notification.NotificationRequest
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

/**
 * Integration tests for notification dispatching subsystem.
 *
 * @author Ali Dehghani
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(NatsExtension::class)
@TestPropertySource(properties = ["sms-providers.use=invalid"])
internal class NotificationDispatcherIT {

    /**
     * A mock implementation of [Notifier] to handle SMS notifications.
     */
    @MockBean(name = "sms")
    private lateinit var smsProvider: Notifier

    /**
     * A mock implementation of [Notifier] to handle call notifications.
     */
    @MockBean(name = "call")
    private lateinit var callProvider: Notifier

    /**
     * Enables us to communicate with the Nats server.
     */
    @Autowired
    private lateinit var connection: Connection

    /**
     * To interact with published metrics.
     */
    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun setUp() {
        stub {
            on { smsProvider.canNotify(any<SmsNotification>()) } doReturn true
            on { callProvider.canNotify(any<CallNotification>()) } doReturn true
        }
        meterRegistry.clear()
    }

    @Test
    fun `Dispatch -- Can't Parse the Notification - Won't Interact with Notifiers`() {
        connection.publish("notifier.notifications.sms", byteArrayOf(10, 12))
        waitForConsumerToCatchUp()

        verifyZeroInteractions(smsProvider, callProvider)

        val received = receivedCounter()
        val handled = handledTimer("failed", "InvalidProtocolBufferException")
        assertThat(received.count()).isOne()
        assertThat(handled.count()).isOne()
        assertThat(handled.measure()).isNotEmpty
        assertThat(handled.takeSnapshot().percentileValues().map { it.percentile() })
            .containsExactlyInAnyOrder(0.5, 0.75, 0.9, 0.95, 0.99)
    }

    @Test
    fun `Dispatch -- Invalid Type -- Should Return Silently and Record Metrics`() {
        val request = NotificationRequest.newBuilder().setNotificationType(INVALID).build()
        connection.publish("notifier.notifications.sms", request.toByteArray())
        waitForConsumerToCatchUp()

        verifyZeroInteractions(smsProvider, callProvider)

        val received = receivedCounter()
        val handled = handledTimer("failed", "InvalidNotificationType")
        assertThat(received.count()).isOne()
        assertThat(handled.count()).isOne()
        assertThat(handled.measure()).isNotEmpty
        assertThat(handled.takeSnapshot().percentileValues().map { it.percentile() })
            .containsExactlyInAnyOrder(0.5, 0.75, 0.9, 0.95, 0.99)
    }

    @Test
    fun `Dispatch -- Nobody Can Handle It -- Then Should Return Silently and Record the Failure`() {
        reset(smsProvider, callProvider)
        val request = NotificationRequest.newBuilder().setNotificationType(SMS).build()
        connection.publish("notifier.notifications.sms", request.toByteArray())
        waitForConsumerToCatchUp()

        verify(smsProvider).canNotify(any<SmsNotification>())
        verify(callProvider).canNotify(any<SmsNotification>())
        verifyNoMoreInteractions(smsProvider, callProvider)

        val received = receivedCounter()
        val handled = handledTimer("failed", "NoNotificationHandler")
        assertThat(received.count()).isOne()
        assertThat(handled.count()).isOne()
        assertThat(handled.measure()).isNotEmpty
        assertThat(handled.takeSnapshot().percentileValues().map { it.percentile() })
            .containsExactlyInAnyOrder(0.5, 0.75, 0.9, 0.95, 0.99)
    }

    @ParameterizedTest
    @MethodSource("provideFailedNotifications")
    fun `Dispatch -- When Failed -- Should've Interacted with Providers`(f: FailedNotification, expected: String) {
        val captor = argumentCaptor<SmsNotification>()
        stub {
            on { runBlocking { smsProvider.notify(captor.capture()) } } doReturn f
        }

        val request = NotificationRequest.newBuilder()
            .setNotificationType(SMS)
            .addRecipient("09121231234")
            .setMessage("Message")
            .build()
        connection.publish("notifier.notifications.sms", request.toByteArray())
        waitForConsumerToCatchUp()

        verify(smsProvider).canNotify(any())
        verify(callProvider, atMost(1)).canNotify(any())
        verifyNoMoreInteractions(callProvider)
        runBlocking { verify(smsProvider).notify(any()) }

        assertThat(captor.firstValue.message).isEqualTo("Message")
        assertThat(captor.firstValue.recipients).contains("09121231234")

        val received = receivedCounter()
        val handled = handledTimer("failed", expected)
        assertThat(received.count()).isOne()
        assertThat(handled.count()).isOne()
        assertThat(handled.measure()).isNotEmpty
        assertThat(handled.takeSnapshot().percentileValues().map { it.percentile() })
            .containsExactlyInAnyOrder(0.5, 0.75, 0.9, 0.95, 0.99)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "successful log"])
    fun `Dispatch -- When Successful -- Should've Interacted with Providers`(log: String) {
        val captor = argumentCaptor<CallNotification>()
        stub {
            on { runBlocking { callProvider.notify(captor.capture()) } } doReturn SuccessfulNotification(if (log.isBlank()) null else log)
        }

        val request = NotificationRequest.newBuilder()
            .setNotificationType(CALL)
            .addRecipient("09121231234")
            .setMessage("Message")
            .build()
        connection.publish("notifier.notifications.call", request.toByteArray())
        waitForConsumerToCatchUp()

        verify(callProvider).canNotify(any())
        verify(smsProvider, atMost(1)).canNotify(any())
        verifyNoMoreInteractions(smsProvider)
        runBlocking { verify(callProvider).notify(any()) }

        assertThat(captor.firstValue.message).isEqualTo("Message")
        assertThat(captor.firstValue.recipients).contains("09121231234")

        val received = receivedCounter()
        val handled = handledTimer()
        assertThat(received.count()).isOne()
        assertThat(handled.count()).isOne()
        assertThat(handled.measure()).isNotEmpty
        assertThat(handled.takeSnapshot().percentileValues().map { it.percentile() })
            .containsExactlyInAnyOrder(0.5, 0.75, 0.9, 0.95, 0.99)
    }

    private fun receivedCounter() = meterRegistry.get("notifier.notifications.received").counter()

    private fun handledTimer(status: String = "ok", exception: String = "none") =
        meterRegistry.get("notifier.notifications.handled").tag("status", status).tag("exception", exception).timer()

    private fun waitForConsumerToCatchUp() {
        Thread.sleep(1000)
    }

    companion object {

        @JvmStatic
        fun provideFailedNotifications() = listOf(
            arguments(FailedNotification(exception = IllegalArgumentException(""), log = "failed"), "IllegalArgumentException"),
            arguments(FailedNotification(log = "failed"), "Unknown"),
            arguments(FailedNotification(), "Unknown")
        )
    }
}
