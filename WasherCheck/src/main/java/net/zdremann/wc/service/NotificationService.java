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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import net.zdremann.wc.BuildConfig;
import net.zdremann.wc.R;
import net.zdremann.wc.ui.RoomViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


import javax.inject.Inject;
import javax.inject.Provider;


import static java.util.concurrent.TimeUnit.*;
import static net.zdremann.wc.provider.WasherCheckContract.*;

public class NotificationService extends InjectingIntentService {

    public static final String TAG = "NotificationService";
    private static final long DEFAULT_WAIT_FOR_CYCLE_COMPLETE = MILLISECONDS.convert(5, MINUTES);
    private static final long WIGGLE_CHECK_TIME = MILLISECONDS.convert(30, SECONDS);

    @Inject
    Provider<RoomRefresher> roomRefresherProvider;

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(BuildConfig.DEBUG) {
            Log.v(TAG, "Checking for satisfied pending notifications");
        }
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        final Intent broadcastIntent = new Intent("net.zdremann.wc.NEED_PENDING_NOTIF_CHECK");
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, 0);

        final ContentResolver contentResolver = getContentResolver();
        final Cursor notifications = contentResolver.query(PendingNotificationMachine.CONTENT_URI, null, null, null, PendingNotificationMachine.ROOM_ID);
        if (notifications == null) {
            throw new IllegalStateException("Notification Cursor is Invalid");
        }

        final String[] machineProjection =
                {MachineStatus._ID, MachineStatus.MACHINE_TYPE, MachineStatus.NUMBER, MachineStatus.STATUS, MachineStatus.TIME_REMAINING};

        final int notif_idx_id = notifications.getColumnIndexOrThrow(PendingNotificationMachine._ID);
        final int notif_idx_room_id = notifications.getColumnIndexOrThrow(PendingNotificationMachine.ROOM_ID);
        final int notif_idx_type = notifications.getColumnIndexOrThrow(PendingNotificationMachine.MACHINE_TYPE);
        final int notif_idx_number = notifications.getColumnIndexOrThrow(PendingNotificationMachine.NUMBER);
        final int notif_idx_status = notifications.getColumnIndexOrThrow(PendingNotificationMachine.DESIRED_STATUS);

        final LongSparseArray<Cursor> rooms = new LongSparseArray<Cursor>();

        long nextCheckMillis = Long.MAX_VALUE;
        List<Long> finishedNotifications = new ArrayList<Long>();

        while (notifications.moveToNext()) {
            long roomId = notifications.getLong(notif_idx_room_id);
            Cursor machines = rooms.get(roomId);
            if (machines == null) {
                final AsyncTask<Long,Void,Void> refresher = roomRefresherProvider.get().execute(roomId);
                try {
                    refresher.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                machines = contentResolver.query(MachineStatus.fromRoomId(roomId), machineProjection, null, null, null);
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
                        long currentMachineTime = machines.getLong(4);
                        if (currentMachineTime <= 0)
                            currentMachineTime = DEFAULT_WAIT_FOR_CYCLE_COMPLETE;
                        else
                            currentMachineTime += WIGGLE_CHECK_TIME;
                        nextCheckMillis = Math.min(nextCheckMillis, currentMachineTime);
                    }
                    break;
                }
            }
        }

        // Clean up cursors
        notifications.close();
        for (int i = 0; i < rooms.size(); i++) {
            rooms.valueAt(i).close();
        }

        for (Long notificationId : finishedNotifications) {
            contentResolver.delete(PendingNotification.fromId(notificationId), null, null);
        }

        if (finishedNotifications.size() > 0) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            final String title = String.format("%d machines ready", finishedNotifications.size());
            builder.setAutoCancel(true).setDefaults(Notification.DEFAULT_ALL)
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setTicker(title)
                    .setUsesChronometer(true)
                    .setContentIntent(
                            PendingIntent.getActivity(this, 1,
                                    new Intent(this, RoomViewer.class), 0));
            final Notification notification = builder.build();
            notificationManager.notify(0, notification);
        }

        if (nextCheckMillis != Long.MAX_VALUE) {
            am.cancel(pendingIntent);
            if(BuildConfig.DEBUG) {
                Log.v(TAG, "Pending notification check retry in " + SECONDS.convert(nextCheckMillis, MILLISECONDS) + " seconds");
            }
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + nextCheckMillis, pendingIntent);
        }

        PendingNotifCheckNeededBroadcastRec.completeWakefulIntent(intent);
    }
}
