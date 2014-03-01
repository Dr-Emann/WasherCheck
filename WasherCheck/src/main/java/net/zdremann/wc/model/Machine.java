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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

import air.air.net.zdremann.zsuds.R;

public class Machine implements Comparable<Machine>, Parcelable {
    public static final long NO_TIME_REMAINING = -1;
    public static final long NO_ESUDS_ID = -1;
    public static final Creator<Machine> CREATOR = new Creator<Machine>() {

        @NotNull
        public Machine createFromParcel(@NotNull Parcel source) {
            return new Machine(source);
        }

        @NotNull
        public Machine[] newArray(int size) {
            return new Machine[size];
        }
    };

    public final int num;
    public final long roomId;
    @NotNull
    public final Type type;
    public final long esudsId;
    @NotNull
    public Status status = Status.UNKNOWN;
    public long timeRemaining = NO_TIME_REMAINING;

    public Machine(final long roomId, final long esudsId, int num, @NotNull final Type type) {
        this.esudsId = esudsId;
        this.num = num;
        this.type = type;
        this.roomId = roomId;
    }

    private Machine(Parcel in) {
        this.roomId = in.readLong();
        this.esudsId = in.readLong();
        this.num = in.readInt();
        this.type = Type.fromInt(in.readInt());
        this.status = Status.fromInt(in.readInt());
        this.timeRemaining = in.readLong();
    }

    public boolean hasTimeRemaining() {
        return timeRemaining != NO_TIME_REMAINING;
    }

    public int compareTo(Machine another) {
        if (this.roomId != another.roomId)
            return this.roomId < another.roomId ? -1 : 1;
        if (!this.type.equals(another.type)) {
            return this.type.compareTo(another.type);
        }
        if (!this.status.equals(another.status)) {
            return this.status.compareTo(another.status);
        }
        int timeComp = (this.timeRemaining < another.timeRemaining) ?
                       -1 :
                       (this.timeRemaining > another.timeRemaining) ? 1 : 0;
        if (timeComp != 0) {
            return timeComp;
        }

        return this.num < another.num ? -1 : this.num > another.num ? 1 : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Machine))
            return false;

        Machine other = (Machine) o;
        return this.roomId == other.roomId &&
              this.esudsId == other.esudsId &&
              this.num == other.num &&
              this.type.equals(other.type) &&
              this.status.equals(other.status) &&
              this.timeRemaining == other.timeRemaining;
    }

    @Override
    public int hashCode() {
        int result = 12;
        result = 31 * result + (int) (roomId ^ (roomId >>> 32));
        result = 31 * result + (int) (esudsId ^ (esudsId >>> 32));
        result = 31 * result + num;
        result = 31 * result + type.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + (int) (timeRemaining ^ (timeRemaining >>> 32));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Machine{")
              .append("roomId=").append(roomId)
              .append(", id=").append(esudsId)
              .append(", num=").append(num)
              .append(", type=").append(type.toString())
              .append(", status=").append(status.toString());

        if (hasTimeRemaining())
            builder.append(", timeRemaining=").append(timeRemaining);

        builder.append('}');
        return builder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel destination, int flags) {
        destination.writeLong(roomId);
        destination.writeLong(esudsId);
        destination.writeInt(num);
        destination.writeInt(type.ordinal());
        destination.writeInt(status.ordinal());
        destination.writeLong(timeRemaining);
    }

    public long staticId() {
        return ((this.roomId & 0xFFFFFFFF00000000L) ^ (this.roomId << 32))
              | (this.type.ordinal() << 30) | this.num;
    }

    public static enum Type {
        WASHER(R.string.machine_type_washer_plural, R.plurals.machine_type_washer),
        DRYER(R.string.machine_type_dryer_plural, R.plurals.machine_type_dryer),
        UNKNOWN(R.string.machine_type_unknown, R.plurals.machine_type_unknown);
        private final int stringResource;
        private final int pluralResource;

        private Type(int stringResource, int pluralResource) {
            this.stringResource = stringResource;
            this.pluralResource = pluralResource;
        }

        public int stringResource() {
            return this.stringResource;
        }

        public int pluralResource() {
            return this.pluralResource;
        }

        @NotNull
        public String toString(@NotNull final Context context) {
            return context.getString(this.stringResource);
        }

        @NotNull
        public String toString(@NotNull final Context context, final int number) {
            return toString(context.getResources(), number);
        }

        @NotNull
        public String toString(@NotNull final Resources resources, final int number) {
            return resources.getQuantityString(pluralResource, number);
        }

        @NotNull
        public static Type fromInt(int i) {
            final Type[] values = values();
            if (i < 0 || i > values.length) {
                return UNKNOWN;
            }

            return values[i]; // TODO: Cache values, to prevent array copy
        }
    }

    public static enum Status {
        AVAILABLE(R.string.machine_status_available, R.color.text_machine_available),
        CYCLE_COMPLETE(R.string.machine_status_cycle_complete, R.color.text_machine_cyclecomplete),
        IN_USE(R.string.machine_status_in_use, R.color.text_machine_inuse),
        UNAVAILABLE(R.string.machine_status_unavailable, R.color.text_machine_unavailable),
        UNKNOWN(R.string.machine_status_unknown, R.color.text_machine_unknown);
        private final int stringResource;
        private final int colorResource;

        private Status(int stringResource, int colorResource) {
            this.stringResource = stringResource;
            this.colorResource = colorResource;
        }

        public int colorResource() {
            return this.colorResource;
        }

        public int stringResource() {
            return this.stringResource;
        }

        @NotNull
        public ColorStateList getColor(@NotNull final Context context) {
            ColorStateList color = context.getResources().getColorStateList(colorResource);
            assert color != null;
            return color;
        }

        @NotNull
        public String toString(@NotNull final Context context) {
            return context.getString(stringResource);
        }

        @NotNull
        public static Status fromInt(int i) {
            final Status[] values = values();
            if (i < 0 || i > values.length) {
                return UNKNOWN;
            }

            return values[i]; // TODO: Cache values, to prevent array copy
        }
    }
}
