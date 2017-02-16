package com.weihuoya.bboo.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.weihuoya.bboo.FileContentProvider;
import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;
import com.weihuoya.bboo._P;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class HTMLViewerActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PERMISSIONS = 0;
    public static final String AndroidNameSpace = "http://schemas.android.com/apk/res/android";
    public static final String AndroidManifest = "AndroidManifest.xml";

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private String mPackageName;
    private String mManifest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_htmlviewer);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        _G.attachContext(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressBar = (ProgressBar)findViewById(R.id.load_progress_bar);

        // Call createInstance() explicitly. createInstance() is called in
        // BrowserFrame by WebView. As it is called in WebCore thread, it can
        // happen after onResume() is called. To use getInstance() in onResume,
        // createInstance() needs to be called first.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }

        mWebView = (WebView) findViewById(R.id.webview);

        // Setup callback support for title and progress bar
        mWebView.setWebChromeClient( new MyWebChromeClient() );
        // loading page
        mWebView.setWebViewClient( new MyWebViewClient() );

        // Configure the webview
        WebSettings s = mWebView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        s.setUseWideViewPort(true);
        s.setSaveFormData(false);
        s.setBlockNetworkLoads(true);
        if (Build.VERSION.SDK_INT <= 18) {
            s.setSavePassword(false);
        }
        //s.setDefaultTextEncodingName("utf-8");

        // Javascript is purposely disabled, so that nothing can be
        // automatically run.
        s.setJavaScriptEnabled(false);

        // Restore a webview if we are meant to restore
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            // Check the intent for the content to view
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // the default implementation requires each view to have an id. As the
        // browser handles the state itself and it doesn't use id for the views,
        // don't call the default implementation. Otherwise it will trigger the
        // warning like this, "couldn't save which view has focus because the
        // focused view XXX has no id".
        mWebView.saveState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        CookieSyncManager.getInstance().stopSync();
        mWebView.stopLoading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.viewer_options_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_viewer_search);
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setQuery("", false);
                searchView.clearFocus();
                return true;
            }
        });

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mWebView.findAllAsync(newText);
                return false;
            }
        });

        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebView.findNext(true);
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        SearchView searchView;

        int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.menu_viewer_search:
                searchView = (SearchView) menuItem.getActionView();
                searchView.setIconified(false);
                searchView.requestFocusFromTouch();
                break;
            case R.id.menu_viewer_save:
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    SaveStringToFile(mManifest);
                } else {
                    requestWritePermission();
                }
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }

        return true;
    }

    private void handleIntent(Intent intent) {
        String filename = "";
        Uri uri = intent.getData();
        if(uri != null) {
            String scheme = uri.getScheme();
            filename = uri.getPath();
            if(scheme.equals("file")) {
                String contentUri = FileContentProvider.BASE_URI + uri.getEncodedPath() + "?" + intent.getType();
                mWebView.loadUrl(contentUri);
            } else {
                mWebView.loadUrl(intent.getData().toString() + "?" + intent.getType());
            }
        } else {
            String pkgName = intent.getStringExtra("package");
            if(pkgName != null && !pkgName.isEmpty()) {
                Context context = null;
                filename = AndroidManifest;
                mPackageName = pkgName;
                try {
                    context = _G.getContext().createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e) {
                    _G.log(e.toString());
                }
                if(context != null) {
                    try {
                        loadManifest(context);
                    } catch (Exception e) {
                        _G.log(e.toString());
                    }
                }
            } else {
                String command = intent.getStringExtra("command");
                if(command != null && !command.isEmpty()) {
                    loadCommandLog(command);
                }
            }
        }
        setTitle(filename);
    }

    protected void loadCommandLog(String command) {
        new MyCommandExecTask().execute(command);
    }

    protected void loadManifest(Context context) throws XmlPullParserException, IOException {
        XmlResourceParser parser = context.getAssets().openXmlResourceParser(AndroidManifest);
        StringBuilder content = new StringBuilder();
        String namespace, tagName, attrName, attrValue;
        int i, eventType, attrCount, savedDepth = 1, outerDepth;

        content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        content.append(System.lineSeparator());

        while( (eventType = parser.next()) != XmlPullParser.END_DOCUMENT ) {
            if(eventType == XmlPullParser.START_TAG) {
                outerDepth = parser.getDepth();
                tagName = parser.getName();

                if(savedDepth < outerDepth) {
                    content.append('>');
                    content.append(System.lineSeparator());
                }

                for(i = 1; i < outerDepth; ++i) {
                    content.append("    ");
                }
                content.append('<');
                content.append(tagName);

                if("manifest".equals(tagName)) {
                    content.append(" xmlns:android=\"http://schemas.android.com/apk/res/android\"");
                }

                attrCount = parser.getAttributeCount();
                for(i = 0; i < attrCount; ++i) {
                    namespace = parser.getAttributeNamespace(i);
                    attrName = parser.getAttributeName(i);
                    attrValue = parser.getAttributeValue(i);

                    if("label".equals(attrName) || "description".equals(attrName)) {
                        if(attrValue != null && attrValue.matches("^@\\d+$")) {
                            int resId = Integer.parseInt(attrValue.substring(1));
                            attrValue = context.getString(resId);
                            //attrValue = new String(attrValue.getBytes("GBK"));
                            //attrValue = URLEncoder.encode(attrValue, "UTF-8");
                        }
                    } else if("configChanges".equals(attrName)) {
                        //attrValue = ConvertConfigChanges(attrValue);
                    } else if("screenOrientation".equals(attrName)) {

                    } else if("windowSoftInputMode".equals(attrName)) {

                    }

                    content.append(' ');
                    if(AndroidNameSpace.equals(namespace)) {
                        content.append("android:");
                    }
                    content.append(attrName);
                    content.append('=');
                    content.append('"');
                    content.append(attrValue);
                    content.append('"');
                }

                savedDepth = outerDepth;
            } else if(eventType == XmlPullParser.END_TAG) {
                outerDepth = parser.getDepth();
                tagName = parser.getName();
                if(savedDepth == outerDepth) {
                    content.append('/');
                } else {
                    for(i = 1; i < outerDepth; ++i) {
                        content.append("    ");
                    }
                    content.append('<');
                    content.append('/');
                    content.append(tagName);
                }
                content.append('>');
                content.append(System.lineSeparator());
            }
        }

        mManifest = content.toString();
        //mWebView.loadData(mManifest, "text/html", "UTF-8");
        //mWebView.loadDataWithBaseURL(null, mManifest, "text/html", "UTF-8", null);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
        //    String base64 = Base64.encodeToString(mManifest.getBytes(), Base64.DEFAULT);
        //    mWebView.loadData(base64, "text/xml; charset=utf-8", "base64");
        //} else {
            mWebView.loadData(mManifest, "text/xml; charset=utf-8", "utf-8");
        //}
    }

    protected String ConvertConfigChanges(String value) {
        final long configChanges = Integer.decode(value);
        StringBuilder result = new StringBuilder();

        Map<Integer, String> valueMap = new ArrayMap<>();
        valueMap.put(ActivityInfo.CONFIG_MCC, "mcc");
        valueMap.put(ActivityInfo.CONFIG_MNC, "mnc");
        valueMap.put(ActivityInfo.CONFIG_LOCALE, "locale");
        valueMap.put(ActivityInfo.CONFIG_TOUCHSCREEN, "touchscreen");
        valueMap.put(ActivityInfo.CONFIG_KEYBOARD, "keyboard");
        valueMap.put(ActivityInfo.CONFIG_KEYBOARD_HIDDEN, "keyboardHidden");
        valueMap.put(ActivityInfo.CONFIG_NAVIGATION, "navigation");
        valueMap.put(ActivityInfo.CONFIG_SCREEN_LAYOUT, "screenLayout");
        valueMap.put(ActivityInfo.CONFIG_FONT_SCALE, "fontScale");
        valueMap.put(ActivityInfo.CONFIG_UI_MODE, "uiMode");
        valueMap.put(ActivityInfo.CONFIG_ORIENTATION, "orientation");
        valueMap.put(ActivityInfo.CONFIG_SCREEN_SIZE, "screenSize");
        valueMap.put(ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE, "smallestScreenSize");

        for(Map.Entry<Integer, String> entry : valueMap.entrySet()) {
            if((configChanges & entry.getKey()) > 0) {
                result.append(entry.getValue());
                result.append('|');
            }
        }

        if(result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
        }

        return result.toString();
    }

    protected void SaveStringToFile(String content) {
        //boolean sdcardAvailable = false;
        boolean sdcardWriteable = false;
        String sdcardState = Environment.getExternalStorageState();

        if(Environment.MEDIA_MOUNTED.equals(sdcardState)) {
            //sdcardAvailable = true;
            sdcardWriteable = true;
        } else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(sdcardState)) {
            //sdcardAvailable = true;
            sdcardWriteable = false;
        }

        if(sdcardWriteable) {
            File sdcardPath = Environment.getExternalStorageDirectory();
            String outputFile = sdcardPath.getAbsolutePath() +
                    File.separator + "Download" +
                    File.separator + mPackageName + "_" + AndroidManifest;
            try {
                FileWriter writer = new FileWriter(new File(outputFile));
                writer.write(content);
                writer.flush();
                writer.close();
                Toast.makeText(this, "Save to: " + outputFile, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                _G.log(e.toString());
            }
        }
    }

    protected void requestWritePermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                _G.log("need permission: WRITE_EXTERNAL_STORAGE");
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SaveStringToFile(mManifest);
            }
        }
    }

    class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onReceivedTitle(WebView view, String title) {
            //HTMLViewerActivity.this.setTitle(title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress*100);
            if (newProgress == 100) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.getInstance().sync();
                } else {
                    CookieManager.getInstance().flush();
                }
            }
        }
    }

    class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            _G.log("onPageStarted");
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mProgressBar.setVisibility(View.GONE);
            _G.log("onPageFinished");
        }
    }

    class MyCommandExecTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            String command = params[0];
            StringBuilder content = new StringBuilder();

            try {
                String line = null;
                Process p = Runtime.getRuntime().exec(command);
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while((line = input.readLine()) != null) {
                    content.append(line);
                    content.append('\n');
                }
                input.close();
            } catch (IOException e) {
                _G.log(e.toString());
            }

            return content.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            mManifest = result;
            mWebView.loadData(result, "text/plain; charset=utf-8", "utf-8");
        }
    }

}
