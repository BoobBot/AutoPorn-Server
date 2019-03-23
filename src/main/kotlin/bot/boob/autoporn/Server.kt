package bot.boob.autoporn

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import java.io.FileReader
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Server {

    companion object {
        public val logger = LoggerFactory.getLogger(Server::class.java) as Logger
        public val database = Database()

        public val webhookClient = WebhookClient()
        public lateinit var api: BbApi

        public val executor = Executors.newSingleThreadScheduledExecutor()!!

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val config = Properties()
            config.load(FileReader("config.properties"))

            if (args.any { it.contains("debug") }) {
                logger.warn("Running in debug mode")
                logger.level = Level.DEBUG
            } else {
                Sentry.init(config.getProperty("sentry_dsn"))
            }

            api = BbApi(config.getProperty("bb_key"))

            val postInterval = config.getProperty("interval", "5").toLong()
            executor.scheduleAtFixedRate({ post() }, 0L, postInterval, TimeUnit.MINUTES)
        }

        fun post() {
            val hooks = database.getAllWebhooks()

            for (hook in hooks) {
                api.get(hook.getString("category")).thenAccept {
                    webhookClient.post(hook.getString("webhook"), it)
                    // TODO check if unknown webhook, delete if so
                }
            }
        }
    }

}