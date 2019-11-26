package ir.jibit.notifier.provider.sms.kavehnegar

import ir.jibit.notifier.provider.*
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException

/**
 * A Kavehnegar implementation of [Notifier] responsible for sending text messages and
 * making calls.
 *
 * @param properties Encapsulates the Kavehnegar related properties.
 * @param okHttpClient The OK HTTP client used to send requests to Kavehnegar.
 *
 * @author Ali Dehghani
 */
@Service
@EnableConfigurationProperties(KavehnegarProperties::class)
@ConditionalOnProperty(name = ["sms-providers.use"], havingValue = "kavehnegar")
class KavehnegarNotifier(private val properties: KavehnegarProperties,
                         private val okHttpClient: OkHttpClient) : Notifier {

    /**
     * Retrofit client to send requests to Kavehnegar.
     */
    private val client = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .baseUrl(properties.baseUrl!!)
        .build()
        .create(KavehnegarClient::class.java)

    /**
     * Can only handle SMS and call notifications.
     */
    override fun canNotify(notification: Notification) =
        notification is SmsNotification || notification is CallNotification

    /**
     * Builds the HTTP request and sends it to Kavehnegar
     */
    override fun notify(notification: Notification): NotificationResponse {
        try {
            val response = when (notification) {
                is CallNotification -> makeCall(notification)
                is SmsNotification -> sendSms(notification)
                else -> return FailedNotification(log = "The ${notification::class.simpleName} is not supported")
            }

            val errorBody = response.errorBody()?.string()
            if (!response.isSuccessful)
                return FailedNotification(log = errorBody)

            return SuccessfulNotification(response.body())
        } catch (ex: IOException) {
            return FailedNotification(ex)
        }
    }

    /**
     * Responsible to call the make call API.
     */
    private fun makeCall(notification: Notification): Response<String> {
        notification as CallNotification
        val receptors = notification.recipients.joinToString()

        return client.makeCall(properties.token!!, receptors, notification.message).execute()
    }

    /**
     * Responsible to call the send sms API.
     */
    private fun sendSms(notification: Notification): Response<String> {
        notification as SmsNotification
        val receptors = notification.recipients.joinToString()

        return client.sendSms(properties.token!!, receptors, notification.message, properties.sender!!).execute()
    }
}
