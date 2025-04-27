package bot.boob.autoporn

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.TimeUnit

class Database(host: String, port: String, name: String, user: String, auth: String) {
    private val db: HikariDataSource

    init {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mariadb://$host:$port/$name"
            username = user
            password = auth
            leakDetectionThreshold = TimeUnit.SECONDS.toMillis(10)
            driverClassName = "org.mariadb.jdbc.Driver"
        }
        db = HikariDataSource(config)
    }

    // webhook structure:
    // guildId   -> ID of the guild the channel belongs to
    // channelId -> ID of the channel the webhook belongs to
    // category  -> image category
    // webhook   -> webhook url for posting

    fun getAllWebhooks(): List<WebhookConfiguration> {
        return find("SELECT guildId, channelId, category, webhook FROM webhooks")
            .map { WebhookConfiguration(it["guildId"], it["channelId"], it["category"], it["webhook"]) }
    }

    fun deleteWebhook(guildId: Long, channelId: Long, category: String) {
        execute("DELETE FROM webhooks WHERE guildId = ? AND channelId = ? AND category = ?", guildId, channelId, category)
    }

    private fun execute(query: String, vararg parameters: Any?) {
        db.connection.use { conn ->
            buildPreparedStatement(conn, query, *parameters).use {
                it.executeUpdate()
            }
        }
    }

    private fun find(query: String, vararg parameters: Any): List<Row> {
        db.connection.use { conn ->
            buildPreparedStatement(conn, query, *parameters).use {
                it.executeQuery().use { result ->
                    val rows = mutableListOf<Row>()

                    while (result.next()) {
                        rows.add(buildRow(result))
                    }

                    return rows
                }
            }
        }
    }

    private fun buildPreparedStatement(connection: Connection, query: String, vararg parameters: Any?): PreparedStatement {
        val statement = connection.prepareStatement(query)

        for (i in parameters.indices) {
            statement.setObject(i + 1, parameters[i])
        }

        return statement
    }

    private fun buildRow(result: ResultSet): Row {
        val data = mutableMapOf<String, Any?>()

        for (i in 1..result.metaData.columnCount) {
            data[result.metaData.getColumnName(i)] = result.getObject(i)
        }

        return Row(data)
    }

    inner class Row(val dataDoNotAccessDirectly: Map<String, Any?>) {
        inline operator fun <reified T> get(column: String): T {
            return dataDoNotAccessDirectly[column]
                ?.let { it as? T ?: throw IllegalStateException("Database type does not match ${T::class.simpleName} (got type ${it::class.java.simpleName})") }
                ?: throw IllegalStateException("Key $column does not exist")
        }
    }
}
