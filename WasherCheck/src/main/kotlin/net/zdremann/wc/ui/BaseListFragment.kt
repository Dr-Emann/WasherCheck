package net.zdremann.wc.ui

import android.content.Context
import android.support.v4.app.ListFragment
import net.zdremann.wc.ActivityComponent

open class BaseListFragment: ListFragment() {
    lateinit var component: ActivityComponent
        private set

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        component = (activity as BaseActivity).component
    }
}