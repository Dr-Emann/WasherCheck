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
import android.util.JsonReader;

import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import net.zdremann.wc.model.Machine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class GaeMachineGetter extends InternetMachineGetter {

    @Inject
    public GaeMachineGetter(Tracker gaTracker, ConnectivityManager connectivityManager) {
        super(gaTracker, connectivityManager);
    }

    @Override
    public List<Machine> getMachines(long roomId) throws IOException {
        super.getMachines(roomId);

        List<Machine> result = new ArrayList<Machine>();
        final URL url = new URL("http://net-zdremann-wc.appspot.com/status/" + roomId);
        long timeStart = System.currentTimeMillis();
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        JsonReader reader = new JsonReader(
              new InputStreamReader(connection.getInputStream())
        );
        reader.beginArray();
        while(reader.hasNext()) {
            reader.beginObject();
            long esuds_id = Machine.NO_ESUDS_ID;
            long timeRemaining = Machine.NO_TIME_REMAINING;
            Machine.Status status = Machine.Status.UNKNOWN;
            int number = -1;
            Machine.Type type = Machine.Type.UNKNOWN;
            while(reader.hasNext()) {
                String nextName = reader.nextName();
                if("esuds_id".equals(nextName)) {
                    esuds_id = reader.nextLong();
                }
                else if("number".equals(nextName)) {
                    number = reader.nextInt();
                }
                else if("status".equals(nextName)) {
                    String statusStr = reader.nextString();
                    if("Available".equalsIgnoreCase(statusStr))
                        status = Machine.Status.AVAILABLE;
                    else if("Cycle Complete".equalsIgnoreCase(statusStr))
                        status = Machine.Status.CYCLE_COMPLETE;
                    else if("In Use".equalsIgnoreCase(statusStr))
                        status = Machine.Status.IN_USE;
                    else if("Unavailable".equalsIgnoreCase(statusStr))
                        status = Machine.Status.UNAVAILABLE;
                    else
                        status = Machine.Status.UNAVAILABLE;
                }
                else if("type".equals(nextName)) {
                    String typeName = reader.nextString();
                    assert typeName != null;
                    if(typeName.toLowerCase().contains("washer"))
                        type = Machine.Type.WASHER;
                    else if(typeName.toLowerCase().contains("dryer"))
                        type = Machine.Type.DRYER;
                    else
                        type = Machine.Type.UNKNOWN;
                }
                else if("timeRemaining".equals(nextName)) {
                    timeRemaining = reader.nextLong();
                }
                else {
                    reader.skipValue();
                }
            }

            reader.endObject();

            Machine machine = new Machine(roomId, esuds_id, number, type);
            machine.status = status;
            machine.timeRemaining = timeRemaining;

            result.add(machine);
        }
        long timeEnd = System.currentTimeMillis();
        gaTracker.send(
              MapBuilder.createTiming("loading", timeEnd - timeStart, "room_loading", null).build()
        );

        return result;
    }
}
