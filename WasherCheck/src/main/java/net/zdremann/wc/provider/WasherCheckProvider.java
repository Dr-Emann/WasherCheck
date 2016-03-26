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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.zdremann.wc.ApplicationComponent;
import net.zdremann.wc.ApplicationModule;
import net.zdremann.wc.DaggerApplicationComponent;
import net.zdremann.wc.WcApplication;

import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

import static net.zdremann.wc.provider.WasherCheckContract.*;
import static net.zdremann.wc.provider.WasherCheckDatabase.*;

public class WasherCheckProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int MACHINES = 10;
    private static final int MACHINES_BY_ROOM = 11;
    private static final int MACHINES_ID = 12;
    private static final int STATUS_UPDATE = 20;
    private static final int STATUS_UPDATE_ID = 21;
    private static final int MACHINE_GROUPING = 30;
    private static final int MACHINE_GROUPING_ID = 31;
    private static final int MACHINE_STATUS = 40;
    private static final int MACHINE_STATUS_BY_ROOM = 41;
    private static final int MACHINE_STATUS_ID = 42;

    static {
        URI_MATCHER.addURI(AUTHORITY, Machine.PATH, MACHINES);
        URI_MATCHER.addURI(AUTHORITY, Machine.PATH + "/room/#", MACHINES_BY_ROOM);
        URI_MATCHER.addURI(AUTHORITY, Machine.PATH + "/#", MACHINES_ID);

        URI_MATCHER.addURI(AUTHORITY, StatusUpdate.PATH, STATUS_UPDATE);
        URI_MATCHER.addURI(AUTHORITY, StatusUpdate.PATH + "/#", STATUS_UPDATE_ID);

        URI_MATCHER.addURI(AUTHORITY, MachineGroup.PATH, MACHINE_GROUPING);
        URI_MATCHER.addURI(AUTHORITY, MachineGroup.PATH + "/#", MACHINE_GROUPING_ID);

        URI_MATCHER.addURI(AUTHORITY, MachineStatus.PATH, MACHINE_STATUS);
        URI_MATCHER.addURI(AUTHORITY, MachineStatus.PATH + "/room/#", MACHINE_STATUS_BY_ROOM);
        URI_MATCHER.addURI(AUTHORITY, MachineStatus.PATH + "/#", MACHINE_STATUS_ID);
    }

    private ApplicationComponent mComponent;

    @Inject
    WasherCheckDatabase mDbOpener;
    @Inject
    ContentResolver mContentResolver;

    @Nullable
    private static String appendWhere(@Nullable String current, @Nullable String toAppend) {
        if (TextUtils.isEmpty(current))
            return toAppend;
        else if (TextUtils.isEmpty(toAppend))
            return current;
        else
            return current + " AND " + toAppend;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        assert context != null;
        mComponent = DaggerApplicationComponent.builder()
            .applicationModule(new ApplicationModule(context)).build();
        mComponent.inject(this);
        return false;
    }

    @Override
    public Cursor query(
            @NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        final int uriType = URI_MATCHER.match(uri);
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        String groupBy = null;
        builder.setTables(getTable(uriType));

        switch (uriType) {
        case MACHINES_ID:
            builder.appendWhere(MachineColumns._ID + "=" + uri.getLastPathSegment());
        case MACHINES:
            if (TextUtils.isEmpty(sortOrder))
                sortOrder = MachineColumns.ROOM_ID + "," + MachineColumns.MACHINE_TYPE + "," + MachineColumns.NUMBER;
            break;
        case MACHINES_BY_ROOM:
            builder.appendWhere(MachineColumns.ROOM_ID + "=" + uri.getLastPathSegment());
            if (TextUtils.isEmpty(sortOrder))
                sortOrder = MachineColumns.ROOM_ID + "," + MachineColumns.MACHINE_TYPE + "," + MachineColumns.NUMBER;
            break;
        case STATUS_UPDATE_ID:
            builder.appendWhere(StatusUpdateColumns._ID + "=" + uri.getLastPathSegment());
            break;
        case MACHINE_GROUPING_ID:
            builder.appendWhere(MachineGroupColumns._ID + "=" + uri.getLastPathSegment());
            break;
        case MACHINE_STATUS_ID:
            builder.appendWhere(MachineStatusColumns._ID + "=" + uri.getLastPathSegment());
            // Intentional lack of break
        case MACHINE_STATUS:
            if (TextUtils.isEmpty(sortOrder))
                sortOrder = MachineStatusColumns.ROOM_ID + "," + MachineStatusColumns.MACHINE_TYPE +
                      "," + MachineStatusColumns.REPORTED_TIME_REMAINING + "," + MachineStatusColumns.NUMBER;
            break;
        case MACHINE_STATUS_BY_ROOM:
            if (TextUtils.isEmpty(sortOrder))
                sortOrder = MachineStatusColumns.ROOM_ID + "," + MachineStatusColumns.MACHINE_TYPE +
                      ", " + MachineStatusColumns.STATUS + "," + MachineStatusColumns.REPORTED_TIME_REMAINING +
                      "," + MachineStatusColumns.NUMBER;
            builder.appendWhere(MachineStatusColumns.ROOM_ID + "=" + uri.getLastPathSegment());
            break;
        default:
            // No filter by default
        }

        final SQLiteDatabase db = mDbOpener.getReadableDatabase();

        assert db != null;

        Cursor cursor = builder
              .query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);

        assert cursor != null;

        cursor.setNotificationUri(mContentResolver, uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
        case MACHINES:
        case MACHINES_BY_ROOM:
            return Machine.CONTENT_TYPE;
        case MACHINES_ID:
            return Machine.CONTENT_ITEM_TYPE;
        case STATUS_UPDATE:
            return StatusUpdate.CONTENT_TYPE;
        case STATUS_UPDATE_ID:
            return StatusUpdate.CONTENT_ITEM_TYPE;
        case MACHINE_GROUPING:
            return MachineGroup.CONTENT_TYPE;
        case MACHINE_GROUPING_ID:
            return MachineGroup.CONTENT_ITEM_TYPE;
        case MACHINE_STATUS:
        case MACHINE_STATUS_BY_ROOM:
            return MachineStatus.CONTENT_TYPE;
        case MACHINE_STATUS_ID:
            return MachineStatus.CONTENT_ITEM_TYPE;
        default:
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int uriType = URI_MATCHER.match(uri);
        long id;

        String tableName = getTable(uriType);

        final SQLiteDatabase db = mDbOpener.getWritableDatabase();

        assert db != null;

        switch (uriType) {
        case MACHINES_ID:
        case STATUS_UPDATE_ID:
        case MACHINE_GROUPING:
        case MACHINE_GROUPING_ID:
        case MACHINE_STATUS_ID:
            throw new UnsupportedOperationException("Cannot insert into URI: " + uri);
        case MACHINE_STATUS_BY_ROOM:
            values.put(MachineStatusColumns.ROOM_ID, Long.parseLong(uri.getLastPathSegment()));
        case MACHINE_STATUS:
            db.beginTransaction();
            ContentValues cv = new ContentValues();
            String number = values.getAsString(MachineStatusColumns.NUMBER);
            String type = values.getAsString(MachineStatusColumns.MACHINE_TYPE);
            String roomId = values.getAsString(MachineStatusColumns.ROOM_ID);
            String esudsId = values.getAsString(MachineStatusColumns.ESUDS_ID);

            if ("-1".equals(esudsId))
                esudsId = null;

            cv.put(MachineColumns.NUMBER, number);
            cv.put(MachineColumns.ESUDS_ID, esudsId);
            cv.put(MachineColumns.ROOM_ID, roomId);
            cv.put(MachineColumns.MACHINE_TYPE, type);

            long machineId = db.insertWithOnConflict(
                  MachineTable.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE
            );
            if (machineId == -1) {
                final Cursor c = db.query(
                      MachineTable.TABLE_NAME, new String[]{MachineColumns._ID},
                      MachineColumns.ROOM_ID + "=?" + " AND " + MachineColumns.MACHINE_TYPE + "=?" +
                            " AND " + MachineColumns.NUMBER + "=?",
                      new String[]{roomId, type, number}, null, null, null, "1"
                );
                c.moveToFirst();
                machineId = c.getLong(0);
                c.close();
            }

            if (machineId == -1) {
                db.endTransaction();
                return null;
            }
            cv.clear();
            cv.put(StatusUpdateColumns.MACHINE_ID, machineId);
            cv.put(
                  StatusUpdateColumns.LAST_UPDATED,
                  values.getAsLong(MachineStatusColumns.LAST_UPDATED)
            );
            cv.put(StatusUpdateColumns.STATUS, values.getAsInteger(MachineStatusColumns.STATUS));
            cv.put(
                  StatusUpdateColumns.REPORTED_TIME_REMAINING,
                  values.getAsLong(MachineStatusColumns.REPORTED_TIME_REMAINING)
            );
            id = db.insertWithOnConflict(
                  StatusUpdateTable.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE
            );
            db.setTransactionSuccessful();
            db.endTransaction();
            return MachineStatus.fromId(id);
        default:
            id = db.insert(tableName, null, values);
            switch (uriType) {
            case MACHINES:
                return Machine.fromId(id);
            case STATUS_UPDATE:
                return StatusUpdate.fromId(id);
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
            }
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int uriType = URI_MATCHER.match(uri);

        String tableName = getTable(uriType);

        final SQLiteDatabase db = mDbOpener.getWritableDatabase();

        assert db != null;

        switch (uriType) {
        case MACHINE_GROUPING_ID:
        case MACHINE_GROUPING:
        case MACHINE_STATUS:
        case MACHINE_STATUS_BY_ROOM:
        case MACHINE_STATUS_ID:
            throw new UnsupportedOperationException("Cannot delete from URI: " + uri);
        case MACHINES_ID:
        case MACHINES_BY_ROOM:
            String column = uriType == MACHINES_ID ? MachineColumns._ID : MachineColumns.ROOM_ID;
            selection = appendWhere(selection, column + "=" + uri.getLastPathSegment());
        case MACHINES:
            break;

        case STATUS_UPDATE_ID:
            selection = appendWhere(
                  selection, StatusUpdateColumns._ID + "=" + uri.getLastPathSegment()
            );
        case STATUS_UPDATE:
            break;
        default:
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        return db.delete(tableName, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int uriType = URI_MATCHER.match(uri);

        String tableName = getTable(uriType);

        final SQLiteDatabase db = mDbOpener.getWritableDatabase();

        assert db != null;

        switch (uriType) {
        case MACHINE_GROUPING_ID:
        case MACHINE_GROUPING:
        case MACHINE_STATUS:
        case MACHINE_STATUS_BY_ROOM:
        case MACHINE_STATUS_ID:
            throw new UnsupportedOperationException("Cannot update URI: " + uri);

        case MACHINES_ID:
        case MACHINES_BY_ROOM:
            String column = uriType == MACHINES_ID ? MachineColumns._ID : MachineColumns.ROOM_ID;
            selection = appendWhere(selection, column + "=" + uri.getLastPathSegment());
        case MACHINES:
            break;

        case STATUS_UPDATE_ID:
            selection = appendWhere(
                  selection, StatusUpdateColumns._ID + "=" + uri.getLastPathSegment()
            );
        case STATUS_UPDATE:
            break;

        default:
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        return db.update(tableName, values, selection, selectionArgs);
    }

    private String getTable(int type) {
        switch (type) {
        case MACHINES:
        case MACHINES_BY_ROOM:
        case MACHINES_ID:
            return MachineTable.TABLE_NAME;
        case STATUS_UPDATE:
        case STATUS_UPDATE_ID:
            return StatusUpdateTable.TABLE_NAME;
        case MACHINE_STATUS:
        case MACHINE_STATUS_BY_ROOM:
        case MACHINE_STATUS_ID:
            return MachineStatusView.VIEW_NAME;
        case MACHINE_GROUPING:
        case MACHINE_GROUPING_ID:
            return MachineGroupTable.TABLE_NAME;
        default:
            throw new UnsupportedOperationException("Unknown URI");
        }
    }
}
