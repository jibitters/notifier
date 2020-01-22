package ir.jibit.notifier.config.http

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient
import java.util.concurrent.ExecutorService

/**
 * Contains HTTP client configurations.
 *
 * @author Ali Dehghani
 */
@Configuration
@EnableConfigurationProperties(HttpProperties::class)
class HttpConfiguration {

    /**
     * Registers a bean of type [HttpClient] with customized timeouts.
     */
    @Bean
    fun httpClient(properties: HttpProperties, ioDispatcher: ExecutorService): HttpClient = HttpClient.newBuilder()
        .connectTimeout(properties.timeout)
        .executor(ioDispatcher)
        .build()
}
