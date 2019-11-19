package ir.jibit.notifier.config.http

import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Contains HTTP client configurations.
 */
@Configuration
@EnableConfigurationProperties(HttpProperties::class)
class HttpConfiguration {

    /**
     * Registers a bean of type [OkHttpClient] with customized timeouts.
     */
    @Bean
    fun httpClient(properties: HttpProperties): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(properties.callTimeout!!)
        .connectTimeout(properties.connectTimeout!!)
        .readTimeout(properties.readTimeout!!).build()
}
