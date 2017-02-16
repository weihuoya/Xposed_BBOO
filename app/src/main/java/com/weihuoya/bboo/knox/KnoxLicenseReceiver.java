package com.weihuoya.bboo.knox;

import android.app.Activity;
import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.weihuoya.bboo._G;
import com.weihuoya.bboo._P;

public class KnoxLicenseReceiver extends BroadcastReceiver {

    private static Activity activityObj;

    public KnoxLicenseReceiver() {
    }

    //Getting the current Activity instance
    public KnoxLicenseReceiver(Activity activityObj) {
        KnoxLicenseReceiver.activityObj = activityObj;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            if(action == null) {
                return;
            } else if(action.equals(EnterpriseLicenseManager.ACTION_LICENSE_STATUS)) {
                int errorCode = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                if (errorCode == EnterpriseLicenseManager.ERROR_NONE) {
                    //If license is successfully activated
                    _G.log("KnoxLicenseReceiver activated");
                    KnoxManager.setLicenseActivate(true);
                } else {
                    //If license activation failed
                    _G.log("KnoxLicenseReceiver error: " + errorCode);
                    KnoxManager.setLicenseActivate(false);
                }
            }
        }
    }
}
