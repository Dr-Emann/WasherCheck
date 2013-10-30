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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class MachinesLoadedBroadcastReceiver extends WakefulBroadcastReceiver {
    public static final String BROADCAST_TAG = "net.zdremann.wc.MACHINES_LOADED";
    public static final String EXTRA_ROOM_IDS = "net.zdremann.wc.roomIds";
    public static final String EXTRA_SUCCESSFUL_LOAD = "net.zdremann.wc.successful";

    public static Intent createBroadcastIntent(boolean successful, long... roomIds) {
        final Intent intent = new Intent(BROADCAST_TAG);
        intent.putExtra(EXTRA_ROOM_IDS, roomIds);
        intent.putExtra(EXTRA_SUCCESSFUL_LOAD, successful);
        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = PendingNotificationCheckService.createServiceIntent(
              context, intent.getLongArrayExtra(EXTRA_ROOM_IDS)
        );
        startWakefulService(context, serviceIntent);
    }
}
