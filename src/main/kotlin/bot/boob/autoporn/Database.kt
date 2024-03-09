package bot.boob.autoporn

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import org.bson.Document

class Database(mongoDbUrl: String) {
    private val mongo = MongoClients.create(mongoDbUrl)

    private val autoPorn = mongo.getDatabase("autoporn")
    private val webhooks = autoPorn.getCollection("webhooks")
    private val webhooksv2 = autoPorn.getCollection("webhooksv2")

    // webhook structure:
    // category  -> image category
    // channelId -> ID of the channel the webhook belongs to
    // webhook   -> webhook url for posting

    fun migrateV1Webhooks() {
        val configs = webhooks.find().toList()

        for (config in configs) {
            val guildId = config.getString("_id")

            val doc = Document("webhook", config.getString("webhook"))
                .append("category", config.getString("category"))
                .append("channelId", config.getString("channelId"))

            webhooksv2.updateOne(
                eq("_id", guildId),
                Updates.push("webhooks", doc),
                UpdateOptions().upsert(true)
            )

            webhooks.deleteOne(eq("_id", guildId))
            println("[whv2-migration] $guildId migrated")
        }
    }

    fun getAllWebhooks(): List<WebhookConfiguration> {
        return webhooksv2.find().flatMap {
            val guildId = it.getString("_id")

            it.getList("webhooks", Document::class.java).map { cfg ->
                WebhookConfiguration(guildId, cfg.getString("category"), cfg.getString("channelId"), cfg.getString("webhook"))
            }
        }

        // Keys:
        // _id     -> guild id
        // webhooks -> [[...webhook structure], ...]
    }

    fun deleteWebhook(guildId: String, channelId: String, category: String) {
        webhooksv2.updateOne(
            eq("_id", guildId),
            Updates.pull("webhooks", and(eq("channelId", channelId), eq("category", category)))
        )
    }
}
