package com.weihuoya.bboo;

/**
 * Created by zhangwei1 on 2016/3/7.
 */

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;


import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.UserHandle;


import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookInitPackageResources;


import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import de.robv.android.xposed.XC_MethodHook;


public class XposedHook implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    public static XSharedPreferences preferences;
    public static final String ModulePackageName = XposedHook.class.getPackage().getName();
    public static final String XposedPackageName = "de.robv.android.xposed.installer";

    public XposedHook() {
    }

    @Override
    public void initZygote(StartupParam param) throws Throwable {
        //XposedBridge.log("initZygote modulePath: " + param.modulePath);

        preferences = new XSharedPreferences(ModulePackageName, _G.PREF_INCLUDE_PACKAGES);
        preferences.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam param) throws Throwable {
        //if(param.packageName.equals("android")) {
        //    hookBroadcast(param.classLoader);
        //}

        if (param.appInfo == null ||
                (param.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 ||
                param.packageName.equals(XposedPackageName) || param.packageName.equals(ModulePackageName) ||
                !param.isFirstApplication) {
            return;
        }

        if(isTargetPackage(param.packageName)) {
            dumpPackageInfo(param);
            HookContextService(param.classLoader);
            hookApplicationPackageManager(param.classLoader);
        } else {
            XposedBridge.log("$$$ handleLoadPackage packageName: " + param.packageName + " processName: " + param.processName);
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam param) throws Throwable {
        //XposedBridge.log("handleInitPackageResources packageName: " + param.packageName);
		if (param.packageName.equals("com.sec.android.app.launcher")) {
            // samsuang touchwiz ScrollingLauncherWallpaper
            param.res.setReplacement("com.sec.android.app.launcher", "bool", "config_fixedWallpaperOffset", false);
        }
    }

    protected boolean isTargetPackage(String packageName) {
        if(packageName == null) {
            return false;
        } else {
            boolean result = preferences.getBoolean(packageName, false);
            XposedBridge.log("$$$ isTargetPackage : " + packageName + ", " + String.valueOf(result));
            return result;
        }
    }

    protected boolean isSystemPackage(String packageName) {
        if(packageName == null) {
            return false;
        } else if(packageName.startsWith("android") ||
                packageName.startsWith("com.meizu") ||
                packageName.startsWith("com.android") ||
                packageName.startsWith("com.google.android")) {
            return true;
        } else {
            return false;
        }
    }

    protected void hookBroadcast(ClassLoader loader) {
        Class<?> clazz = null;

        try {
            clazz = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: com.android.server.pm.PackageManagerService");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "queryIntentReceivers", Intent.class, String.class, int.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent)param.args[0];
                            String resolvedType = (String)param.args[1];
                            int flags = (int)param.args[2];
                            int userId = (int)param.args[3];
                            //XposedBridge.log("$$$ PackageManagerService queryIntentReceivers: " + intent.getAction() + ", " + resolvedType + ", " + flags + ", " + userId);
                            //
                            //if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                            @SuppressWarnings("unchecked")
                            List<ResolveInfo> newReceivers = (List<ResolveInfo>)param.getResult();

                            if(newReceivers != null) {
                                boolean isTargetIntent = false;
                                Iterator<ResolveInfo> iter = newReceivers.iterator();

                                StringBuilder sb = new StringBuilder();
                                sb.append("$$$ queryIntentReceivers: ");
                                sb.append(intent.getAction());
                                sb.append(" {");

                                while(iter.hasNext()) {
                                    ResolveInfo receiver = iter.next();
                                    if(isTargetPackage(receiver.activityInfo.packageName)) {
                                        isTargetIntent = true;
                                        iter.remove();
                                    } else if(isSystemPackage(receiver.activityInfo.packageName)) {
                                        continue;
                                    } else {
                                        sb.append(receiver.activityInfo.packageName);
                                        sb.append(", ");
                                    }
                                }

                                if(isTargetIntent) {
                                    sb.append("}");
                                    XposedBridge.log(sb.toString());
                                }
                            }
                            //}
                        }
                    }
            );

            clazz = null;
        }


        try {
            clazz = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: com.android.server.am.ActivityManagerService");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "systemReady", Runnable.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // public abstract class com.android.server.IntentResolver<F extends IntentFilter, R extends Object>
                            Object mReceiverResolver = XposedHelpers.getObjectField(param.thisObject, "mReceiverResolver");
                            XposedHelpers.findAndHookMethod(
                                    mReceiverResolver.getClass().getSuperclass(),
                                    "queryIntent", Intent.class, String.class, boolean.class, int.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            Intent intent = (Intent)param.args[0];
                                            String resolvedType = (String)param.args[1];
                                            boolean defaultOnly = (boolean)param.args[2];
                                            int userId = (int)param.args[3];
                                            //XposedBridge.log("$$$ mReceiverResolver queryIntent: " + intent.getAction() + ", " + resolvedType + ", " + defaultOnly + ", " + userId);
                                            // com.android.server.am.BroadcastFilter
                                            //Class<?> clazz = XposedHelpers.findClass("com.android.server.am.BroadcastFilter", param.thisObject.getClass().getClassLoader());
                                            //List<Object> registeredReceiversForUser = (List<Object>)param.getResult();
                                            //for(int i = 0; i < registeredReceiversForUser.size(); ++i) {
                                            //    clazz.cast(registeredReceiversForUser.get(i)).toString();
                                            //}
                                        }
                                    }
                            );
                        }
                    }
            );
            clazz = null;
        }



        /*
        try  {
            clazz = XposedHelpers.findClass("com.android.server.firewall.IntentFirewall", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: com.android.server.firewall.IntentFirewall");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "checkBroadcast", Intent.class, int.class, int.class, String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent) param.args[0];
                            int callerUid = (int) param.args[1];
                            int receivingUid = (int) param.args[4];

                            String action = intent.getAction();
                            //XposedBridge.log("$$$ hookBroadcast caller: " + callerUid + ", receiver: " + receivingUid + ", action: " + action);

                            if(action == null) {
                                return;
                            } else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                                XposedBridge.log("$$$ 111");
                            } else if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                                XposedBridge.log("$$$ 222");
                            } else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                                XposedBridge.log("$$$ 333");
                            }
                        }
                    });

            clazz = null;
        }

        try {
            clazz = XposedHelpers.findClass("com.android.server.net.NetworkPolicyManagerService", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: com.android.server.net.NetworkPolicyManagerService");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "systemReady",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("$$$ NetworkPolicyManagerService systemReady");

                            Object mProcessObserverClass = XposedHelpers.getObjectField(param.thisObject, "mProcessObserver");
                            XposedHelpers.findAndHookMethod(
                                    mProcessObserverClass.getClass(),
                                    "onForegroundActivitiesChanged", int.class, int.class, boolean.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            if((boolean)param.args[2]) {
                                                XposedBridge.log("$$$ onForegroundActivitiesChanged: " + param.args[1]);
                                            }
                                        }
                                    }
                            );
                        }
                    }
            );

            clazz = null;
        }*/
    }

    protected void HandleSamsungTouchWiz(LoadPackageParam param) {
        Class<?> clazz = null;
        String method = null;
        XC_MethodHook hook = null;

        if (param.packageName.equals("com.android.systemui")) {
            // samsuang DisableBatteryFullAlert
            try {
                clazz = XposedHelpers.findClass("com.android.systemui.power.PowerUI", param.classLoader);
                method = "notifyFullBatteryNotification";
                hook = XC_MethodReplacement.DO_NOTHING;
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("$$$ class not found: com.android.systemui.power.PowerUI");
            }
        } else if (param.packageName.equals("com.android.phone")) {
            // samsung EnableCallRecording
            try {
                clazz = XposedHelpers.findClass("com.android.phone.PhoneFeature", param.classLoader);
                method = "hasFeature";
                hook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if ("voice_call_recording".equals(param.args[0])) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                };
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("$$$ class not found: com.android.phone.PhoneFeature");
            }
        } else if (param.packageName.equals("com.android.mms")) {
            // samsuang Long SMS to MMS Conversion Disabler
            try {
                clazz = XposedHelpers.findClass("com.android.mms.MmsConfig", param.classLoader);
                method = "getSmsToMmsTextThreshold";
                hook = new XC_MethodReplacement() {
                    protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam) throws Throwable {
                        return 255;
                    }
                };
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("$$$ class not found: com.android.mms.MmsConfig");
            }
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(clazz, method, hook);
            clazz = null;
        }
    }

    protected void HookContextService(ClassLoader loader) {
        Class<?> clazz = null;

        try {
            clazz = XposedHelpers.findClass("android.app.ContextImpl", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: android.app.ContextImpl");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "startService", Intent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Intent service = (Intent)param.args[0];
                            XposedBridge.log("$$$ ContextImpl startService: " + service.getAction());
                        }
                    }
            );

            clazz = null;
        }

        /*try {
            clazz = XposedHelpers.findClass("android.content.ContextWrapper", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: android.content.ContextWrapper");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "attachBaseContext", Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context base = (Context)param.args[0];

                            XposedHelpers.findAndHookMethod(
                                    base.getClass(),
                                    "startService", Intent.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                            Intent service = (Intent)param.args[0];
                                            XposedBridge.log("$$$ ContextWrapper startService: " + service.getAction());
                                        }
                                    }
                            );

                            XposedHelpers.findAndHookMethod(
                                    base.getClass(),
                                    "startServiceAsUser", Intent.class, UserHandle.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                            Intent service = (Intent)param.args[0];
                                            XposedBridge.log("$$$ ContextWrapper startServiceAsUser: " + service.getAction());
                                        }
                                    }
                            );
                        }
                    }
            );
            clazz = null;
        }*/

        try {
            clazz = XposedHelpers.findClass("android.os.ServiceManager", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: android.os.ServiceManager");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "addService", String.class, IBinder.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String name = (String)param.args[0];
                            IBinder service = (IBinder)param.args[1];
                            XposedBridge.log("$$$ ServiceManager addService: " + name + ", " + service.toString());
                        }
                    }
            );
            clazz = null;
        }
    }

    protected void dumpPackageInfo(LoadPackageParam param) {
        try {
            Object activityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context mContext = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");

            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(param.packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);

            StringBuilder sb = new StringBuilder();
            sb.append("$$$ dumpPackageInfo: ");
            sb.append(param.packageName);
            sb.append(", version: ");
            sb.append(packageInfo.versionName);

            if(packageInfo.activities != null && packageInfo.activities.length > 0) {
                sb.append(", activities: ");
                sb.append(packageInfo.activities.length);
            }

            if(packageInfo.services != null && packageInfo.services.length > 0) {
                sb.append(", services: ");
                sb.append(packageInfo.services.length);
            }

            XposedBridge.log(sb.toString());
            //StringBuilder activities = new StringBuilder().append("activities: ");
            //for(ActivityInfo activityInfo : packageInfo.activities) {
            //    activities.append(activityInfo.name).append(", ");
            //}
            //XposedBridge.log(activities.toString());

            //StringBuilder services = new StringBuilder().append("services: ");
            //for(ServiceInfo serviceInfo : packageInfo.services) {
            //    services.append(serviceInfo.name).append(", ");
            //}
            //XposedBridge.log(services.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void hookApplicationPackageManager(ClassLoader loader) {
        Class<?> clazz = null;

        try {
            clazz = XposedHelpers.findClass("android.app.ApplicationPackageManager", loader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("$$$ class not found: android.app.ApplicationPackageManager");
        }

        if(clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getInstalledApplications", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            @SuppressWarnings("unchecked")
                            List<ApplicationInfo> applicationInfoList = (List<ApplicationInfo>) param.getResult();

                            ArrayList<ApplicationInfo> to_remove = new ArrayList<>();
                            for (ApplicationInfo info : applicationInfoList) {
                                if (info.packageName.contains(ModulePackageName) || info.packageName.contains(XposedPackageName)) {
                                    to_remove.add(info);
                                }
                            }

                            applicationInfoList.removeAll(to_remove);

                            //for(ApplicationInfo info : applicationInfoList) {
                            //    XposedBridge.log("getInstalledApplications: " + info.packageName);
                            //}
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getInstalledPackages", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            @SuppressWarnings("unchecked")
                            List<PackageInfo> packageInfoList = (List<PackageInfo>) param.getResult();

                            ArrayList<PackageInfo> to_remove = new ArrayList<>();
                            for (PackageInfo info : packageInfoList) {
                                if (info.packageName.contains(ModulePackageName) || info.packageName.contains(XposedPackageName)) {
                                    to_remove.add(info);
                                }
                            }

                            packageInfoList.removeAll(to_remove);

                            //for(PackageInfo info : packageInfoList) {
                            //    XposedBridge.log("getInstalledPackages: " + info.packageName);
                            //}
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    clazz,
                    "setComponentEnabledSetting", ComponentName.class, int.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            ComponentName component = (ComponentName)param.args[0];
                            XposedBridge.log("$$$ setComponentEnabledSetting: " + component.toString());
                            param.args[1] = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                        }
                    }
            );

            clazz = null;
        }
    }

}
