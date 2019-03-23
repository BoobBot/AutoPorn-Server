package bot.boob.autoporn

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class WebhookClient {

    private val applicationJson = MediaType.parse("application/json")
    private val client = OkHttpClient()

    fun post(url: String, img: String, guildId: String) {
        val imgObj = JSONObject()
        imgObj.put("url", img)

        val embed = JSONObject()
        embed.put("color", 16711680) // red
        embed.put("image", imgObj)

        val embeds = JSONArray()
        embeds.put(embed)

        val payload = JSONObject()
        payload.put("embeds", embeds)

        val req = Request.Builder()
            .url(url)
            .post(RequestBody.create(applicationJson, payload.toString()))
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Server.logger.error("Error while making Webhook Request", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val body = response.body()

                    if (body == null) {
                        Server.logger.warn(
                            "Invalid status code while posting: {} - {}",
                            response.code(),
                            response.message()
                        )
                        return
                    }

                    val json = JSONObject(body.string())

                    when (json.getInt("code")) {
                        10015 -> Server.database.deleteWebhook(guildId)
                    }
                }
            }
        })
    }

}