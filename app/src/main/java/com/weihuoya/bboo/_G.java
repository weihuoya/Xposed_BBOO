package com.weihuoya.bboo;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;

import java.util.List;

/**
 * Created by zhangwei on 2016/4/24.
 */
public class _G {
    public static final String PREF_INCLUDE_PACKAGES = "IncludedPackage";
    public static List<AppItemModel> ApplicationList;

    public static String formatDiskSize(long size) {
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        double value = size;
        double base = 1024.0f;
        int scale = 0;

        while(value > base && (scale + 1) < units.length) {
            value /= base;
            scale += 1;
        }

        return new java.text.DecimalFormat("#.##").format(value) + units[scale];
    }

    public static void log(String msg) {
        Log.d("BOLT", msg);
    }

    public static void logObjectId(Object o) {
        if(o != null) {
            log(o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o)));
        } else {
            log("object@null");
        }
    }

    public static boolean isServiceRunning(String className) {
        if(mRunningServices == null) {
            ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            mRunningServices = manager.getRunningServices(Integer.MAX_VALUE);
        }

        for (ActivityManager.RunningServiceInfo info : mRunningServices) {
            if (info.service.getClassName().equals(className)){
                return true;
            }
        }

        return false;
    }

    public static LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(mContext);
    }

    public static String getString(int resId) {
        return mContext.getString(resId);
    }

    public static PackageManager getPackageManager() {
        return mContext.getPackageManager();
    }

    public static SharedPreferences getSharedPreferences(String name, int mode) {
        return mContext.getSharedPreferences(name, mode);
    }

    public static SharedPreferences getIncludePackagePrefs() {
        if(mPreferences == null) {
            mPreferences = mContext.getSharedPreferences(PREF_INCLUDE_PACKAGES, Context.MODE_PRIVATE);
        }
        return mPreferences;
    }

    public static SearchRecentSuggestions getSearchSuggestions() {
        if(mSuggestions == null) {
            mSuggestions = new SearchRecentSuggestions(mContext, BBOOSuggestionsProvider.AUTHORITY, BBOOSuggestionsProvider.MODE);
        }
        return mSuggestions;
    }

    public static void attachContext(Context context) {
        mContext = context;
        mPreferences = null;
        mSuggestions = null;
        mRunningServices = null;
    }

    public static Context getContext() {
        return mContext;
    }

    // private
    private static Context mContext;
    private static SharedPreferences mPreferences;
    private static SearchRecentSuggestions mSuggestions;
    private static List<ActivityManager.RunningServiceInfo> mRunningServices;
}
