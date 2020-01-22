package ir.jibit.notifier.provider.mail

import ir.jibit.notifier.MailExtension
import ir.jibit.notifier.config.dispatcher.IoDispatcher
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.SuccessfulNotification
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for [MailNotifier].
 *
 * @author Ali Dehghani
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class, MailExtension::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
@TestPropertySource(properties = ["spring.mail.host=localhost", "spring.mail.port=2500"])
internal class MailNotifierIT {

    /**
     * Subject under test.
     */
    @Autowired
    private lateinit var notifier: MailNotifier

    @ParameterizedTest
    @MethodSource("provideNotifications")
    fun `Only Supports SMS and Call Notifications`(notification: Notification, expected: Boolean) {
        assertThat(notifier.canNotify(notification)).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("provideInvalidMails")
    fun `Given Invalid Mails, It Should Return FailedNotification`(notification: Notification, expected: String) {
        val response = notifier.notify(notification).join()
        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isEqualTo(expected)
    }

    @Test
    fun `Given Invalid Type, Should Throw Exception`() {
        assertThatThrownBy { notifier.notify(UnsupportedNotification).join() }
            .isInstanceOf(ClassCastException::class.java)
    }

    @ParameterizedTest
    @MethodSource("provideNotificationsToSend")
    fun `Given Valid Notifications, It Should Be Able To Actually Send Them`(notification: Notification) {
        assertThat(notifier.notify(notification).join()).isInstanceOf(SuccessfulNotification::class.java)
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun provideNotifications() = listOf(
            arguments(MailNotification("", "", setOf()), true),
            arguments(UnsupportedNotification, false)
        )

        @JvmStatic
        @Suppress("unused")
        fun provideInvalidMails() = listOf(
            arguments(MailNotification("", "", emptySet()), "Email should have at least one recipient"),
            arguments(MailNotification("", "", setOf("")), "Email should have a valid subject"),
            arguments(MailNotification("   ", "", setOf("")), "Email should have a valid subject"),
            arguments(MailNotification("Subject", "", setOf("")), "Email should have a valid body"),
            arguments(MailNotification("Subject", "  ", setOf("")), "Email should have a valid body")
        )

        @JvmStatic
        @Suppress("unused")
        fun provideNotificationsToSend() = listOf(
            arguments(MailNotification("Subject", "Message", setOf("a@g.com", "b@g.com"))),
            arguments(MailNotification("Subject", "Message", setOf("a@g.com"), "me")),
            arguments(MailNotification("Subject", "Message", setOf("a@g.com"), "me", setOf("b@g.com"))),
            arguments(MailNotification("Subject", "Message", setOf("a@g.com"), "me", setOf("d@g.com"), setOf("c@g.com")))
        )
    }

    /**
     * A simple test configuration to pickup the required notifiers.
     */
    @TestConfiguration
    @Suppress("ProtectedInFinal")
    @ImportAutoConfiguration(MailSenderAutoConfiguration::class)
    @ComponentScan(basePackageClasses = [MailNotifier::class, IoDispatcher::class])
    protected class MailNotifierITConfig

    /**
     * An invalid implementation of a notification.
     */
    private object UnsupportedNotification : Notification
}
