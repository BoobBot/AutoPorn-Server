package bot.boob.autoporn

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class WebhookClient {
    private val client = HttpClient.newHttpClient()

    fun post(url: String, category: String, img: String, guildId: String, channelId: String) {
        val imgObj = JSONObject().put("url", img)

        val embed = JSONObject()
            .put("color", 10451438) // not red
            .put("image", imgObj)

        val embeds = JSONArray().put(embed)

        val payload = JSONObject()
            .put("username", "BoobBot AutoPorn")
            .put("avatar_url", "https://boob.bot/static/assets/images/avatar.png")
            .put("embeds", embeds)

        val req = HttpRequest.newBuilder(URI(url))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), Charsets.UTF_8))
            .header("content-type", "application/json; charset=utf-8")
            .build()

        client.sendAsync(req, BodyHandlers.ofString())
            .thenAccept {
                val code = it.statusCode()
                val body = it.body()

                if (code < 200 || code >= 300) {
                    val json = JSONObject(body)
                    var handled = true

                    if (!json.isNull("code")) {
                        when (json.getInt("code")) {
                            UNKNOWN_WEBHOOK -> Server.database.deleteWebhook(guildId, channelId, category)
                            else -> handled = false
                        }
                    } else {
                        handled = false
                    }

                    if (!handled) {
                        Server.logger.warn("Invalid status code while posting to $url: $code - $body")
                    }
                }
            }
    }

    companion object {
        private const val UNKNOWN_WEBHOOK = 10015
    }
}
