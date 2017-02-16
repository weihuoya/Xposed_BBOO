package com.weihuoya.bboo.model;

import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;
import com.jaredrummler.android.processes.models.Statm;
import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;
import com.weihuoya.bboo._P;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by zhangwei on 2016/4/24.
 */
public class PackageModel implements Comparable<PackageModel> {

    // for manifest
    private ManifestModel mManifest;
    private Context mContext;

    // for package
    private PackageInfo mPackageInfo;
    private android.content.pm.PackageStats mPackageStats;
    private boolean isPackageStatsLoading;

    // for process
    private AndroidAppProcess mProcess;
    private Stat mProcessStat;
    private Statm mProcessStatm;

    // common properties
    private String name;
    private int dominantColor;

    public PackageModel(PackageInfo pkg) {
        mPackageInfo = pkg;
        isPackageStatsLoading = false;
        name = null;
        mPackageStats = null;
    }

    public PackageModel(AndroidAppProcess process) {
        PackageManager pm = _G.getPackageManager();
        mProcess = process;
        name = null;
        mProcessStat = null;
        mProcessStatm = null;

        try {
            mProcessStat = process.stat();
            mProcessStatm = process.statm();
        } catch (IOException e) {
            _G.log(e.toString());
        }

        try {
            mPackageInfo = pm.getPackageInfo(process.getPackageName(), PackageManager.GET_META_DATA);
        } catch(PackageManager.NameNotFoundException e) {
            mPackageInfo = null;
            _G.log(e.toString());
        }
    }

    public boolean isXposedModule() {
        boolean xposedModule = false;
        PackageInfo pkg = getPackageInfo();
        if(pkg != null && pkg.applicationInfo != null && pkg.applicationInfo.metaData != null) {
            xposedModule = pkg.applicationInfo.metaData.getBoolean("xposedmodule", false);
        }
        return xposedModule;
    }

    public boolean isStepCounter() {
        boolean result = false;
        PackageInfo pkg = getPackageInfo();
        if(pkg != null && pkg.applicationInfo != null && pkg.applicationInfo.metaData != null) {
            String value = pkg.applicationInfo.metaData.getString("com.samsung.android.health.permission.read");
            if(value != null) {
                _G.log("### StepCounter: " + value);
                result = true;
            }
        }
        return result;
    }

    public boolean isEdgeApp() {
        boolean result = false;
        PackageInfo pkg = getPackageInfo();
        if(pkg != null && pkg.applicationInfo != null && pkg.applicationInfo.metaData != null) {
            String value = pkg.applicationInfo.metaData.getString("com.samsung.android.cocktail.mode");
            if(value != null) {
                _G.log("### EdgeApp: " + value);
                result = true;
            }
        }
        return result;
    }

    public boolean isBlocked() {
        String packageName = getPackageName();
        if(packageName != null) {
            return _P.isBlocked(packageName);
        } else {
            return false;
        }
    }

    public boolean isUnstallEnabled() {
        return ! isSystemApplication();
    }

    public boolean isApplicationEnabled() {
        PackageInfo pkg = getPackageInfo();
        if(pkg != null && pkg.applicationInfo != null) {
            return pkg.applicationInfo.enabled;
        } else {
            return true;
        }
    }

    public boolean isSystemApplication() {
        PackageInfo pkg = getPackageInfo();
        if(pkg != null && pkg.applicationInfo != null) {
            return (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;
        } else {
            return true;
        }
    }

    public boolean isHaveAdMob() {
        boolean result = false;
        PackageInfo pkg = getPackageInfo();
        if(pkg.activities != null && pkg.activities.length > 0) {
            for(ActivityInfo info : pkg.activities) {
                if(info.name.contains(".ads.") || info.name.contains("adcolony")) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public boolean isNotificationEnabled() {
        final String CHECK_OP_NO_THROW = "checkOpNoThrow";
        final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

        if(mPackageInfo != null && mPackageInfo.applicationInfo != null) {
            int uid = mPackageInfo.applicationInfo.uid;
            AppOpsManager AppOps = (AppOpsManager) _G.getContext().getSystemService(Context.APP_OPS_SERVICE);
            String pkgName = getPackageName();
            try {
                Method checkOpNoThrowMethod = AppOpsManager.class.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);
                Field opPostNotificationValue = AppOpsManager.class.getDeclaredField(OP_POST_NOTIFICATION);
                int opFlag = (int)opPostNotificationValue.get(Integer.class);
                return ((int)checkOpNoThrowMethod.invoke(AppOps, opFlag, uid, pkgName) == AppOpsManager.MODE_ALLOWED);
            } catch (Exception e) {
                _G.log(e.toString());
            }
        }
        return false;
    }

    public void setNotificationEnabled(boolean enabled) {
        final String SET_MODE = "setMode";
        final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
        final String CANCEL_ALL_NOTIFICATIONS_INT = "cancelAllNotificationsInt";

        String pkgName = getPackageName();
        int uid = mPackageInfo.applicationInfo.uid;
        AppOpsManager AppOps = (AppOpsManager) _G.getContext().getSystemService(Context.APP_OPS_SERVICE);
        NotificationManager notificationManager = (NotificationManager) _G.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            Method setModeMethod = AppOpsManager.class.getMethod(SET_MODE, Integer.TYPE, Integer.TYPE, String.class, Integer.TYPE);
            Field opPostNotificationValue = AppOpsManager.class.getDeclaredField(OP_POST_NOTIFICATION);
            int opFlag = (int)opPostNotificationValue.get(Integer.class);
            setModeMethod.invoke(AppOps, opFlag, uid, pkgName, enabled ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
        } catch (Exception e) {
            _G.log(e.toString());
        }

        try {
            //Method getServiceMethod = NotificationManager.class.getMethod("getService");
            //Object service = getServiceMethod.invoke(null);

            //Class NotificationManagerService = _G.getContext().getClassLoader().loadClass("com.android.server.notification.NotificationManagerService");
            //Method cancelAllNotificationsIntMethod = NotificationManagerService.getMethod(CANCEL_ALL_NOTIFICATIONS_INT, String.class, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE);
            //cancelAllNotificationsIntMethod.invoke(notificationManager, pkgName, 0, 0, true, uid);
        } catch (Exception e) {
            _G.log(e.toString());
        }
    }

    public String getName() {
        if(name == null) {
            AndroidAppProcess process = getAppProcess();
            PackageInfo pkg = getPackageInfo();

            if(process != null) {
                name = process.name;
            } else if(pkg != null) {
                name = pkg.packageName;
            }

            if(pkg != null && pkg.applicationInfo != null) {
                PackageManager pm = _G.getPackageManager();
                name = pm.getApplicationLabel(pkg.applicationInfo).toString();
            }

            int index = name.lastIndexOf(".");
            if(index != -1 && index + 2 < name.length()) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }

    public String getPackageName() {
        String packageName = null;
        PackageInfo pkg = getPackageInfo();
        AndroidAppProcess prc = getAppProcess();

        if(pkg != null) {
            packageName = pkg.packageName;
        } else if(prc != null) {
            packageName = prc.getPackageName();
        }

        return packageName;
    }

    public String getPackageDescription() {
        PackageInfo pkg = getPackageInfo();
        android.content.pm.PackageStats pkgStats = getPackageStats();
        String pkgVersion = "";
        String pkgSize = "";
        String sdkVersion = "";

        if(pkg != null) {
            if(pkg.versionName != null) {
                pkgVersion = pkg.versionName;
            }

            if(pkg.applicationInfo != null) {
                sdkVersion = "API" + pkg.applicationInfo.targetSdkVersion;
            }

            if(pkgStats != null) {
                pkgSize = _G.formatDiskSize(pkgStats.codeSize);
            }
        }

        if(pkgVersion.length() > 24) {
            pkgVersion = pkgVersion.substring(24);
        }

        return String.format("v%s  %s  %s", pkgVersion, pkgSize, sdkVersion);
    }

    public String getProcessDescription() {
        String processName = mProcess.name;
        //String processState = "";
        StringBuilder sb = new StringBuilder();

        int index = processName.lastIndexOf(":");
        if(index != -1 && index + 1 < processName.length()) {
            processName = processName.substring(index + 1);
        } else {
            index = processName.lastIndexOf(".");
            if(index != -1 && index + 1 < processName.length()) {
                processName = processName.substring(index + 1);
            }
        }

        sb.append(processName);
        sb.append("[");
        sb.append(mProcess.pid);
        sb.append("]  ");

        long size = 0;
        //long startTime = 0;

        /*if(mProcessStat != null) {
            //startTime = mProcessStat.stime();
            char state = mProcessStat.state();
            switch (state) {
                case 'R':
                    processState = _G.getString(R.string.process_state_r);
                    break;
                case 'S':
                    processState = _G.getString(R.string.process_state_s);
                    break;
                case 'D':
                    processState = _G.getString(R.string.process_state_d);
                    break;
                case 'Z':
                    processState = _G.getString(R.string.process_state_z);
                    break;
                default:
                    processState = String.valueOf(state);
                    break;
            }
        }*/

        if(mProcessStatm != null) {
            size = mProcessStatm.getSize();
        }

        sb.append(_G.formatDiskSize(size));
        sb.append("  ");

        //sb.append(processState);

        return sb.toString();
    }

    public Drawable getIcon() {
        PackageInfo pkg = getPackageInfo();
        Drawable icon = null;
        if(pkg != null && pkg.applicationInfo != null) {
            icon = _G.getPackageManager().getApplicationIcon(pkg.applicationInfo);
        } else {
            icon = _G.getContext().getDrawable(android.R.drawable.sym_def_app_icon);
        }
        return icon;
    }

    public int getDominantColor() {
        if(dominantColor == -1) {
            Drawable icon = getIcon();
            if(icon == null) {
                _G.log("$$$ getDominantColor: icon is null");
            } else if(icon instanceof BitmapDrawable) {
                int defaultColor = _G.getThemeColor(android.R.attr.colorPrimary, -1);
                Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
                Palette palette = Palette.from(bitmap).generate();
                dominantColor = palette.getVibrantColor(defaultColor);
            } else {
                _G.log("$$$ getDominantColor: not a BitmapDrawable");
            }
        }
        return dominantColor;
    }

    public int getContrastColor() {
        int contrast = -1;
        int dominant = getDominantColor();
        if(dominant != -1) {
            //contrast = Color.rgb(255-Color.red(dominant), 255-Color.green(dominant), 255-Color.blue(dominant));
            double x = (299 * Color.red(dominant) + 587 * Color.green(dominant) + 114 * Color.blue(dominant)) / 1000;
            contrast = x >= 128 ? Color.BLACK : Color.WHITE;
        }
        return contrast;
    }

    public Context getContext() {
        if(mContext == null) {
            try {
                mContext = _G.getContext().createPackageContext(getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            } catch (PackageManager.NameNotFoundException e) {
                _G.log(e.toString());
            }
        }

        return mContext;
    }

    public ManifestModel getManifest() {
        if(mManifest == null) {
            Context context = getContext();
            if(context != null) {
                AssetManager assetManager = mContext.getAssets();
                try {
                    XmlResourceParser parser = assetManager.openXmlResourceParser("AndroidManifest.xml");
                    ManifestModel manifestModel = new ManifestModel(this);
                    manifestModel.loadManifest(parser);
                    mManifest = manifestModel;
                } catch (Exception e) {
                    _G.log(e.toString());
                }
            }
        }

        return mManifest;
    }

    public String getString(int resId) {
        Context context = getContext();
        return context.getString(resId);
    }

    public Drawable getDrawable(int resId) {
        return mContext.getDrawable(resId);
    }

    public void releaseContext() {
        mContext = null;
    }

    @Nullable
    public android.content.pm.PackageStats getPackageStats() {
        if(mPackageStats == null && !isPackageStatsLoading) {
            PackageManager pm = _G.getPackageManager();
            String packageName = getPackageName();
            Method getPackageSizeInfo = null;

            isPackageStatsLoading = true;

            try {
                getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
            } catch (NoSuchMethodException e) {
                _G.log(e.toString());
            }

            if(getPackageSizeInfo != null) {
                try {
                    getPackageSizeInfo.invoke(pm, packageName, new IPackageStatsObserver.Stub() {
                        @Override
                        public void onGetStatsCompleted(android.content.pm.PackageStats pStats, boolean succeeded)
                                throws android.os.RemoteException {
                            mPackageStats = pStats;
                            isPackageStatsLoading = false;
                        }
                    });
                } catch (Exception e) {
                    _G.log(mPackageInfo.packageName + ": " + e.getMessage());
                }
            }
        }
        return mPackageStats;
    }

    public void setPackageInfo(PackageInfo info) {
        mPackageInfo = info;
    }

    public PackageInfo getPackageInfo() {
        return mPackageInfo;
    }

    public AndroidAppProcess getAppProcess() {
        return mProcess;
    }

    public int compareTo(@NonNull PackageModel that) {
        String thisName = this.getPackageName();
        String thatName = that.getPackageName();

        int index = thisName.lastIndexOf(".");
        if(index != -1 && index + 1 < thisName.length()) {
            thisName = thisName.substring(index + 1);
        }

        index = thatName.lastIndexOf(".");
        if(index != -1 && index + 1 < thatName.length()) {
            thatName = thatName.substring(index + 1);
        }

        return thisName.toUpperCase().compareTo(thatName.toUpperCase());
    }
}
