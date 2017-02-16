package com.weihuoya.bboo.root;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.weihuoya.bboo.R;

public class RootMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.root));

        TextView rootInfo = (TextView) findViewById(R.id.root_info);
        rootInfo.setText(getRootInfo());
    }

    protected String getRootInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("Path: ");
        sb.append(RootManager.getSuPath());
        sb.append("\n");
        sb.append("Version: ");
        sb.append(RootManager.getSuVersion());

        return sb.toString();
    }
}
