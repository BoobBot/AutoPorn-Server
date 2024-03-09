package bot.boob.autoporn

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import java.io.FileReader
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Server {

    companion object {
        val logger = LoggerFactory.getLogger(Server::class.java) as Logger

        lateinit var database: Database
        lateinit var api: BbApi

        val webhookClient = WebhookClient()
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

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
            database = Database(config.getProperty("mongo_db_url"))

            val postInterval = config.getProperty("interval", "5").toLong()
            executor.scheduleAtFixedRate(::post, 0L, postInterval, TimeUnit.MINUTES)
        }

        fun post() {
            runCatching { database.migrateV1Webhooks() }.onFailure { Sentry.capture(it) }

            try {
                val hooks = database.getAllWebhooks()

                for (config in hooks) {
                    api.get(config.category).thenAccept {
                        webhookClient.post(config.webhook, config.category, it, config.guildId, config.channelId)
                    }.exceptionally {
                        it.printStackTrace()
                        return@exceptionally null
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

}