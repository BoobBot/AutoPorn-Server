package bot.boob.autoporn

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class Database(mongoDbUrl: String) {

    private val mongo = MongoClients.create(mongoDbUrl)

    private val autoPorn = mongo.getDatabase("autoporn")
    private val webhooks = autoPorn.getCollection("webhooks")


    fun getAllWebhooks(): List<Document> {
        return webhooks.find().toList()

        // Keys:
        // _id     -> guild id
        // webhook -> webhook url
    }

    fun deleteWebhook(guildId: String) {
        webhooks.deleteOne(eq("_id", guildId))
    }

}