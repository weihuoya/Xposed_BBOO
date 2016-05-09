package com.weihuoya.bboo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class AppDetailActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private AppItemModel mAppModel;

    public class AppInfoLoadTask extends AsyncTask<String, Void, AppItemModel> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(AppDetailActivity.this);
            dialog.setMessage(getString(R.string.app_loading));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected AppItemModel doInBackground(String... params) {
            AppItemModel target = null;
            PackageInfo pkg = null;
            PackageManager pm = _G.getPackageManager();
            try {
                pkg = pm.getPackageInfo(params[0], 0xFFFFFFFF);
            } catch (Exception e) {
                _G.log(e.getMessage());
            }

            if(pkg != null) {
                target = new AppItemModel(pkg);
                if(_G.ApplicationList != null) {
                    ListIterator<AppItemModel> iter = _G.ApplicationList.listIterator();
                    while(iter.hasNext()) {
                        AppItemModel model = (AppItemModel)iter.next();
                        if(model.getPackageName().equals(pkg.packageName)) {
                            iter.remove();
                            iter.add(target);
                            break;
                        }
                    }
                }
            }

            return target;
        }

        @Override
        protected void onPostExecute(AppItemModel result) {
            mAppModel = result;
            dialog.dismiss();
        }
    }


    public class ActionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Bundle mModel;

        public ActionViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int actionId = mModel.getInt("text", 0);
            if(actionId > 0) {
                final AppItemModel appModel = mAppModel;
                Intent intent = new Intent(AppDetailActivity.this, ActionListActivity.class);
                intent.putExtra("package", appModel.getPackageName());
                intent.putExtra("action", actionId);
                ActivityCompat.startActivity(AppDetailActivity.this, intent, null);
            }
        }

        public void bindModel(Bundle model) {
            int viewType = model.getInt("type", 0);
            if(viewType == R.layout.app_action_item) {
                bindItemModel(model);
            } else if(viewType == R.layout.app_detail_header) {
                bindHeaderModel(model);
            } else if(viewType == R.layout.app_size_item) {
                bindSizeModel(model);
            }
            mModel = model;
        }

        private void bindHeaderModel(Bundle model) {
            final AppItemModel appModel = mAppModel;
            ImageView iconView = (ImageView)itemView.findViewById(R.id.app_icon);
            TextView nameView = (TextView)itemView.findViewById(R.id.app_name);
            TextView packageView = (TextView)itemView.findViewById(R.id.app_package);
            TextView descriptionView = (TextView)itemView.findViewById(R.id.app_desc);

            ImageView blockedView = (ImageView)itemView.findViewById(R.id.blocked_icon);
            ImageView xposedView = (ImageView)itemView.findViewById(R.id.xposed_icon);
            ImageView systemView = (ImageView)itemView.findViewById(R.id.system_icon);

            iconView.setImageDrawable(appModel.getIcon());
            nameView.setText(appModel.getName());
            packageView.setText(appModel.getPackageName());
            descriptionView.setText(appModel.getDescription());

            blockedView.setVisibility(appModel.isBlocked() ? View.VISIBLE : View.INVISIBLE);
            xposedView.setVisibility(appModel.isXposedModule() ? View.VISIBLE : View.INVISIBLE);
            systemView.setVisibility(appModel.isSystemApplication() ? View.VISIBLE : View.INVISIBLE);
        }

        private void bindSizeModel(Bundle model) {
            final android.content.pm.PackageStats pkgStats = mAppModel.getPackageStats();
            String phoneFMT = _G.getString(R.string.phone_usage_size);
            String sdcardFMT = _G.getString(R.string.sdcard_usage_size);
            TextView phoneView = null;
            TextView sdcardView = null;
            LinearLayout linearLayout = null;

            if(pkgStats.codeSize > 0 || pkgStats.externalCodeSize > 0) {
                phoneView = (TextView)itemView.findViewById(R.id.code_size_phone);
                sdcardView = (TextView)itemView.findViewById(R.id.code_size_sdcard);
                phoneView.setText(String.format(phoneFMT, _G.formatDiskSize(pkgStats.codeSize)));
                sdcardView.setText(String.format(sdcardFMT, _G.formatDiskSize(pkgStats.externalCodeSize)));
            } else {
                linearLayout = (LinearLayout)itemView.findViewById(R.id.code_size_layout);
                linearLayout.setVisibility(View.GONE);
            }

            if(pkgStats.dataSize > 0 || pkgStats.externalDataSize > 0) {
                phoneView = (TextView)itemView.findViewById(R.id.data_size_phone);
                sdcardView = (TextView)itemView.findViewById(R.id.data_size_sdcard);
                phoneView.setText(String.format(phoneFMT, _G.formatDiskSize(pkgStats.dataSize)));
                sdcardView.setText(String.format(sdcardFMT, _G.formatDiskSize(pkgStats.externalDataSize)));
            } else {
                linearLayout = (LinearLayout)itemView.findViewById(R.id.data_size_layout);
                linearLayout.setVisibility(View.GONE);
            }

            if(pkgStats.cacheSize > 0 || pkgStats.externalCacheSize > 0) {
                phoneView = (TextView)itemView.findViewById(R.id.cache_size_phone);
                sdcardView = (TextView)itemView.findViewById(R.id.cache_size_sdcard);
                phoneView.setText(String.format(phoneFMT, _G.formatDiskSize(pkgStats.cacheSize)));
                sdcardView.setText(String.format(sdcardFMT, _G.formatDiskSize(pkgStats.externalCacheSize)));
            } else {
                linearLayout = (LinearLayout)itemView.findViewById(R.id.cache_size_layout);
                linearLayout.setVisibility(View.GONE);
            }

            if(pkgStats.externalObbSize > 0) {
                phoneView = (TextView)itemView.findViewById(R.id.obb_size_phone);
                sdcardView = (TextView)itemView.findViewById(R.id.obb_size_sdcard);
                phoneView.setText(String.format(phoneFMT, _G.formatDiskSize(0)));
                sdcardView.setText(String.format(sdcardFMT, _G.formatDiskSize(pkgStats.externalObbSize)));
            } else {
                linearLayout = (LinearLayout)itemView.findViewById(R.id.obb_size_layout);
                linearLayout.setVisibility(View.GONE);
            }
        }

        private void bindItemModel(Bundle model) {
            ImageView iconView = (ImageView)itemView.findViewById(R.id.action_icon);
            TextView nameView = (TextView)itemView.findViewById(R.id.action_name);

            String textValue = getString(model.getInt("text"));
            if(textValue.contains("%")) {
                textValue = String.format(textValue, model.getInt("value"));
            }

            iconView.setImageResource(model.getInt("icon"));
            nameView.setText(textValue);
        }
    }


    public class ActionListAdapter extends RecyclerView.Adapter<ActionViewHolder> {

        private List<Bundle> mSource;

        @Override
        public int getItemViewType(int position) {
            return mSource.get(position).getInt("type", 0);
        }

        @Override
        public ActionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new ActionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ActionViewHolder holder, int position) {
            holder.bindModel(mSource.get(position));
        }

        @Override
        public int getItemCount() {
            return mSource.size();
        }

        public void setSource(List<Bundle> source) {
            mSource = source;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        _G.attachContext(this);

        handleIntent(getIntent());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listInfos);
        if(mAppModel != null && recyclerView != null) {
            final GridDividerDecoration decoration = new GridDividerDecoration(this.getDrawable(R.drawable.line_divider));
            final ActionListAdapter adapter = new ActionListAdapter();
            adapter.setSource(getActionList());

            GridLayoutManager manager = new GridLayoutManager(this, 4);
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int viewType = adapter.getItemViewType(position);
                    if (viewType == R.layout.app_detail_header || viewType == R.layout.app_size_item) {
                        return 4;
                    } else {
                        return 1;
                    }
                }
            });

            setTitle(mAppModel.getName());

            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(decoration);
            recyclerView.setHasFixedSize(true);
        }
        mRecyclerView = recyclerView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if(itemId == R.id.aboutMe) {
            Toast.makeText(this, "About Me", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void handleIntent(Intent intent) {
        String packageName = intent.getStringExtra("package");
        LoadPackageInfo(packageName);
        _G.log(String.valueOf(intent.getAction()) + ": " + String.valueOf(packageName));
    }

    private void LoadPackageInfo(String packageName) {
        AppItemModel target = null;

        if(_G.ApplicationList != null) {
            for(AppItemModel model : _G.ApplicationList) {
                if(model.getPackageName().equals(packageName)) {
                    target = model;
                    break;
                }
            }
        }

        if(target == null || target.getPackageInfo().activities == null) {
            //new AppInfoLoadTask().execute(packageName);
            PackageInfo pkg = null;
            PackageManager pm = _G.getPackageManager();
            try {
                int flags = PackageManager.GET_META_DATA |
                        PackageManager.GET_ACTIVITIES |
                        PackageManager.GET_SERVICES |
                        PackageManager.GET_PERMISSIONS |
                        PackageManager.GET_PROVIDERS |
                        PackageManager.GET_RECEIVERS;
                pkg = pm.getPackageInfo(packageName, flags);
            } catch (Exception e) {
                _G.log(e.getMessage());
            }

            if(pkg != null) {
                if(_G.ApplicationList != null) {
                    for (AppItemModel model : _G.ApplicationList) {
                        if (model.getPackageName().equals(pkg.packageName)) {
                            model.setPackageInfo(pkg);
                            break;
                        }
                    }
                } else {
                    target = new AppItemModel(pkg);
                }
            }
        }

        mAppModel = target;
    }

    private List<Bundle> getActionList() {
        Bundle model = null;
        ArrayList<Bundle> dataset = new ArrayList<>();
        PackageInfo pkg = mAppModel.getPackageInfo();
        android.content.pm.PackageStats pkgStats = mAppModel.getPackageStats();

        model = new Bundle();
        model.putInt("type", R.layout.app_detail_header);
        dataset.add(model);

        if(pkgStats != null && (pkgStats.codeSize > 0 || pkgStats.dataSize > 0)) {
            model = new Bundle();
            model.putInt("type", R.layout.app_size_item);
            dataset.add(model);
        }

        if(pkg.activities != null && pkg.activities.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_activities);
            model.putInt("icon", android.R.drawable.ic_menu_today);
            model.putInt("value", pkg.activities.length);
            dataset.add(model);
        }

        if(pkg.services != null && pkg.services.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_services);
            model.putInt("icon", android.R.drawable.ic_menu_compass);
            model.putInt("value", pkg.services.length);
            dataset.add(model);
        }

        if(pkg.receivers != null && pkg.receivers.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_receivers);
            model.putInt("icon", android.R.drawable.ic_menu_camera);
            model.putInt("value", pkg.receivers.length);
            dataset.add(model);
        }

        if(pkg.providers != null && pkg.providers.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_providers);
            model.putInt("icon", android.R.drawable.ic_menu_crop);
            model.putInt("value", pkg.providers.length);
            dataset.add(model);
        }

        if(pkg.permissions != null && pkg.permissions.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_permissions);
            model.putInt("icon", android.R.drawable.ic_menu_agenda);
            model.putInt("value", pkg.permissions.length);
            dataset.add(model);
        }

        if(pkg.requestedPermissions != null && pkg.requestedPermissions.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_reqPermissions);
            model.putInt("icon", android.R.drawable.ic_menu_mapmode);
            model.putInt("value", pkg.requestedPermissions.length);
            dataset.add(model);
        }

        if(pkg.reqFeatures != null && pkg.reqFeatures.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.app_action_item);
            model.putInt("text", R.string.action_item_reqFeatures);
            model.putInt("icon", android.R.drawable.ic_menu_directions);
            model.putInt("value", pkg.reqFeatures.length);
            dataset.add(model);
        }

        return dataset;
    }
}
