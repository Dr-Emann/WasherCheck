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

public class MachineGroupingBuilder {
    static final String LOCATION_PROVIDER = "static";
    private final Location location = new Location(LOCATION_PROVIDER);
    private long id;
    private String name;
    private MachineGrouping.Type type;
    private Integer color;

    public MachineGroupingBuilder setColor(Integer color) {
        this.color = color;
        return this;
    }

    public MachineGroupingBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public MachineGroupingBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public MachineGroupingBuilder setType(MachineGrouping.Type type) {
        this.type = type;
        return this;
    }

    public MachineGroupingBuilder setLatitude(double latitude) {
        this.location.setLatitude(latitude);
        return this;
    }

    public MachineGroupingBuilder setLongitude(double longitude) {
        this.location.setLongitude(longitude);
        return this;
    }

    public MachineGroupingBuilder setLocation(Location location) {
        this.location.set(location);
        return this;
    }

    public MachineGrouping build() {
        if (name == null)
            throw new IllegalStateException("Name is required to be set");
        return new MachineGrouping(id, name, type, location, color);
    }
}
