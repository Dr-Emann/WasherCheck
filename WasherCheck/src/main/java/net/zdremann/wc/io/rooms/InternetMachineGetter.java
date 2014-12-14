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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.analytics.Tracker;

import net.zdremann.wc.model.Machine;

import java.io.IOException;
import java.util.List;

abstract class InternetMachineGetter implements MachineGetter {
    protected final Tracker gaTracker;
    private final ConnectivityManager connectivityManager;

    InternetMachineGetter(Tracker gaTracker, ConnectivityManager connectivityManager) {
        this.gaTracker = gaTracker;
        this.connectivityManager = connectivityManager;
    }

    @Override
    public List<Machine> getMachines(long roomId) throws IOException {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if(activeNetwork == null || !activeNetwork.isConnected()) {
            throw new IOException("Not connected to the internet");
        }
        return null;
    }
}
