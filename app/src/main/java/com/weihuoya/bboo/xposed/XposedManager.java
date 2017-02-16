package com.weihuoya.bboo.xposed;

import com.weihuoya.bboo._G;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by zhangwei on 2016/8/7.
 */
public class XposedManager {

    // xposed module hook method
    public static boolean isXposedModuleEnabled() {
        return false;
    }

    // xposed module hook method
    public static List<String> getLoadedPackages() {
        return null;
    }

    // xposed module hook method
    public static List<String> getQueriedIntents() {
        return null;
    }

    public static int getInstalledAppProcessVersion() {
        int version = 0;
        FileInputStream input = null;

        try {
            input = new FileInputStream("/system/bin/app_process");
        } catch (FileNotFoundException e) {
            _G.log(e.toString());
        }

        if(input != null) {
            String line = null;
            Pattern PATTERN_APP_PROCESS_VERSION = Pattern.compile(".*with Xposed support \\(version (.+)\\).*");
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            try {
                while ((line = reader.readLine()) != null) {
                    if (line.contains("getXposedVersion")) {
                        _G.log(":: getInstalledAppProcessVersion");
                        _G.log(line);
                        Matcher m = PATTERN_APP_PROCESS_VERSION.matcher(line);
                        if (m.find()) {
                            //version = extractIntPart(m.group(1));
                            //reader.close();
                            break;
                        }
                    }
                }
                input.close();
            } catch (IOException e) {
                _G.log(e.toString());
            }
        }

        return version;
    }

    public static int getJarInstalledVersion() {
        final String BASE_DIR = "/data/data/de.robv.android.xposed.installer/";
        final String JAR_PATH = BASE_DIR + "bin/XposedBridge.jar";
        final String JAR_PATH_NEWVERSION = JAR_PATH + ".newversion";

        int version = 0;
        FileInputStream input = null;

        try {
            if(new File(JAR_PATH_NEWVERSION).exists()) {
                input = new FileInputStream(JAR_PATH_NEWVERSION);
            } else {
                input = new FileInputStream(JAR_PATH);
            }
        } catch (FileNotFoundException e) {
            _G.log(e.toString());
        }

        if(input != null) {
            try {
                JarEntry entry = null;
                JarInputStream jis = new JarInputStream(input);
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals("assets/VERSION")) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(jis));
                        version = extractIntPart(reader.readLine());
                        reader.close();
                        break;
                    }
                }
                input.close();
                jis.close();
            } catch (IOException e) {
                _G.log(e.toString());
            }
        }

        return version;
    }

    private static int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }
}
