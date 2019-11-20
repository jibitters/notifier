@file:Suppress("ProtectedInFinal")

package ir.jibit.notifier.listener

import com.nhaarman.mockitokotlin2.*
import io.micrometer.core.instrument.MeterRegistry
import io.nats.client.Connection
import ir.jibit.notifier.NatsExtension
import ir.jibit.notifier.config.nats.NatsConfiguration
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import ir.jibit.notifier.stubs.Notification.NotificationRequest
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.CALL
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.SMS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration

/**
 * Integration tests for notification dispatching subsystem.
 */
@ActiveProfiles("test")
@MockBean(MeterRegistry::class)
@ExtendWith(SpringExtension::class, NatsExtension::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
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

    @BeforeEach
    fun setUp() {
        stub {
            on { smsProvider.canNotify(any<SmsNotification>())} doReturn true
            on { callProvider.canNotify(any<CallNotification>())} doReturn true
        }
        connection.flush(Duration.ofSeconds(1))
    }

    @Test
    fun `Dispatch -- Can't Parse the Notification - Won't Interact with Notifiers`() {
        connection.publish("notifier.notifications.sms", byteArrayOf(10, 12))
        waitForConsumerToCatchUp()

        verifyZeroInteractions(smsProvider, callProvider)
    }

    @Test
    fun `Dispatch -- When Failed -- Should've Interacted with Providers`() {
        val captor = argumentCaptor<SmsNotification>()
        stub {
            on { smsProvider.notify(captor.capture()) } doReturn FailedNotification()
        }

        val request = NotificationRequest.newBuilder()
            .setType(SMS)
            .addRecipients("09121231234")
            .setMessage("Message")
            .build()
        connection.publish("notifier.notifications.sms", request.toByteArray())
        waitForConsumerToCatchUp()

        verify(smsProvider).canNotify(any())
        verify(callProvider, atMost(1)).canNotify(any())
        verifyNoMoreInteractions(callProvider)
        verify(smsProvider).notify(any())

        assertThat(captor.firstValue.message).isEqualTo("Message")
        assertThat(captor.firstValue.recipients).contains("09121231234")
    }

    @Test
    fun `Dispatch -- When Successful -- Should've Interacted with Providers`() {
        val captor = argumentCaptor<CallNotification>()
        stub {
            on { callProvider.notify(captor.capture()) } doReturn SuccessfulNotification()
        }

        val request = NotificationRequest.newBuilder()
            .setType(CALL)
            .addRecipients("09121231234")
            .setMessage("Message")
            .build()
        connection.publish("notifier.notifications.call", request.toByteArray())
        waitForConsumerToCatchUp()

        verify(callProvider).canNotify(any())
        verify(smsProvider, atMost(1)).canNotify(any())
        verifyNoMoreInteractions(smsProvider)
        verify(callProvider).notify(any())

        assertThat(captor.firstValue.message).isEqualTo("Message")
        assertThat(captor.firstValue.recipients).contains("09121231234")
    }

    private fun waitForConsumerToCatchUp() {
        Thread.sleep(3000)
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = [NatsConfiguration::class, NotificationDispatcher::class])
    protected class NotificationDispatcherITConfig
}
