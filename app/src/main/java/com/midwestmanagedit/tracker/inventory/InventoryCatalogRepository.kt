package com.midwestmanagedit.tracker.inventory

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface InventoryGateway {
    suspend fun signIn(serverUrl: String, email: String, password: String, code: String): InventorySession
    suspend fun fetchInventory(session: InventorySession): List<InventoryCatalogItem>
    suspend fun signOut(session: InventorySession)
}

interface InventoryStorage {
    fun loadSession(): InventorySession?
    fun saveSession(session: InventorySession?)
    fun loadCatalog(): InventoryCatalogSnapshot?
    fun saveCatalog(snapshot: InventoryCatalogSnapshot?)
}

class InventoryApiException(val status: Int, message: String) : Exception(message)

data class InventoryCatalogState(
    val items: List<InventoryCatalogItem> = emptyList(),
    val serverUrl: String = "",
    val email: String = "",
    val accountKey: String = "",
    val syncedAtEpochMs: Long? = null,
    val authenticated: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
)

/** A failed refresh never replaces the last good snapshot. Account changes clear it. */
class InventoryCatalogRepository(
    private val gateway: InventoryGateway,
    private val storage: InventoryStorage,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var session = runCatching { storage.loadSession() }.getOrNull()
    private var snapshot = runCatching { storage.loadCatalog() }.getOrNull()?.let { saved ->
        runCatching { saved.copy(items = validateInventoryCatalog(saved.items)) }.getOrNull()
    }?.takeIf { cached -> session == null ||
        (session!!.serverUrl == cached.serverUrl && session!!.userId == cached.userId) }
    private val mutableState = MutableStateFlow(currentState())
    val state: StateFlow<InventoryCatalogState> = mutableState.asStateFlow()

    private fun currentState(busy: Boolean = false, error: String? = null) = InventoryCatalogState(
        items = snapshot?.items.orEmpty(),
        serverUrl = session?.serverUrl ?: snapshot?.serverUrl.orEmpty(),
        email = session?.email ?: snapshot?.email.orEmpty(),
        accountKey = snapshot?.let { "${it.serverUrl}|${it.userId}" }.orEmpty(),
        syncedAtEpochMs = snapshot?.syncedAtEpochMs,
        authenticated = session?.let { it.expiresAtEpochMs > now() } == true,
        busy = busy,
        error = error,
    )

    suspend fun signIn(serverInput: String, emailInput: String, password: String, code: String) = mutex.withLock {
        val serverUrl = inventoryServerUrl(serverInput)
        val email = emailInput.trim()
        require(email.isNotBlank() && password.isNotEmpty() && code.matches(Regex("[0-9]{6}"))) {
            "Enter your OPS V2 email, password, and six-digit verification code."
        }
        mutableState.value = currentState(busy = true)
        try {
            val signedIn = gateway.signIn(serverUrl, email, password, code)
            require(signedIn.serverUrl == serverUrl && signedIn.userId.isNotBlank() &&
                signedIn.token.isNotBlank() && signedIn.expiresAtEpochMs > now()) { "OPS V2 returned an invalid session." }
            if (snapshot?.let { it.serverUrl != signedIn.serverUrl || it.userId != signedIn.userId } == true) {
                storage.saveCatalog(null)
                snapshot = null
            }
            storage.saveSession(signedIn)
            session = signedIn
            refreshLocked()
        } catch (cancelled: CancellationException) {
            mutableState.value = currentState()
            throw cancelled
        } catch (error: Exception) {
            mutableState.value = currentState(error = error.message ?: "Could not connect to OPS V2.")
            throw error
        }
    }

    suspend fun refresh(force: Boolean = false) = mutex.withLock {
        val current = session ?: return@withLock
        if (current.expiresAtEpochMs <= now()) {
            storage.saveSession(null)
            session = null
            mutableState.value = currentState(error = "Sign in again to refresh. Your saved catalog is still available offline.")
            return@withLock
        }
        if (!force && snapshot?.syncedAtEpochMs?.let { now() - it in 0 until 300_000 } == true) return@withLock
        mutableState.value = currentState(busy = true)
        refreshLocked()
    }

    private suspend fun refreshLocked() {
        val current = session ?: return
        try {
            val items = validateInventoryCatalog(gateway.fetchInventory(current))
            val fresh = InventoryCatalogSnapshot(current.serverUrl, current.userId, current.email, now(), items)
            storage.saveCatalog(fresh)
            snapshot = fresh
            mutableState.value = currentState()
        } catch (cancelled: CancellationException) {
            mutableState.value = currentState()
            throw cancelled
        } catch (error: Exception) {
            if (error is InventoryApiException && error.status in setOf(401, 403)) {
                storage.saveSession(null)
                session = null
            }
            mutableState.value = currentState(error = when {
                error is InventoryApiException && error.status in setOf(401, 403) ->
                    "Sign in again to refresh. Your saved catalog is still available offline."
                else -> "Inventory could not refresh. Your saved catalog is still available."
            })
        }
    }

    suspend fun importCatalog(imported: InventoryCatalogSnapshot) = mutex.withLock {
        require(imported.syncedAtEpochMs > 0 && imported.syncedAtEpochMs <= now() + 300_000 &&
            imported.userId.isNotBlank() && imported.email.isNotBlank()) { "Invalid inventory catalog metadata." }
        val validated = imported.copy(serverUrl = catalogSourceUrl(imported.serverUrl), items = validateInventoryCatalog(imported.items))
        val current = session
        require(current == null || (current.serverUrl == validated.serverUrl && current.userId == validated.userId)) {
            "Disconnect the current OPS connection before importing a catalog from another server or account."
        }
        require(snapshot?.let { it.serverUrl == validated.serverUrl && it.userId == validated.userId &&
            it.syncedAtEpochMs > validated.syncedAtEpochMs } != true) { "This file is older than the catalog already saved on your phone." }
        storage.saveCatalog(validated)
        snapshot = validated
        mutableState.value = currentState()
    }

    suspend fun signOut() = mutex.withLock {
        val oldSession = session
        // Clear locally before trying the server, so disconnect also works without a signal.
        storage.saveSession(null)
        storage.saveCatalog(null)
        session = null
        snapshot = null
        mutableState.value = currentState()
        if (oldSession != null) {
            try { gateway.signOut(oldSession) } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { /* Local credentials have already been removed. */ }
        }
    }
}
