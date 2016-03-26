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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;

import net.zdremann.wc.ActivityComponent;
import net.zdremann.wc.Main;
import net.zdremann.wc.WcApplication;
import net.zdremann.wc.io.locations.LocationsProxy;
import net.zdremann.wc.model.MachineGrouping;
import net.zdremann.wc.model.MachineGrouping.Type;
import net.zdremann.wc.ui.RoomChooserFragment.RoomChooserListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.R;

public class RoomChooserActivity extends BaseActivity implements RoomChooserListener {

    public static final String ARG_GROUPING_ID = "groupingId";
    public static final String ARG_FIRST_CHOICE = "firstChoice";
    public static final long ROOT_ID = 0;
    private final Criteria mLocationCriteria = new Criteria();
    private long mRootId;
    private MenuItem mGuessLocationItem;

    @Inject
    LocationManager mLocationManager;
    @Inject
    LocationsProxy mLocationsProxy;
    @Inject
    @Main
    SharedPreferences mPreferences;

    public RoomChooserActivity() {
        super();
        mLocationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getComponent().inject(this);

        mRootId = getIntent().getLongExtra(ARG_GROUPING_ID, Long.MIN_VALUE);

        MachineGrouping grouping = mLocationsProxy.getGrouping(mRootId);
        updateTitle(grouping);

        setContentView(R.layout.activity_room_chooser);
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            RoomChooserFragment fragment = new RoomChooserFragment();
            fragment.setArguments(getIntent().getExtras());
            transaction.replace(android.R.id.content, fragment);
            transaction.commit();
        }

        if (savedInstanceState == null && getIntent().getBooleanExtra(ARG_FIRST_CHOICE, false))
            guessLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.room_chooser, menu);

        mGuessLocationItem = menu.findItem(R.id.guess_location);
        mGuessLocationItem.setVisible(hasLocationAbility());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                MachineGrouping parent = mLocationsProxy.parentOf(mRootId);
                FragmentManager fm = getSupportFragmentManager();

                if (parent == null) {
                    if (!mPreferences.contains(RoomViewer.ARG_ROOM_ID)) {
                        this.setResult(RESULT_CANCELED);
                        this.finish();
                    } else {
                        NavUtils.navigateUpFromSameTask(this);
                    }
                } else if (fm.getBackStackEntryCount() > 0) {
                    updateTitle(parent);
                    mRootId = parent.id;
                    fm.popBackStack();
                } else {
                    updateTitle(parent);
                    mRootId = parent.id;
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    Fragment fragment = new RoomChooserFragment();

                    transaction.setCustomAnimations(
                            android.R.anim.slide_in_left, android.R.anim.slide_out_right
                    );

                    Bundle data = new Bundle();
                    data.putLong(ARG_GROUPING_ID, mRootId);

                    fragment.setArguments(data);
                    transaction.replace(android.R.id.content, fragment);
                    transaction.commit();
                }
                return true;
            case R.id.guess_location:
                guessLocation();
                return true;
        }
        return false;
    }

    protected void guessLocation() {
        if (hasLocationAbility()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            @Nullable
            final Location lastKnownLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastKnownLocation == null)
                return;
            MachineGrouping candidate = mLocationsProxy.getClosestLocation(lastKnownLocation);
            if (candidate != null && lastKnownLocation.distanceTo(candidate.location) < 8047) {
                Toast.makeText(this, "Guessing Location: " + candidate.name, Toast.LENGTH_SHORT)
                      .show();
                onRoomChosen(candidate);
            }
        }
    }

    public void onRoomChosen(@NotNull MachineGrouping newRoot) {
        mRootId = newRoot.id;
        if (Type.ROOM.equals(newRoot.type)) {
            Intent data = new Intent();
            data.putExtra(RoomViewer.ARG_ROOM_ID, newRoot.id);

            MachineGrouping college = newRoot;
            while(college.type != Type.SCHOOL) {
                college = college.parent;
                assert college != null;
            }

            WcApplication.getComponent().gaTracker().send(
                  new HitBuilders.EventBuilder()
                        .setCategory("room").setAction("changed")
                        .setLabel(String.valueOf(mRootId))
                        .setCustomDimension(1, String.valueOf(mRootId))
                        .setCustomDimension(2, String.valueOf(college.name)).build()
            );

            setResult(RESULT_OK, data);

            finish();
        } else {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.addToBackStack(null);
            transaction.setCustomAnimations(
                  R.anim.slide_in_right, R.anim.slide_out_left,
                  android.R.anim.slide_in_left, android.R.anim.slide_out_right
            );

            Fragment fragment = new RoomChooserFragment();
            Bundle data = new Bundle();
            data.putLong(ARG_GROUPING_ID, newRoot.id);
            fragment.setArguments(data);
            transaction.replace(android.R.id.content, fragment);
            transaction.commitAllowingStateLoss();
            updateTitle(newRoot);
        }
    }

    private void updateTitle(@Nullable MachineGrouping grouping) {
        if (grouping == null || grouping.id == ROOT_ID)
            this.setTitle(R.string.title_room_chooser);
        else
            this.setTitle(grouping.name);
    }

    private boolean hasLocationAbility() {
        return mLocationManager.getProviders(mLocationCriteria, true).size() > 0;
    }

    @Override
    public String toString() {
        return "RoomChooserActivity{" +
              "mRootId=" + mRootId +
              '}';
    }
}
