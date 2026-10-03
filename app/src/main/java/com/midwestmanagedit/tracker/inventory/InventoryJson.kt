package com.midwestmanagedit.tracker.inventory

import org.json.JSONArray
import org.json.JSONObject

internal fun parseInventoryItems(array: JSONArray): List<InventoryCatalogItem> {
    require(array.length() <= 10_000) { "Inventory catalog is too large." }
    return validateInventoryCatalog((0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        InventoryCatalogItem(
            sku = item.getString("sku"),
            description = item.getString("description"),
            unit = item.getString("unit"),
            quantityOnHand = item.getString("quantityOnHand").toDouble(),
            unitCost = item.getString("unitCost").toDouble(),
            active = item.getBoolean("active"),
        )
    })
}

internal fun InventoryCatalogSnapshot.toJson(): JSONObject = JSONObject()
    .put("schema", "mmit.inventory-catalog.v1")
    .put("serverUrl", serverUrl).put("userId", userId).put("email", email)
    .put("syncedAtEpochMs", syncedAtEpochMs)
    .put("items", JSONArray().apply {
        items.forEach { item -> put(JSONObject()
            .put("sku", item.sku).put("description", item.description).put("unit", item.unit)
            .put("quantityOnHand", item.quantityOnHand.toString()).put("unitCost", item.unitCost.toString())
            .put("active", item.active)) }
    })

internal fun parseInventorySnapshot(root: JSONObject): InventoryCatalogSnapshot {
    require(root.getString("schema") == "mmit.inventory-catalog.v1") { "Unsupported saved catalog." }
    return InventoryCatalogSnapshot(
        catalogSourceUrl(root.getString("serverUrl")), root.getString("userId"), root.getString("email"),
        root.getLong("syncedAtEpochMs"), parseInventoryItems(root.getJSONArray("items")),
    ).also { require(it.userId.isNotBlank() && it.syncedAtEpochMs > 0) { "Invalid saved catalog." } }
}
