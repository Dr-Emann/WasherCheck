package net.zdremann.wc

import air.air.net.zdremann.zsuds.BuildConfig
import air.air.net.zdremann.zsuds.R
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker
import dagger.Module
import dagger.Provides
import net.zdremann.ForApplication
import javax.inject.Singleton

@Module
class ContextModule {
    @Provides
    internal fun provideGoogleAnalytics(@ForApplication context: Context): GoogleAnalytics {
        val analytics = GoogleAnalytics.getInstance(context)
        analytics.setDryRun(
                BuildConfig.DEBUG ||
                        BuildConfig.VERSION_NAME.contains("dirty") ||
                        Build.FINGERPRINT.startsWith("generic"))
        return analytics
    }

    @Provides
    @Singleton
    internal fun provideGoogleTracker(googleAnalytics: GoogleAnalytics): Tracker {
        return googleAnalytics.newTracker(R.xml.global_tracker)
    }

    @Provides
    @Singleton
    internal fun provideLocationManager(@ForApplication context: Context): LocationManager {
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @Provides
    @Singleton
    internal fun provideAlarmManager(@ForApplication context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Provides
    @Singleton
    internal fun provideNotificationManager(@ForApplication context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    @Main
    internal fun provideMainSharedPreferences(@ForApplication context: Context): SharedPreferences {
        return context.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    internal fun provideContentResolver(@ForApplication context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    internal fun provideConnectivityManager(@ForApplication context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    companion object {
        const val MAIN_PREFS_NAME = "main"
    }
}