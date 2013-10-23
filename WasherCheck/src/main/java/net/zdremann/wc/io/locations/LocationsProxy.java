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

package net.zdremann.wc.io.locations;

import android.location.Location;
import android.support.v4.util.LongSparseArray;

import net.zdremann.wc.model.MachineGrouping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

public class LocationsProxy {
    @NotNull
    private MachineGrouping mRoot;
    private final LongSparseArray<MachineGrouping> mLocations = new LongSparseArray<MachineGrouping>();
    private final NavigableMap<Location, MachineGrouping> mLocationMap = new TreeMap<Location, MachineGrouping>(
          new Comparator<Location>() {
              @Override
              public int compare(final Location lhs, final Location rhs) {
                  double dist1 = lhs.getLatitude() + lhs.getLongitude();
                  double dist2 = rhs.getLatitude() + rhs.getLongitude();

                  return Double.compare(dist1, dist2);
              }
          }
    );

    @Inject
    public LocationsProxy(@NotNull @Root MachineGrouping root) {
        mRoot = root;
        initCaches();
    }

    @NotNull
    public MachineGrouping getRoot() {
        return mRoot;
    }

    @Nullable
    public MachineGrouping getClosestLocation(@NotNull Location target) {
        @Nullable
        final Map.Entry<Location, MachineGrouping> above;
        @Nullable
        final Map.Entry<Location, MachineGrouping> below;

        above = mLocationMap.ceilingEntry(target);
        below = mLocationMap.floorEntry(target);

        @Nullable
        final Map.Entry<Location, MachineGrouping> closest;

        if (above == null)
            closest = below;
        else if (below == null)
            closest = above;
        else if (target.distanceTo(above.getKey()) < target.distanceTo(below.getKey()))
            closest = above;
        else
            closest = below;

        if (closest == null)
            return null;

        return closest.getValue();
    }

    @Nullable
    public MachineGrouping parentOf(long groupingId) {
        MachineGrouping grouping = getGrouping(groupingId);
        return (grouping != null) ? parentOf(grouping) : null;
    }

    @Nullable
    public MachineGrouping getGrouping(long groupingId) {
        return mLocations.get(groupingId);
    }

    @Nullable
    public MachineGrouping parentOf(@NotNull MachineGrouping grouping) {
        return grouping.parent;
    }

    private void initCaches() {
        cacheTree(mRoot);
    }

    private void cacheTree(@NotNull MachineGrouping node) {
        mLocations.put(node.id, node);
        mLocationMap.put(node.location, node);

        for (MachineGrouping child : node.children) {
            cacheTree(child);
        }
    }
}
