@file:Suppress("ProtectedInFinal")

package ir.jibit.notifier.config.nats

import com.nhaarman.mockitokotlin2.mock
import io.nats.client.Connection
import io.nats.client.Connection.Status.CLOSED
import io.nats.client.Connection.Status.CONNECTED
import ir.jibit.notifier.NatsExtension
import ir.jibit.notifier.listener.NotificationDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for [NatsConfiguration] configuration class.
 *
 * @author Ali Dehghani
 */
@ActiveProfiles("test")
@ExtendWith(NatsExtension::class)
internal class NatsConfigurationIT {

    @Test
    fun `On Shutdown - The Nats Connection Should be Closed`() {
        var connection: Connection? = null
        val port = System.getProperty("TEST_NATS_PORT")?.toLong() ?: 4222
        ApplicationContextRunner()
            .withPropertyValues("nats.servers=nats://localhost:$port")
            .withUserConfiguration(NatsConfigurationITTestConfig::class.java, NatsConfiguration::class.java)
            .run {
                connection = it.getBean(Connection::class.java)
                assertThat(connection?.status).isEqualTo(CONNECTED)
            }

        assertThat(connection?.status).isEqualTo(CLOSED)
    }

    /**
     * Test configuration.
     */
    @EnableConfigurationProperties(NatsProperties::class)
    protected class NatsConfigurationITTestConfig {

        @Bean
        fun dispatcher(): NotificationDispatcher = mock()
    }
}
