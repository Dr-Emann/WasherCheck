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

package net.zdremann.wc;

import android.app.Application;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.BuildConfig;
import dagger.ObjectGraph;

public class MyApplication extends Application {
    private static ObjectGraph sObjectGraph;

    @Inject
    GoogleAnalytics googleAnalytics;

    @Inject
    public MyApplication() {
        super();
        sObjectGraph = ObjectGraph.create(new ApplicationModule(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sObjectGraph.inject(this);
        googleAnalytics.setDryRun(BuildConfig.DEBUG);
        googleAnalytics.getLogger().setLogLevel(
              BuildConfig.DEBUG ?
              Logger.LogLevel.VERBOSE :
              Logger.LogLevel.WARNING
        );
    }

    public static ObjectGraph getApplicationGraph() {
        return sObjectGraph;
    }
}
