package com.weihuoya.bboo.knox;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;

import com.weihuoya.bboo._G;
import com.weihuoya.bboo._P;

public class KnoxAdminReceiver extends DeviceAdminReceiver {

    private static Activity activityObj;

    public KnoxAdminReceiver() {
    }

    //Getting the current Activity instance
    public KnoxAdminReceiver(Activity activityObj) {
        KnoxAdminReceiver.activityObj = activityObj;
    }

    //If device admin is enabled
    @Override
    public void onEnabled(Context context, Intent intent) {
        _G.log("KnoxAdminReceiver: onEnabled");
        KnoxManager.setAdminActivate(true);
    }

    //When request for disabling device admin is made
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Do you want to disable the administrator?";
    }

    //When device admin is disabled
    @Override
    public void onDisabled(Context context, Intent intent) {
        _G.log("KnoxAdminReceiver: onDisabled");
        KnoxManager.setAdminActivate(false);
    }
}
