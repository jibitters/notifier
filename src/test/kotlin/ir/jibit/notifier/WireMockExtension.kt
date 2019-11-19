package ir.jibit.notifier

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import ir.jibit.notifier.util.logger
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit 5 extension to setting up a WireMock server before all tests and
 * tearing it down after all of them.
 */
class WireMockExtension : BeforeAllCallback, AfterAllCallback {

    private var server: WireMockServer? = null

    override fun beforeAll(ctx: ExtensionContext?) {
        server = WireMockServer(0)
        server?.start()
        val port = server?.port()!!

        System.setProperty("TEST_WIREMOCK_PORT", "$port")
        WireMock.configureFor(port)
        log.info("WireMock is listening on $port")
    }

    override fun afterAll(ctx: ExtensionContext?) {
        server?.stop()
        System.clearProperty("TEST_WIREMOCK_PORT")
        log.info("WireMock has stopped successfully")
    }

    companion object {
        private val log = logger<WireMockExtension>()
    }
}
