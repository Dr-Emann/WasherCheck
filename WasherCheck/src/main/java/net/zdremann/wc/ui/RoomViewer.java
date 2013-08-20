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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.zdremann.wc.Main;
import net.zdremann.wc.R;
import net.zdremann.wc.io.locations.LocationsProxy;
import net.zdremann.wc.model.MachineGrouping;

import javax.inject.Inject;

public class RoomViewer extends InjectingActivity {
    public static final String ARG_ROOM_ID = "room_id";
    @Inject
    LocationsProxy mLocationsProxy;
    @Inject
    @Main
    SharedPreferences mPreferences;

    private Handler mHandler = new Handler();
    private long mRoomId;

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.room_viewer, menu);
        MenuItem fakeIoMenuItem = menu.findItem(R.id.action_fake_io);

        assert fakeIoMenuItem != null;

        fakeIoMenuItem.setChecked(mPreferences.getBoolean("net.zdremann.wc.fake_io", false));
        return true;
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
            case R.id.action_fake_io:
                boolean useFakeIo = !item.isChecked();
                mPreferences.edit().putBoolean("net.zdremann.wc.fake_io", useFakeIo).apply();
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
                    mPreferences.edit().putLong(ARG_ROOM_ID, roomId).apply();

                    gaTracker.setStartSession(mRoomId != roomId);
                    gaTracker.setCustomDimension(1, String.valueOf(roomId));
                    gaTracker.sendEvent("Room", "Chosen", String.valueOf(roomId), 0L);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            recreate();
                        }
                    });
                    setRoomId(roomId);
                } else {
                    if (!mPreferences.contains(ARG_ROOM_ID))
                        finish();
                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mPreferences.contains(ARG_ROOM_ID)) {
            setRoomId(mPreferences.getLong(ARG_ROOM_ID, 0));
        } else {
            startChooseSchool(true);
        }

        MachineGrouping grouping = mLocationsProxy.getGrouping(mRoomId);

        if (grouping != null)
            setTheme(grouping.getTheme().getResource());

        setContentView(R.layout.activity_room_viewer);

        gaTracker.setCustomDimension(1, String.valueOf(mRoomId));
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

        setTitle(roomGroup != null ? roomGroup.name : null);

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
