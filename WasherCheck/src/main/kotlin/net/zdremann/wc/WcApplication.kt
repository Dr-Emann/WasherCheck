package net.zdremann.wc

import android.app.Application

class WcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        component = DaggerApplicationComponent.builder().applicationModule(ApplicationModule(this)).build()
        component.inject(this)
    }

    companion object {
        lateinit var component: ApplicationComponent
            private set
            @JvmStatic get
    }
}
