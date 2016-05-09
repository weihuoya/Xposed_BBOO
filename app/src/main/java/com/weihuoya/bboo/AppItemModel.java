package com.weihuoya.bboo;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * Created by zhangwei on 2016/4/24.
 */
public class AppItemModel implements Comparable<AppItemModel> {
    private PackageInfo mPackageInfo;
    private android.content.pm.PackageStats mPackageStats;
    private String name;
    private boolean xposedModule;

    public AppItemModel(PackageInfo pkg) {
        PackageManager pm = _G.getPackageManager();
        mPackageInfo = pkg;
        name = pm.getApplicationLabel(pkg.applicationInfo).toString();
        xposedModule = false;

        int index = name.lastIndexOf(".");
        if(index != -1 && index + 1 < name.length()) {
            name = name.substring(index + 1);
        }

        if(pkg.applicationInfo.metaData != null) {
            xposedModule = pkg.applicationInfo.metaData.getBoolean("xposedmodule", false);
        }

        try {
            Method getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
            getPackageSizeInfo.invoke(pm, pkg.packageName, new IPackageStatsObserver.Stub() {
                @Override
                public void onGetStatsCompleted(android.content.pm.PackageStats pStats, boolean succeeded)
                        throws android.os.RemoteException {
                    mPackageStats = pStats;
                }
            });
        } catch (Exception e) {
            _G.log(pkg.packageName + ": " + e.getMessage());
        }
    }

    public boolean isXposedModule() {
        return xposedModule;
    }

    public boolean isBlocked() {
        return _G.getIncludePackagePrefs().getBoolean(mPackageInfo.packageName, false);
    }

    public boolean isSystemApplication() {
        return (mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return mPackageInfo.packageName;
    }

    public String getDescription() {
        String pkgVersion = mPackageInfo.versionName;
        String pkgSize = null;
        String sdkVersion = null;

        if(pkgVersion.length() > 24) {
            pkgVersion = pkgVersion.substring(24);
        }

        if(mPackageStats != null && mPackageStats.codeSize > 0) {
            pkgSize = _G.formatDiskSize(mPackageStats.codeSize);
        } else {
            pkgSize = "";
        }

        if(mPackageInfo.applicationInfo != null) {
            int sdk = mPackageInfo.applicationInfo.targetSdkVersion;
            sdkVersion = "API" + sdk;
        } else {
            sdkVersion = "";
        }

        return String.format("v%s  %s  %s", pkgVersion, pkgSize, sdkVersion);
    }

    public Drawable getIcon() {
        return _G.getPackageManager().getApplicationIcon(mPackageInfo.applicationInfo);
    }

    @Nullable
    public android.content.pm.PackageStats getPackageStats() {
        return mPackageStats;
    }

    public void setPackageInfo(PackageInfo info) {
        mPackageInfo = info;
    }

    public PackageInfo getPackageInfo() {
        return mPackageInfo;
    }

    public int compareTo(@NonNull AppItemModel that) {
        if (this.name == null) {
            return -1;
        } else if (that.name == null) {
            return 1;
        } else {
            return this.name.toUpperCase().compareTo(that.name.toUpperCase());
        }
    }
}
