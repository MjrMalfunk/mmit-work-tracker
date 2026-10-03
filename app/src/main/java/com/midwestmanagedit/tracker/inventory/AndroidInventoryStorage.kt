package com.midwestmanagedit.tracker.inventory

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Passwords and OTPs are never stored. The session token is encrypted by Android Keystore. */
class AndroidInventoryStorage(context: Context) : InventoryStorage {
    private val preferences = context.getSharedPreferences("inventory_connection", Context.MODE_PRIVATE)
    private val catalog = AtomicFile(File(context.filesDir, "inventory-catalog.json"))
    private val alias = "mmit.inventory.session.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    override fun loadSession(): InventorySession? {
        val saved = preferences.getString("encrypted_session", null) ?: return null
        return try {
            val parts = saved.split(':', limit = 2)
            require(parts.size == 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
                updateAAD(alias.toByteArray(Charsets.UTF_8))
            }
            val data = JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
            InventorySession(inventoryServerUrl(data.getString("serverUrl")), data.getString("userId"),
                data.getString("email"), data.getString("token"), data.getLong("expiresAtEpochMs"))
        } catch (_: Exception) {
            // A lost/invalid Keystore key requires a fresh sign-in; never use an unreadable token.
            preferences.edit().remove("encrypted_session").commit()
            null
        }
    }

    override fun saveSession(session: InventorySession?) {
        if (session == null) {
            check(preferences.edit().remove("encrypted_session").commit()) { "Could not clear the saved connection." }
            return
        }
        val data = JSONObject().put("serverUrl", session.serverUrl).put("userId", session.userId)
            .put("email", session.email).put("token", session.token).put("expiresAtEpochMs", session.expiresAtEpochMs)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key())
            updateAAD(alias.toByteArray(Charsets.UTF_8))
        }
        val encrypted = cipher.doFinal(data.toString().toByteArray(Charsets.UTF_8))
        val saved = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString("encrypted_session", saved).commit()) { "Could not save the connection securely." }
    }

    override fun loadCatalog(): InventoryCatalogSnapshot? {
        if (!catalog.baseFile.exists()) return null
        return catalog.openRead().use { input ->
            val bytes = input.readBytesLimited(2 * 1024 * 1024)
            parseInventorySnapshot(JSONObject(String(bytes, Charsets.UTF_8)))
        }
    }

    override fun saveCatalog(snapshot: InventoryCatalogSnapshot?) {
        if (snapshot == null) { catalog.delete(); return }
        val data = snapshot.toJson().toString().toByteArray(Charsets.UTF_8)
        require(data.size <= 2 * 1024 * 1024) { "Inventory catalog is too large." }
        val output = catalog.startWrite()
        try { output.write(data); catalog.finishWrite(output) }
        catch (error: Exception) { catalog.failWrite(output); throw error }
    }
}
