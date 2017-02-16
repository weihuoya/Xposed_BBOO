package com.weihuoya.bboo;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.EnterpriseDeviceManager;
import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.widget.Toast;

import com.weihuoya.bboo.activity.AppDetailActivity;
import com.weihuoya.bboo.activity.MainActivity;
import com.weihuoya.bboo.model.PackageModel;
import com.weihuoya.bboo.knox.KnoxAdminReceiver;
import com.weihuoya.bboo.xposed.XposedManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Created by zhangwei on 2016/6/13.
 */
public class _P {

    public interface KnoxActivateListener {
        void onLicenseActivate(boolean success);
        void onAdminActivate(boolean success);
    }

    // blocked package
    public static final String PREF_INCLUDE_PACKAGES = "IncludedPackage";

    public static String UninstallPackage;
    public static String DetailPackage;

    public static List<PackageModel> PackageList;
    public static List<PackageModel> ProcessList;

    public static PackageModel getPackageDetail(String packageName) {
        PackageModel target = null;
        if(PackageList != null) {
            for(PackageModel model : PackageList) {
                if(model.getPackageName().equals(packageName)) {
                    target = model;
                }
            }
        }

        if(target == null || target.getPackageInfo().activities == null) {
            PackageInfo pkg = null;
            PackageManager pm = _G.getPackageManager();

            try {
                int flags = PackageManager.GET_META_DATA |
                        PackageManager.GET_ACTIVITIES |
                        PackageManager.GET_SERVICES |
                        PackageManager.GET_PERMISSIONS |
                        PackageManager.GET_PROVIDERS |
                        PackageManager.GET_RECEIVERS |
                        PackageManager.GET_SIGNATURES;
                pkg = pm.getPackageInfo(packageName, flags);
            } catch (Exception e) {
                _G.log(e.getMessage());
            }

            if(pkg != null) {
                if(target != null) {
                    target.setPackageInfo(pkg);
                } else {
                    target = new PackageModel(pkg);
                }
            }
        }

        return target;
    }

    public static boolean deactivateAdminForKnox() {
        _G.log("deactivateAdminForKnox:");
        DevicePolicyManager manager = _G.getDevicePolicyManager();
        if(manager != null) {
            Context context = _G.getContext();
            ComponentName component = new ComponentName(context, KnoxAdminReceiver.class);
            if(manager.isAdminActive(component)) {
                manager.removeActiveAdmin(component);
                _G.log("deactivateAdminForKnox: removeActiveAdmin");
                return true;
            }
            _G.log("deactivateAdminForKnox: isAdminActive false");
        }
        _G.log("deactivateAdminForKnox: return false");
        return false;
    }

    public void clearAllPackagesCache() {
        PackageManager pm = _G.getPackageManager();
        Method freeStorageAndNotify = null;

        try {
            freeStorageAndNotify = pm.getClass().getMethod("freeStorageAndNotify", Long.class, IPackageDataObserver.class);
        } catch (NoSuchMethodException e) {
            _G.log(e.toString());
        }

        if(freeStorageAndNotify != null) {
            File dataFile = Environment.getDataDirectory();
            String dataPath = dataFile.getAbsolutePath();
            StatFs dataStat = new StatFs(dataPath);
            long storageSize = dataStat.getBlockSizeLong() * dataStat.getBlockCountLong();

            try {
                freeStorageAndNotify.invoke(pm, storageSize, new IPackageDataObserver() {
                    @Override
                    public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
                        _G.log("### onRemoveCompleted: " + packageName);
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                });
            } catch (Exception e) {
                _G.log(e.toString());
            }
        }
    }

    public static void showPackageDetail(String packageName) {
        Context context = _G.getContext();
        Intent intent = new Intent(context, AppDetailActivity.class);
        intent.putExtra("package", packageName);
        context.startActivity(intent);
        DetailPackage = packageName;
    }

    public static void uninstallPackage(String packageName) {
        Context context = _G.getContext();
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_DELETE, packageUri);
        // package fully removed
        intent.putExtra(Intent.EXTRA_DATA_REMOVED, true);
        context.startActivity(intent);
        UninstallPackage = packageName;
    }

    public static void setPackageRemoved(String packageName) {
        if(packageName.equals(UninstallPackage)) {
            Context context = _G.getContext();
            Toast.makeText(context, packageName + " fully removed", Toast.LENGTH_SHORT).show();

            if(context instanceof AppDetailActivity) {
                ((AppDetailActivity)context).handlePackageRemoved(packageName);
            } else if(context instanceof MainActivity) {
                ((MainActivity)context).handlePackageRemoved(packageName);
            }
        }

        if(PackageList != null) {
            for(int i = 0; i < PackageList.size(); ++i) {
                if(PackageList.get(i).getPackageName().equals(packageName)) {
                    PackageList.remove(i);
                    break;
                }
            }
        }

        if(ProcessList != null) {
            for(int i = 0; i < ProcessList.size(); ++i) {
                if(ProcessList.get(i).getPackageName().equals(packageName)) {
                    ProcessList.remove(i);
                }
            }
        }

        _G.log("$$$ package removed: " + packageName);
    }

    public static void setPackageAdded(String packageName) {
        if(PackageList != null) {
            Context context = _G.getContext();
            PackageManager pm = _G.getPackageManager();

            try {
                PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
                PackageList.add(new PackageModel(pkgInfo));
                Collections.sort(PackageList);
            } catch (PackageManager.NameNotFoundException e) {
                _G.log(e.toString());
            }

            Toast.makeText(context, packageName + " added", Toast.LENGTH_SHORT).show();
            _G.log("$$$ package added: " + packageName);
        }
    }

    public static boolean blockPackage(String packageName, boolean isBlock) {
        boolean result = false;
        if(XposedManager.isXposedModuleEnabled()) {
            result = true;
        } else {
            EnterpriseDeviceManager EDM = _G.getEnterpriseDeviceManager();
            if(EDM != null) {
                try {
                    if(isBlock) {
                        result = EDM.getApplicationPolicy().setDisableApplication(packageName);
                    } else {
                        result = EDM.getApplicationPolicy().setEnableApplication(packageName);
                    }
                } catch (SecurityException e) {
                    Toast.makeText(_G.getContext(), "device admin is not activated!", Toast.LENGTH_SHORT).show();
                    _G.log(e.toString());
                }
            } else {
                Toast.makeText(_G.getContext(), "enterprise device manager is missing!", Toast.LENGTH_SHORT).show();
            }
        }
        if(result) {
            SharedPreferences.Editor preferencesEditor = _G.getSharedPreferences(PREF_INCLUDE_PACKAGES).edit();
            preferencesEditor.putBoolean(packageName, isBlock).apply();
        }
        return result;
    }

    public static boolean isBlocked(String packageName) {
        return _G.getSharedPreferences(PREF_INCLUDE_PACKAGES).getBoolean(packageName, false);
    }

    private static KnoxActivateListener mKnoxListener;
}
