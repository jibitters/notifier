package ir.jibit.notifier.config.retrofit

import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.util.concurrent.TimeUnit.SECONDS
import javax.validation.constraints.NotNull

/**
 * Contains [OkHttpClient] configurations.
 *
 * @author Younes Rahimi
 */
@Validated
@Configuration
@ConfigurationProperties(prefix = "ok-http")
class OkHttpClientConfig {

    /**
     * Represents the OkHttp call timeout in seconds.
     */
    @NotNull(message = "OkHttp call timeout is required")
    var callTimeout: Long? = null

    /**
     * Represents the OkHttp connect timeout in seconds.
     */
    @NotNull(message = "OkHttp connect timeout is required")
    var connectTimeout: Long? = null

    /**
     * Represents the OkHttp read timeout in seconds.
     */
    @NotNull(message = "OkHttp read timeout is required")
    var readTimeout: Long? = null

    /**
     * Registers a bean of type [OkHttpClient] with customized timeouts.
     */
    @Bean
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(callTimeout!!, SECONDS)
        .connectTimeout(connectTimeout!!, SECONDS)
        .readTimeout(readTimeout!!, SECONDS).build()
}
