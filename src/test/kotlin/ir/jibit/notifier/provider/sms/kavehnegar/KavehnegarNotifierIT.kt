package ir.jibit.notifier.provider.sms.kavehnegar

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import ir.jibit.notifier.config.retrofit.OkHttpClientConfig
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import ir.jibit.notifier.util.Jackson
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for [KavehnegarNotifier] implementation.
 *
 * @author Younes Rahimi
 */
@AutoConfigurationPackage
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
class KavehnegarNotifierIT {

    /**
     * subject under test.
     */
    @Autowired
    private lateinit var kavehnegarNotifier: KavehnegarNotifier

    @ParameterizedTest
    @MethodSource("providesNotificationsForCanNotify")
    fun `This implementation can only handle SMS notifications`(notification: Notification, expected: Boolean) {
        assertThat(kavehnegarNotifier.canNotify(notification))
            .isEqualTo(expected)
    }

    @Test
    fun `We should be able to handle successful operations in sms scenario`(): Unit = runBlocking {
        val token = "fake-token"
        val url = "/v1/$token/sms/send.json?receptor=09951183180&message=hi&sender=10002002200220"
        val json = """
            {
                "return": {
                    "status":200,
                    "message":"تایید شد"
                },
                "entries": [
                    {
                         "messageid": 8792343,
                         "message": "hi",
                         "status": 1,
                         "statustext": "در صف ارسال",
                         "sender": 1000660,
                         "receptor": "09951183180",
                         "date": 1356619709,
                         "cost": 120
                    }
                ]
            }
        """.trimIndent()

        stubFor(post(url).willReturn(ok().withBody(json)))
        val notification = SmsNotification("hi", setOf("09951183180"))
        val notify = kavehnegarNotifier.notify(notification)

        assertThat(notify).isInstanceOf(SuccessfulNotification::class.java)
        notify as SuccessfulNotification
        assertThat(notify.log).isNotNull()
        val kavehnegarResponse: KavehnegarResponse = Jackson.fromJson(notify.log!!)
        assertThat(kavehnegarResponse.entries?.last()?.messageId).isEqualTo(8792343)
        assertThat(kavehnegarResponse.entries?.last()?.message).isEqualTo("hi")
        assertThat(kavehnegarResponse.entries?.last()?.cost).isEqualTo(120)
        assertThat(kavehnegarResponse.entries?.last()?.date).isEqualTo(1356619709)
        assertThat(kavehnegarResponse.entries?.last()?.receptor).isEqualTo("09951183180")
        assertThat(kavehnegarResponse.entries?.last()?.sender).isEqualTo("1000660")
        assertThat(kavehnegarResponse.entries?.last()?.status).isEqualTo(1)
        assertThat(kavehnegarResponse.entries?.last()?.statusText).isEqualTo("در صف ارسال")

        assertThat(kavehnegarResponse.`return`.message).isEqualTo("تایید شد")
        assertThat(kavehnegarResponse.`return`.status).isEqualTo(200)
        Unit
    }

    @Test
    fun `We should be able to handle successful operations in call scenario`(): Unit = runBlocking {
        val token = "fake-token"
        val url = "/v1/$token/call/maketts.json?receptor=09951183180&message=hi"
        val json = """
            {
    "return": {
        "status": 200,
        "message": "تایید شد"
    },
    "entries": [
        {
            "messageid": 570629719,
            "message": "hi",
            "status": 5,
            "statustext": "ارسال به مخابرات",
            "sender": "02138062",
            "receptor": "09361183180",
            "date": 1551258694,
            "cost": 90
        }
    ]
}
        """.trimIndent()

        stubFor(post(url).willReturn(ok().withBody(json)))
        val notification = CallNotification("hi", setOf("09951183180"))
        val notify = kavehnegarNotifier.notify(notification)

        val kavehnegarResponse = notify as KavehnegarResponse
        assertThat(kavehnegarResponse.entries?.last()?.messageId).isEqualTo(570629719)
        assertThat(kavehnegarResponse.entries?.last()?.message).isEqualTo("hi")
        assertThat(kavehnegarResponse.entries?.last()?.cost).isEqualTo(90)
        assertThat(kavehnegarResponse.entries?.last()?.date).isEqualTo(1551258694)
        assertThat(kavehnegarResponse.entries?.last()?.receptor).isEqualTo("09361183180")
        assertThat(kavehnegarResponse.entries?.last()?.sender).isEqualTo("02138062")
        assertThat(kavehnegarResponse.entries?.last()?.status).isEqualTo(5)
        assertThat(kavehnegarResponse.entries?.last()?.statusText).isEqualTo("ارسال به مخابرات")

        assertThat(kavehnegarResponse.`return`.message).isEqualTo("تایید شد")
        assertThat(kavehnegarResponse.`return`.status).isEqualTo(200)
        Unit
    }

    @Test
    fun `We should be able to handle failed operations`(): Unit = runBlocking {
        val token = "fake-token"
        val url = "/v1/$token/sms/send.json?receptor=09951183180&message=%D8%B3%D9%84%D8%A7%D9%85&sender=10002002200220"
        val json = """{
                        "return": {
                            "status": 414,
                            "message": "تعداد رکورد ها بیشتر از حد مجاز است "
                        },
                        "entries": null
                    }""".trimIndent()

        stubFor(post(url).willReturn(ok().withBody(json)))
        val notification = SmsNotification("سلام", setOf("09951183180"))
        val notify = kavehnegarNotifier.notify(notification)

        assertThat(notify.toString()).isEqualTo("Empty Response with 200 status code")
        Unit
    }

    /**
     * Registers a the [KavehnegarNotifier] implementation into Spring's application context.
     */
    @TestConfiguration
    @EnableConfigurationProperties(OkHttpClientConfig::class, KavehnegarProperties::class)
    @ComponentScan(basePackageClasses = [KavehnegarNotifier::class, OkHttpClientConfig::class])
    class KavehnegarNotifierTestConfig {

        @Bean
        fun kavehnegarNotifier(kavehnegarClient: KavehnegarClient, kavehnegarProperties: KavehnegarProperties): KavehnegarNotifier =
            KavehnegarNotifier(kavehnegarClient, kavehnegarProperties.also { it.baseUrl = wireMockServer.baseUrl() })
    }

    companion object {

        /**
         * The mock server which we're going to use to mock the SMS API.
         */
        private lateinit var wireMockServer: WireMockServer

        /**
         * Spins up the mock server before all tests.
         */
        @JvmStatic
        @BeforeAll
        fun startServer() {
            wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
            wireMockServer.start()
            configureFor(wireMockServer.port())
        }

        /**
         * Stops the server after all tests.
         */
        @AfterAll
        @JvmStatic
        fun stopServer() {
            wireMockServer.stop()
        }

        @JvmStatic
        private fun providesNotificationsForCanNotify() = listOf(
            of(SmsNotification("message", setOf("0912xx")), true),
            of(CallNotification("Hello", setOf("0934xx")), true),
            of(OtherNotification(), false)
        )
    }

    internal class OtherNotification : Notification
}

