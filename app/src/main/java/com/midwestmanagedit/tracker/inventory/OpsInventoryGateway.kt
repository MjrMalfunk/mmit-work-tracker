package com.midwestmanagedit.tracker.inventory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

internal fun InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "OPS V2 response is too large." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

class OpsInventoryGateway : InventoryGateway {
    override suspend fun signIn(serverUrl: String, email: String, password: String, code: String): InventorySession {
        val body = JSONObject().put("email", email).put("password", password).put("totpCode", code)
        val data = request(serverUrl, "/api/v1/auth/login", "POST", body = body).getJSONObject("data")
        val user = data.getJSONObject("user")
        require(user.getString("role") in setOf("OWNER", "ADMIN", "OPERATOR")) {
            "This OPS V2 account does not have inventory access."
        }
        return InventorySession(serverUrl, user.getString("id"), user.getString("email"),
            data.getString("token"), Instant.parse(data.getString("expiresAt")).toEpochMilli())
    }

    override suspend fun fetchInventory(session: InventorySession): List<InventoryCatalogItem> =
        parseInventoryItems(request(session.serverUrl, "/api/v1/inventory", token = session.token).getJSONArray("data"))

    override suspend fun signOut(session: InventorySession) {
        request(session.serverUrl, "/api/v1/auth/logout", "POST", session.token)
    }

    private suspend fun request(
        serverUrl: String, path: String, method: String = "GET", token: String? = null, body: JSONObject? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val origin = inventoryServerUrl(serverUrl)
        val connection = URL(origin + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw InventoryApiException(status, when (status) {
                401 -> "Sign-in expired or the email, password, or verification code is incorrect."
                403 -> "This account does not have inventory access."
                in 300..399 -> "OPS V2 redirected this address. Enter the final HTTPS server address."
                else -> "OPS V2 could not complete this request (HTTP $status)."
            })
            val bytes = connection.inputStream.use { it.readBytesLimited(2 * 1024 * 1024) }
            if (bytes.isEmpty()) JSONObject() else JSONObject(String(bytes, Charsets.UTF_8))
        } finally { connection.disconnect() }
    }
}
