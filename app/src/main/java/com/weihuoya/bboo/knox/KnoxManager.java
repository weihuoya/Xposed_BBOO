package com.weihuoya.bboo.knox;

// knox standard sdk
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.EnterpriseDeviceManager;
import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

// knox premium sdk
import com.sec.enterprise.knox.EnterpriseKnoxManager;
import com.sec.enterprise.knox.license.KnoxEnterpriseLicenseManager;

import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;

/**
 * Created by zhangwei on 2016/8/7.
 */
public class KnoxManager {
    public static final int REQUEST_ADMIN_ENABLE = 1001;
    public static final int REQUEST_ADMIN_DISABLE = 1002;
    public static final int REQUEST_ELM_ACTIVATED = 1003;

    public static final String StandardKLMS = "8D5AE132CAC616A3C92EAEE53371C4B326028DDABCBE0816A1C7C3F61E42FDEA1B9DC8C7D0BE770652A2E628328E79CF3CA7DC8E5734A969708BDC02849C6F68";

    private static final String PREF_SAMSUNG_KNOX = "PREF_SAMSUNG_KNOX";
    private static ProgressDialog progressDialog = null;

    private static DevicePolicyManager mDPM = null;

    private static EnterpriseDeviceManager mEDM = null;
    private static EnterpriseLicenseManager mELM = null;

    private static EnterpriseKnoxManager mEKM = null;
    private static KnoxEnterpriseLicenseManager mKLM = null;

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case KnoxManager.REQUEST_ADMIN_ENABLE:
                break;
            case KnoxManager.REQUEST_ADMIN_DISABLE:
                break;
            case KnoxManager.REQUEST_ELM_ACTIVATED:
                break;
        }
    }

    public static String getWarrantyBit() {
        return _G.getSystemProperty("ro.boot.warranty_bit", "");
    }

    public static Context getContext() {
        return _G.getContext();
    }

    public static DevicePolicyManager getDevicePolicyManager() {
        if(mDPM == null) {
            mDPM = (DevicePolicyManager) getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
        return mDPM;
    }

    public static EnterpriseDeviceManager getEnterpriseDeviceManager() {
        if(mEDM == null) {
            //noinspection ResourceType
            mEDM = (EnterpriseDeviceManager) getContext().getSystemService(EnterpriseDeviceManager.ENTERPRISE_POLICY_SERVICE);
        }
        return mEDM;
    }

    public static EnterpriseLicenseManager getEnterpriseLicenseManager() {
        if(mELM == null) {
            mELM = EnterpriseLicenseManager.getInstance(getContext());
        }
        return mELM;
    }

    public static EnterpriseKnoxManager getEnterpriseKnoxManager() {
        if(isKnoxEnabled() && mEKM == null) {
            mEKM = EnterpriseKnoxManager.getInstance();
        }
        return mEKM;
    }

    public static KnoxEnterpriseLicenseManager getKnoxEnterpriseLicenseManager() {
        if(mKLM == null) {
            mKLM = KnoxEnterpriseLicenseManager.getInstance(getContext());
        }
        return mKLM;
    }

    public static boolean isKnoxEnabled() {
        return getEnterpriseDeviceManager() != null;
    }

    public static String getStandardVersion() {
        if(getEnterpriseDeviceManager() != null) {
            return getEnterpriseDeviceManager().getEnterpriseSdkVer().toString();
        } else {
            return null;
        }
    }

    public static String getPremiumVersion() {
        if(getEnterpriseKnoxManager() != null) {
            return getEnterpriseKnoxManager().getVersion().toString();
        } else {
            return null;
        }
    }

    public static boolean activateStandardLicense(String klms) {
        try {
            EnterpriseLicenseManager manager = getEnterpriseLicenseManager();
            manager.activateLicense(klms, getContext().getPackageName());
        } catch (RuntimeException e) {
            Toast.makeText(getContext(), "standard license manager is missing!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean activatePremiumLicense(String klms) {
        try {
            KnoxEnterpriseLicenseManager manager = getKnoxEnterpriseLicenseManager();
            manager.activateLicense(klms, getContext().getPackageName());
        } catch (RuntimeException e) {
            Toast.makeText(getContext(), "premium license manager is missing!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean isStandardLicenseActivate(String klms) {
        String value = _G.getSharedPreferences(PREF_SAMSUNG_KNOX).getString("standard", null);
        return value != null && value.equals(klms);
    }

    public static boolean isPremiumLicenseActivate(String klms) {
        String value = _G.getSharedPreferences(PREF_SAMSUNG_KNOX).getString("premium", null);
        return value != null && value.equals(klms);
    }

    public static void setLicenseActivate(boolean status) {
        if(status) {
            activateAdmin();
        }

        if(progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public static boolean isAdminActive() {
        DevicePolicyManager manager = getDevicePolicyManager();
        ComponentName component = new ComponentName(getContext(), KnoxAdminReceiver.class);
        return manager.isAdminActive(component);
    }

    public static boolean activateAdmin() {
        Context context = getContext();
        DevicePolicyManager manager = getDevicePolicyManager();
        ComponentName component = new ComponentName(context, KnoxAdminReceiver.class);

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
        ((Activity) context).startActivityForResult(intent, REQUEST_ADMIN_ENABLE);

        _G.log("activateAdminForKnox: startActivityForResult");

        return true;
    }

    public static boolean deactivateAdmin() {
        Context context = getContext();
        DevicePolicyManager manager = getDevicePolicyManager();
        ComponentName component = new ComponentName(context, KnoxAdminReceiver.class);

        if(manager.isAdminActive(component)) {
            manager.removeActiveAdmin(component);
            return true;
        }

        return false;
    }

    public static void setAdminActivate(boolean status) {
        if(progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public static boolean setApplicationEnabled(String packageName, boolean enabled) {
        boolean result = false;
        EnterpriseDeviceManager manager = getEnterpriseDeviceManager();

        try {
            if(enabled) {
                result = manager.getApplicationPolicy().setDisableApplication(packageName);
            } else {
                result = manager.getApplicationPolicy().setEnableApplication(packageName);
            }
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "device admin is not activated!", Toast.LENGTH_SHORT).show();
        }

        return result;
    }
}
