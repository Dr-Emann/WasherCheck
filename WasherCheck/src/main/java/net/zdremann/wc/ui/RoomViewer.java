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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.zdremann.wc.GcmRegistrationId;
import net.zdremann.wc.Main;
import net.zdremann.wc.io.locations.LocationsProxy;
import net.zdremann.wc.model.MachineGrouping;
import net.zdremann.wc.service.ClearCompletedNotificationsService;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.BackupAgent;
import air.air.net.zdremann.zsuds.BuildConfig;
import air.air.net.zdremann.zsuds.R;

public class RoomViewer extends InjectingActivity {
    public static final String ARG_ROOM_ID = "room_id";
    private static final String TAG = "RoomViewer";
    public static final float STATUS_BAR_DARKEN_AMOUNT = 0.85f;

    @Inject
    LocationsProxy mLocationsProxy;

    @Inject
    @Main
    SharedPreferences mPreferences;

    @Inject
    GoogleCloudMessaging mGoogleCloudMessaging;

    @Inject
    @GcmRegistrationId
    Future<String> mGcmRegistrationId;

    private long mRoomId;

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
            new AsyncTask<Void, Void, HttpResponse>() {
                @Override
                protected HttpResponse doInBackground(Void... params) {
                    try {
                        HttpClient client = new DefaultHttpClient();
                        HttpPost post = new HttpPost(
                              "http://net-zdremann-wc.appspot.com/unregister"
                        );
                        List<NameValuePair> pair = new ArrayList<NameValuePair>();
                        pair.add(new BasicNameValuePair("device-id", mGcmRegistrationId.get()));
                        post.setEntity(new UrlEncodedFormEntity(pair));

                        return client.execute(post);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(HttpResponse httpResponse) {
                    super.onPostExecute(httpResponse);
                    if (httpResponse == null || httpResponse.getStatusLine()
                          .getStatusCode() != 200) {
                        gaTracker.send(
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
