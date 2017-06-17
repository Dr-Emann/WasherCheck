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

package net.zdremann.wc.model

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable

import air.air.net.zdremann.zsuds.R

data class Machine @JvmOverloads constructor(
        val roomId: Long,
        val esudsId: Long,
        val num: Int,
        val type: Type,
        var status: Status = Machine.Status.UNKNOWN,
        var timeRemaining: Long = NO_TIME_REMAINING): Parcelable, Comparable<Machine> {

    private constructor(parcel: Parcel): this(
            roomId = parcel.readLong(),
            esudsId = parcel.readLong(),
            num = parcel.readInt(),
            type = Type.fromInt(parcel.readInt()),
            status = Status.fromInt(parcel.readInt()),
            timeRemaining = parcel.readLong())

    fun hasTimeRemaining(): Boolean {
        return timeRemaining != NO_TIME_REMAINING
    }

    override fun compareTo(other: Machine): Int {
        if (this.roomId != other.roomId)
            return if (this.roomId < other.roomId) -1 else 1
        if (this.type != other.type) {
            return this.type.compareTo(other.type)
        }
        if (this.status != other.status) {
            return this.status.compareTo(other.status)
        }
        val timeComp = if (this.timeRemaining < other.timeRemaining)
            -1
        else if (this.timeRemaining > other.timeRemaining) 1 else 0
        if (timeComp != 0) {
            return timeComp
        }

        return if (this.num < other.num) -1 else if (this.num > other.num) 1 else 0
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(destination: Parcel, flags: Int) {
        destination.writeLong(roomId)
        destination.writeLong(esudsId)
        destination.writeInt(num)
        destination.writeInt(type.ordinal)
        destination.writeInt(status.ordinal)
        destination.writeLong(timeRemaining)
    }

    enum class Type constructor(val stringResource: Int, val pluralResource: Int) {
        WASHER(R.string.machine_type_washer_plural, R.plurals.machine_type_washer),
        DRYER(R.string.machine_type_dryer_plural, R.plurals.machine_type_dryer),
        UNKNOWN(R.string.machine_type_unknown, R.plurals.machine_type_unknown);

        fun toString(context: Context): String {
            return context.getString(this.stringResource)
        }

        fun toString(context: Context, number: Int): String {
            return toString(context.resources, number)
        }

        fun toString(resources: Resources, number: Int): String {
            return resources.getQuantityString(pluralResource, number)
        }

        companion object {
            @JvmStatic
            fun fromInt(i: Int): Type {
                val values = values()
                if (i < 0 || i > values.size) {
                    return UNKNOWN
                }

                return values[i]
            }
        }
    }

    enum class Status constructor(val stringResource: Int, val colorResource: Int) {
        AVAILABLE(R.string.machine_status_available, R.color.text_machine_available),
        CYCLE_COMPLETE(R.string.machine_status_cycle_complete, R.color.text_machine_cyclecomplete),
        IN_USE(R.string.machine_status_in_use, R.color.text_machine_inuse),
        UNAVAILABLE(R.string.machine_status_unavailable, R.color.text_machine_unavailable),
        UNKNOWN(R.string.machine_status_unknown, R.color.text_machine_unknown);

        fun getColor(context: Context): ColorStateList {
            val color = context.resources.getColorStateList(colorResource)!!
            return color
        }

        fun toString(context: Context): String {
            return context.getString(stringResource)
        }

        companion object {
            @JvmStatic
            fun fromInt(i: Int): Status {
                val values = values()
                if (i < 0 || i > values.size) {
                    return UNKNOWN
                }

                return values[i]
            }
        }
    }

    companion object {
        @JvmField
        val NO_TIME_REMAINING: Long = -1
        @JvmField
        val NO_ESUDS_ID: Long = -1
        @JvmField
        val CREATOR: Parcelable.Creator<Machine> = object : Parcelable.Creator<Machine> {
            override fun createFromParcel(source: Parcel): Machine {
                return Machine(source)
            }

            override fun newArray(size: Int): Array<Machine?> {
                return arrayOfNulls(size)
            }
        }
    }
}
