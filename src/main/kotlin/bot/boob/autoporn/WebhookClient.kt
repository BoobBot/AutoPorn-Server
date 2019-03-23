package bot.boob.autoporn

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.awt.Color
import java.io.IOException

class WebhookClient {

    private val applicationJson = MediaType.parse("application/json")
    private val client = OkHttpClient()

    fun post(url: String, img: String) {
        val imgObj = JSONObject()
        imgObj.put("url", img)

        val embed = JSONObject()
        embed.put("type", "rich")
        //embed.put("color", Color.RED.rgb)
        embed.put("image", imgObj)

        val embeds = JSONArray()
        embeds.put(embed)

        val payload = JSONObject()
        payload.put("embeds", embeds)

        val req = Request.Builder()
            .url(url)
            .post(RequestBody.create(applicationJson, payload.toString()))
            .build()

        println(url)

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("call failure")
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                println("hi")
                println(response.code())
                println(response.body()!!.string())
            }
        })
    }

}