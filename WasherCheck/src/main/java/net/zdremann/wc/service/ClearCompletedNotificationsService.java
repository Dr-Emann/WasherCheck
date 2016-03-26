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

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import net.zdremann.wc.WcApplication;
import net.zdremann.wc.provider.WasherCheckDatabase;

import javax.inject.Inject;

public class ClearCompletedNotificationsService extends IntentService {
    private static final String NAME = "ClearCompNotifService";

    @Inject
    WasherCheckDatabase mDbOpener;

    public ClearCompletedNotificationsService() {
        super(NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        WcApplication.getComponent().inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SQLiteDatabase db = null;
        try {
            db = mDbOpener.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(NAME, "Unable to open database", e);
        }
        try {
            if (db != null)
                db.delete(
                      WasherCheckDatabase.CompletedMachineNotificationTable.TABLE_NAME,
                      null, null
                );
        } finally {
            if (db != null)
                db.close();
        }
    }
}
