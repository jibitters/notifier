package ir.jibit.notifier.config.http

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
     * Registers a bean of type [OkHttpClient] with customized timeouts.
     */
    @Bean
    fun httpClient(properties: HttpProperties, ioDispatcher: ExecutorService): OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(ioDispatcher))
        .callTimeout(properties.callTimeout)
        .connectTimeout(properties.connectTimeout)
        .readTimeout(properties.readTimeout).build()
}
