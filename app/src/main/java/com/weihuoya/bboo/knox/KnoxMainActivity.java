package com.weihuoya.bboo.knox;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.tool.util.StringUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.TextView;

import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;

import java.util.ArrayList;
import java.util.List;

public class KnoxMainActivity extends AppCompatActivity {

    private ProgressDialog mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_knox_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.knox));

        TextView knoxInfo = (TextView) findViewById(R.id.knox_info);
        knoxInfo.setText(getKnoxInfo());
    }

    protected String extractVersion(String text) {
        if(text != null) {
            String[] tokens = text.split("_");
            List<String> result = new ArrayList<String>();
            for (String token : tokens) {
                if(TextUtils.isDigitsOnly(token)) {
                    result.add(token);
                }
            }
            return TextUtils.join(".", result);
        } else {
            return "";
        }
    }

    protected String getKnoxInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("Warranty: ");
        sb.append(KnoxManager.getWarrantyBit());
        sb.append("\n");
        sb.append("Standard: ");
        sb.append(extractVersion(KnoxManager.getStandardVersion()));
        sb.append("\n");
        sb.append("Premium: ");
        sb.append(extractVersion(KnoxManager.getPremiumVersion()));

        return sb.toString();
    }

    protected boolean activateStandardKnox(String klms) {
        if(klms != null && mDialog == null) {
            mDialog = new ProgressDialog(this);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setMessage(getString(R.string.app_elm_activating));
            mDialog.show();
            return KnoxManager.activateStandardLicense(klms);
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _G.log("$$$ onActivityResult: " + requestCode + ", " + resultCode);
        KnoxManager.onActivityResult(requestCode, resultCode, data);
    }
}
