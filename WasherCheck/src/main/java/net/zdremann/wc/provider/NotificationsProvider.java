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

package net.zdremann.wc.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import net.zdremann.wc.provider.NotificationsContract.Notifications;

import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.DATE;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.EXTENDED;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.NUMBER;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.ROOM_ID;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.STATUS;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns.TYPE;
import static net.zdremann.wc.provider.NotificationsContract.NotificationsColumns._ID;

public class NotificationsProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int NOTIFICATIONS = 1;
    private static final int NOTIFICATIONS_ID = 2;
    private static final int NOTIFIED_ROOMS = 3;

    static {
        URI_MATCHER.addURI(NotificationsContract.AUTHORITY, "notifications", NOTIFICATIONS);
        URI_MATCHER.addURI(NotificationsContract.AUTHORITY, "notifications/rooms", NOTIFIED_ROOMS);
        URI_MATCHER.addURI(NotificationsContract.AUTHORITY, "notifications/#", NOTIFICATIONS_ID);
    }

    private WashersDatabase mWashersDatabase;

    @Override
    public boolean onCreate() {
        mWashersDatabase = new WashersDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, String selection, String[] selectionArgs,
                        final String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(Notifications.TABLE_NAME);

        final int uriType = URI_MATCHER.match(uri);

        switch (uriType) {
            case NOTIFICATIONS_ID:
                builder.appendWhere(_ID + "=" + uri.getLastPathSegment());
                break;
            case NOTIFIED_ROOMS:
            case NOTIFICATIONS:
                // No Filter
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mWashersDatabase.getReadableDatabase();

        assert db != null;

        final Context ctx = getContext();
        if (ctx == null)
            throw new IllegalStateException("Query called before onCreate");

        Cursor cursor = builder.query(db, projection,
                selection, selectionArgs,
                ((uriType == NOTIFIED_ROOMS) ? ROOM_ID : null), null, sortOrder);

        assert cursor != null;

        cursor.setNotificationUri(ctx.getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(final Uri uri) {
        final int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case NOTIFICATIONS:
            case NOTIFIED_ROOMS:
                return Notifications.CONTENT_TYPE;
            case NOTIFICATIONS_ID:
                return Notifications.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final int uriType = URI_MATCHER.match(uri);
        long id;

        if (uriType != NOTIFICATIONS)
            throw new UnsupportedOperationException("Cannot insert into " + uri);

        SQLiteDatabase db = mWashersDatabase.getWritableDatabase();
        assert db != null;

        id = db.insert(Notifications.TABLE_NAME, null, values);

        return Uri.withAppendedPath(Notifications.CONTENT_URI, String.valueOf(id));
    }

    @Override
    public int delete(final Uri uri, String selection, String[] selectionArgs) {
        final int uriType = URI_MATCHER.match(uri);
        int rowsAffected;
        switch (uriType) {
            case NOTIFICATIONS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection))
                    selection = _ID + "=" + id;
                else
                    selection = selection + " and " + _ID + "=" + id;
                break;
            case NOTIFICATIONS:
                // Nothing to do
                break;
            default:
                throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }

        final SQLiteDatabase db = mWashersDatabase.getWritableDatabase();
        assert db != null;

        final Context ctx = getContext();
        if (ctx == null)
            throw new IllegalStateException("delete called before onCreate");

        rowsAffected = db.delete(Notifications.TABLE_NAME, selection, selectionArgs);
        ctx.getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public int update(final Uri uri, final ContentValues values, String selection, String[] selectionArgs) {
        final int uriType = URI_MATCHER.match(uri);
        int rowsAffected;

        switch (uriType) {
            case NOTIFICATIONS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection))
                    selection = _ID + "=" + id;
                else
                    selection = selection + " and " + _ID + "=" + id;
                break;
            case NOTIFICATIONS:
                // Nothing to do
                break;
            default:
                throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }

        final SQLiteDatabase db = mWashersDatabase.getWritableDatabase();
        assert db != null;

        final Context ctx = getContext();
        if (ctx == null)
            throw new IllegalStateException("delete called before onCreate");

        rowsAffected = db.update(Notifications.TABLE_NAME, values, selection, selectionArgs);
        ctx.getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    private static class WashersDatabase extends SQLiteOpenHelper {
        public static final String IDX_NAME = "idx_room_num";
        private static final String DB_NAME = "washers.db";
        private static final int DB_VERSION = 1;
        private static final String SQL_CREATE =
                "CREATE TABLE " + Notifications.TABLE_NAME + "(" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        DATE + " INTEGER NOT NULL, " +
                        EXTENDED + " INTEGER NOT NULL DEFAULT " + Notifications.EXTENDED_VALUE_NORMAL + ", " +
                        ROOM_ID + " INTEGER NOT NULL, " +
                        NUMBER + " INTEGER, " +
                        TYPE + " INTEGER NOT NULL, " +
                        STATUS + " INTEGER NOT NULL )";
        private static final String SQL_CREATE_INDEX =
                "CREATE UNIQUE INDEX " + IDX_NAME + " ON " + Notifications.TABLE_NAME +
                        "(" + ROOM_ID + ", " + NUMBER + ")";

        public WashersDatabase(final Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
            db.execSQL(SQL_CREATE_INDEX);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP INDEX IF EXISTS " + IDX_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + Notifications.TABLE_NAME);
                onCreate(db);
            }
        }
    }
}
