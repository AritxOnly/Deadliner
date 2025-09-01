package com.aritxonly.deadliner

import android.app.Application
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.sync.SyncService
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.web.WebUtils

object AppSingletons {
    lateinit var db: DatabaseHelper
        private set
    lateinit var web: WebUtils
        private set
    lateinit var sync: SyncService
        private set

    fun init(app: Application) {
        db = DatabaseHelper.getInstance(app)

        web = WebUtils(
            baseUrl = GlobalUtils.webDavBaseUrl,
            username = GlobalUtils.webDavUser,
            password = GlobalUtils.webDavPass
        )

        sync = SyncService(db, web)
    }

    fun updateWeb() {
        web = WebUtils(
            baseUrl = GlobalUtils.webDavBaseUrl,
            username = GlobalUtils.webDavUser,
            password = GlobalUtils.webDavPass
        )

        sync = SyncService(db, web)
    }
}
