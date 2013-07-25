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

/**
 * Created by DremannZ on 7/23/13.
 */
@SuppressWarnings("UnusedDeclaration")
public class MachinesContract {
    public static final String AUTHORITY = "net.zdremann.machines";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    static final String CACHE_PARAMETER_AGE = "cache_age";

    public static class Machines implements BaseColumns, MachinesColumns {
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.net.zdremann.wc.provider.machines";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.net.zdremann.wc.provider.machines";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "machines");
        public static final String DEFAULT_SORT = TYPE + " ASC, " + STATUS + " ASC, " + TIME_REMAINING + " ASC";

        public static final String[] ALL_COLUMNS =
                {_ID, ROOM_ID, MACHINE_ID, NUMBER, TYPE, STATUS, TIME_REMAINING};

        public static Uri buildRoomUri(long roomId, long cacheAge) {
            final Uri.Builder builder = CONTENT_URI.buildUpon().appendPath("rooms")
                    .appendPath(String.valueOf(roomId))
                    .appendQueryParameter(CACHE_PARAMETER_AGE, String.valueOf(cacheAge));
            return builder.build();
        }
    }

    public static interface MachinesColumns {
        public static final String ROOM_ID = "room_id";
        public static final String MACHINE_ID = "machine_id";
        public static final String NUMBER = "number";
        public static final String TYPE = "type";
        public static final String STATUS = "status";
        public static final String TIME_REMAINING = "time_remaining";
    }
}
