/*
 * Copyright (c) 2014. Zachary Dremann
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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import net.zdremann.wc.model.Machine;
import net.zdremann.wc.provider.WasherCheckContract.PendingNotification;
import net.zdremann.wc.provider.WasherCheckContract.PendingNotificationMachineStatus;
import net.zdremann.wc.ui.RoomViewer;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.R;

import static java.util.concurrent.TimeUnit.*;

public class PendingNotificationCheckService extends InjectingIntentService {
    private static final long MINUTE = 60 * 1000L;
    public static final String TAG = "PendingNotificationCheckService";
    private static final long DEFAULT_TIME_CYCLE_COMPLETE = 5 * MINUTE;
    private static final long DEFAULT_TIME_IN_USE = DEFAULT_TIME_CYCLE_COMPLETE;
    private static final long DEFAULT_TIME_UNAVAILABLE = 45 * MINUTE;
    private static final long DEFAULT_TIME_UNKNOWN = 30 * MINUTE;
    private static final String[] NOTIFICATION_PROJECTION = {
          PendingNotificationMachineStatus._ID, PendingNotificationMachineStatus.NOTIF_CREATED,
          PendingNotificationMachineStatus.STATUS, PendingNotificationMachineStatus.DESIRED_STATUS,
          PendingNotificationMachineStatus.REPORTED_TIME_REMAINING
    };
    private static final String ARG_ROOM_IDS = "net.zdremann.wc.room_ids";
    @Inject
    ContentResolver mContentResolver;
    @Inject
    AlarmManager mAlarmManager;
    @Inject
    NotificationManager mNotificationManager;

    public PendingNotificationCheckService() {
        super(TAG);
    }

    public static Intent createServiceIntent(Context ctx, long... roomIds) {
        Intent intent = new Intent(ctx, PendingNotificationCheckService.class);
        intent.putExtra(ARG_ROOM_IDS, roomIds);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long[] roomIds = intent.getLongArrayExtra(ARG_ROOM_IDS);
        int compleatedNotifs = 0;
        ContentValues cv = new ContentValues();
        assert roomIds != null;
        for (long roomId : roomIds) {
            Cursor notifications = mContentResolver.query(
                  PendingNotificationMachineStatus.CONTENT_URI,
                  NOTIFICATION_PROJECTION, PendingNotificationMachineStatus.ROOM_ID + "=" + roomId,
                  null, null
            );
            assert notifications != null;

            try {
                while (notifications.moveToNext()) {
                    long notifId = notifications.getInt(0);
                    int desiredStatus = notifications.getInt(3);
                    int currentStatus = notifications.getInt(2);
                    long statusUpdateTime = notifications.getLong(1);
                    if (currentStatus <= desiredStatus) {
                        compleatedNotifs += 1;
                        mContentResolver.delete(PendingNotification.fromId(notifId), null, null);
                    } else {
                        long estimatedTimeRemaining = getRemainingTime(notifications);
                        long estTimeOfCompletion = statusUpdateTime + estimatedTimeRemaining;
                        cv.put(PendingNotification.EST_TIME_OF_COMPLETION, estTimeOfCompletion);
                        mContentResolver.update(
                              PendingNotification.fromId(notifId), cv, null, null
                        );
                    }
                }
            } finally {
                notifications.close();
            }
        }

        if (compleatedNotifs > 0) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                  .setContentTitle("Machines Ready")
                  .setContentText(compleatedNotifs + " Machines are ready")
                  .setNumber(compleatedNotifs)
                  .setContentIntent(
                        PendingIntent.getActivity(
                              this, 0, new Intent(
                              this, RoomViewer.class
                        ), 0
                        )
                  )
                  .setSmallIcon(R.drawable.ic_launcher)
                  .setAutoCancel(true)
                  .setDefaults(Notification.DEFAULT_ALL);
            Notification notification = builder.build();

            mNotificationManager.notify(0, notification);
        }

        Cursor nextCheckQuery = mContentResolver.query(
              PendingNotification.CONTENT_URI, new String[]{
              "MIN(" + PendingNotification.EST_TIME_OF_COMPLETION + ")"
        }, null, null, null
        );
        assert nextCheckQuery != null;

        try {
            if (nextCheckQuery.moveToFirst()) {
                long nextCheck = nextCheckQuery.getLong(0);
                if (nextCheck > 0) {
                    PendingIntent serviceIntent = PendingIntent.getBroadcast(
                          this, 0, new Intent(PendingNotifCheckNeededBroadcastRec.BROADCAST_TAG), 0
                    );
                    Log.d(
                          TAG, "Refreshing in " + MILLISECONDS.toSeconds(
                          nextCheck - System.currentTimeMillis()
                    ) + " seconds"
                    );
                    mAlarmManager.set(
                          AlarmManager.RTC_WAKEUP, nextCheck, serviceIntent
                    );
                }
            }
        } finally {
            nextCheckQuery.close();
        }
    }

    protected long getRemainingTime(@NotNull final Cursor notification) {
        long timeRemaining = notification.getLong(4);
        if (timeRemaining < 1000) {
            int currentStatus = notification.getInt(2);
            if (currentStatus == Machine.Status.CYCLE_COMPLETE.ordinal())
                return DEFAULT_TIME_CYCLE_COMPLETE;
            else if (currentStatus == Machine.Status.IN_USE.ordinal())
                return DEFAULT_TIME_IN_USE;
            else if (currentStatus == Machine.Status.UNAVAILABLE.ordinal())
                return DEFAULT_TIME_UNAVAILABLE;
            else
                return DEFAULT_TIME_UNKNOWN;
        } else {
            return Math.max(MINUTE, timeRemaining * 6 / 10);
        }
    }
}
