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

public final class NotificationsContract {
    public static final String AUTHORITY = "net.zdremann.wc.notifications";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    @SuppressWarnings("UnusedDeclaration")
    public static final class Notifications implements NotificationsColumns {
        private Notifications() {
        }

        public static final String PATH = "notifications";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.notifications";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.notifications";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        public static final String[] ALL_COLUMNS =
                {_ID, DATE, EXTENDED, ROOM_ID, NUMBER, TYPE, STATUS};

        public static final int EXTENDED_VALUE_NORMAL = 0;
        public static final int EXTENDED_VALUE_EXTENDED = 1;
        public static final int EXTENDED_VALUE_INDEFINITE = 2;

        public static final int TYPE_VALUE_WASHER = 0;
        public static final int TYPE_VALUE_DRYER = 1;
        public static final int TYPE_VALUE_OTHER = 2;

        public static final int STATUS_VALUE_AVAILABLE = 0;
        public static final int STATUS_VALUE_CYCLE_COMPLETE = 1;
        public static final int STATUS_VALUE_IN_USE = 2;
        public static final int STATUS_VALUE_UNAVAILABLE = 3;
        public static final int STATUS_VALUE_UNKNOWN = 4;

        public static Uri fromId(long id) {
            return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        }
    }

    public static interface NotificationsColumns extends BaseColumns {


        /**
         * The date at which the notification was created
         * <p>TYPE: INTEGER</p>
         */
        public static final String DATE = "date";
        /**
         * The extended status of the notification.
         * <p/>
         * <p>TYPE: INTEGER</p>
         * <p>Values: {EXTENDED_VALUE_NORMAL, EXTENDED_VALUE_EXTENDED, EXTENDED_VALUE_INDEFINITE}</p>
         */
        public static final String EXTENDED = "extended";
        /**
         * The room id for which the notification is registered
         * <p>TYPE: INTEGER</p>
         */
        public static final String ROOM_ID = "room_id";
        /**
         * The machine number for which the notification is registered
         * <p>TYPE: INTEGER</p>
         */
        public static final String NUMBER = "machine_num";
        /**
         * The machine type for which the notification is registered, washer, dryer or other
         *
         * @see net.zdremann.wc.model.Machine.Type
         * <p>TYPE: INTEGER</p>
         */
        public static final String TYPE = "machine_type";
        /**
         * The desired satus for the machine to be at when the notification fires.
         *
         * @see net.zdremann.wc.model.Machine.Status
         * <p>TYPE: INTEGER</p>
         */
        public static final String STATUS = "machine_status";
    }

}
