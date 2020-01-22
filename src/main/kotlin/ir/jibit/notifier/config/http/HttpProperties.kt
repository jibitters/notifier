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
     * Represents the HTTP connect timeout in seconds.
     */
    val timeout: Duration = Duration.ofSeconds(1)
)
