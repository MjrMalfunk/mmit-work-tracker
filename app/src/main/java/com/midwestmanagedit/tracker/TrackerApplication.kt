package com.midwestmanagedit.tracker

import android.app.Application
import com.midwestmanagedit.tracker.data.TrackerDatabase
import com.midwestmanagedit.tracker.data.TrackerRepository
import com.midwestmanagedit.tracker.inventory.AndroidInventoryStorage
import com.midwestmanagedit.tracker.inventory.InventoryCatalogRepository
import com.midwestmanagedit.tracker.inventory.OpsInventoryGateway

class TrackerApplication : Application() {
    val database by lazy { TrackerDatabase.get(this) }
    val repository by lazy { TrackerRepository(database) }
    val inventoryCatalog by lazy { InventoryCatalogRepository(OpsInventoryGateway(), AndroidInventoryStorage(this)) }
}
