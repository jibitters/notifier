package ir.jibit.notifier.provider.sms.kavehnegar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory


/**
 * Registers Kavehnegar client.
 *
 * @param client The [OkHttpClient] used by Retrofit to create clients.
 *
 * @author Younes Rahimi
 */
@Configuration
class KavehnegarConfig(private val client: OkHttpClient) {

    /**
     * Registers the [KavehnegarClient] as a Spring Bean.
     *
     * @return An instance of [KavehnegarClient].
     */
    @Bean
    fun kavehnegarClient(properties: KavehnegarProperties): KavehnegarClient {
        val retrofit = getBuilder().baseUrl(properties.baseUrl).build()
        return retrofit.create(KavehnegarClient::class.java)
    }

    /**
     * Creates a specific object mapper used by kavehnegar client.
     */
    private fun objectMapper() =
        ObjectMapper()
            .setPropertyNamingStrategy(SNAKE_CASE)
            .registerModule(KotlinModule())

    /**
     * Retrofit client builder configurator.
     *
     * @return Returns back a builder instance.
     */
    private fun getBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper()))
    }


}
