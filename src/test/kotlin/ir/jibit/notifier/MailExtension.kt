package ir.jibit.notifier

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.subethamail.wiser.Wiser

/**
 * A JUnit 5 extension to setup a mail server before all tests and then tear it down
 * in appropriate time.
 *
 * @author Ali Dehghani
 */
class MailExtension : BeforeAllCallback {

    /**
     * Setups up the Wiser server once and only once.
     */
    override fun beforeAll(ctx: ExtensionContext?) {
        synchronized(server) {
            if (!isStarted) {
                server.start()
            }
        }
    }

    companion object {

        private var isStarted = false

        /**
         * The wiser server.
         */
        private val server: Wiser by lazy {
            val wiser = Wiser()
            wiser.setPort(2500)

            wiser
        }
    }
}
