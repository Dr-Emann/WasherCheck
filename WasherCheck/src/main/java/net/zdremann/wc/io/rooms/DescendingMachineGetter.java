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

import net.zdremann.wc.model.Machine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

public class DescendingMachineGetter implements MachineGetter {
    private static final int MACHINES_RETURNED = 8 * 3;
    private static final AtomicInteger ITERATION = new AtomicInteger(0);

    @Inject
    public DescendingMachineGetter() {
    }

    @Override
    public List<Machine> getMachines(long roomId) throws IOException {
        final int iteration = ITERATION.getAndIncrement();
        final ArrayList<Machine> machines = new ArrayList<Machine>();

        for (int i = 0; i < MACHINES_RETURNED / 3; ++i) {
            final Machine machine = new Machine(roomId, -1, i, Machine.Type.WASHER);
            machine.status = Machine.Status.fromInt(4 - iteration % 5);
            if (machine.status.compareTo(Machine.Status.IN_USE) == 0) {
                machine.minutesRemaining = 1;
            }
            machines.add(machine);
        }
        for (int i = MACHINES_RETURNED / 3; i < 2 * MACHINES_RETURNED / 3; ++i) {
            final Machine machine = new Machine(roomId, -1, i, Machine.Type.DRYER);
            machine.status = Machine.Status.fromInt(4 - iteration % 5);
            if (machine.status.compareTo(Machine.Status.IN_USE) == 0) {
                machine.minutesRemaining = 1;
            }
            machines.add(machine);
        }
        for (int i = 2 * MACHINES_RETURNED / 3; i < MACHINES_RETURNED; ++i) {
            final Machine machine = new Machine(roomId, -1, i, Machine.Type.UNKNOWN);
            machine.status = Machine.Status.fromInt(4 - iteration % 5);
            if (machine.status.compareTo(Machine.Status.IN_USE) == 0) {
                machine.minutesRemaining = 1;
            }
            machines.add(machine);
        }

        return machines;
    }
}
