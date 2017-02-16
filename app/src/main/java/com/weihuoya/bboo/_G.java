package com.weihuoya.bboo;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;

import android.app.admin.DevicePolicyManager;
import android.app.enterprise.EnterpriseDeviceManager;
import android.app.enterprise.license.EnterpriseLicenseManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ActionMenuView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.weihuoya.bboo.model.PackageModel;
import com.weihuoya.bboo.activity.MainActivity;
import com.weihuoya.bboo.activity.AppDetailActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by zhangwei on 2016/4/24.
 */
public class _G {

    public static boolean copyFile(File src, File dst) {
        FileInputStream instream = null;
        FileOutputStream outstream = null;
        FileChannel inchannel = null;
        FileChannel outchannel = null;

        try {
            // channel
            instream = new FileInputStream(src);
            outstream = new FileOutputStream(dst);
            inchannel = instream.getChannel();
            outchannel = outstream.getChannel();
            // transfer
            inchannel.transferTo(0, inchannel.size(), outchannel);
            // close
            inchannel.close();
            outchannel.close();
            instream.close();
            outstream.close();
            //
            return true;
        } catch (IOException e) {
            _G.log(e.toString());
        }
        return false;
    }

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

    public static String getSystemProperty(String propertyName, String defaultValue) {
        String propertyValue = defaultValue;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propertyName);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            propertyValue = input.readLine();
            input.close();
        } catch (IOException e) {
            _G.log(e.toString());
        }
        return propertyValue;
    }

    public static String formatTime(long timestamp) {
        String time = "";
        try {
            DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            time = sdf.format(new Date(timestamp));
        } catch (Exception e) {
            _G.log(e.toString());
        }
        return time;
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
            ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            mRunningServices = manager.getRunningServices(Integer.MAX_VALUE);
        }

        for (ActivityManager.RunningServiceInfo info : mRunningServices) {
            if (info.service.getClassName().equals(className)){
                return true;
            }
        }

        return false;
    }

    public static void colorizeToolbar(Toolbar toolbar, int foregroundColor, int backgroundColor) {
        final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(foregroundColor, PorterDuff.Mode.MULTIPLY);

        toolbar.setTitleTextColor(foregroundColor);
        toolbar.setSubtitleTextColor(foregroundColor);
        toolbar.setBackgroundColor(backgroundColor);

        for(int i = 0; i < toolbar.getChildCount(); ++i) {
            final View view = toolbar.getChildAt(i);

            if(view instanceof ImageButton) {
                ((ImageButton)view).getDrawable().setColorFilter(colorFilter);
            } else if(view instanceof ActionMenuView) {
                for(int j = 0; j < ((ActionMenuView)view).getChildCount(); ++j) {
                    final View innerView = ((ActionMenuView)view).getChildAt(j);
                    if(innerView instanceof ActionMenuItemView) {
                        int drawablesCount = ((ActionMenuItemView)innerView).getCompoundDrawables().length;
                        for(int k = 0; k < drawablesCount; ++k) {
                            if(((ActionMenuItemView)innerView).getCompoundDrawables()[k] != null) {
                                final int finalK = k;
                                //Important to set the color filter in seperate thread,
                                //by adding it to the message queue
                                //Won't work otherwise.
                                innerView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((ActionMenuItemView) innerView).getCompoundDrawables()[finalK].setColorFilter(colorFilter);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    public static String getString(int resId) {
        return getContext().getString(resId);
    }

    public static int getColor(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    public static int getThemeColor(int id, int defaultValue){
        TypedValue value = new TypedValue();
        try{
            Context context = getContext();
            Resources.Theme theme = context.getTheme();
            if(theme != null && theme.resolveAttribute(id, value, true)) {
                if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) {
                    return value.data;
                } else if (value.type == TypedValue.TYPE_STRING) {
                    ContextCompat.getColor(context, value.resourceId);
                }
            }
        } catch(Exception e) {
            _G.log(e.toString());
        }
        return defaultValue;
    }

    public static DevicePolicyManager getDevicePolicyManager() {
        if(mDPM == null) {
            mDPM = (DevicePolicyManager)getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
        return mDPM;
    }

    public static PackageManager getPackageManager() {
        if(mPM == null) {
            mPM = getContext().getPackageManager();
        }
        return mPM;
    }

    public static EnterpriseDeviceManager getEnterpriseDeviceManager() {
        if(mEDM == null) {
            //noinspection ResourceType
            mEDM = (EnterpriseDeviceManager)getContext().getSystemService(EnterpriseDeviceManager.ENTERPRISE_POLICY_SERVICE);
        }

        return mEDM;
    }

    public static SharedPreferences getSharedPreferences(String name) {
        if(mPreferences == null) {
            mPreferences = new HashMap<>();
        }

        SharedPreferences pref = mPreferences.get(name);

        if(pref == null) {
            pref = getContext().getSharedPreferences(name, Context.MODE_PRIVATE);
            mPreferences.put(name, pref);
        }

        return pref;
    }

    public static SearchRecentSuggestions getSearchSuggestions() {
        if(mSuggestions == null) {
            mSuggestions = new SearchRecentSuggestions(getContext(), BBOOSuggestionsProvider.AUTHORITY, BBOOSuggestionsProvider.MODE);
        }
        return mSuggestions;
    }

    public static void attachContext(Context context) {
        mContext = context;
        mSuggestions = null;
        mRunningServices = null;
        mPreferences = null;
        mPM = null;
        mDPM = null;
        mEDM = null;
    }

    public static Context getContext() {
        return mContext;
    }

    // private
    private static Context mContext;
    private static SearchRecentSuggestions mSuggestions;
    private static List<ActivityManager.RunningServiceInfo> mRunningServices;
    private static Map<String, SharedPreferences> mPreferences;
    private static PackageManager mPM;
    private static DevicePolicyManager mDPM;
    private static EnterpriseDeviceManager mEDM;
}
