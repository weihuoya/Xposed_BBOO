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

import butterknife.Bind;
import butterknife.ButterKnife;
import dalvik.system.DexFile;

import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;

//import java.net.URL;
//import java.lang.reflect.InvocationTargetException;


// http://hukai.me/android-training-course-in-chinese/animations/screen-slide.html
// https://github.com/codepath/android_guides/wiki/Using-the-RecyclerView
// http://stackoverflow.com/questions/21585326/implementing-searchview-in-action-bar

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private SharedPreferences mPreferences;
    private SearchRecentSuggestions mSuggestions;

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
            mRecyclerView.setAdapter(new AppListAdapter(result));
            dialog.dismiss();
        }

        private List<AppItemModel> LoadApplicationList() {
            int progress = 0;
            PackageManager pm = getPackageManager();
            List<PackageInfo> pkglist = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            ArrayList<AppItemModel> appList = new ArrayList<AppItemModel>();

            dialog.setMax(pkglist.size());
            for (PackageInfo pkg : pkglist) {
                dialog.setProgress(++progress);
                if (pkg.applicationInfo == null || (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    continue;
                }
                appList.add(new AppItemModel(pkg));
            }

            Collections.sort(appList);
            return appList;
        }
    }

    public class SearchViewEventListener implements SearchView.OnSuggestionListener, SearchView.OnQueryTextListener {

        private SearchView mSearchView;

        public SearchViewEventListener(SearchView searchView) {
            searchView.setOnQueryTextListener(this);
            searchView.setOnSuggestionListener(this);
            mSearchView = searchView;
        }

        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            String suggestion = getSuggestion(position);
            mSearchView.setQuery(suggestion, true);
            return false;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }

        private String getSuggestion(int position) {
            Cursor cursor = (Cursor)mSearchView.getSuggestionsAdapter().getItem(position);
            return cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        }
    }

    public class AppItemModel implements Comparable<AppItemModel> {
        public String appName;
        public String appPackage;
        public String appVersion;
        public Drawable appIcon;
        public String appStatus;
        public long appSize;

        public AppItemModel(PackageInfo pkg) {
            PackageManager pm = getPackageManager();
            boolean blocked = mPreferences.getBoolean(pkg.packageName, false);

            appName = pm.getApplicationLabel(pkg.applicationInfo).toString();
            appPackage = pkg.packageName;
            appVersion = pkg.versionName;
            appIcon = pm.getApplicationIcon(pkg.applicationInfo);
            appStatus = getString(blocked ? R.string.app_status_blocked : R.string.app_status_normal);
            appSize = 0;

            try {
                Method getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
                getPackageSizeInfo.invoke(pm, pkg.packageName, new IPackageStatsObserver.Stub() {
                    @Override
                    public void onGetStatsCompleted(android.content.pm.PackageStats pStats, boolean succeeded) throws android.os.RemoteException {
                        appSize = pStats.codeSize;
                    }
                });
            } catch (Exception e) {
                Log.d("zhangwei", pkg.packageName + ": " + e.getMessage());
            }
        }

        public int compareTo(@NonNull AppItemModel that) {
            if (this.appName == null) {
                return -1;
            } else if (that.appName == null) {
                return 1;
            } else {
                return this.appName.toUpperCase().compareTo(that.appName.toUpperCase());
            }
        }
    }

    public class AppItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @Bind(R.id.app_icon)
        public ImageView appIcon;
        @Bind(R.id.app_name)
        public TextView appName;
        @Bind(R.id.app_package)
        public TextView appPackage;
        @Bind(R.id.app_desc)
        public TextView appDesc;
        @Bind(R.id.app_status)
        public TextView appStatus;

        public AppItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
            Log.d("zhangwei", "ViewHolder: " + holder.getAdapterPosition());
        }
    }

    public class AppListAdapter extends RecyclerView.Adapter<AppItemViewHolder> {
        private List<AppItemModel> mSource;
        private List<AppItemModel> mDataset;
        //private Drawable mDefaultIcon;

        public AppListAdapter(List<AppItemModel> apps) {
            mSource = apps;
            mDataset = new ArrayList<AppItemModel>();
            setQuery(null, false);
        }

        @Override
        public AppItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
            return new AppItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AppItemViewHolder holder, int position) {
            AppItemModel model = mDataset.get(position);

            holder.appIcon.setImageDrawable(model.appIcon);
            holder.appName.setText(String.valueOf(model.appName));
            holder.appPackage.setText(String.valueOf(model.appPackage));
            holder.appDesc.setText(String.format("v%s  %s", model.appVersion, formatDiskSize(model.appSize)));
            holder.appStatus.setText(String.valueOf(model.appStatus));
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public void setQuery(CharSequence query, boolean submit) {
            int queryLength = query == null ? 0 : TextUtils.getTrimmedLength(query);

            mDataset.clear();
            for(AppItemModel model : mSource) {
                if(queryLength == 0 || model.appName.contains(query) || model.appPackage.contains(query)) {
                    mDataset.add(model);
                }
            }

            if(submit) {
                notifyDataSetChanged();
            }
        }

        private String formatDiskSize(long size) {
            String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
            double value = size;
            double base = 1024.0f;
            int scale = 0;

            while(value > base && (scale + 1) < units.length) {
                value /= base;
                scale += 1;
            }

            return new java.text.DecimalFormat("#.##").format(value) + units[scale];
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listApps);
        if(recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        mRecyclerView = recyclerView;
        mPreferences = getSharedPreferences("IncludedPackage", MODE_PRIVATE);
        mSuggestions = new SearchRecentSuggestions(this,
                BBOOSuggestionsProvider.AUTHORITY, BBOOSuggestionsProvider.MODE);

        handleIntent(getIntent());

        setHookPackage();
        findAPILevel();
        findCandidateComponents();

        new AppListLoadTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);


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
                adapter.setQuery(newText, true);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.searchApp) {
            SearchView searchView = (SearchView) menuItem.getActionView();
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();
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
            adapter.setQuery(query, true);
            mRecyclerView.scrollToPosition(0);
            mSuggestions.saveRecentQuery(query, null);
            Log.d("zhangwei", "search query: " + query);
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
                    Log.d("zhangwei", entry);
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

        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.putBoolean(QQPackageName, true);
        preferencesEditor.putBoolean(WechatPackageName, true);
        preferencesEditor.putBoolean(ZhihuPackageName, true);
        preferencesEditor.putBoolean(DoubanPackageName, true);
        preferencesEditor.putBoolean(AlipayPackageName, true);
        preferencesEditor.putBoolean(WandoujiaPackageName, true);
        preferencesEditor.apply();
    }

    protected void findAPILevel() {
        Field[] buildFields = android.os.Build.class.getDeclaredFields();
        for (Field field : buildFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                Object value = null;
                try {
                    value = field.get(field.getType().newInstance());
                } catch (Exception e) {
                    Log.d("zhangwei", e.getMessage());
                }
                Log.d("zhangwei", "android.os.Build." + field.getName() + " = " + String.valueOf(value));
            }
        }

        try {
            Class<?> mClassType = Class.forName("android.os.SystemProperties");
            Method mGetIntMethod = mClassType.getDeclaredMethod("getInt", String.class, int.class);
            mGetIntMethod.setAccessible(true);
            Integer level = (Integer) mGetIntMethod.invoke(null, "ro.build.version.sdk", 14);
            Log.d("zhangwei", "api level: " + level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
