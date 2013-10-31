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

package net.zdremann.wc.service;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.BuildConfig;

import static java.util.concurrent.TimeUnit.*;
import static net.zdremann.wc.provider.WasherCheckContract.PendingNotificationRooms;

public class NotificationService extends InjectingIntentService {

    public static final String TAG = "NotificationService";
    private static final long DEFAULT_WAIT_FOR_CYCLE_COMPLETE = MILLISECONDS.convert(5, MINUTES);
    private static final long WIGGLE_CHECK_TIME = MILLISECONDS.convert(30, SECONDS);

    @Inject
    AlarmManager mAlarmManager;
    @Inject
    ContentResolver mContentResolver;

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Cursor rooms = mContentResolver.query(
              PendingNotificationRooms.CONTENT_URI, PendingNotificationRooms.ALL_COLUMNS, null,
              null, null
        );
        assert rooms != null;
        long[] roomIds = new long[rooms.getCount()];
        int i = 0;

        while (rooms.moveToNext()) {
            roomIds[i++] = rooms.getLong(0);
        }

        rooms.close();

        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Refreshing " + i + " rooms");
        }

        WakefulBroadcastReceiver.startWakefulService(
              this, RoomRefresher.createIntent(
              this, roomIds
        )
        );
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}
