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

package net.zdremann.wc.io.rooms;

import android.database.AbstractCursor;

import net.zdremann.wc.model.Machine;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.zdremann.wc.provider.MachinesContract.Machines.MACHINE_ID;
import static net.zdremann.wc.provider.MachinesContract.Machines.NUMBER;
import static net.zdremann.wc.provider.MachinesContract.Machines.ROOM_ID;
import static net.zdremann.wc.provider.MachinesContract.Machines.STATUS;
import static net.zdremann.wc.provider.MachinesContract.Machines.TIME_REMAINING;
import static net.zdremann.wc.provider.MachinesContract.Machines.TYPE;
import static net.zdremann.wc.provider.MachinesContract.Machines._ID;

public class MachineListCursor extends AbstractCursor {

    private final List<Machine> mMachineList;
    private final String[] mProjection;

    private int index = -1;

    private final int idx_id;
    private final int idx_room_id;
    private final int idx_machine_id;
    private final int idx_number;
    private final int idx_type;
    private final int idx_status;
    private final int idx_time_remaining;

    public MachineListCursor(List<Machine> machines, String[] projection) {
        mMachineList = machines;
        mProjection = projection;

        idx_id = getColumnIndex(_ID);
        idx_room_id = getColumnIndex(ROOM_ID);
        idx_machine_id = getColumnIndex(MACHINE_ID);
        idx_number = getColumnIndex(NUMBER);
        idx_type = getColumnIndex(TYPE);
        idx_status = getColumnIndex(STATUS);
        idx_time_remaining = getColumnIndex(TIME_REMAINING);
    }

    public Machine getMachine() {
        return mMachineList.get(index);
    }

    @Override
    public int getCount() {
        return mMachineList.size();
    }

    @Override
    public String[] getColumnNames() {
        return mProjection;
    }

    @Nullable
    @Override
    public String getString(int i) {
        if (i == idx_machine_id || i == idx_number ||
                i == idx_room_id || i == idx_status ||
                i == idx_type || i == idx_id)
            return String.valueOf(getLong(i));
        else if (i == idx_time_remaining)
            return String.valueOf(getFloat(i));
        else
            return null;
    }

    @Override
    public short getShort(int i) {
        return (short) getLong(i);
    }

    @Override
    public int getInt(int i) {
        if (i == idx_machine_id)
            return (int) getMachine().id;
        else if (i == idx_number)
            return getMachine().num;
        else if (i == idx_room_id)
            return (int) getMachine().roomId;
        else if (i == idx_status)
            return getMachine().status.ordinal();
        else if (i == idx_type)
            return getMachine().type.ordinal();
        else if (i == idx_id)
            return (int) getMachine().staticId();
        else
            return -1;
    }

    @Override
    public long getLong(int i) {
        if (i == idx_machine_id)
            return getMachine().id;
        else if (i == idx_number)
            return getMachine().num;
        else if (i == idx_room_id)
            return getMachine().roomId;
        else if (i == idx_status)
            return getMachine().status.ordinal();
        else if (i == idx_type)
            return getMachine().type.ordinal();
        else if (i == idx_id)
            return getMachine().staticId();
        else
            return -1L;
    }

    @Override
    public float getFloat(int i) {
        if (i == idx_time_remaining)
            return getMachine().minutesRemaining;
        else
            return Float.NaN;
    }

    @Override
    public double getDouble(int i) {
        return getFloat(i);
    }

    @Override
    public int getType(int c) {
        if (c == idx_time_remaining)
            return FIELD_TYPE_FLOAT;
        else
            return FIELD_TYPE_INTEGER;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        index = newPosition;
        return super.onMove(oldPosition, newPosition);
    }
}
