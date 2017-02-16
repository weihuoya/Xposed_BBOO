package com.weihuoya.bboo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class PackageIntentReceiver extends BroadcastReceiver {

    public PackageIntentReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action == null) {
            return;
        } else if(action.equals(Intent.ACTION_PACKAGE_REMOVED) ||
                action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            Uri uri = intent.getData();
            if(uri != null) {
                _P.setPackageRemoved(uri.getSchemeSpecificPart());
            }
        } else if(action.equals(Intent.ACTION_PACKAGE_INSTALL) ||
                action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            Uri uri = intent.getData();
            if(uri != null) {
                _P.setPackageAdded(uri.getSchemeSpecificPart());
            }
        }
    }
}

