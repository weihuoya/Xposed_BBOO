package com.weihuoya.bboo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by zhangwei1 on 2016/3/23.
 */
public abstract class ProxyApplication extends Application {
    protected abstract void initProxyApplication();

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        initProxyApplication();
    }

    @Override
    public String getPackageName() {
        return "";
    }

    @Override
    public void onCreate() {
        super.onCreate();

        String className = getApplicationName();
        Application delegate = null;
        try {
            Class delegateClass = Class.forName(className, true, getClassLoader());
            delegate = (Application) delegateClass.newInstance();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if(delegate != null) {
            Context base = getBaseContext();
            try {
                Object mPackageInfo = getField(base, "mPackageInfo");
                setField(mPackageInfo, "mApplication", delegate);
                setField(base, "mOuterContext", delegate);

                Object mActivityThread = getField(mPackageInfo, "mActivityThread");
                setField(mActivityThread, "mInitialApplication", delegate);

                @SuppressWarnings("unchecked")
                ArrayList<Application> mAllApplications = (ArrayList<Application>) getField(mActivityThread, "mAllApplications");
                for (int i = 0; i < mAllApplications.size(); i++) {
                    if (mAllApplications.get(i) == this) {
                        mAllApplications.set(i, delegate);
                    }
                }

                Method attach = Application.class.getDeclaredMethod("attach", Context.class);
                attach.setAccessible(true);
                attach.invoke(delegate, base);
                delegate.onCreate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected Object getField(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        return prepareField(obj.getClass(), fieldName).get(obj);
    }

    protected void setField(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        prepareField(obj.getClass(), fieldName).set(obj, value);
    }

    private Field prepareField(Class c, String fieldName) throws NoSuchFieldException {
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (Exception e) {
                //e.printStackTrace();
            } finally {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException();
    }

    private String getApplicationName() {
        String className = "android.app.Application";
        String key = "DELEGATE_APPLICATION_CLASS_NAME";
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(super.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = appInfo.metaData;
            if (bundle != null && bundle.containsKey(key)) {
                String value = bundle.getString(key);
                if (value != null) {
                    if(value.startsWith(".")) {
                        className = super.getPackageName() + value;
                    } else {
                        className = value;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
        }
        return className;
    }
}
