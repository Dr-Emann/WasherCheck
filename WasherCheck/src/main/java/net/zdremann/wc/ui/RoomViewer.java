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

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.zdremann.util.AsyncTaskResult;
import net.zdremann.wc.ActivityComponent;
import net.zdremann.wc.GcmRegistrationId;
import net.zdremann.wc.Main;
import net.zdremann.wc.WcApplication;
import net.zdremann.wc.io.locations.LocationsProxy;
import net.zdremann.wc.model.MachineGrouping;
import net.zdremann.wc.service.ClearCompletedNotificationsService;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Future;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.BackupAgent;
import air.air.net.zdremann.zsuds.BuildConfig;
import air.air.net.zdremann.zsuds.R;

public class RoomViewer extends BaseActivity {
    public static final String ARG_ROOM_ID = "room_id";
    private static final String TAG = "RoomViewer";
    public static final float STATUS_BAR_DARKEN_AMOUNT = 0.85f;

    @Inject
    LocationsProxy mLocationsProxy;

    @Inject
    @Main
    SharedPreferences mPreferences;

    @Nullable
    @Inject
    GoogleCloudMessaging mGoogleCloudMessaging;

    @Inject
    @GcmRegistrationId
    Future<String> mGcmRegistrationId;

    @Inject
    Tracker mTracker;

    private long mRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getComponent().inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.room_viewer, menu);
        MenuItem fakeIoMenuItem = menu.findItem(R.id.action_fake_io);

        assert fakeIoMenuItem != null;

        fakeIoMenuItem.setVisible(BuildConfig.DEBUG);
        fakeIoMenuItem.setChecked(mPreferences.getBoolean("net.zdremann.wc.fake_io", false));

        MenuItem removePendingNotifications = menu.findItem(R.id.action_remove_notifications);
        assert removePendingNotifications != null;
        removePendingNotifications.setVisible(mGoogleCloudMessaging != null);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_change_school:
            startChooseSchool(false);
            return true;
        case R.id.action_change_room:
            startChooseRoom();
            return true;
        case R.id.action_remove_notifications:
            new AsyncTask<Void, Void, AsyncTaskResult<Void>>() {
                @Override
                protected AsyncTaskResult<Void> doInBackground(Void... params) {
                    URL url;
                    try {
                        url = new URL("http://net-zdremann-wc.appspot.com/unregister");
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("This should never happen", e);
                    }
                    HttpURLConnection urlConnection = null;
                    try {
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setDoOutput(true);
                        urlConnection.setDoInput(true);
                        Writer out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                        out.write(URLEncoder.encode("device-id", "UTF-8"));
                        out.write('=');
                        out.write(URLEncoder.encode(mGcmRegistrationId.get(), "UTF-8"));
                        out.flush();
                        out.close();

                        InputStream in = urlConnection.getInputStream();
                        in.close();
                        return AsyncTaskResult.value(null);
                    }
                    catch (Exception e) {
                        return AsyncTaskResult.error(e);
                    }
                    finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    }
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<Void> httpResponse) {
                    if (!httpResponse.isValue()) {
                        WcApplication.getComponent().gaTracker().send(
                                new HitBuilders.ExceptionBuilder()
                                        .setDescription("Unable to remove notifications")
                                        .setFatal(false).build()
                        );

                        Toast.makeText(
                                RoomViewer.this,
                                "Unable to remove notifications, please try again",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }.execute();
            return true;
        case R.id.action_fake_io:
            boolean useFakeIo = !item.isChecked();
            mPreferences.edit().putBoolean("net.zdremann.wc.fake_io", useFakeIo).commit();
            item.setChecked(useFakeIo);
            return true;
        default:
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case 0:
            if (resultCode == RESULT_OK) {
                long roomId = data.getLongExtra(ARG_ROOM_ID, 0);
                mPreferences.edit().putLong(ARG_ROOM_ID, roomId).commit();
                BackupAgent.requestBackup(this);

                setRoomId(roomId);
            } else {
                if (!mPreferences.contains(ARG_ROOM_ID))
                    finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPreferences.contains(ARG_ROOM_ID)) {
            setRoomId(mPreferences.getLong(ARG_ROOM_ID, 0));
        } else {
            startChooseSchool(true);
        }

        mTracker.setScreenName("RoomViewer");
        mTracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, String.valueOf(mRoomId)).build());


        setContentView(R.layout.activity_room_viewer);

        startService(new Intent(this, ClearCompletedNotificationsService.class));
    }

    protected void startChooseRoom() {
        Intent intent = new Intent(this, RoomChooserActivity.class);
        final MachineGrouping parent = mLocationsProxy.parentOf(mRoomId);

        if (parent != null) {
            intent.putExtra(RoomChooserActivity.ARG_GROUPING_ID, parent.id);
        }

        startActivityForResult(intent, 0);
    }

    protected void setRoomId(long roomId) {
        mRoomId = roomId;

        Bundle arguments = new Bundle();
        arguments.putLong(RoomViewFragment.ARG_ROOM_ID, mRoomId);
        arguments.putBoolean(RoomViewFragment.ARG_SELECT_MODE, true);

        final MachineGrouping roomGroup = mLocationsProxy.getGrouping(roomId);

        if (roomGroup != null) {
            getSupportActionBar().setBackgroundDrawable(roomGroup.getColorDrawable(this));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(roomGroup.getColor() != null) {
                    int color = roomGroup.getColor();
                    float[] hsv = new float[3];
                    Color.colorToHSV(color, hsv);
                    // darken the color
                    hsv[2] *= STATUS_BAR_DARKEN_AMOUNT;
                    int darkerColor = Color.HSVToColor(hsv);
                    getWindow().setStatusBarColor(darkerColor);
                    setTaskDescription(new ActivityManager.TaskDescription(null, null, color));
                }
            }
        }
        getSupportActionBar().setTitle(roomGroup != null ? roomGroup.name : null);

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        final Fragment fragment = new RoomViewFragment();
        fragment.setArguments(arguments);
        ft.replace(android.R.id.content, fragment);
        ft.commitAllowingStateLoss();
    }

    protected void startChooseSchool(boolean firstChoice) {
        Intent intent = new Intent(this, RoomChooserActivity.class);
        intent.putExtra(RoomChooserActivity.ARG_GROUPING_ID, RoomChooserActivity.ROOT_ID);
        intent.putExtra(RoomChooserActivity.ARG_FIRST_CHOICE, firstChoice);
        startActivityForResult(intent, 0);
    }

    @Override
    public String toString() {
        return "RoomViewer{" +
              "mRoomId=" + mRoomId +
              '}';
    }
}
