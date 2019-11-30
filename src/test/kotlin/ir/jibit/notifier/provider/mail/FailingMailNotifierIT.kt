@file:Suppress("ProtectedInFinal")

package ir.jibit.notifier.provider.mail

import ir.jibit.notifier.provider.FailedNotification
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.mail.MailSendException
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
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
@TestPropertySource(properties = ["spring.mail.host=localhost", "spring.mail.port=26"])
internal class FailingMailNotifierIT {

    /**
     * Subject under test.
     */
    @Autowired
    private lateinit var notifier: MailNotifier

    @Test
    fun `When We Fail to Send the Notification, We Should Send a FailedNotification`() {
        runBlocking {
            val response = notifier.notify(MailNotification("Subject", "Message", recipients = setOf("0912xxx")))
            assertThat(response).isInstanceOf(FailedNotification::class.java)
            response as FailedNotification
            assertThat(response.exception).isInstanceOf(MailSendException::class.java)
        }
    }

    /**
     * A simple test configuration to pickup the required notifiers.
     */
    @TestConfiguration
    @ComponentScan(basePackageClasses = [MailNotifier::class])
    @ImportAutoConfiguration(MailSenderAutoConfiguration::class)
    protected class FailingMailNotifierITConfig
}
