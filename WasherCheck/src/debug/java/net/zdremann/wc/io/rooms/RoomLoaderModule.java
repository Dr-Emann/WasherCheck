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

import android.content.SharedPreferences;
import android.os.Build;

import net.zdremann.wc.Main;

import java.util.Arrays;

import dagger.Module;
import dagger.Provides;

@Module
public class RoomLoaderModule {
    @Provides
    MachineGetter provideMachineGetter(@Main SharedPreferences preferences,
          EsudsMachineGetter esudsMachineGetter,
          GaeMachineGetter gaeMachineGetter,
          DescendingMachineGetter debugMachineGetter) {
        if(!preferences.getBoolean("net.zdremann.wc.fake_io", false))
            return new FallbackMachineGetter(
                  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
              Arrays.asList(gaeMachineGetter, esudsMachineGetter, debugMachineGetter) :
              Arrays.asList(esudsMachineGetter, debugMachineGetter))
            );
        else
            return debugMachineGetter;
    }
}
