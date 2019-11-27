package ir.jibit.notifier.config.http

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

/**
 * Encapsulates the HTTP client configurations.
 *
 * @author Ali Dehghani
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "http")
class HttpProperties(

    /**
     * Represents the HTTP call timeout in seconds.
     */
    val callTimeout: Duration = Duration.ofSeconds(1),

    /**
     * Represents the HTTP connect timeout in seconds.
     */
    val connectTimeout: Duration = Duration.ofSeconds(1),

    /**
     * Represents the HTTP read timeout in seconds.
     */
    val readTimeout: Duration = Duration.ofSeconds(1)
)
