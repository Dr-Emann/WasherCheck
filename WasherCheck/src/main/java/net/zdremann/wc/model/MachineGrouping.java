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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MachineGrouping implements Parcelable, Comparable<MachineGrouping> {
    public static final Creator<MachineGrouping> CREATOR = new Creator<MachineGrouping>() {
        public MachineGrouping createFromParcel(Parcel in) {
            Location loc = new Location(LOCATION_PROVIDER);
            MachineGrouping value = new MachineGrouping(in.readLong(),
                    in.readString(),
                    Type.values()[in.readInt()], loc);
            loc.setLatitude(in.readDouble());
            loc.setLongitude(in.readDouble());

            in.readTypedList(value.children, CREATOR);
            for (MachineGrouping child : value.children) {
                child.parent = value;
            }
            return value;
        }

        public MachineGrouping[] newArray(int size) {
            return new MachineGrouping[size];
        }
    };
    private static final String LOCATION_PROVIDER = "static";

    public final long id;
    @NotNull
    public final String name;
    @NotNull
    public final Type type;
    @NotNull
    public final List<MachineGrouping> children = new ArrayList<MachineGrouping>();
    @NotNull
    public final Location location;
    @Nullable
    public MachineGrouping parent;

    public MachineGrouping(final long id, @NotNull final String name, @NotNull final Type type, final double latitude, final double longitude) {
        Location loc = new Location(LOCATION_PROVIDER);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);

        this.id = id;
        this.name = name;
        this.type = type;

        this.location = loc;
    }

    public MachineGrouping(final long id, @NotNull final String name, @NotNull final Type type, @NotNull final Location location) {
        this.id = id;
        this.name = name;
        this.type = type;

        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MachineGrouping))
            return false;

        MachineGrouping other = (MachineGrouping) o;
        return (this.type.equals(other.type))
                && (this.name.equals(other.name));
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + location.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "MachineGrouping{" +
                "id=" + id +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", location=" + location +
                '}';
    }

    @Override
    public int compareTo(@NotNull final MachineGrouping another) {
        return this.name.compareTo(another.name);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(@NotNull Parcel destination, int flags) {
        destination.writeLong(id);
        destination.writeString(name);
        destination.writeInt(type.ordinal());
        destination.writeDouble(location.getLatitude());
        destination.writeDouble(location.getLongitude());
        destination.writeTypedList(children);
    }

    public static enum Type {
        ROOT, SCHOOL, CAMPUS, HALL, ROOM
    }
}
