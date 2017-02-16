package com.weihuoya.bboo.xposed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.weihuoya.bboo.R;

import de.robv.android.xposed.XposedBridge;

public class XposedMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.xposed));

        TextView xposedInfo = (TextView) findViewById(R.id.xposed_info);
        xposedInfo.setText(getXposedInfo());
    }

    protected String getXposedInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("AppProcess: ");
        sb.append(XposedBridge.getXposedVersion());
        //sb.append(XposedManager.getInstalledAppProcessVersion());
        //sb.append("\n");
        //sb.append("JarLibrary: ");
        //sb.append(XposedManager.getJarInstalledVersion());

        return sb.toString();
    }
}
