package com.midwestmanagedit.tracker.inventory

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class InventoryCatalogTest {
    private val time = 1_791_010_000_000L
    private val connector = InventoryCatalogItem("RJ45-CAT6", "Cat6 passthrough connector", "each", 50.0, 0.09)
    private val cable = InventoryCatalogItem("CAT6-CMR-BLK", "Black Cat6 CMR cable", "ft", 934.0, 0.17)
    private val session = InventorySession("https://v2.example.com", "7", "keith@example.com", "test-token", time + 60_000)
    private val cached = InventoryCatalogSnapshot(session.serverUrl, session.userId, session.email, time - 600_000, listOf(cable, connector))

    private class Storage(var session: InventorySession?, var catalog: InventoryCatalogSnapshot?) : InventoryStorage {
        var catalogWrites = 0
        override fun loadSession() = session
        override fun saveSession(session: InventorySession?) { this.session = session }
        override fun loadCatalog() = catalog
        override fun saveCatalog(snapshot: InventoryCatalogSnapshot?) { catalog = snapshot; catalogWrites++ }
    }

    private class Gateway(var login: InventorySession, var items: List<InventoryCatalogItem>) : InventoryGateway {
        var fetchError: Exception? = null
        var calls = 0
        override suspend fun signIn(serverUrl: String, email: String, password: String, code: String) = login
        override suspend fun fetchInventory(session: InventorySession): List<InventoryCatalogItem> {
            calls++
            fetchError?.let { throw it }
            return items
        }
        override suspend fun signOut(session: InventorySession) { fetchError?.let { throw it } }
    }

    @Test fun searchesSkuAndDescriptionOfflineWithExactSkuFirst() {
        assertEquals(listOf(cable), searchInventory(cached.items, "black cable"))
        assertEquals(listOf(connector), searchInventory(cached.items, "rj45"))
        assertEquals(connector, searchInventory(cached.items, "RJ45-CAT6").first())
        assertTrue(searchInventory(cached.items, "not stocked").isEmpty())
        assertEquals(1, searchInventory(cached.items, "cat6", 1).size)
    }

    @Test fun onlyRootHttpsAddressesCanReceiveCredentials() {
        assertEquals("https://v2.example.com", inventoryServerUrl(" HTTPS://V2.EXAMPLE.COM:443/ "))
        listOf("http://127.0.0.1:3000", "https://user:password@v2.example.com", "https://v2.example.com/ops", "https://v2.example.com?token=x")
            .forEach { assertTrue(runCatching { inventoryServerUrl(it) }.isFailure) }
        // HTTP desktop origins in imported files are only informational metadata.
        assertEquals("http://127.0.0.1:3000", catalogSourceUrl("http://127.0.0.1:3000"))
    }

    @Test fun catalogValidationRejectsDuplicatesAndInvalidStock() {
        assertTrue(runCatching { validateInventoryCatalog(listOf(cable, cable)) }.isFailure)
        assertTrue(runCatching { validateInventoryCatalog(listOf(cable.copy(quantityOnHand = Double.NaN))) }.isFailure)
        assertTrue(runCatching { validateInventoryCatalog(listOf(cable.copy(quantityOnHand = -1.0))) }.isFailure)
        assertEquals(listOf(connector), validateInventoryCatalog(listOf(cable.copy(active = false), connector)))
    }

    @Test fun quantitiesMatchTheExportContractAndCumulativeLimit() {
        validateMaterialQuantity(0.001)
        validateMaterialQuantity(5.125, 9994.875)
        listOf(0.0, 0.0001, 1.2345, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertTrue(runCatching { validateMaterialQuantity(it) }.isFailure)
        }
        assertTrue(runCatching { validateMaterialQuantity(1.0, 9999.001) }.isFailure)
    }

    @Test fun connectionFailureRetainsLastGoodCatalog() = runTest {
        val store = Storage(session, cached)
        val api = Gateway(session, listOf(connector)).apply { fetchError = IOException("no signal") }
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.refresh(force = true)
        assertEquals(cached.items, repo.state.value.items)
        assertEquals(cached, store.catalog)
        assertEquals(0, store.catalogWrites)
        assertFalse(repo.state.value.busy)
        assertNotNull(repo.state.value.error)
    }

    @Test fun malformedResponseCannotReplaceGoodCache() = runTest {
        val store = Storage(session, cached)
        val repo = InventoryCatalogRepository(Gateway(session, listOf(connector, connector)), store) { time }
        repo.refresh(force = true)
        assertEquals(cached, store.catalog)
        assertEquals(cached.items, repo.state.value.items)
    }

    @Test fun expiredSessionLeavesOfflineSearchAvailable() = runTest {
        val store = Storage(session.copy(expiresAtEpochMs = time), cached)
        val api = Gateway(session, emptyList())
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.refresh(force = true)
        assertNull(store.session)
        assertFalse(repo.state.value.authenticated)
        assertEquals(cached.items, repo.state.value.items)
        assertEquals(0, api.calls)
    }

    @Test fun rejectedSessionIsRemovedWithoutLosingOfflineCache() = runTest {
        val store = Storage(session, cached)
        val api = Gateway(session, emptyList()).apply { fetchError = InventoryApiException(401, "expired") }
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.refresh(force = true)
        assertNull(store.session)
        assertEquals(cached.items, repo.state.value.items)
    }

    @Test fun differentAccountCannotReuseOldCatalogAfterSignIn() = runTest {
        val store = Storage(session, cached)
        val api = Gateway(session.copy(userId = "8", email = "other@example.com"), emptyList()).apply {
            fetchError = IOException("no signal")
        }
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.signIn(session.serverUrl, "other@example.com", "password", "123456")
        assertNull(store.catalog)
        assertTrue(repo.state.value.items.isEmpty())
        assertEquals("8", store.session!!.userId)
    }

    @Test fun cancelledRefreshPreservesCacheAndClearsBusyFlag() = runTest {
        val store = Storage(session, cached)
        val api = Gateway(session, emptyList()).apply { fetchError = CancellationException("cancelled") }
        val repo = InventoryCatalogRepository(api, store) { time }
        assertTrue(runCatching { repo.refresh(force = true) }.exceptionOrNull() is CancellationException)
        assertEquals(cached, store.catalog)
        assertFalse(repo.state.value.busy)
    }

    @Test fun refreshRemovesInactiveItemsAndKeepsZeroStockSelectable() = runTest {
        val store = Storage(session, cached)
        val api = Gateway(session, listOf(cable.copy(active = false), connector.copy(quantityOnHand = 0.0)))
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.refresh(force = true)
        assertEquals(listOf(connector.copy(quantityOnHand = 0.0)), repo.state.value.items)
        assertEquals(time, repo.state.value.syncedAtEpochMs)
    }

    @Test fun frequentResumeEventsAreThrottledButManualRefreshIsNot() = runTest {
        val store = Storage(session, cached.copy(syncedAtEpochMs = time))
        val api = Gateway(session, listOf(connector))
        val repo = InventoryCatalogRepository(api, store) { time }
        repeat(3) { repo.refresh() }
        assertEquals(0, api.calls)
        repo.refresh(force = true)
        assertEquals(1, api.calls)
    }

    @Test fun offlineDisconnectRemovesLocalCredentialsAndCatalog() = runTest {
        val store = Storage(session, cached)
        val api = Gateway(session, emptyList()).apply { fetchError = IOException("no signal") }
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.signOut()
        assertNull(store.session)
        assertNull(store.catalog)
        assertTrue(repo.state.value.items.isEmpty())
    }

    @Test fun desktopCatalogCanBeImportedWithoutNetworkOrSession() = runTest {
        val store = Storage(null, null)
        val api = Gateway(session, emptyList())
        val repo = InventoryCatalogRepository(api, store) { time }
        repo.importCatalog(cached.copy(serverUrl = "http://127.0.0.1:3000"))
        assertEquals(cached.items, repo.state.value.items)
        assertFalse(repo.state.value.authenticated)
        assertEquals(0, api.calls)
        assertTrue(runCatching { repo.importCatalog(cached.copy(serverUrl = "http://127.0.0.1:3000", syncedAtEpochMs = time - 700_000)) }.isFailure)
    }

    @Test fun importCannotCrossAnActiveConnectionAccountBoundary() = runTest {
        val store = Storage(session, cached)
        val repo = InventoryCatalogRepository(Gateway(session, emptyList()), store) { time }
        assertTrue(runCatching { repo.importCatalog(cached.copy(userId = "8")) }.isFailure)
        assertEquals(cached, store.catalog)
        assertTrue(runCatching { repo.importCatalog(cached.copy(syncedAtEpochMs = time + 600_000)) }.isFailure)
    }
}
