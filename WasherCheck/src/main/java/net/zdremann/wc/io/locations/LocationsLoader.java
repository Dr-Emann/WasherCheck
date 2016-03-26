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
import android.support.v4.content.AsyncTaskLoader;

import net.zdremann.ForApplication;
import net.zdremann.wc.model.MachineGrouping;

import javax.inject.Inject;

import dagger.Lazy;

public class LocationsLoader extends AsyncTaskLoader<MachineGrouping> {

    private MachineGrouping mRoot;
    private Lazy<LocationsProxy> mLocations;

    @Inject
    public LocationsLoader(@ForApplication Context context, Lazy<LocationsProxy> proxy) {
        super(context);
        mLocations = proxy;
    }

    @Override
    public MachineGrouping loadInBackground() {
        return mLocations.get().getRoot();
    }

    @Override
    public void deliverResult(MachineGrouping root) {
        mRoot = root;

        if (isStarted())
            super.deliverResult(root);
    }

    @Override
    protected void onStartLoading() {
        if (mRoot != null)
            deliverResult(mRoot);

        if (takeContentChanged() || mRoot == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        mRoot = null;
    }
}
