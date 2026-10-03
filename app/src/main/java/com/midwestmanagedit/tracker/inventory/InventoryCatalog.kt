package com.midwestmanagedit.tracker.inventory

import java.net.URI
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class InventoryCatalogItem(
    val sku: String,
    val description: String,
    val unit: String,
    val quantityOnHand: Double,
    val unitCost: Double,
    val active: Boolean = true,
)

data class InventoryCatalogSnapshot(
    val serverUrl: String,
    val userId: String,
    val email: String,
    val syncedAtEpochMs: Long,
    val items: List<InventoryCatalogItem>,
)

data class InventorySession(
    val serverUrl: String,
    val userId: String,
    val email: String,
    val token: String,
    val expiresAtEpochMs: Long,
) {
    override fun toString() = "InventorySession(serverUrl=$serverUrl, userId=$userId, token=[redacted])"
}

/** Only root HTTPS origins are accepted; credentials never follow redirects. */
fun inventoryServerUrl(input: String): String {
    val uri = try { URI(input.trim()) } catch (_: Exception) {
        throw IllegalArgumentException("Enter your OPS V2 HTTPS server address.")
    }
    require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
        "Use the HTTPS address of OPS V2 that your phone can reach."
    }
    require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
        (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
        (uri.port == -1 || uri.port in 1..65535)) {
        "Enter only the server address, without /ops, credentials, or query parameters."
    }
    return URI("https", null, uri.host.lowercase(Locale.ROOT),
        if (uri.port == 443) -1 else uri.port, null, null, null).toASCIIString()
}

/** Imported files may describe a desktop HTTP origin. It is metadata, never a request target. */
fun catalogSourceUrl(input: String): String {
    val uri = try { URI(input.trim()) } catch (_: Exception) {
        throw IllegalArgumentException("Invalid catalog source address.")
    }
    require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() &&
        uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
        (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
        (uri.port == -1 || uri.port in 1..65535)) { "Invalid catalog source address." }
    return URI(uri.scheme, null, uri.host.lowercase(Locale.ROOT), uri.port, null, null, null).toASCIIString()
}

fun validateInventoryCatalog(items: List<InventoryCatalogItem>): List<InventoryCatalogItem> {
    require(items.size <= 10_000) { "Inventory catalog is too large." }
    val skus = mutableSetOf<String>()
    items.forEach { item ->
        require(item.sku.matches(Regex("[A-Z0-9][A-Z0-9._-]{0,99}")) && skus.add(item.sku)) {
            "Inventory contains an invalid or duplicate SKU."
        }
        require(item.description.isNotBlank() && item.description.length <= 10_000 &&
            item.unit.isNotBlank() && item.unit.length <= 100 &&
            item.quantityOnHand.isFinite() && item.quantityOnHand >= 0 &&
            item.unitCost.isFinite() && item.unitCost >= 0) { "Inventory contains an invalid item." }
    }
    return items.filter { it.active }.sortedBy { it.sku }
}

/** Search both identifiers and descriptions locally; an exact SKU ranks first. */
fun searchInventory(items: List<InventoryCatalogItem>, query: String, limit: Int = 20): List<InventoryCatalogItem> {
    val text = query.trim().lowercase(Locale.ROOT)
    val terms = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return items.asSequence().filter { item ->
        item.active && terms.all { term ->
            item.sku.lowercase(Locale.ROOT).contains(term) || item.description.lowercase(Locale.ROOT).contains(term)
        }
    }.sortedWith(compareBy<InventoryCatalogItem> {
        when {
            it.sku.equals(text, ignoreCase = true) -> 0
            it.sku.lowercase(Locale.ROOT).startsWith(text) -> 1
            else -> 2
        }
    }.thenBy { it.sku }).take(limit.coerceIn(1, 100)).toList()
}

/** Match the import contract: positive quantities, three decimal places, bounded totals. */
fun validateMaterialQuantity(quantity: Double, alreadyRecorded: Double = 0.0) {
    require(quantity.isFinite() && quantity in 0.001..10_000.0 &&
        abs(quantity * 1000 - round(quantity * 1000)) < 0.000001) {
        "Enter a quantity from 0.001 to 10,000, with up to three decimal places."
    }
    require(alreadyRecorded.isFinite() && alreadyRecorded >= 0 && quantity + alreadyRecorded <= 10_000.0) {
        "The total recorded for one SKU cannot exceed 10,000."
    }
}
