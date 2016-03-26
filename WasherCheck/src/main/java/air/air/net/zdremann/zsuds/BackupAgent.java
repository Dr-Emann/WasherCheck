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

package air.air.net.zdremann.zsuds;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupManager;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import net.zdremann.wc.ApplicationModule;
import net.zdremann.wc.ContextModule;

public class BackupAgent extends BackupAgentHelper {
    static final String MY_PREFS_BACKUP_KEY = "myprefs";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(
              this,
              ContextModule.MAIN_PREFS_NAME
        );
        addHelper(MY_PREFS_BACKUP_KEY, helper);
    }

    public static void requestBackup(Context context) {
        BackupManager bm = new BackupManager(context.getApplicationContext());
        bm.dataChanged();
    }
}
