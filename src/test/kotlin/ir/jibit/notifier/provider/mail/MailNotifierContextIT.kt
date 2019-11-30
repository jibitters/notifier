@file:Suppress("ProtectedInFinal")

package ir.jibit.notifier.provider.mail

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for [MailNotifier]'s conditional registration.
 *
 * @author Ali Dehghani
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
internal class MailNotifierContextIT {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `When There Is No JavaMailSender, Then We Should Not Register Mail Notifier`() {
        assertThrows<NoSuchBeanDefinitionException> { context.getBean(MailNotifier::class.java) }
    }

    /**
     * A simple test configuration to pickup the required notifiers.
     */
    @TestConfiguration
    @ComponentScan(basePackageClasses = [MailNotifier::class])
    @ImportAutoConfiguration(MailSenderAutoConfiguration::class)
    protected class MailNotifierContextITConfig
}
