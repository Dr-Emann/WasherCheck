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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import net.zdremann.wc.Main;
import net.zdremann.wc.io.rooms.MachineGetter;
import net.zdremann.wc.model.Machine;
import net.zdremann.wc.provider.WasherCheckContract;
import net.zdremann.wc.ui.RoomViewer;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;

public class RoomRefresher extends InjectingIntentService {
    private static final String TAG = "RoomRefresherService";
    private static final String ARG_ROOM_IDS = "net.dremann.wc.room_ids";

    @Inject
    MachineGetter mMachineGetter;
    @Inject
    ContentResolver mResolver;
    @Inject
    @Main
    Lazy<SharedPreferences> mLazyPreferences;

    public static Intent createIntent(Context ctx, long... roomIds) {
        Intent intent = new Intent(ctx, RoomRefresher.class);
        intent.putExtra(ARG_ROOM_IDS, roomIds);
        return intent;
    }

    public RoomRefresher() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long[] roomIds = intent.getLongArrayExtra(ARG_ROOM_IDS);
        if (roomIds == null || roomIds.length == 0) {
            Log.d(TAG, "Called without roomIds. This may be a mistake");
            roomIds = new long[]{mLazyPreferences.get().getLong(RoomViewer.ARG_ROOM_ID, 0)};
        }

        boolean successful = true;

        for (long roomId : roomIds) {
            try {
                final List<Machine> machines = mMachineGetter.getMachines(roomId);
                long time = System.currentTimeMillis();

                ContentValues[] values = new ContentValues[machines.size()];
                int i = 0;

                for (Machine machine : machines) {
                    ContentValues cv = new ContentValues();
                    cv.put(WasherCheckContract.MachineStatus.ESUDS_ID, machine.esudsId);
                    cv.put(WasherCheckContract.MachineStatus.NUMBER, machine.num);
                    cv.put(WasherCheckContract.MachineStatus.MACHINE_TYPE, machine.type.ordinal());
                    cv.put(WasherCheckContract.MachineStatus.ROOM_ID, machine.roomId);
                    cv.put(WasherCheckContract.MachineStatus.STATUS, machine.status.ordinal());
                    cv.put(
                          WasherCheckContract.MachineStatus.REPORTED_TIME_REMAINING,
                          machine.timeRemaining
                    );
                    cv.put(WasherCheckContract.MachineStatus.LAST_UPDATED, time);
                    values[i++] = cv;
                }

                mResolver.delete(WasherCheckContract.Machine.fromRoomId(roomId), null, null);

                mResolver.bulkInsert(WasherCheckContract.MachineStatus.CONTENT_URI, values);

                mResolver.notifyChange(WasherCheckContract.MachineStatus.fromRoomId(roomId), null);
            } catch (IOException e) {
                successful = false;
            }
        }

        final Intent broadcastIntent =
              MachinesLoadedBroadcastReceiver.createBroadcastIntent(successful, roomIds);
        this.sendBroadcast(broadcastIntent);

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}
