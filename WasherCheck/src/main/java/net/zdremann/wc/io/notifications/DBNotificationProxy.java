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

package net.zdremann.wc.io.notifications;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import net.zdremann.wc.model.Machine;
import net.zdremann.wc.model.Notification;
import net.zdremann.wc.provider.NotificationsContract.Notifications;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.DATE;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.EXTENDED;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.NUMBER;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.ROOM_ID;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.STATUS;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.TYPE;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns._ID;

/**
 * Created by DremannZ on 6/25/13.
 */
public class DBNotificationProxy implements NotificationsProxy {


    private final ContentResolver mResolver;

    @Inject
    public DBNotificationProxy(ContentResolver resolver) {

        mResolver = resolver;
    }

    @NotNull
    @Override
    public List<Notification> getNotifications() {
        String[] projection = {_ID, DATE, EXTENDED, ROOM_ID, NUMBER, TYPE, STATUS};
        final Cursor cursor = mResolver.query(Notifications.CONTENT_URI, projection, null, null, null);

        assert cursor != null;

        List<Notification> notifications = new ArrayList<Notification>(cursor.getCount());

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            Date date = new Date(cursor.getLong(1));
            int extended = cursor.getInt(2);
            long roomId = cursor.getLong(3);
            int machineNum = cursor.getInt(4);
            Machine.Type type = Machine.Type.fromInt(cursor.getInt(5));
            Machine.Status status = Machine.Status.fromInt(cursor.getInt(6));

            //noinspection MagicConstant
            notifications.add(new Notification(id, date, extended, roomId, machineNum, type, status));
        }

        return notifications;
    }

    @Nullable
    @Override
    public Notification getNotification(long id) {
        String[] projection = {DATE, EXTENDED, ROOM_ID, NUMBER, TYPE, STATUS};
        final Cursor cursor = mResolver.query(Notifications.CONTENT_URI, projection, null, null, null);

        assert cursor != null;

        if (!cursor.moveToNext())
            return null;

        Date date = new Date(cursor.getLong(0));
        int extended = cursor.getInt(1);
        long roomId = cursor.getLong(2);
        int machineNum = cursor.getInt(3);
        Machine.Type type = Machine.Type.fromInt(cursor.getInt(4));
        Machine.Status status = Machine.Status.fromInt(cursor.getInt(5));

        //noinspection MagicConstant
        return new Notification(id, date, extended, roomId, machineNum, type, status);
    }

    @Nullable
    @Override
    public Notification insertNotification(Notification toInsert) {
        ContentValues cv = new ContentValues(6);

        cv.put(DATE, toInsert.creationDate.getTime());
        cv.put(EXTENDED, toInsert.extended);
        cv.put(ROOM_ID, toInsert.roomId);
        cv.put(NUMBER, toInsert.machineNum);
        cv.put(TYPE, toInsert.machineType.ordinal());
        cv.put(STATUS, toInsert.desiredStatus.ordinal());

        final Uri uri = mResolver.insert(Notifications.CONTENT_URI, cv);

        if (uri == null)
            return null;

        toInsert.id = Long.parseLong(uri.getLastPathSegment());
        return toInsert;
    }

    @Override
    public int delete(long id) {
        final Uri uri = Notifications.fromId(id);

        return mResolver.delete(uri, null, null);
    }
}
