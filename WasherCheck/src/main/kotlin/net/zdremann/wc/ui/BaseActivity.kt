package net.zdremann.wc.ui

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import net.zdremann.wc.ActivityComponent
import net.zdremann.wc.WcApplication

open class BaseActivity: AppCompatActivity() {
    lateinit var component: ActivityComponent
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = WcApplication.component.activityComponent(ActivityModule(this))
    }
}

