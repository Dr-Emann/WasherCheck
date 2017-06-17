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

import android.os.SystemClock;

import net.zdremann.wc.model.Machine.Type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Room extends AbstractCollection<Machine> {
    protected final List<Machine> washers = new ArrayList<Machine>();
    protected final List<Machine> dryers = new ArrayList<Machine>();
    protected final List<Machine> others = new ArrayList<Machine>();
    protected final Map<Machine.Type, List<Machine>> machineMap;
    public final long id;

    public long getTimeLoaded() {
        return timeLoaded;
    }

    public void setTimeLoaded(final long timeLoaded) {
        this.timeLoaded = timeLoaded;
    }

    /**
     * Measured by SystemClock.elapsedRealtime()
     */
    private long timeLoaded;

    public Room(final long id) {
        this.id = id;
        timeLoaded = SystemClock.elapsedRealtime();

        EnumMap<Type, List<Machine>> tmpMap = new EnumMap<Type, List<Machine>>(Type.class);
        tmpMap.put(Type.WASHER, washers);
        tmpMap.put(Type.DRYER, dryers);
        tmpMap.put(Type.UNKNOWN, others);
        machineMap = tmpMap;
    }

    @Nullable
    public Machine getMachine(@NotNull final Machine.Type type, final int number) {
        final List<Machine> typedMachines = get(type);

        if (typedMachines.isEmpty()) {
            return null;
        }

        for (final Machine machine : typedMachines) {
            if (machine.getNum() == number)
                return machine;
        }

        return null;
    }

    @NotNull
    public List<Machine> get(@Nullable final Machine.Type type) {
        if (type == null)
            return Collections.emptyList();
        else
            return machineMap.get(type);
    }

    @NotNull
    public List<Machine> getWashers() {
        return washers;
    }

    @NotNull
    public List<Machine> getDryers() {
        return dryers;
    }

    @NotNull
    public List<Machine> getOthers() {
        return others;
    }

    @NotNull
    public Set<Machine.Type> getTypes() {
        // TODO: Cache types, set a dirty flag on insert/remove
        Type[] allTypes = Type.values();
        EnumSet<Machine.Type> result = EnumSet.noneOf(Machine.Type.class);

        for (Type type : allTypes) {
            if (has(type))
                result.add(type);
        }

        return result;
    }

    public boolean has(final Machine.Type type) {
        return get(type).size() > 0;
    }

    public boolean hasWashers() {
        return washers.size() > 0;
    }

    public boolean hasDryers() {
        return dryers.size() > 0;
    }

    public boolean hasOthers() {
        return others.size() > 0;
    }

    public synchronized void sort() {
        for (List<Machine> l : machineMap.values()) {
            Collections.sort(l);
        }
    }

    @Override
    public boolean add(@Nullable final Machine machine) {
        if (machine == null)
            return false;

        List<Machine> machines = get(machine.getType());
        return machines.add(machine);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Machine> from) {
        boolean allComplete = true;
        for (Machine m : from) {
            allComplete = add(m) && allComplete;
        }
        return allComplete;
    }

    @Override
    public boolean isEmpty() {
        for (Type type : getTypes()) {
            if (!get(type).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public Iterator<Machine> iterator() {
        return new RoomMachineIterator();
    }

    @Override
    public int size() {
        int size = 0;
        for (Machine.Type type : getTypes()) {
            size += get(type).size();
        }
        return size;
    }

    private class RoomMachineIterator implements Iterator<Machine> {
        int enumIndex = 0;
        Iterator<Machine> innerIterator = get(Machine.Type.fromInt(enumIndex)).iterator();

        public boolean hasNext() {
            while (!innerIterator.hasNext() && enumIndex + 1 < Machine.Type.values().length) {
                innerIterator = get(Machine.Type.values()[++enumIndex]).iterator();
            }
            return innerIterator.hasNext();
        }

        public Machine next() {
            return innerIterator.next();
        }

        public void remove() {
            innerIterator.remove();
        }
    }

    @Override
    public String toString() {
        return "Room{" +
              "id=" + id +
              ", timeLoaded=" + new Date(timeLoaded).toString() +
              '}';
    }
}
