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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import net.zdremann.wc.model.Machine;
import net.zdremann.wc.provider.WasherCheckDatabase;
import net.zdremann.wc.ui.RoomViewer;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.R;

public class GcmBroadcastService extends InjectingIntentService {
    private static final String NAME = "GcmBroadcastService";
    private static final int NOTIFICATION_ID = 1;

    @Inject
    WasherCheckDatabase mDbOpener;

    @Inject
    NotificationManager mNotificationManager;

    public GcmBroadcastService() {
        super(NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            int machineNumber;
            int machineType;
            int machineStatus;
            long roomId;
            long date = System.currentTimeMillis();
            try {
                machineNumber = Integer.parseInt(intent.getStringExtra("machine-number"));
            } catch (NumberFormatException e) {
                machineNumber = -1;
            }
            try {
                machineType = Integer.parseInt(intent.getStringExtra("machine-type"));
            } catch (NumberFormatException e) {
                machineType = Machine.Type.UNKNOWN.ordinal();
            }
            try {
                roomId = Long.parseLong(intent.getStringExtra("machine-type"));
            } catch (NumberFormatException e) {
                roomId = -1;
            }
            try {
                machineStatus = Integer.parseInt(intent.getStringExtra("machine-status"));
            } catch (NumberFormatException e) {
                machineStatus = -1;
            }
            SQLiteDatabase db = null;
            int count = 1;
            String title;
            String text;
            String tickerText;
            try {
                db = mDbOpener.getWritableDatabase();
            } catch (SQLiteException e) {
                Log.e(NAME, "Cannot open database");
                count = 1;
            }
            if (db != null) {
                ContentValues cv = new ContentValues();
                cv.put("machine_number", machineNumber);
                cv.put("machine_type", machineType);
                cv.put("machine_status", machineStatus);
                cv.put("room_id", roomId);
                cv.put("date", date);
                try {
                    db.insertOrThrow(
                          WasherCheckDatabase.CompletedMachineNotificationTable.TABLE_NAME, null, cv
                    );

                    Cursor c = db.query(
                          WasherCheckDatabase.CompletedMachineNotificationTable.TABLE_NAME,
                          new String[]{"count(*)"},
                          null, null,
                          null, null, null
                    );
                    c.moveToFirst();
                    count = c.getInt(0);
                    c.close();
                } catch (SQLException e) {
                    Log.e(NAME, "Unable to write to database", e);
                    count = 1;
                }
                finally {
                    db.close();
                }
            }

            if (count == 1) {
                text = singleMachineText(this, machineNumber, machineType, machineStatus);
            } else {
                text = getString(R.string.notify_text_multiple_machines, count);
            }

            tickerText = "Machine(s) ready";
            title = "Machine(s) ready";

            Intent activityIntent = new Intent(this, RoomViewer.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Intent cancelIntent = new Intent(this, ClearCompletedNotificationsService.class);
            PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
            PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
            Notification notification = new NotificationCompat.Builder(this)
                  .setTicker(tickerText)
                  .setContentTitle(title)
                  .setContentText(text)
                  .setSmallIcon(R.drawable.ic_launcher)
                  .setDefaults(Notification.DEFAULT_ALL)
                  .setWhen(System.currentTimeMillis())
                  .setContentIntent(activityPendingIntent)
                  .setDeleteIntent(cancelPendingIntent)
                  .setAutoCancel(true)
                  .build();
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        } finally {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private static String singleMachineText(
          Context context, int machineNumber, int machineType, int machineStatus) {
        Resources resources = context.getResources();
        return resources.getString(
              R.string.notify_text_single_machine, Machine.Type.fromInt(machineType).toString(
                    context, 1
              ), machineNumber,
              Machine.Status.fromInt(machineStatus).toString(context)
        );
    }
}
