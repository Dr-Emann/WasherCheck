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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import net.zdremann.wc.ForApplication;
import net.zdremann.wc.io.locations.LocationsProxy;
import net.zdremann.wc.model.MachineGrouping;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public class WasherCheckDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "WasherCheckDatabase.db";
    private static final int DB_VERSION = 3;
    private final LocationsProxy mLocations;

    @Inject
    public WasherCheckDatabase(@ForApplication Context context, LocationsProxy locations) {
        super(context, DB_NAME, null, DB_VERSION);
        mLocations = locations;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        MachineTable.onCreate(db);
        PendingNotificationTable.onCreate(db);
        StatusUpdateTable.onCreate(db);
        MachineStatusView.onCreate(db);
        MachineGroupTable.onCreate(db, mLocations);
        PendingNotificationMachineView.onCreate(db);
        PendingNotificationMachineStatusView.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MachineTable.onUpgrade(db, oldVersion, newVersion);
        PendingNotificationTable.onUpgrade(db, oldVersion, newVersion);
        StatusUpdateTable.onUpgrade(db, oldVersion, newVersion);
        MachineStatusView.onUpgrade(db, oldVersion, newVersion);
        MachineGroupTable.onUpgrade(db, mLocations, oldVersion, newVersion);
        PendingNotificationMachineView.onUpgrade(db, oldVersion, newVersion);
        PendingNotificationMachineStatusView.onUpgrade(db, oldVersion, newVersion);
    }

    static final class PendingNotificationTable
          implements WasherCheckContract.PendingNotificationColumns {

        public static final String TABLE_NAME = WasherCheckContract.PendingNotification.PATH;
        public static final String SQL_CREATE =
              "CREATE TABLE " + TABLE_NAME + " (\n" +
                    _ID + " INTEGER PRIMARY KEY,\n" +
                    MACHINE_ID + " INTEGER NOT NULL " +
                    "REFERENCES " + MachineTable.TABLE_NAME + "(" + MachineTable._ID + "),\n" +
                    NOTIF_CREATED + " INTEGER NOT NULL,\n" +
                    EXTENDED + " INTEGER NOT NULL DEFAULT " + 0 + " ,\n" +
                    DESIRED_STATUS + " INTEGER NOT NULL,\n" +
                    EST_TIME_OF_COMPLETION + " INTEGER NOT NULL DEFAULT " + Long.MAX_VALUE + " )";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
        }

        public static void onUpgrade(
              final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        }
    }

    static final class PendingNotificationMachineView
          implements WasherCheckContract.PendingNotificationMachineColumns {
        public static final String VIEW_NAME = WasherCheckContract.PendingNotificationMachine.PATH;
        public static final String SQL_CREATE =
              "CREATE VIEW " + VIEW_NAME + " AS\n" +
                    "SELECT " + PendingNotificationTable.TABLE_NAME + "." + _ID + ", " +
                    MACHINE_ID + ", " + NUMBER + ", " + MACHINE_TYPE + ", " + ROOM_ID + ", " +
                    ESUDS_ID + ", " + NOTIF_CREATED + ", " + EXTENDED + ", " + DESIRED_STATUS + ", " +
                    EST_TIME_OF_COMPLETION + "\n" +
                    "FROM " + MachineTable.TABLE_NAME + " INNER JOIN " + PendingNotificationTable.TABLE_NAME +
                    " ON " + MachineTable.TABLE_NAME + "." + _ID + " = " +
                    PendingNotificationTable.TABLE_NAME + "." + MACHINE_ID;

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
        }

        public static void onUpgrade(
              final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP VIEW IF EXISTS " + VIEW_NAME);
                onCreate(db);
            }
        }
    }

    static final class PendingNotificationMachineStatusView
          implements WasherCheckContract.PendingNotificationMachineStatusColumns {
        public static final String VIEW_NAME = WasherCheckContract.PendingNotificationMachineStatus.PATH;
        public static final String SQL_CREATE =
              "CREATE VIEW " + VIEW_NAME + " AS\n" +
                    "SELECT " + PendingNotificationTable.TABLE_NAME + "." + _ID + ", " +
                    PendingNotificationTable.TABLE_NAME + "." + MACHINE_ID + ", " + NUMBER + ", " + MACHINE_TYPE + ", " + ROOM_ID + ", " +
                    ESUDS_ID + ", " + NOTIF_CREATED + ", " + EXTENDED + ", " + DESIRED_STATUS + ", " +
                    STATUS + ", " + LAST_UPDATED + ", " + REPORTED_TIME_REMAINING + ", " +
                    EST_TIME_OF_COMPLETION + "\n" +
                    "FROM " +
                    MachineTable.TABLE_NAME + " INNER JOIN " + PendingNotificationTable.TABLE_NAME +
                    " ON " + MachineTable.TABLE_NAME + "." + _ID + " = " + PendingNotificationTable.TABLE_NAME + "." + MACHINE_ID +
                    " INNER JOIN " + StatusUpdateTable.TABLE_NAME +
                    " ON " + StatusUpdateTable.TABLE_NAME + "." + MACHINE_ID + " = " + MachineTable.TABLE_NAME + "." + _ID;

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
        }

        public static void onUpgrade(
              final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP VIEW IF EXISTS " + VIEW_NAME);
                onCreate(db);
            }
        }
    }

    static final class MachineTable
          implements WasherCheckContract.MachineColumns {
        public static final String TABLE_NAME = WasherCheckContract.Machine.PATH;
        public static final String SQL_CREATE =
              "CREATE TABLE " + TABLE_NAME + " (\n" +
                    _ID + " INTEGER PRIMARY KEY,\n" +
                    NUMBER + " INTEGER NOT NULL,\n" +
                    MACHINE_TYPE + " INTEGER NOT NULL,\n" +
                    ROOM_ID + " INTEGER NOT NULL,\n" +
                    ESUDS_ID + " INTEGER UNIQUE,\n" +
                    "UNIQUE (" + ROOM_ID + "," + NUMBER + "," + MACHINE_TYPE + ") " +
                    "ON CONFLICT IGNORE\n" +
                    ")";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
        }

        public static void onUpgrade(
              final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        }
    }

    static final class MachineGroupTable
          implements WasherCheckContract.MachineGroupColumns {
        public static final String TABLE_NAME = WasherCheckContract.MachineGroup.PATH;
        public static final String SQL_CREATE =
              "CREATE TABLE " + TABLE_NAME + " (\n" +
                    _ID + " INTEGER PRIMARY KEY,\n" +
                    GROUP_TYPE + " INTEGER,\n" +
                    NAME + " TEXT NOT NULL,\n" +
                    LATITUDE + " REAL,\n" +
                    LONGITUDE + " REAL,\n" +
                    PARENT + " INTEGER " +
                    "REFERENCES " + TABLE_NAME + "(" + _ID + "),\n" +
                    THEME + " INTEGER)";
        public static final String SQL_INSERT =
              "INSERT INTO " + TABLE_NAME + " VALUES (?,?,?,?,?,?,?)";

        public static void onCreate(SQLiteDatabase db, LocationsProxy locations) {
            db.execSQL(SQL_CREATE);
            db.beginTransaction();
            final SQLiteStatement insertStatement = db.compileStatement(SQL_INSERT);

            assert insertStatement != null;

            insertGrouping(locations.getRoot(), insertStatement);
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        private static void insertGrouping(
              @NotNull MachineGrouping grouping, @NotNull SQLiteStatement insertStatement) {
            insertStatement.bindLong(1, grouping.id);
            insertStatement.bindLong(2, grouping.type.ordinal());
            insertStatement.bindString(3, grouping.name);
            insertStatement.bindDouble(4, grouping.location.getLatitude());
            insertStatement.bindDouble(5, grouping.location.getLongitude());
            if (grouping.parent != null)
                insertStatement.bindLong(6, grouping.parent.id);
            if (grouping.getColor() != null)
                insertStatement.bindLong(7, grouping.getColor());
            insertStatement.executeInsert();
            insertStatement.clearBindings();
            for (MachineGrouping child : grouping.children) {
                insertGrouping(child, insertStatement);
            }
        }

        public static void onUpgrade(
              final SQLiteDatabase db, LocationsProxy locations, final int oldVersion,
              final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db, locations);
            }
        }
    }

    static final class StatusUpdateTable
          implements WasherCheckContract.StatusUpdateColumns {
        public static final String TABLE_NAME = WasherCheckContract.StatusUpdate.PATH;
        public static final String SQL_CREATE =
              "CREATE TABLE " + TABLE_NAME + " (\n" +
                    _ID + " INTEGER PRIMARY KEY,\n" +
                    MACHINE_ID + " INTEGER UNIQUE " +
                    "ON CONFLICT REPLACE " +
                    "REFERENCES " + MachineTable.TABLE_NAME + "(" + MachineTable._ID + "),\n" +
                    STATUS + " INTEGER NOT NULL,\n" +
                    REPORTED_TIME_REMAINING + " INTEGER,\n" +
                    LAST_UPDATED + " INTEGER NOT NULL )";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
        }

        public static void onUpgrade(
              final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        }
    }

    static final class MachineStatusView
          implements WasherCheckContract.MachineColumns, WasherCheckContract.StatusUpdateColumns {
        public static final String VIEW_NAME = WasherCheckContract.MachineStatus.PATH;
        public static final String SQL_CREATE =
              "CREATE VIEW " + VIEW_NAME + " AS\n" +
                    "SELECT " + MachineTable.TABLE_NAME + "." + _ID + ", " + MACHINE_ID + ", " + NUMBER + ", " +
                    MACHINE_TYPE + ", " + ROOM_ID + ", " + ESUDS_ID + ", " + STATUS + ", " +
                    REPORTED_TIME_REMAINING + ", " + LAST_UPDATED + "\n" +
                    "FROM " + MachineTable.TABLE_NAME + " INNER JOIN " + StatusUpdateTable.TABLE_NAME +
                    " ON " + MachineTable.TABLE_NAME + "." + _ID + " = " +
                    StatusUpdateTable.TABLE_NAME + "." + MACHINE_ID;

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE);
        }

        public static void onUpgrade(
              final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (oldVersion < DB_VERSION) {
                db.execSQL("DROP VIEW IF EXISTS " + VIEW_NAME);
                onCreate(db);
            }
        }
    }
}
