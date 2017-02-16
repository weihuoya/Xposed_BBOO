package com.weihuoya.bboo.root;

import com.weihuoya.bboo._G;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Created by zhangwei on 2016/8/7.
 */
public class RootManager {

    public static boolean isRootedDevice() {
        return getSuPath() != null;
    }

    public static String getSuPath() {
        final String[] suPaths ={
                "/data/local/",
                "/data/local/bin/",
                "/data/local/xbin/",
                "/sbin/",
                "/system/bin/",
                "/system/bin/.ext/",
                "/system/bin/failsafe/",
                "/system/sd/xbin/",
                "/system/usr/we-need-root/",
                "/system/xbin/"
        };

        String result = "";
        String filename = "su";

        for (String path : suPaths) {
            String completePath = path + filename;
            File f = new File(completePath);
            if (f.exists()) {
                result = completePath;
                break;
            }
        }

        return result;
    }

    public static String getSuVersion() {
        String value = "";
        try {
            Process p = Runtime.getRuntime().exec("su -v");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((value = input.readLine()) != null) {
                if(value.length() > 0 && Character.isDigit(value.charAt(0))) {
                    break;
                }
            }
            input.close();
        } catch (IOException e) {
            _G.log(e.toString());
        }
        return value;
    }

}
