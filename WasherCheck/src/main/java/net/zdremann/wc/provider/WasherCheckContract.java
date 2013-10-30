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

import android.net.Uri;
import android.provider.BaseColumns;

public class WasherCheckContract {
    public static final String AUTHORITY = "net.zdremann.wc";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final class StatusUpdate implements StatusUpdateColumns {
        private StatusUpdate() {
        }

        public static final String PATH = "status_update";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.statusupdate";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.statusupdate";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {MACHINE_ID, STATUS, REPORTED_TIME_REMAINING, LAST_UPDATED};

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public interface StatusUpdateColumns extends BaseColumns, MachineReference {
        public static final String STATUS = "status";
        public static final String REPORTED_TIME_REMAINING = "time_remaining";
        public static final String LAST_UPDATED = "last_updated";
    }

    public static final class Machine implements MachineColumns {
        private Machine() {
        }

        public static final String PATH = "machine";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.machine";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.machine";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {_ID, NUMBER, MACHINE_TYPE, ROOM_ID, ESUDS_ID};

        public static Uri fromRoomId(long roomId) {
            return CONTENT_URI.buildUpon().appendPath("room").appendPath(String.valueOf(roomId))
                  .build();
        }

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public static interface MachineColumns extends BaseColumns, RoomReference {
        public static final String NUMBER = "number";
        public static final String MACHINE_TYPE = "machine_type";
        public static final String ESUDS_ID = "esuds_id";
    }

    public static final class MachineGroup implements MachineGroupColumns {
        private MachineGroup() {
        }

        public static final String PATH = "machine_group";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.machinegroup";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.machinegroup";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {_ID, GROUP_TYPE, NAME, LATITUDE, LONGITUDE, PARENT, THEME};

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public static interface MachineGroupColumns extends BaseColumns {
        public static final String GROUP_TYPE = "group_type";
        public static final String NAME = "name";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String PARENT = "parent";
        public static final String THEME = "theme";
    }

    public static final class PendingNotificationMachine
          implements PendingNotificationMachineColumns {
        private PendingNotificationMachine() {
        }

        public static final String PATH = "pending_notification_machine";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.notification_machine";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.notification_machine";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {
                    MACHINE_ID, NUMBER, MACHINE_TYPE, ROOM_ID, ESUDS_ID, NOTIF_CREATED, EXTENDED,
                    DESIRED_STATUS, EST_TIME_OF_COMPLETION
              };

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public static interface PendingNotificationMachineColumns
          extends PendingNotificationColumns, MachineColumns {

    }

    public static final class PendingNotificationRooms
          implements PendingNotificationRoomsColumns {
        private PendingNotificationRooms() {
        }

        public static final String PATH = "pending_notification_rooms";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.notification_machine_rooms";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.notification_machine_rooms";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {
                    ROOM_ID
              };
    }

    public static interface PendingNotificationRoomsColumns extends RoomReference {

    }

    public static final class PendingNotificationMachineStatus
          implements PendingNotificationMachineStatusColumns {
        private PendingNotificationMachineStatus() {
        }

        public static final String PATH = "pending_notification_machine_status";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.notification_machine_status";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.notification_machine_status";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {
                    MACHINE_ID, NUMBER, MACHINE_TYPE, ROOM_ID, ESUDS_ID,
                    NOTIF_CREATED, EXTENDED, DESIRED_STATUS, STATUS, LAST_UPDATED,
                    REPORTED_TIME_REMAINING, EST_TIME_OF_COMPLETION
              };

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public static interface PendingNotificationMachineStatusColumns
          extends PendingNotificationColumns, MachineColumns, StatusUpdateColumns {

    }

    public static final class PendingNotification implements PendingNotificationColumns {
        private PendingNotification() {
        }

        public static final String PATH = "pending_notification";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.notification";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.notification";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {_ID, MACHINE_ID, NOTIF_CREATED, EXTENDED, DESIRED_STATUS, EST_TIME_OF_COMPLETION};

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public static interface PendingNotificationColumns extends BaseColumns, MachineReference {
        /**
         * The date at which the notification was created
         * <p>TYPE: INTEGER</p>
         */
        public static final String NOTIF_CREATED = "notif_create";
        /**
         * The extended status of the notification.
         * <p/>
         * <p>TYPE: INTEGER</p>
         * <p>Values: {EXTENDED_VALUE_NORMAL, EXTENDED_VALUE_EXTENDED, EXTENDED_VALUE_INDEFINITE}</p>
         */
        public static final String EXTENDED = "extended";
        /**
         * The desired stat us for the machine to be at when the notification fires.
         *
         * @see net.zdremann.wc.model.Machine.Status
         * <p>TYPE: INTEGER</p>
         */
        public static final String DESIRED_STATUS = "desired_status";
        /**
         * The estimated number of milliseconds until the pending notification is fulfilled
         * <p>TYPE: INTEGER</p>
         */
        public static final String EST_TIME_OF_COMPLETION = "estimated_completion";
    }

    public static final class MachineStatus implements MachineStatusColumns {
        private MachineStatus() {
        }

        public static final String PATH = "machine_status";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.machine_status";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.machine_status";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
              {
                    _ID, MACHINE_ID, NUMBER, MACHINE_TYPE, ROOM_ID, ESUDS_ID, STATUS,
                    REPORTED_TIME_REMAINING, LAST_UPDATED
              };

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }

        public static Uri fromRoomId(long roomId) {
            return CONTENT_URI.buildUpon().appendPath("room").appendPath(String.valueOf(roomId))
                  .build();
        }
    }

    public static interface MachineStatusColumns extends MachineColumns, StatusUpdateColumns {

    }

    static interface RoomReference {
        public static final String ROOM_ID = "room_id";
    }

    static interface MachineReference {
        /**
         * The id for the machine.
         * <p>TYPE: INTEGER</p>
         */
        public static final String MACHINE_ID = "machine_id";
    }
}
