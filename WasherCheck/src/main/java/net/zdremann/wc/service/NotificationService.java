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
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.widget.Toast;

import net.zdremann.wc.provider.MachinesContract.Machines;
import net.zdremann.wc.provider.NotificationsContract.Notifications;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class NotificationService extends IntentService {

    public static final String TAG = "NotificationService";
    private static final long DEFAULT_WAIT_FOR_CYCLE_COMPLETE = MILLISECONDS.convert(5, MINUTES);

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        final ContentResolver contentResolver = getContentResolver();
        final Cursor notifications = contentResolver.query(Notifications.CONTENT_URI, null, null, null, Notifications.ROOM_ID);
        if (notifications == null || !notifications.moveToFirst()) {
            throw new IllegalStateException("Notifications Cursor is Invalid");
        }

        final String[] machineProjection =
                {Machines._ID, Machines.TYPE, Machines.NUMBER, Machines.STATUS, Machines.TIME_REMAINING};

        final int notif_idx_id = notifications.getColumnIndexOrThrow(Notifications._ID);
        final int notif_idx_room_id = notifications.getColumnIndexOrThrow(Notifications.ROOM_ID);
        final int notif_idx_type = notifications.getColumnIndexOrThrow(Notifications.TYPE);
        final int notif_idx_number = notifications.getColumnIndexOrThrow(Notifications.NUMBER);
        final int notif_idx_status = notifications.getColumnIndexOrThrow(Notifications.STATUS);

        final LongSparseArray<Cursor> rooms = new LongSparseArray<Cursor>();

        long nextCheckMillis = Long.MAX_VALUE;
        List<Long> finishedNotifications = new ArrayList<Long>();

        while (notifications.moveToNext()) {
            long roomId = notifications.getLong(notif_idx_room_id);
            Cursor machines = rooms.get(roomId);
            if (machines == null) {
                machines = contentResolver.query(Machines.buildRoomUri(roomId, MILLISECONDS.convert(1, MINUTES)), machineProjection, null, null, null);
                assert machines != null;
                rooms.put(roomId, machines);
            }

            for (boolean hasMachine = machines.moveToFirst(); hasMachine; hasMachine = machines.moveToNext()) {
                // TODO: Check if machine found
                if (machines.getInt(1) == notifications.getInt(notif_idx_type) &&
                        machines.getInt(2) == notifications.getInt(notif_idx_number)) {
                    if (machines.getInt(3) <= notifications.getInt(notif_idx_status)) {
                        finishedNotifications.add(notifications.getLong(notif_idx_id));
                    } else {
                        long checkThisNext = (long) (6000 * machines.getFloat(4));
                        if (checkThisNext <= 0)
                            checkThisNext = DEFAULT_WAIT_FOR_CYCLE_COMPLETE;
                        nextCheckMillis = Math.min(nextCheckMillis, checkThisNext);
                    }
                }
            }
        }

        for (Long notificationId : finishedNotifications) {
            contentResolver.delete(Notifications.fromId(notificationId), null, null);
        }

        Toast.makeText(this, "Found " + finishedNotifications.size() + " machines", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Found " + finishedNotifications.size() + " machines");

        if (nextCheckMillis != Long.MAX_VALUE) {
            am.cancel(pendingIntent);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + nextCheckMillis, pendingIntent);
        }
    }
}
