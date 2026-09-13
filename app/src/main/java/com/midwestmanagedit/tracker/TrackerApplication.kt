package com.midwestmanagedit.tracker

import android.app.Application
import com.midwestmanagedit.tracker.data.TrackerDatabase
import com.midwestmanagedit.tracker.data.TrackerRepository

class TrackerApplication : Application() {
    val database by lazy { TrackerDatabase.get(this) }
    val repository by lazy { TrackerRepository(database) }
}
