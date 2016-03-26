package net.zdremann.wc

import com.google.android.gms.analytics.Tracker
import dagger.Component
import net.zdremann.wc.io.locations.LocationsProxy
import net.zdremann.wc.io.locations.LocationsProxyModule
import net.zdremann.wc.io.rooms.RoomLoaderModule
import net.zdremann.wc.provider.WasherCheckProvider
import net.zdremann.wc.service.ClearCompletedNotificationsService
import net.zdremann.wc.service.GcmBroadcastService
import net.zdremann.wc.service.RoomRefresher
import net.zdremann.wc.ui.ActivityModule
import javax.inject.Singleton

@Component(modules = arrayOf(
        ApplicationModule::class,
        ContextModule::class,
        LocationsProxyModule::class,
        RoomLoaderModule::class))
@Singleton
interface ApplicationComponent {
    fun inject(application: WcApplication)

    fun inject(service: ClearCompletedNotificationsService)
    fun inject(service: GcmBroadcastService)
    fun inject(service: RoomRefresher)

    fun inject(provider: WasherCheckProvider)

    fun activityComponent(activityModule: ActivityModule): ActivityComponent
    fun gaTracker(): Tracker

    fun locationProxy(): LocationsProxy
}

