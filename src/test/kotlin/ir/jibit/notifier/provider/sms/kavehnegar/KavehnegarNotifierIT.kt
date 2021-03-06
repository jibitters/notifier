package ir.jibit.notifier.provider.sms.kavehnegar

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.http.Fault.MALFORMED_RESPONSE_CHUNK
import ir.jibit.notifier.WireMockExtension
import ir.jibit.notifier.config.dispatcher.IoDispatcher
import ir.jibit.notifier.config.http.HttpConfiguration
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.IOException
import java.net.http.HttpTimeoutException
import java.util.stream.Stream

/**
 * Integration tests for Kavehnegar notifier.
 *
 * @author Ali Dehghani
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class, WireMockExtension::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
internal class KavehnegarNotifierIT {

    /**
     * Subject under test.
     */
    @Autowired
    private lateinit var notifier: KavehnegarNotifier

    @ParameterizedTest
    @MethodSource("provideNotifications")
    fun `Only Supports SMS and Call Notifications`(notification: Notification, expected: Boolean) {
        assertThat(notifier.canNotify(notification)).isEqualTo(expected)
    }

    @Test
    fun `Invalid Notification -- Should Return FailedNotification`() {
        val response = notifier.notify(UnsupportedNotification).join()
        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.exception).isNull()
        assertThat(response.log).isEqualTo("The UnsupportedNotification is not supported")
    }

    @Test
    fun `SMS Notification -- 2xx Response -- Should Return SuccessfulNotification`() {
        val apiResponse = successfulResponse()
        val url = "/v1/token/sms/send.json?receptor=09129129123&message=message&sender=sender"
        stubFor(post(url).willReturn(ok(apiResponse)))

        val sms = SmsNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(SuccessfulNotification::class.java)
        response as SuccessfulNotification
        assertThat(response.log).isEqualTo(apiResponse)
    }

    @Test
    fun `SMS Notification -- 4xx or 5xx Response -- Should Return FailedNotification`() {
        val apiResponse = fourXXXResponse()
        val url = "/v1/token/sms/send.json?receptor=09129129123&message=message&sender=sender"
        stubFor(post(url).willReturn(status(412).withBody(apiResponse)))

        val sms = SmsNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isEqualTo(apiResponse)
        assertThat(response.exception).isNull()
    }

    @Test
    fun `SMS Notification -- Unexpected Response -- Should Return FailedNotification`() {
        val url = "/v1/token/sms/send.json?receptor=09129129123&message=message&sender=sender"
        stubFor(post(url).willReturn(aResponse().withFault(MALFORMED_RESPONSE_CHUNK)))

        val sms = SmsNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isNull()
        assertThat(response.exception).hasCauseInstanceOf(IOException::class.java)
    }

    @Test
    fun `SMS Notification -- Unexpected Delay -- Should Return FailedNotification`() {
        val url = "/v1/token/sms/send.json?receptor=09129129123&message=message&sender=sender"
        stubFor(post(url).willReturn(aResponse().withFixedDelay(2000).withBody("")))

        val sms = SmsNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isNull()
        assertThat(response.exception).hasCauseInstanceOf(HttpTimeoutException::class.java)
    }

    @Test
    fun `CALL Notification -- 2xx Response -- Should Return SuccessfulNotification`() {
        val apiResponse = successfulResponse()
        val url = "/v1/token/call/maketts.json?receptor=09129129123&message=message"
        stubFor(post(url).willReturn(ok(apiResponse)))

        val sms = CallNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(SuccessfulNotification::class.java)
        response as SuccessfulNotification
        assertThat(response.log).isEqualTo(apiResponse)
    }

    @Test
    fun `CALL Notification -- 4xx or 5xx Response -- Should Return FailedNotification`() {
        val apiResponse = fourXXXResponse()
        val url = "/v1/token/call/maketts.json?receptor=09129129123&message=message"
        stubFor(post(url).willReturn(status(412).withBody(apiResponse)))

        val sms = CallNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isEqualTo(apiResponse)
        assertThat(response.exception).isNull()
    }

    @Test
    fun `CALL Notification -- Unexpected Response -- Should Return FailedNotification`() {
        val url = "/v1/token/call/maketts.json?receptor=09129129123&message=message"
        stubFor(post(url).willReturn(aResponse().withFault(MALFORMED_RESPONSE_CHUNK)))

        val sms = CallNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isNull()
        assertThat(response.exception).hasCauseExactlyInstanceOf(IOException::class.java)
    }

    @Test
    fun `CALL Notification -- Unexpected Delay -- Should Return FailedNotification`() {
        val url = "/v1/token/call/maketts.json?receptor=09129129123&message=message"
        stubFor(post(url).willReturn(aResponse().withFixedDelay(2000).withBody("")))

        val sms = CallNotification("message", setOf("09129129123"))
        val response = notifier.notify(sms).join()

        assertThat(response).isInstanceOf(FailedNotification::class.java)
        response as FailedNotification
        assertThat(response.log).isNull()
        assertThat(response.exception).hasCauseInstanceOf(HttpTimeoutException::class.java)
    }

    private fun successfulResponse() = """
        {
            "return": {
                "status": 200,
                "message": "تایید شد"
            },
            "entries": [
                {
                    "messageid": 8792343,
                    "message": "خدمات پیام کوتاه کاوه نگار",
                    "status": 1,
                    "statustext": "در صف ارسال",
                    "sender": "10004346",
                    "receptor": "09125258596",
                    "date": 1356619709,
                    "cost": 120
                }
            ]
        }
        """.trimIndent()

    private fun fourXXXResponse() = """
        {
            "return": {
                "status": 412,
                "message": "ارسال کننده نامعتبر است"
            },
            "entries": null
        }
        """.trimIndent()

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun provideNotifications(): Stream<Arguments> = Stream.of(
            arguments(SmsNotification("", setOf()), true),
            arguments(CallNotification("", setOf()), true),
            arguments(UnsupportedNotification, false)
        )
    }

    /**
     * A simple test configuration to pickup the required notifiers.
     */
    @TestConfiguration
    @Suppress("ProtectedInFinal")
    @ComponentScan(basePackageClasses = [KavehnegarNotifier::class, HttpConfiguration::class, IoDispatcher::class])
    protected class KavehnegarNotifierITConfig

    /**
     * An invalid implementation of a notification.
     */
    private object UnsupportedNotification : Notification
}
