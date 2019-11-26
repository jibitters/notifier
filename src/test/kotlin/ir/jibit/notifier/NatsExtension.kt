package ir.jibit.notifier

import ir.jibit.notifier.util.logger
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

/**
 * A JUnit 5 extension to setup a Nats server before all tests and tearing it down
 * on after all of them.
 *
 * @author Ali Dehghani
 */
class NatsExtension : BeforeAllCallback, AfterAllCallback {

    /**
     * Logger.
     */
    private val log = logger<NatsExtension>()

    /**
     * The Nats container.
     */
    private var container: GenericContainer<*>? = null

    /**
     * Starts the Nats container and waits for it to be ready for use.
     */
    override fun beforeAll(ctx: ExtensionContext?) {
        if (!isCiEnv()) {
            log.info("About to start the Nats container")
            val container = GenericContainer<Nothing>("nats:2.1.0-scratch")
            container.withExposedPorts(4222)
            container.waitingFor(LogMessageWaitStrategy().withRegEx(".*Server is ready.*"))
            container.start()
            System.setProperty("TEST_NATS_PORT", "${container.firstMappedPort}")
            log.info("The Nats has been started and is listening to ${container.firstMappedPort}")
        }
    }

    /**
     * Tears down the nats container.
     */
    override fun afterAll(ctx: ExtensionContext?) {
        if (!isCiEnv()) {
            log.info("About to stop the Nats container")
            container?.stop()
            System.clearProperty("TEST_NATS_PORT")
        }
    }

    private fun isCiEnv() = System.getenv("CI") == "true"
}
