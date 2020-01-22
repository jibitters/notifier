package ir.jibit.notifier.provider.sms.kavehnegar

import ir.jibit.notifier.config.http.HttpProperties
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.NotificationResponse
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URI.create
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.noBody
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ExecutorService
import java.util.function.BiFunction

/**
 * A Kavehnegar implementation of [Notifier] responsible for sending text messages and
 * making calls.
 *
 * @param properties Encapsulates the Kavehnegar related properties.
 * @param httpProperties Encapsulates the HTTP configurations.
 * @param httpClient The HTTP client used to send requests to Kavehnegar.
 * @param ioExecutor Responsible for running all IO-bound tasks.
 *
 * @author Ali Dehghani
 */
@Service
@EnableConfigurationProperties(KavehnegarProperties::class)
@ConditionalOnProperty(name = ["sms-providers.use"], havingValue = "kavehnegar")
class KavehnegarNotifier(private val properties: KavehnegarProperties,
                         private val httpProperties: HttpProperties,
                         private val httpClient: HttpClient,
                         private val ioExecutor: ExecutorService) : Notifier {

    /**
     * Can only handle SMS and call notifications.
     */
    override fun canNotify(notification: Notification) =
        notification is SmsNotification || notification is CallNotification

    /**
     * Builds the HTTP request and sends it to Kavehnegar
     */
    override fun notify(notification: Notification): CompletableFuture<NotificationResponse> {
        try {
            return when (notification) {
                is CallNotification -> makeCall(notification)
                is SmsNotification -> sendSms(notification)
                else -> return completedFuture(FailedNotification(log = "The ${notification::class.simpleName} is not supported"))
            }
        } catch (e: Exception) {
            return completedFuture(FailedNotification(e))
        }
    }

    /**
     * Responsible to call the make call API.
     */
    private fun makeCall(notification: Notification): CompletableFuture<NotificationResponse> {
        val uri = properties.getUri(notification as CallNotification)
        val request = HttpRequest.newBuilder(uri).timeout(httpProperties.timeout).POST(noBody()).build()

        return httpClient.sendAsync(request, BodyHandlers.ofString()).handleIo { response, exception ->
            if (exception != null) FailedNotification(exception = exception)
            else handleResponse(response)
        }
    }

    /**
     * Responsible to call the send sms API.
     */
    private fun sendSms(notification: Notification): CompletableFuture<NotificationResponse> {
        val uri = properties.getUri(notification as SmsNotification)
        val request = HttpRequest.newBuilder(uri).timeout(httpProperties.timeout).POST(noBody()).build()

        return httpClient.sendAsync(request, BodyHandlers.ofString()).handleIo { response, exception ->
            if (exception != null) FailedNotification(exception = exception)
            else handleResponse(response)
        }
    }

    /**
     * If the status code from the Kavehnegar's API is anything less than 300, then we would consider this as a successful
     * API call. Otherwise, it would be considered as a failed attempt.
     */
    private fun handleResponse(response: HttpResponse<String>): NotificationResponse {
        if (response.statusCode() < 300) {
            return SuccessfulNotification(response.body())
        }

        return FailedNotification(log = response.body())
    }

    /**
     * Constructs the appropriate URL for sending a SMS based on the [KavehnegarProperties] and
     * the given [SmsNotification] details.
     */
    private fun KavehnegarProperties.getUri(notification: SmsNotification): URI {
        val receptors = notification.recipients.joinToString().urlEncode()
        val message = notification.message.urlEncode()

        return create("${baseUrl}v1/$token/sms/send.json?receptor=${receptors}&message=${message}&sender=${sender}")
    }

    /**
     * Constructs the appropriate URL for making a Call based on the [KavehnegarProperties] and the given
     * [CallNotification].
     */
    private fun KavehnegarProperties.getUri(notification: CallNotification): URI {
        val receptors = notification.recipients.joinToString().urlEncode()
        val message = notification.message.urlEncode()

        return create("${baseUrl}v1/$token/call/maketts.json?receptor=${receptors}&message=$message")
    }

    private fun <T, U> CompletableFuture<T>.handleIo(fn: (T, Throwable?) -> U): CompletableFuture<U> =
        handleAsync(BiFunction { ok, failure -> fn(ok, failure) }, ioExecutor)

    private fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8)
}
