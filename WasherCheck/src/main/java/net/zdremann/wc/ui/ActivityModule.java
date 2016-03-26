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

package net.zdremann.wc.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import net.zdremann.ActivityScope;
import net.zdremann.ForActivity;
import net.zdremann.ForApplication;
import net.zdremann.util.CompletedFuture;
import net.zdremann.wc.AppVersion;
import net.zdremann.wc.GcmRegistrationId;
import net.zdremann.wc.Main;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


import dagger.Module;
import dagger.Provides;

@Module
public class ActivityModule {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "app_version";
    private static final String SENDER_ID = "639684360936";
    private final Activity mActivity;

    public ActivityModule(Activity activity) {
        mActivity = activity;
    }

    @Provides
    public Activity provideActivity() {
        return mActivity;
    }

    @Provides
    @ForActivity
    public Context provideActivityContext(Activity activity) {
        return activity;
    }

    @Provides
    @ActivityScope
    public LayoutInflater provideLayoutInflater(@ForActivity Context context) {
        return LayoutInflater.from(context);
    }

    @Provides
    @Nullable
    GoogleCloudMessaging provideGcm(Activity activity) {
        if (checkPlayServices())
            return GoogleCloudMessaging.getInstance(activity);
        else
            return null;
    }

    @Provides
    @GcmRegistrationId
    Future<String> provideGcmRegistrationId(
          @ForApplication final Context context,
          @Main final SharedPreferences preferences,
          @Nullable final GoogleCloudMessaging gcm) {
        if (gcm == null) {
            return new CompletedFuture<String>(null);
        }
        final FutureTask<String> task = new FutureTask<String>(
              new Callable<String>() {
                  @Override
                  public String call() throws Exception {
                      final String registrationId = preferences.getString(PROPERTY_REG_ID, "");
                      if (TextUtils.isEmpty(registrationId)) {
                          Log.i("Gcm Registration", "Registration not found");
                          try {
                              String regId = gcm.register(SENDER_ID);

                              storeRegistrationId(context, preferences, regId);
                              return regId;
                          } catch (IOException e) {
                              Log.e("GcmRegister", "Can't register for GCM", e);
                              throw e;
                          }
                      } else {
                          return registrationId;
                      }
                  }
              }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            AsyncTask.THREAD_POOL_EXECUTOR.execute(task);
        else {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void[] params) {
                    task.run();
                    return null;
                }
            }.execute();
        }
        return task;
    }

    private void storeRegistrationId(Context context, SharedPreferences preferences, String regId) {
        int appVersion = provideAppVersion(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    @Provides
    @AppVersion
    public int provideAppVersion(@ForApplication Context context) {
        try {
            //noinspection ConstantConditions
            PackageInfo packageInfo = context.getPackageManager()
                  .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen
            throw new RuntimeException("Could not get package name", e);
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int resultCode = googleApi.isGooglePlayServicesAvailable(mActivity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApi.isUserResolvableError(resultCode)) {
                googleApi.getErrorDialog(mActivity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("PlayServiceCheck", "Unsupported device");
            }
            return false;
        }
        return true;
    }
}
