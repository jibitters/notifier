package ir.jibit.notifier.config.nats

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * Encapsulates configuration properties to customize the behavior of the notifier.
 *
 * @author Ali Dehghani
 */
@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "nats")
class NatsProperties(

    /**
     * Collection of Nats services to connect with.
     */
    @field:NotEmpty(message = "At least one NATS server address is required (nats.servers)")
    val servers: Set<String>? = null,

    /**
     * Determines the core pool size of the thread pool executor response for dispatching messages.
     */
    @field:NotNull(message = "The core pool size of NATS dispatcher is required (nats.pool-size)")
    val poolSize: Int? = Runtime.getRuntime().availableProcessors(),

    /**
     * The NATS topic to listen to.
     */
    @field:NotNull(message = "You should specify a topic to listen for incoming notification requests (nats.subject)")
    val subject: String? = "notifier.notifications.*"
)
