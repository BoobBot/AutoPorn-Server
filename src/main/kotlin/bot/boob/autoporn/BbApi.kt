package bot.boob.autoporn

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture

public class BbApi(private val apiKey: String) {
    private val client = OkHttpClient()

    fun get(category: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()

        val req = Request.Builder()
            .url("https://boob.bot/api/v2/img/$category")
            .header("Key", apiKey)
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        val json = JSONObject(body.string())
                        future.complete(json.getString("url"))
                        return
                    }
                }

                future.completeExceptionally(Exception(response.message()))
            }
        })

        return future
    }

}