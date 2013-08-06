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
import java.util.Random;

import javax.inject.Inject;

import static java.util.concurrent.TimeUnit.*;

public class RandomMachineGetter implements MachineGetter {
    private static final int CAPACITY = 32;

    @Inject
    public RandomMachineGetter() {
    }

    @Override
    public List<Machine> getMachines(long roomId) throws IOException {
        ArrayList<Machine> machines = new ArrayList<Machine>(CAPACITY);
        final Random random = new Random();
        for (int i = 0; i < CAPACITY / 3; i++) {
            final Machine machine = new Machine(roomId, 0, i, Machine.Type.WASHER);
            machine.status = Machine.Status.fromInt(random.nextInt(4));
            if (machine.status.compareTo(Machine.Status.CYCLE_COMPLETE) > 0)
                machine.timeRemaining = MILLISECONDS.convert(random.nextInt(30), MINUTES);
            machines.add(machine);
        }
        for (int i = CAPACITY / 3; i < CAPACITY * 2 / 3; i++) {
            final Machine machine = new Machine(roomId, 0, i, Machine.Type.DRYER);
            machine.status = Machine.Status.fromInt(random.nextInt(4));
            if (machine.status.compareTo(Machine.Status.CYCLE_COMPLETE) > 0)
                machine.timeRemaining = MILLISECONDS.convert(random.nextInt(60), MINUTES);
            machines.add(machine);
        }
        for (int i = CAPACITY * 2 / 3; i < CAPACITY; i++) {
            final Machine machine = new Machine(roomId, 0, i, Machine.Type.WASHER);
            machine.status = Machine.Status.fromInt(random.nextInt(4));
            if (machine.status.compareTo(Machine.Status.CYCLE_COMPLETE) > 0)
                machine.timeRemaining = MILLISECONDS.convert(random.nextInt(1000), MINUTES);
            machines.add(machine);
        }

        return machines;
    }
}
