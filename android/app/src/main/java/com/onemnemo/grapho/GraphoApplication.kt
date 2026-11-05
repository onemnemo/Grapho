package com.onemnemo.grapho

import android.app.Application

class GraphoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReports.install(this)
    }
}
