/* //device/content/providers/media/src/com/android/providers/media/MediaScannerReceiver.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.os.SystemProperties;

import java.io.File;
import java.io.IOException;

public class MediaScannerReceiver extends BroadcastReceiver {
    private final static String TAG = "MediaScannerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
	String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
        String action = intent.getAction();
        Uri uri = intent.getData();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // scan internal storage
            scan(context, MediaProvider.INTERNAL_VOLUME, null);
			scan(context, MediaProvider.EXTERNAL_VOLUME, "/mnt/external_sd");//scan sdcard and udisk when boot to prevent external.db is correct
			scan(context, MediaProvider.EXTERNAL_VOLUME, "/mnt/usb_storage");
			scan(context, MediaProvider.EXTERNAL_VOLUME, externalStoragePath);
        } 
		else if(action.equals(Intent.ACTION_USER_SWITCHED))
		{
			Log.d(TAG,"----MediaScannerReceiver get ACTION_USER_SWITCHED---");
			scan(context, MediaProvider.EXTERNAL_VOLUME, "/mnt/external_sd");//scan public storage to update databases
			scan(context, MediaProvider.EXTERNAL_VOLUME, "/mnt/usb_storage");
			scan(context, MediaProvider.EXTERNAL_VOLUME, "/mnt/internal_sd");
		}
		else {
            if (uri.getScheme().equals("file")) {
                // handle intents related to external storage
                String path = uri.getPath();
                //String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
                String legacyPath = Environment.getLegacyExternalStorageDirectory().getPath();

                try {
                    path = new File(path).getCanonicalPath();
                } catch (IOException e) {
                    Log.e(TAG, "couldn't canonicalize " + path);
                    return;
                }
                if (path.startsWith(legacyPath)) {
                    path = externalStoragePath + path.substring(legacyPath.length());
                }

                Log.d(TAG, "action: " + action + " path: " + path);
                String packageName = intent.getStringExtra("package");
                if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    // scan whenever any volume is mounted
                    scan(context, MediaProvider.EXTERNAL_VOLUME, path);
                } else if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action) &&
                        path != null && (path.startsWith(externalStoragePath + "/") ||
                        		"RockExplorer".equals(packageName))) {
                	File file = new File(path);
                	if(file != null && file.isDirectory()){
                		scan(context,MediaProvider.EXTERNAL_VOLUME, path);
                	}else{
                		scanFile(context, path);
                	}
                }
            }
        }
    }

    private void scan(Context context, String volume, String path) {
        Bundle args = new Bundle();
        args.putString("volume", volume);
        args.putString("path", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }    

    private void scanFile(Context context, String path) {
        Bundle args = new Bundle();
        args.putString("filepath", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }    
}
