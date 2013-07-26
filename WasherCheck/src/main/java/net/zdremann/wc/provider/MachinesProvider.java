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

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import net.zdremann.wc.io.rooms.MachineGetter;
import net.zdremann.wc.io.rooms.MachineListCursor;
import net.zdremann.wc.io.rooms.RoomLoaderModule;
import net.zdremann.wc.model.Machine;
import net.zdremann.wc.provider.MachinesContract.Machines;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static net.zdremann.wc.provider.MachinesContract.AUTHORITY;
import static net.zdremann.wc.provider.MachinesContract.CACHE_PARAMETER_AGE;

public class MachinesProvider extends InjectingProvider {

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int MACHINES_ROOM_ID = 1;

    static {
        URI_MATCHER.addURI(AUTHORITY, "machines/rooms/#", MACHINES_ROOM_ID);
    }

    private final Object cacheLock = new Object();
    @Inject
    MachineGetter mRoomGetter;
    private List<Machine> cache;
    private long cachedRoom = -1;
    private long lastCacheUpdate = -1;

    @Override
    public boolean onCreate() {
        super.onCreate();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null)
            projection = Machines.ALL_COLUMNS;
        if (sortOrder == null)
            sortOrder = Machines.DEFAULT_SORT;

        int uriType = URI_MATCHER.match(uri);
        List<Machine> machines;
        long roomId;
        long cacheLength;
        switch (uriType) {
            case MACHINES_ROOM_ID:
                roomId = Long.parseLong(uri.getLastPathSegment());
                cacheLength = Long.parseLong(uri.getQueryParameter(CACHE_PARAMETER_AGE));
                synchronized (cacheLock) {
                    if (roomId != cachedRoom || System.currentTimeMillis() - cacheLength > lastCacheUpdate)
                        try {
                            machines = loadMachines(roomId);
                        } catch (IOException e) {
                            return null;
                        }
                    else
                        machines = cache;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }

        Collections.sort(machines);

        return new MachineListCursor(machines, projection);
    }

    private List<Machine> loadMachines(long roomId) throws IOException {
        List<Machine> machines = mRoomGetter.getMachines(roomId);
        cache = machines;
        lastCacheUpdate = System.currentTimeMillis();
        cachedRoom = roomId;
        return machines;
    }

    @Override
    public String getType(Uri uri) {
        int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case MACHINES_ROOM_ID:
                return Machines.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("Cannot insert: read only");
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException("Cannot delete: read only");
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new UnsupportedOperationException("Cannot update: read only");
    }

    @Override
    protected List<Object> getModules() {
        final ArrayList<Object> modules = new ArrayList<Object>(super.getModules());
        modules.add(new RoomLoaderModule());
        return modules;
    }
}
