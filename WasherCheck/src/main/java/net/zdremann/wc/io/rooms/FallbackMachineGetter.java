/*
 * Copyright (c) 2014. Zachary Dremann
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
import java.util.List;

public class FallbackMachineGetter implements MachineGetter {
    private final List<? extends MachineGetter> getters;

    public FallbackMachineGetter(List<? extends MachineGetter> getters) {
        this.getters = getters;
    }

    @Override
    public List<Machine> getMachines(long roomId) throws IOException {
        IOException lastException = null;
        for (MachineGetter getter : getters) {
            try {
                return getter.getMachines(roomId);
            }
            catch (IOException e) {
                // Continue to next getter
                lastException = e;
            }
            catch (Exception ignore) {
                // Ignore non-IO exceptions
            }
        }
        if(lastException != null)
            throw lastException;
        else
            throw new IOException();
    }
}
