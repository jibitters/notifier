package ir.jibit.notifier.config.dispatcher

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Encapsulates configuration about the IO thread-pool we're going use for
 * executing notification requests.
 *
 * @author Ali Dehghani
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "io-dispatcher")
class DispatcherProperties(

    /**
     * Determines the number of threads this dispatcher going to have. If not present,
     * the default number is twice the number of CPU cores.
     */
    val poolSize: Int = Runtime.getRuntime().availableProcessors() * 2,

    /**
     * The prefix for thread names.
     */
    val threadPrefix: String = "notifier-io"
)
