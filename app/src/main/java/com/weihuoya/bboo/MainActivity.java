package com.weihuoya.bboo;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import dalvik.system.DexFile;

import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;

    public class AppListLoadTask extends AsyncTask<Void, Void, List<AppItemModel>> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage(getString(R.string.app_loading));
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected List<AppItemModel> doInBackground(Void... params) {
            return LoadApplicationList();
        }

        @Override
        protected void onPostExecute(List<AppItemModel> result) {
            AppListAdapter adapter = (AppListAdapter) mRecyclerView.getAdapter();
            adapter.setSource(result);
            _G.ApplicationList = result;
            dialog.dismiss();
        }

        private List<AppItemModel> LoadApplicationList() {
            int progress = 0;
            PackageManager pm = _G.getPackageManager();
            List<PackageInfo> pkglist = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            ArrayList<AppItemModel> appList = new ArrayList<AppItemModel>();

            dialog.setMax(pkglist.size());
            for (PackageInfo pkg : pkglist) {
                dialog.setProgress(++progress);
                //if (pkg.applicationInfo == null || (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                if (pkg.applicationInfo == null) {
                    continue;
                }
                appList.add(new AppItemModel(pkg));
            }

            Collections.sort(appList);
            return appList;
        }
    }

    public class AppItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @Bind(R.id.app_icon)
        protected ImageView appIcon;
        @Bind(R.id.app_name)
        protected TextView appName;
        @Bind(R.id.app_package)
        protected TextView appPackage;
        @Bind(R.id.app_desc)
        protected TextView appDesc;
        @Bind(R.id.xposed_icon)
        protected ImageView xposedIcon;
        @Bind(R.id.blocked_icon)
        protected ImageView blockedIcon;
        @Bind(R.id.system_icon)
        protected ImageView systemIcon;

        public AppItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);

            Intent intent = new Intent(MainActivity.this, AppDetailActivity.class);
            intent.putExtra("package", appPackage.getText());
            ActivityCompat.startActivity(MainActivity.this, intent, null);
        }

        public void bindModel(AppItemModel model) {
            appIcon.setImageDrawable(model.getIcon());
            appName.setText(model.getName());
            appPackage.setText(model.getPackageName());
            appDesc.setText(model.getDescription());

            xposedIcon.setVisibility(model.isXposedModule() ? View.VISIBLE : View.INVISIBLE);
            blockedIcon.setVisibility(model.isBlocked() ? View.VISIBLE : View.INVISIBLE);
            systemIcon.setVisibility(model.isSystemApplication() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public class AppListAdapter extends RecyclerView.Adapter<AppItemViewHolder> {
        private List<AppItemModel> mSource;
        private List<AppItemModel> mDataset;

        public AppListAdapter() {
            mDataset = new ArrayList<AppItemModel>();
        }

        @Override
        public AppItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
            return new AppItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AppItemViewHolder holder, int position) {
            holder.bindModel(mDataset.get(position));
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public void setSource(List<AppItemModel> apps) {
            mSource = apps;
            setQuery(null);
        }

        public void setQuery(CharSequence query) {
            int queryLength = query == null ? 0 : TextUtils.getTrimmedLength(query);

            mDataset.clear();
            for(AppItemModel model : mSource) {
                if(queryLength == 0 || model.getName().contains(query) || model.getPackageName().contains(query)) {
                    mDataset.add(model);
                }
            }

            notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _G.attachContext(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listApps);
        if(recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new AppListAdapter());
        }
        mRecyclerView = recyclerView;

        handleIntent(getIntent());
        setHookPackage();
        findAPILevel();
        findCandidateComponents();
        handleLocation();

        new AppListLoadTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.searchApp);
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setQuery("", false);
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
                AppListAdapter adapter = (AppListAdapter) mRecyclerView.getAdapter();
                adapter.setQuery(newText);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.searchApp) {
            SearchView searchView = (SearchView) menuItem.getActionView();
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();
        } else if(itemId == R.id.aboutMe) {
            Toast.makeText(this, "About Me", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            AppListAdapter adapter = (AppListAdapter)mRecyclerView.getAdapter();
            adapter.setQuery(query);
            mRecyclerView.scrollToPosition(0);
            _G.getSearchSuggestions().saveRecentQuery(query, null);
            _G.log("search query: " + query);
        }
    }

    protected void findCandidateComponents() {
        String packageName = this.getClass().getPackage().getName();
        //String sourceDir = this.getApplicationInfo().sourceDir;
        String sourceDir = this.getPackageCodePath();

        try {
            DexFile dexFile = new DexFile(sourceDir);
            Enumeration<String> entries = dexFile.entries();

            while (entries.hasMoreElements()) {
                String entry = entries.nextElement();
                if (entry.contains(packageName) && !entry.contains("$")) {
                    _G.log(entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setHookPackage() {
        final String QQPackageName = "com.tencent.mobileqq";
        final String WechatPackageName = "com.tencent.mm";
        final String ZhihuPackageName = "com.zhihu.android";
        final String DoubanPackageName = "com.douban.frodo";
        final String AlipayPackageName = "com.eg.android.AlipayGphone";
        final String WandoujiaPackageName = "com.wandoujia.phoenix2";

        SharedPreferences.Editor preferencesEditor = _G.getIncludePackagePrefs().edit();
        preferencesEditor.putBoolean(QQPackageName, true);
        preferencesEditor.putBoolean(WechatPackageName, true);
        preferencesEditor.putBoolean(ZhihuPackageName, true);
        preferencesEditor.putBoolean(DoubanPackageName, true);
        preferencesEditor.putBoolean(AlipayPackageName, true);
        preferencesEditor.putBoolean(WandoujiaPackageName, true);
        preferencesEditor.apply();
    }

    protected void findAPILevel() {
        Locale defaultLocale = Locale.getDefault();
        String country = defaultLocale.getCountry();
        String language = defaultLocale.getLanguage();
        _G.log("country: " + country + ", language: " + language);

        Field[] buildFields = android.os.Build.class.getDeclaredFields();
        for (Field field : buildFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                Object value = null;
                try {
                    value = field.get(field.getType().newInstance());
                } catch (Exception e) {
                    _G.log(e.getMessage());
                }
                _G.log("android.os.Build." + field.getName() + " = " + String.valueOf(value));
            }
        }

        try {
            Class<?> mClassType = Class.forName("android.os.SystemProperties");
            Method mGetIntMethod = mClassType.getDeclaredMethod("getInt", String.class, int.class);
            mGetIntMethod.setAccessible(true);
            Integer level = (Integer) mGetIntMethod.invoke(null, "ro.build.version.sdk", 14);
            _G.log("api level: " + level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void handleLocation() {
        String url = "http://ip-api.com/json";
        BasicRequestQueue.getInstance().getJson(url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String countryCode = "";
                try {
                    if("success".equals(response.getString("status"))) {
                        countryCode = response.getString("countryCode");
                    } else {
                        _G.log("ip-api message: " + response.getString("message"));
                    }
                } catch (Exception e) {
                    _G.log(e.getMessage());
                }
                _G.log("ip-api countryCode: " + countryCode);
            }
        });
    }
}
