package com.weihuoya.bboo;

/**
 * Created by zhangwei1 on 2016/3/7.
 */

import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;


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


import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookInitPackageResources;


import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import de.robv.android.xposed.XC_MethodHook;


public class XposedHook implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    protected final String ModulePackageName;
    protected final String XposedPackageName;

    protected final String QQPackageName;
    protected final String WechatPackageName;
    protected final String ZhihuPackageName;

    public XposedHook() {
        ModulePackageName = this.getClass().getPackage().getName();
        XposedPackageName = "de.robv.android.xposed.installer";

        QQPackageName = "com.tencent.mobileqq";
        WechatPackageName = "com.tencent.mm";
        ZhihuPackageName = "com.zhihu.android";
    }

    @Override
    public void initZygote(StartupParam param) throws Throwable {
        //XposedBridge.log("initZygote modulePath: " + param.modulePath);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam param) throws Throwable {
        if(param.packageName.equals("android")) {
            hookBroadcast(param.classLoader);
        }

        if (param.appInfo == null ||
                (param.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 ||
                param.packageName.equals(XposedPackageName) || param.packageName.equals(ModulePackageName) ||
                !param.isFirstApplication) {
            return;
        }

        if(param.packageName.equals(QQPackageName) || param.packageName.equals(WechatPackageName) || param.packageName.equals(ZhihuPackageName)) {
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
                            XposedBridge.log("$$$ PackageManagerService queryIntentReceivers: " + intent.getAction() + ", " + resolvedType + ", " + flags + ", " + userId);
                            //
                            //List<ResolveInfo> newReceivers = (List<ResolveInfo>)param.getResult();
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
                            // public abstract class IntentResolver<F extends IntentFilter, R extends Object>
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
                                            XposedBridge.log("$$$ mReceiverResolver queryIntent: " + intent.getAction() + ", " + resolvedType + ", " + defaultOnly + ", " + userId);
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

        try {
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
        }
    }

    protected void dumpPackageInfo(LoadPackageParam param) {
        try {
            Object activityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context mContext = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");

            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(param.packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
            XposedBridge.log("$$$ dumpPackageInfo: " + param.packageName + ", " + param.appInfo.uid);
            XposedBridge.log("version: " + packageInfo.versionName);

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
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loader,
                "getInstalledApplications", int.class, new XC_MethodHook() {
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

                        for(ApplicationInfo info : applicationInfoList) {
                            XposedBridge.log("getInstalledApplications: " + info.packageName);
                        }
                    }
                });

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loader,
                "getInstalledPackages", int.class, new XC_MethodHook() {
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

                        for(PackageInfo info : packageInfoList) {
                            XposedBridge.log("getInstalledPackages: " + info.packageName);
                        }
                    }
                });
    }

}
