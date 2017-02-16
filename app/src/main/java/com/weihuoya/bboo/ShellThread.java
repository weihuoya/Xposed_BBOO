package com.weihuoya.bboo;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangwei1 on 2016/7/26.
 */
public class ShellThread extends Thread {
    private String mCmd;
    private Process mShell;
    private BufferedReader mInputStream;
    private BufferedReader mErrorStream;
    private OutputStreamWriter mOutputStream;
    private boolean mShellRunning;
    private List<String> mCommands;

    public ShellThread(boolean isRoot) {
        if(isRoot) {
            mCmd = "su";
        } else {
            mCmd = "/system/bin/sh";
        }

        mCommands = new ArrayList<>();
        mShellRunning = false;
    }

    public void runCommand(String cmd) {
        if(!TextUtils.isEmpty(cmd) && mShellRunning) {
            synchronized (mCommands) {
                mCommands.add(cmd);
            }
        }
    }

    @Override
    public void run() {
        Process shell = null;
        BufferedReader inputStream = null;
        BufferedReader errorStream = null;
        OutputStreamWriter outputStream = null;

        try {
            shell = Runtime.getRuntime().exec(mCmd);
        } catch (IOException e) {
            _G.log(e.toString());
            return;
        }

        if(mShell != null) {
            try {
                inputStream = new BufferedReader(new InputStreamReader(shell.getInputStream(), "UTF-8"));
                errorStream = new BufferedReader(new InputStreamReader(shell.getErrorStream(), "UTF-8"));
                outputStream = new OutputStreamWriter(shell.getOutputStream(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                _G.log(e.toString());
                return;
            }
        }

        if(outputStream != null) {
            int status = 0;
            int tokenCount = 0;
            String line = null;
            Pattern tokenPattern = Pattern.compile("^\\{(.*?)\\}$");

            try {
                outputStream.write("echo {start}");
                outputStream.flush();
            } catch (IOException e) {
                _G.log(e.toString());
                return;
            }

            while(status == 0) {
                try {
                    line = inputStream.readLine();
                } catch (IOException e) {
                    _G.log(e.toString());
                    status = -1;
                }

                if(TextUtils.isEmpty(line)) {
                    try {
                        line = errorStream.readLine();
                    } catch (IOException e) {
                        _G.log(e.toString());
                        status = -1;
                    }

                    if(TextUtils.isEmpty(line)) {
                        String cmd = null;
                        synchronized (mCommands) {
                            if(mCommands.size() > 0) {
                                cmd = mCommands.get(0);
                                mCommands.remove(0);
                            }
                        }
                        if(cmd != null) {
                            try {
                                outputStream.write(cmd);
                                outputStream.write("\necho {" + tokenCount + "}\n");
                                tokenCount += 1;
                            } catch (IOException e) {
                                _G.log(e.toString());
                                status = -1;
                            }
                        }
                    } else {
                        _G.log("shell err: " + line);
                    }
                } else {
                    Matcher matcher = tokenPattern.matcher(line);
                    if(matcher.find()) {
                        String token = matcher.group();
                        if(TextUtils.isEmpty(token)) {
                            // nothing
                        } else if("start".equals(token)) {
                            mShellRunning = true;
                            _G.log("start shell");
                        } else {
                            int count = Integer.parseInt(token);
                            _G.log("token count: " + count);
                        }
                    } else {
                        _G.log("shell log: " + line);
                    }
                }
            }
            mShellRunning = false;
        }
    }
}
