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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;

import net.zdremann.wc.ApplicationModule;
import net.zdremann.wc.ForActivity;
import net.zdremann.wc.io.IOModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by DremannZ on 6/21/13.
 */
@Module(
        addsTo = ApplicationModule.class,
        injects = {
                RoomViewer.class,
                RoomViewFragment.class,
                RoomChooserActivity.class,
                RoomChooserFragment.class
        },
        includes = {
                IOModule.class
        }
)
public class ActivityModule {
    private final Activity mActivity;

    public ActivityModule(Activity activity) {
        mActivity = activity;
    }

    @Provides
    @ForActivity
    public Context provideActivityContext() {
        return mActivity;
    }

    @Provides
    @Singleton
    public LayoutInflater provideLayoutInflater(@ForActivity Context context) {
        return LayoutInflater.from(context);
    }
}
