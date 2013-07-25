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

import android.content.Context;

import net.zdremann.wc.ForApplication;
import net.zdremann.wc.R;
import net.zdremann.wc.model.MachineGrouping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by DremannZ on 6/20/13.
 */
public class ResourcesLocationsGetter implements LocationsGetter {
    private static final Map<String, MachineGrouping.Type> ALLOWED_TYPES;

    static {
        Map<String, MachineGrouping.Type> mutableAllowedTypes = new HashMap<String, MachineGrouping.Type>(5);
        mutableAllowedTypes.put("root", MachineGrouping.Type.ROOT);
        mutableAllowedTypes.put("school", MachineGrouping.Type.SCHOOL);
        mutableAllowedTypes.put("campus", MachineGrouping.Type.CAMPUS);
        mutableAllowedTypes.put("hall", MachineGrouping.Type.HALL);
        mutableAllowedTypes.put("room", MachineGrouping.Type.ROOM);
        ALLOWED_TYPES = Collections.unmodifiableMap(mutableAllowedTypes);
    }

    private final Context mContext;

    @Inject
    public ResourcesLocationsGetter(@ForApplication Context context) {
        mContext = context;
    }

    @NotNull
    @Override
    public MachineGrouping load() {
        XmlPullParser parser = mContext.getResources().getXml(R.xml.rooms);
        int eventType;
        assert parser != null;
        try {
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("root".equals(parser.getName())) {
                            return readGrouping(parser);
                        }
                }
            }
        } catch (XmlPullParserException e) {
            throw new IllegalStateException("XML Data is bad");
        } catch (IOException e) {
            throw new IllegalStateException("XML Data is bad");
        }
        throw new IllegalStateException("XML Data is bad");
    }

    @NotNull
    private MachineGrouping readGrouping(@NotNull XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, "", null);
        int id;
        try {
            id = Integer.parseInt(parser.getAttributeValue(null, "id"));
        } catch (NumberFormatException nfe) {
            throw new XmlPullParserException("Not a valid id:" + parser.getAttributeValue(null, "id") + " "
                    + parser.getPositionDescription());
        }
        String name = parser.getAttributeValue(null, "name");
        MachineGrouping.Type type = getType(parser.getName());
        double latitude;
        double longitude;
        String latStr = parser.getAttributeValue(null, "latitude");
        String lonStr = parser.getAttributeValue(null, "longitude");

        if (latStr == null || lonStr == null) {
            latitude = longitude = Double.NaN;
        } else {
            try {
                latitude = Double.parseDouble(latStr);
                longitude = Double.parseDouble(lonStr);
            } catch (NumberFormatException nfe) {
                latitude = longitude = Double.NaN;
            }

        }

        MachineGrouping grouping = new MachineGrouping(id, name, type, latitude, longitude);

        List<MachineGrouping> children = new ArrayList<MachineGrouping>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String childName = parser.getName();
            if (validType(childName))
                children.add(readGrouping(parser));
        }

        grouping.children.addAll(children);

        for (MachineGrouping child : children) {
            child.parent = grouping;
        }

        parser.require(XmlPullParser.END_TAG, "", null);
        return grouping;
    }

    protected boolean validType(@Nullable String from) {
        return ALLOWED_TYPES.containsKey(from);
    }

    @NotNull
    protected MachineGrouping.Type getType(@NotNull String from) {
        return ALLOWED_TYPES.get(from);
    }
}
