package bot.boob.autoporn

import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

class BbApi(private val apiKey: String) {
    private val client = HttpClient.newHttpClient()

    fun get(category: String): CompletableFuture<String> {
        val req = HttpRequest.newBuilder(URI("https://boob.bot/api/v2/img/$category"))
            .GET()
            .header("Key", apiKey)
            .build()

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply { JSONObject(it.body()).getString("url") }
    }
}
