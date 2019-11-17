package ir.jibit.notifier.provider.sms.kavehnegar

import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.NotificationResponse
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import ir.jibit.notifier.util.logger
import okhttp3.Headers
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import retrofit2.Response
import java.io.IOException

/**
 * A Kavehnegar implementation of [Notifier] responsible for sending text messages.
 *
 * @param kavehnegarClient The Retrofit client used to send requests to Kavehnegar.
 * @param properties Encapsulates the Kavehnegar related properties.
 *
 * @author Younes Rahimi
 */
@Service
class KavehnegarNotifier(private val kavehnegarClient: KavehnegarClient,
                         private val properties: KavehnegarProperties) : Notifier {

    private val log = logger<KavehnegarNotifier>()

    /**
     * Can only handle SMS and call notifications.
     */
    override fun canNotify(notification: Notification) =
        notification is SmsNotification || notification is CallNotification

    /**
     * Builds the HTTP request and sends it to Kavehnegar
     */
    override suspend fun notify(notification: Notification): NotificationResponse {
        try {
            val response = when (notification) {
                is CallNotification -> makeCall(notification)
                is SmsNotification -> sendSms(notification)
                else -> throw IllegalStateException()
            }

            val errorBody = errorBody(response)
            log.debug("Notify {}. Request {}, Response Body {}, Response Error Body: {}",
                notification.javaClass.simpleName, notification, response.body(), errorBody)

            if (!response.isSuccessful)
                return FailedNotification(createHttpClientError(notification.toString(), response, errorBody))

            val body = response.body()
            if (body == null || body.`return`.status != 200)
                return FailedNotification(createHttpClientError(notification.toString(),
                    response, body.toString()))

            return body.toNotification()
        } catch (ex: IOException) {
            return FailedNotification(ex)
        }
    }

    /**
     * Responsible to call kavehnegar make call API.
     */
    private suspend fun makeCall(notification: Notification): Response<KavehnegarResponse> {
        notification as CallNotification
        val receptors = notification.recipients.joinToString()

        return kavehnegarClient.makeCall(properties.token, receptors, notification.message)
    }

    /**
     * Responsible to call kavehnegar send sms API.
     */
    private suspend fun sendSms(notification: Notification): Response<KavehnegarResponse> {
        notification as SmsNotification
        val receptors = notification.recipients.joinToString()

        return kavehnegarClient.sendSms(properties.token, receptors, notification.message, properties.sender)
    }

    /**
     * Gets the error body of response as string.
     *
     * @param response Retrofit response.
     * @return ToString value of error body.
     */
    private fun errorBody(response: Response<*>): String = try {
        if (response.errorBody() != null)
            response.errorBody()!!.string()
        else
            "No Error"
    } catch (e: IOException) {
        log.warn("Can't generate response error body string. Error {}", e.message)

        "Error on getting error Body!"
    }

    /**
     * Creates a [HttpClientErrorException] from the Retrofit [Response].
     */
    private fun createHttpClientError(request: String, response: Response<*>, errorBody: String): HttpClientErrorException =
        HttpClientErrorException.create(
            getStatueCode(response.code()),
            response.message(),
            convertHeaders(response.headers(), request),
            errorBody.toByteArray(),
            null
        )

    /**
     * Find the [HttpStatus] with code equals to `code`.
     *
     * @param code Http code.
     * @return The founded [HttpStatus].
     */
    private fun getStatueCode(code: Int): HttpStatus =
        HttpStatus.values().firstOrNull { it.value() == code } ?: throw IllegalStateException()

    /**
     * Convert OkHttp [Headers] to Spring [HttpHeaders].
     *
     * @param headers The OkHttp headers.
     * @param request The string representation of request.
     * @return The converted [HttpHeaders].
     */
    private fun convertHeaders(headers: Headers, request: String): HttpHeaders = HttpHeaders().apply {
        putAll(headers.toMultimap())
        add(X_REQUEST, request)
    }

    companion object {
        /**
         * A custom header key used to persist original request in [HttpClientErrorException].
         */
        const val X_REQUEST = "X-REQUEST"
    }
}
