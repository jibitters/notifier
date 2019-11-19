package ir.jibit.notifier.config.nats

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import ir.jibit.notifier.listener.NotificationDispatcher
import ir.jibit.notifier.util.logger
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

/**
 * Configuration responsible for creating a Nats connection and a dispatcher to
 * listen for new messages and forwards them to appropriate notification providers.
 */
@Configuration
@EnableConfigurationProperties(NatsProperties::class)
class NatsConfiguration {

    /**
     * The logger.
     */
    private val log = logger<NatsConfiguration>()

    /**
     * The Nats connection.
     */
    private var connection: Connection? = null

    /**
     * Creates a new Nats connection at application start and dispatches a new listener to handle
     * incoming messages.
     */
    @Bean
    fun connection(properties: NatsProperties, dispatcher: NotificationDispatcher): Connection {
        val options = Options.Builder()
            .servers(properties.servers!!.toTypedArray())
            .executor(Executors.newWorkStealingPool(properties.poolSize!!))
            .build()

        connection = Nats.connect(options)
        connection!!.createDispatcher { dispatcher.dispatch(it.data) }
            .subscribe(properties.subject, "notifier-group")

        return connection!!
    }

    /**
     * Closes the Nats connection on application teardown.
     */
    @PreDestroy
    fun closeConnection() {
        log.info("About to shutdown the Nats connection")
        connection?.close()
        log.info("Nats connection has been destroyed")
    }
}
