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

package net.zdremann.wc.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.google.analytics.tracking.android.EasyTracker;

import net.zdremann.wc.MyApplication;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import air.air.net.zdremann.zsuds.BuildConfig;
import dagger.ObjectGraph;

abstract class InjectingActivity extends ActionBarActivity {

    @Inject
    EasyTracker gaTracker;

    protected ObjectGraph activityGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityGraph = MyApplication.getApplicationGraph().plus(getModules().toArray());

        if (BuildConfig.DEBUG)
            activityGraph.validate();

        activityGraph.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        gaTracker.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        gaTracker.activityStop(this);
    }

    public void inject(Object injected) {
        assert activityGraph != null;
        assert injected != null;
        activityGraph.inject(injected);
    }

    public <T> T get(Class<T> clazz) {
        return activityGraph.get(clazz);
    }

    protected List<Object> getModules() {
        return Arrays.<Object>asList(new ActivityModule(this));
    }

    @Override
    protected void onDestroy() {
        activityGraph = null;
        super.onDestroy();
    }
}
