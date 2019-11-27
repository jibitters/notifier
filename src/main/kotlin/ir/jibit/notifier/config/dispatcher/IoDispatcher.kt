package ir.jibit.notifier.config.dispatcher

import ir.jibit.notifier.config.PrefixedThreadFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Encapsulates the IO Dispatcher beans and configurations.
 *
 * @author Ali Dehghani
 */
@Configuration
@EnableConfigurationProperties(DispatcherProperties::class)
class IoDispatcher {

    /**
     * The [ExecutorService] responsible for executing notifications.
     */
    @Bean
    fun ioExecutor(properties: DispatcherProperties): ExecutorService = with(properties) {
        ThreadPoolExecutor(poolSize, poolSize, 0, MILLISECONDS, LinkedBlockingQueue(), PrefixedThreadFactory(threadPrefix))
    }
}
