@file:Suppress("ProtectedInFinal")

package ir.jibit.notifier.provider.sms.kavehnegar

import ir.jibit.notifier.config.dispatcher.IoDispatcher
import ir.jibit.notifier.config.http.HttpConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for Kavehnegar notifier and application context.
 *
 * @author Ali Dehghani
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["sms-providers.use=invalid"])
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
internal class KavehnegarNotifierContextIT {

    /**
     * Spring's context to query a few beans.
     */
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `When Disabled -- Should Not Register Kavehnegar Related Beans`() {
        assertThrows<NoSuchBeanDefinitionException> { context.getBean(KavehnegarNotifier::class.java) }
        assertThrows<NoSuchBeanDefinitionException> { context.getBean(KavehnegarProperties::class.java) }
    }

    /**
     * A simple test configuration to pickup the required notifiers.
     */
    @TestConfiguration
    @ComponentScan(basePackageClasses = [KavehnegarNotifier::class, HttpConfiguration::class, IoDispatcher::class])
    protected class KavehnegarNotifierContextITConfig
}
