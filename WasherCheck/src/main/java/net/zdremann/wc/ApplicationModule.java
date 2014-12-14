/*
 * Copyright (c) 2013. Zachary Dremann
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.zdremann.wc;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import net.zdremann.wc.io.IOModule;
import net.zdremann.wc.service.RoomRefresher;

import javax.inject.Singleton;

import air.air.net.zdremann.zsuds.BuildConfig;
import air.air.net.zdremann.zsuds.R;
import dagger.Module;
import dagger.Provides;

@Module(
      injects = {
            MyApplication.class,
            RoomRefresher.class
      },
      includes = {
            IOModule.class
      },
      library = true
)
public class ApplicationModule {
    public static final String MAIN_PREFS_NAME = "main";
    private final MyApplication mApplication;

    public ApplicationModule(MyApplication application) {
        mApplication = application;
    }

    @Provides
    @ForApplication
    public Context provideApplicationContext() {
        return mApplication;
    }

    @Provides
    GoogleAnalytics provideGoogleAnalytics(@ForApplication Context context) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
        analytics.setDryRun(
              BuildConfig.DEBUG ||
                    BuildConfig.VERSION_NAME.contains("dirty") ||
                    Build.FINGERPRINT.startsWith("generic")
        );
        analytics.getLogger().setLogLevel(BuildConfig.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.WARNING);
        return analytics;
    }
    @Provides @Singleton
    Tracker provideGoogleTracker(GoogleAnalytics googleAnalytics) {
        return googleAnalytics.newTracker(R.xml.global_tracker);
    }

    @Provides
    @Singleton
    LocationManager provideLocationManager(@ForApplication Context context) {
        return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Provides
    @Singleton
    AlarmManager provideAlarmManager(@ForApplication Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @Provides
    @Singleton
    NotificationManager provideNotificationManager(@ForApplication Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    @Singleton
    @Main
    SharedPreferences provideMainSharedPreferences(@ForApplication Context context) {
        return context.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    ContentResolver provideContentResolver(@ForApplication Context context) {
        return context.getContentResolver();
    }

    @Provides
    @Singleton
    ConnectivityManager provideConnectivityManager(@ForApplication Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
}
