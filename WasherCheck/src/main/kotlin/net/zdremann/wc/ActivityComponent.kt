package net.zdremann.wc

import dagger.Subcomponent
import net.zdremann.ActivityScope
import net.zdremann.wc.ui.*

@Subcomponent(modules = arrayOf(ActivityModule::class))
@ActivityScope
interface ActivityComponent{
    fun inject(roomChooserActivity: RoomChooserActivity)
    fun inject(roomViewer: RoomViewer)
    fun inject(aboutActivity: AboutActivity)

    fun injectFragment(fragment: RoomChooserFragment)
    fun injectFragment(fragment: NotificationListFragment)
    fun injectFragment(fragment: RoomViewFragment)
}