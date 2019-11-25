package ir.jibit.notifier.config.http

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import javax.validation.constraints.NotNull

/**
 * Encapsulates the HTTP client configurations.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "http")
class HttpProperties(

    /**
     * Represents the HTTP call timeout in seconds.
     */
    @field:NotNull(message = "HTTP call timeout is required (http.call-timeout)")
    val callTimeout: Duration? = Duration.ofSeconds(1),

    /**
     * Represents the HTTP connect timeout in seconds.
     */
    @field:NotNull(message = "HTTP connect timeout is required (http.connect-timeout)")
    val connectTimeout: Duration? = Duration.ofSeconds(1),

    /**
     * Represents the HTTP read timeout in seconds.
     */
    @field:NotNull(message = "HTTP read timeout is required (http.read-timeout)")
    val readTimeout: Duration? = Duration.ofSeconds(1)
)
