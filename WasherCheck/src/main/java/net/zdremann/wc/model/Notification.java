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

package net.zdremann.wc.model;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static net.zdremann.wc.provider.NotificationsContract.Notifications.EXTENDED_VALUE_EXTENDED;
import static net.zdremann.wc.provider.NotificationsContract.Notifications.EXTENDED_VALUE_INDEFINITE;
import static net.zdremann.wc.provider.NotificationsContract.Notifications.EXTENDED_VALUE_NORMAL;

public class Notification {
    @NotNull
    public final Date creationDate;
    public final Long roomId;
    public final int machineNum;
    @NotNull
    public final Machine.Type machineType;
    @MagicConstant(intValues = {
            EXTENDED_VALUE_NORMAL,
            EXTENDED_VALUE_EXTENDED,
            EXTENDED_VALUE_INDEFINITE
    })
    public final int extended;
    @NotNull
    public final Machine.Status desiredStatus;
    public long id;

    public Notification(long id, @NotNull Date creationDate,
                        @MagicConstant(intValues = {
                                EXTENDED_VALUE_NORMAL,
                                EXTENDED_VALUE_EXTENDED,
                                EXTENDED_VALUE_INDEFINITE
                        }) int extended, long roomId, int machineNum,
                        @NotNull Machine.Type machineType, @NotNull Machine.Status desiredStatus) {
        this.creationDate = creationDate;
        this.roomId = roomId;
        this.machineNum = machineNum;
        this.machineType = machineType;
        this.desiredStatus = desiredStatus;
        this.id = id;
        this.extended = extended;
    }

    public Notification(@NotNull Date creationDate,
                        @MagicConstant(intValues = {
                                EXTENDED_VALUE_NORMAL,
                                EXTENDED_VALUE_EXTENDED,
                                EXTENDED_VALUE_INDEFINITE
                        }) int extended, long roomId, int machineNum,
                        @NotNull Machine.Type machineType, @NotNull Machine.Status desiredStatus) {
        this(-1, creationDate, extended, roomId, machineNum, machineType, desiredStatus);
    }

    /**
     * A machine "matches" a notification if they describe the same physical machine.
     * This ignores the status of the machine.
     *
     * @param machine The machine against which to test
     * @return true only if this notification is intended for supplied machine
     * @see #fulfilledBy(Machine)
     */
    public boolean matchedBy(@NotNull Machine machine) {
        return roomId == machine.roomId &&
                machineNum == machine.num &&
                machineType == machine.type;
    }

    /**
     * A machine "fulfills" a notification if they describe the same physical machine, and
     * the status is the same or better than the {@link #desiredStatus}
     *
     * @param machine The machine against which to test
     * @return true only if this notification is fulfilled by the supplied machine
     * @see #matchedBy(Machine)
     */
    public boolean fulfilledBy(@NotNull Machine machine) {
        return matchedBy(machine) && desiredStatus.compareTo(machine.status) <= 0;
    }

    @Override
    public int hashCode() {
        int result = 17;
        long dateLong = creationDate.getTime();

        result = result * 31 + (int) (dateLong ^ (dateLong >>> 32));
        result = result * 31 + (int) (roomId ^ (roomId >>> 32));
        result = result * 31 + machineNum;
        result = result * 31 + machineType.hashCode();
        result = result * 31 + desiredStatus.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Notification))
            return false;

        Notification other = (Notification) o;
        return creationDate.equals(other.creationDate) &&
                roomId.equals(other.roomId) &&
                machineNum == other.machineNum &&
                machineType == other.machineType &&
                desiredStatus == other.desiredStatus;
    }
}
