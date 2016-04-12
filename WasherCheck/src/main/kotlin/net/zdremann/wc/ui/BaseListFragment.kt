package net.zdremann.wc.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.ListFragment
import net.zdremann.wc.ActivityComponent

open class BaseListFragment: ListFragment() {
    lateinit var component: ActivityComponent
        private set

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        component = (activity as BaseActivity).component
    }
}