package com.wandr.wear

import android.app.Application

class WandrWearApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
