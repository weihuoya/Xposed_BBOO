package com.weihuoya.bboo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
//import java.net.URL;
import java.util.Enumeration;

import dalvik.system.DexFile;


//import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findAPILevel();
        findCandidateComponents();
    }

    protected void findCandidateComponents() {
        String packageName = this.getClass().getPackage().getName();
        //String sourceDir = this.getApplicationInfo().sourceDir;
        String sourceDir = this.getPackageCodePath();

        try {
            DexFile dexFile = new DexFile(sourceDir);
            Enumeration<String> entries = dexFile.entries();

            while(entries.hasMoreElements()) {
                String entry = entries.nextElement();
                if(entry.contains(packageName) && !entry.contains("$")) {
                    Log.d("zhangwei", entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void findAPILevel() {
        try  {
            Class<?> mClassType = Class.forName("android.os.SystemProperties");
            Method mGetIntMethod = mClassType.getDeclaredMethod("getInt", String.class, int.class);
            mGetIntMethod.setAccessible(true);
            Integer level = (Integer)mGetIntMethod.invoke(null, "ro.build.version.sdk", 14);
            Log.d("zhangwei", "api level: " + level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
