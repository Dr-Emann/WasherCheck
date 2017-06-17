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

import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.zdremann.wc.model.Machine;

import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.BuildConfig;

public class EsudsMachineGetter extends InternetMachineGetter {
    private static final String TAG = "EsudsMachineGetter";
    private static final long ONE_MINUTE = 60000;
    @Inject
    EsudsRoomHtmlDownloader mDownloader;
    @Inject
    EsudsRoomHtmlParser mParser;

    @Inject
    public EsudsMachineGetter(Tracker gaTracker, ConnectivityManager connectivityManager) {
        super(gaTracker, connectivityManager);
    }

    @Override
    public List<Machine> getMachines(long roomId) throws IOException {
        super.getMachines(roomId);

        List<Machine> machines;
        Reader reader = null;
        try {
            reader = mDownloader.getReader(roomId);
            long timeStart = System.currentTimeMillis();

            machines = mParser.readMachines(roomId, reader);

            long parseTime = System.currentTimeMillis() - timeStart;

            gaTracker.send(
                  new HitBuilders.TimingBuilder()
                        .setCategory("loading").setValue(parseTime)
                        .setVariable("room_loading").setLabel("parsing").build()
            );
            return machines;
        } catch (IOException e) {
            gaTracker.send(
                  new HitBuilders.ExceptionBuilder().setFatal(false).setDescription(
                        "downloading: " + e.getMessage()
                  ).build()
            );
            throw e;
        } catch (XmlPullParserException e) {
            gaTracker.send(
                  new HitBuilders.ExceptionBuilder().setFatal(false).setDescription(
                        "Wrong format when downloading room: " + roomId
                  ).build()
            );
            Log.d(TAG, "Wrong format when downloading for room " + roomId);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                throw new IOException(e);
            else
                throw new IOException();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static class EsudsRoomHtmlDownloader {
        private static final String TAG = "EsudsRoomHtmlDownloader";
        private static final String BASE_URL = "http://esuds.net/RoomStatus/machineStatus.i";
        private static final String ROOM_NUM_ATTR_NAME = "bottomLocationId";
        private static final Pattern HTML_REMOVE_PATTERN;

        @Inject
        Tracker gaTracker;

        static {
            final String[] HTML_REMOVALS = new String[]{
                  "xmlns=\"[^\"]*\"", "<!DOCTYPE[^>]*>",
                  "<script[^>]*>(?>.*?</script>)", "&nbsp;"
            };
            StringBuilder regexp = new StringBuilder(HTML_REMOVALS[0]);
            for (int i = 1; i < HTML_REMOVALS.length; i++) {
                regexp.append('|').append(HTML_REMOVALS[i]);
            }
            HTML_REMOVE_PATTERN = Pattern
                  .compile(regexp.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }

        @Inject
        public EsudsRoomHtmlDownloader() {

        }

        public Reader getReader(long roomId) throws IOException {
            // TODO: Filter stream live
            final long timeStart = SystemClock.elapsedRealtime();
            final char[] buffer = new char[4 * 1024];
            final StringBuilder builder = new StringBuilder();
            final URL url = roomUrl(roomId);
            Reader reader = null;
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(30000);
                connection.setUseCaches(false);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException(connection.getResponseMessage());
                }
                InputStream is = connection.getInputStream();
                reader = new InputStreamReader(is);
                int sizeRead;
                while ((sizeRead = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, sizeRead);
                }
            } finally {
                if (reader != null)
                    reader.close();
            }

            String html = builder.toString();
            final Matcher m = HTML_REMOVE_PATTERN.matcher(html);
            html = m.replaceAll("");

            long timeSpent = SystemClock.elapsedRealtime() - timeStart;
            gaTracker.send(
                  new HitBuilders.TimingBuilder()
                        .setCategory("loading").setValue(timeSpent)
                        .setVariable("room_loading").setLabel("download").build()
            );

            if (BuildConfig.DEBUG)
                Log.i(
                      TAG,
                      "Html Loading took " + (SystemClock.elapsedRealtime() - timeStart) + " millis"
                );

            return new StringReader(html);
        }

        @NotNull
        private static URL roomUrl(long roomId) {
            URL url;
            try {
                url = new URL(BASE_URL + "?" + ROOM_NUM_ATTR_NAME + "=" + String.valueOf(roomId));
            } catch (MalformedURLException mue) {
                throw new IllegalStateException();
            }
            return url;
        }
    }

    public static class EsudsRoomHtmlParser {
        private static final String TAG = "EsudsRoomHtmlParser";

        @Inject
        public EsudsRoomHtmlParser() {

        }

        @NotNull
        public List<Machine> readMachines(
              long roomId, Reader reader) throws XmlPullParserException, IOException {
            long startTime = SystemClock.elapsedRealtime();
            List<Machine> result = new ArrayList<Machine>();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(reader);

            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "tr".equals(parser.getName())) {
                    String classAttr = parser.getAttributeValue(null, "class");
                    if ("even".equals(classAttr) || "odd".equals(classAttr))
                        result.add(readMachine(roomId, parser));
                }
            }

            if (BuildConfig.DEBUG)
                Log.i(
                      TAG, "Parsing took " + (SystemClock.elapsedRealtime() - startTime) + " millis"
                );

            return result;
        }

        @NotNull
        protected Machine readMachine(
              final long roomId,
              @NotNull
              XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG, "", "tr");

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, "", "td");
            int machineId = readId(parser);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, "", "td");
            int machineNum = readNum(parser);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, "", "td");
            Machine.Type machineType = readType(parser);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, "", "td");
            Machine.Status machineStatus = readStatus(parser);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, "", "td");
            long machineTimeRemaining = readTimeRemaining(parser);

            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, "", "tr");
            return new Machine(roomId, machineId, machineNum, machineType, machineStatus, machineTimeRemaining);
        }

        protected int readId(
              @NotNull XmlPullParser parser) throws XmlPullParserException, IOException {
            int eventType = parser.next();
            int value = -1;
            while (eventType != XmlPullParser.END_TAG || !"td".equals(parser.getName())) {
                if (eventType == XmlPullParser.START_TAG && "input".equals(parser.getName())) {
                    try {
                        value = Integer.parseInt(parser.getAttributeValue(null, "value"));
                    } catch (NumberFormatException nfe) {
                        value = -1;
                    }
                }
                eventType = parser.next();
            }

            return value;
        }

        protected int readNum(
              @NotNull XmlPullParser parser) throws XmlPullParserException, IOException {
            int num;
            try {
                num = Integer.parseInt(parser.nextText());
            } catch (NumberFormatException nfe) {
                num = -1;
            }

            return num;
        }

        @NotNull
        protected Machine.Status readStatus(
              @NotNull XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, null, "font");

            final String text = parser.nextText();

            parser.nextTag();

            parser.require(XmlPullParser.END_TAG, null, "td");
            if ("available".equalsIgnoreCase(text))
                return Machine.Status.AVAILABLE;
            else if ("cycle complete".equalsIgnoreCase(text))
                return Machine.Status.CYCLE_COMPLETE;
            else if ("in use".equalsIgnoreCase(text))
                return Machine.Status.IN_USE;
            else if ("unavailable".equalsIgnoreCase(text))
                return Machine.Status.UNAVAILABLE;
            else
                return Machine.Status.UNKNOWN;
        }

        protected long readTimeRemaining(
              @NotNull XmlPullParser parser) throws XmlPullParserException, IOException {
            final String text = parser.nextText();
            long time;
            if (TextUtils.isEmpty(text)) {
                time = Machine.NO_TIME_REMAINING;
            } else {
                try {
                    time = (long) (Double.parseDouble(text) * ONE_MINUTE);
                } catch (NumberFormatException nfe) {
                    Log.d(TAG, "Unknown Time Remaining: " + text);
                    time = Machine.NO_TIME_REMAINING;
                }
            }

            return time;
        }

        @NotNull
        protected Machine.Type readType(
              @NotNull XmlPullParser parser) throws XmlPullParserException, IOException {
            String text = parser.nextText();
            if (text.contains("Washer") || text.contains("washer"))
                return Machine.Type.WASHER;
            else if (text.contains("Dryer") || text.contains("dryer"))
                return Machine.Type.DRYER;
            else
                return Machine.Type.UNKNOWN;
        }
    }
}
