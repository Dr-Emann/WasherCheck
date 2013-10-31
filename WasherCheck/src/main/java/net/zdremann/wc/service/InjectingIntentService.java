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

package net.zdremann.wc.service;

import android.app.IntentService;

import net.zdremann.wc.MyApplication;

import java.util.Arrays;
import java.util.List;

import air.air.net.zdremann.zsuds.BuildConfig;
import dagger.ObjectGraph;

public abstract class InjectingIntentService extends IntentService {
    protected ObjectGraph activityGraph;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public InjectingIntentService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        activityGraph = MyApplication.getApplicationGraph().plus(getModules().toArray());

        if (BuildConfig.DEBUG)
            activityGraph.validate();

        activityGraph.inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        return Arrays.<Object>asList(new ServiceModule());
    }
}
