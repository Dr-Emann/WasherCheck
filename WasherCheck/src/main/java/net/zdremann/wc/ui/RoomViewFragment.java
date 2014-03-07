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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.zdremann.wc.ForActivity;
import net.zdremann.wc.GcmRegistrationId;
import net.zdremann.wc.io.rooms.TmpRoomLoader;
import net.zdremann.wc.model.Machine;
import net.zdremann.wc.service.MachinesLoadedBroadcastReceiver;
import net.zdremann.wc.service.RoomRefresher;
import net.zdremann.wc.ui.widget.SimpleSectionedListAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.R;

import static java.util.concurrent.TimeUnit.*;
import static net.zdremann.wc.provider.WasherCheckContract.*;

public class RoomViewFragment extends InjectingListFragment
      implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ROOM_ID = "room_id";
    public static final String ARG_SELECT_MODE = "select_mode";
    private static final int MSG_REFRESH_SUCCESS = 0x000;
    private static final int MSG_REFRESH_FAILURE = 0x001;
    private static final int MSG_REFRESH_START = 0x002;
    private static final long MINUTE = 60 * 1000;

    private final Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(MSG_REFRESH_START);
            mActivityContext.startService(RoomRefresher.createIntent(mActivityContext, mRoomId));
        }
    };
    private final BroadcastReceiver mRefreshCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean successful = intent.getBooleanExtra(
                  MachinesLoadedBroadcastReceiver.EXTRA_SUCCESSFUL_LOAD, false
            );
            mHandler.sendEmptyMessage((successful) ? MSG_REFRESH_SUCCESS : MSG_REFRESH_FAILURE);
        }
    };
    MyRoomViewAdapter mRoomViewAdapter;
    SimpleSectionedListAdapter mAdapter;
    @Inject
    @ForActivity
    Context mActivityContext;

    @Inject
    GoogleCloudMessaging mGoogleCloudMessaging;

    @Inject
    @GcmRegistrationId
    Future<String> mGcmRegistrationId;

    private long mRoomId;
    private boolean mIsLoading = false;
    private MenuItem mRefreshItem;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REFRESH_START:
                setIsLoading(true);
                break;
            case MSG_REFRESH_FAILURE:
                Toast.makeText(getActivity(), R.string.error_refresh_general, Toast.LENGTH_SHORT)
                      .show();
                //Intentional fall-through
            case MSG_REFRESH_SUCCESS:
                setIsLoading(false);
                break;
            }
        }
    };
    private ScheduledThreadPoolExecutor mRefreshPool;

    protected void setIsLoading(final boolean isLoading) {
        mIsLoading = isLoading;
        if (mRefreshItem != null) {
            if (isLoading) {
                MenuItemCompat.setActionView(mRefreshItem, R.layout.actionbar_indeterminite_progress);
            } else {
                MenuItemCompat.setActionView(mRefreshItem, null);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new TmpRoomLoader(mActivityContext, mRoomId);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, @Nullable final Cursor cursor) {
        if (getActivity() == null)
            return;

        setEmptyText(getText(R.string.machines_empty));

        final ArrayList<SimpleSectionedListAdapter.Section> sections =
              new ArrayList<SimpleSectionedListAdapter.Section>(Machine.Type.values().length);

        if (cursor != null) {
            cursor.moveToFirst();
            Machine.Type lastType = null;

            int idx_type = cursor.getColumnIndex(MachineStatus.MACHINE_TYPE);
            while (!cursor.isAfterLast()) {
                Machine.Type currentType = Machine.Type.fromInt(cursor.getInt(idx_type));
                if (currentType != lastType)
                    sections.add(
                          new SimpleSectionedListAdapter.Section(
                                cursor.getPosition(),
                                currentType.toString(mActivityContext)
                          )
                    );

                lastType = currentType;
                cursor.moveToNext();
            }

            mRoomViewAdapter.changeCursor(cursor);
            SimpleSectionedListAdapter.Section[] dummy =
                  new SimpleSectionedListAdapter.Section[sections.size()];
            mAdapter.setSections(sections.toArray(dummy));
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mRoomViewAdapter.swapCursor(null);
        setIsLoading(false);
    }

    @Override
    public String toString() {
        return "RoomViewFragment{" +
              "mRoomId=" + mRoomId +
              '}';
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRoomViewAdapter = new MyRoomViewAdapter(mActivityContext);
        mAdapter = new SimpleSectionedListAdapter(
              mActivityContext, R.layout.item_machine_header, mRoomViewAdapter
        );

        setEmptyText(getText(R.string.machines_empty));

        final Bundle arguments = getArguments();

        setListAdapter(mAdapter);

        if (arguments == null) {
            mRoomId = 0;
        } else {
            mRoomId = arguments.getLong(ARG_ROOM_ID);
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //mRefreshRunnable.run();
        mRefreshPool = new ScheduledThreadPoolExecutor(1);
        mRefreshPool.scheduleWithFixedDelay(mRefreshRunnable, 0, 5 * 60, SECONDS);
        mActivityContext.registerReceiver(
              mRefreshCompleteReceiver, new IntentFilter(
              MachinesLoadedBroadcastReceiver.BROADCAST_TAG
        )
        );
    }

    @Override
    public void onStart() {
        super.onStart();

        getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);

        if(mGoogleCloudMessaging != null)
            registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(
          ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;

        final Cursor item = (Cursor) mAdapter.getItem(adapterMenuInfo.position);
        final int idx_status = item.getColumnIndex(MachineStatus.STATUS);

        final int status = item.getInt(idx_status);

        if (status <= Machine.Status.AVAILABLE.ordinal()) {
            return;
        }

        final MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.room_view_contextual, menu);
        final MenuItem completeBtn = menu.findItem(R.id.action_notify_cycle_complete);

        assert completeBtn != null;
        completeBtn.setVisible(status > Machine.Status.CYCLE_COMPLETE.ordinal());
    }

    @Override
    public void onPause() {
        super.onPause();
        mRefreshPool.shutdown();
        mActivityContext.unregisterReceiver(mRefreshCompleteReceiver);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
              .getMenuInfo();
        assert menuInfo != null;
        final int index = menuInfo.position;
        final Cursor cursor = (Cursor) mAdapter.getItem(index);
        final Machine.Status desiredStatus;

        switch (item.getItemId()) {
        case R.id.action_notify_available:
            desiredStatus = Machine.Status.AVAILABLE;
            break;
        case R.id.action_notify_cycle_complete:
            desiredStatus = Machine.Status.CYCLE_COMPLETE;
            break;
        default:
            return super.onContextItemSelected(item);
        }

        new AsyncTask<Void, Void, HttpResponse>() {

            @Override
            protected HttpResponse doInBackground(Void... params) {
                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpPost post = new HttpPost("http://net-zdremann-wc.appspot.com/register");
                    List<NameValuePair> pair = new ArrayList<NameValuePair>();
                    pair.add(new BasicNameValuePair("room-id", String.valueOf(mRoomId)));
                    pair.add(new BasicNameValuePair("device-id", mGcmRegistrationId.get()));
                    pair.add(new BasicNameValuePair("machine-number", cursor.getString(
                          cursor.getColumnIndex(
                                MachineStatusColumns.NUMBER
                          )
                    )));
                    pair.add(new BasicNameValuePair("machine-type", cursor.getString(
                          cursor.getColumnIndex(MachineStatusColumns.MACHINE_TYPE)
                    )));
                    pair.add(new BasicNameValuePair("machine-status", String.valueOf(
                          desiredStatus.ordinal()
                    )));
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
                if(httpResponse.getStatusLine().getStatusCode() != 200) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    try {
                        httpResponse.getEntity().writeTo(buffer);
                        Toast.makeText(getActivity(), buffer.toString(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute();

        //contentResolver.insert(PendingNotification.CONTENT_URI, cv);
        //Intent intent = new Intent("net.zdremann.wc.NEED_PENDING_NOTIF_CHECK");
        //getActivity().sendBroadcast(intent);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_room_view, menu);
        mRefreshItem = menu.findItem(R.id.action_refresh);
        assert mRefreshItem != null;
        if (mIsLoading) {
            MenuItemCompat.setActionView(mRefreshItem, R.layout.actionbar_indeterminite_progress);
        } else {
            MenuItemCompat.setActionView(mRefreshItem, null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            mRefreshPool.execute(mRefreshRunnable);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    static class MyRoomViewAdapter extends CursorAdapter {

        private final LayoutInflater mLayoutInflater;
        private final TypefaceSpan mPostfixSpan = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) ?
                                                  new TypefaceSpan(
                                                        "sans-serif-thin"
                                                  ) :
                                                  new TypefaceSpan("sans-serif-light");
        private int idx_id;
        private int idx_room_id;
        private int idx_machine_id;
        private int idx_number;
        private int idx_type;
        private int idx_status;
        private int idx_time_remaining;

        private MyRoomViewAdapter(@NotNull Context context) {
            super(context, null, false);
            mLayoutInflater = (LayoutInflater.from(context));
        }

        @Nullable
        @Override
        public Cursor swapCursor(@Nullable Cursor newCursor) {
            Cursor cursor = super.swapCursor(newCursor);

            if (newCursor != null) {
                idx_id = newCursor.getColumnIndex(MachineStatus._ID);
                idx_room_id = newCursor.getColumnIndex(MachineStatus.ROOM_ID);
                idx_machine_id = newCursor.getColumnIndex(MachineStatus.MACHINE_ID);
                idx_number = newCursor.getColumnIndex(MachineStatus.NUMBER);
                idx_type = newCursor.getColumnIndex(MachineStatus.MACHINE_TYPE);
                idx_status = newCursor.getColumnIndex(MachineStatus.STATUS);
                idx_time_remaining = newCursor.getColumnIndex(
                      MachineStatus.REPORTED_TIME_REMAINING
                );
            }
            return cursor;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mLayoutInflater.inflate(R.layout.item_machine_row, parent, false);
            assert view != null;
            view.setTag(ViewHolder.from(view));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder vh = (ViewHolder) view.getTag();
            Resources res = context.getResources();

            int number = cursor.getInt(idx_number);
            Machine.Status status = Machine.Status.fromInt(cursor.getInt(idx_status));
            long timeRemaining = cursor.getLong(idx_time_remaining);

            if (number == -1)
                vh.number.setText(R.string.machine_number_unknown);
            else
                vh.number.setText(String.valueOf(number));
            vh.status.setText(status.toString(context));
            vh.status.setTextColor(status.getColor(context));

            if (timeRemaining >= 0) {
                String timeText = String
                      .format("%.0f", (double) timeRemaining / MINUTE);
                String timePostfix = res.getString(R.string.minutes_remaining_postfix);

                Spannable spanRange = new SpannableString(timeText + " " + timePostfix);
                spanRange.setSpan(mPostfixSpan, timeText.length(), spanRange.length(), 0);
                vh.time.setText(spanRange, TextView.BufferType.SPANNABLE);

                vh.time.setVisibility(View.VISIBLE);
            } else {
                vh.time.setVisibility(View.GONE);
            }
        }

        private static class ViewHolder {
            public TextView number;
            public TextView status;
            public TextView time;

            @NotNull
            public static ViewHolder from(@NotNull View v) {
                final ViewHolder vh = new ViewHolder();
                vh.number = (TextView) v.findViewById(R.id.number);
                vh.status = (TextView) v.findViewById(R.id.status);
                vh.time = (TextView) v.findViewById(R.id.time_remaining);

                return vh;
            }
        }
    }
}
