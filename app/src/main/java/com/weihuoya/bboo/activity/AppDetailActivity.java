package com.weihuoya.bboo.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.weihuoya.bboo.GridDividerDecoration;
import com.weihuoya.bboo._P;
import com.weihuoya.bboo.model.ManifestModel;
import com.weihuoya.bboo.model.PackageModel;
import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;

import java.util.ArrayList;
import java.util.List;


public class AppDetailActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PERMISSIONS = 0;
    private RecyclerView mRecyclerView;
    private PackageModel mAppModel;

    public class ActionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Bundle mModel;

        public ActionViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int viewType = mModel.getInt("type", 0);
            if(viewType == R.layout.detail_action_item) {
                final PackageModel appModel = mAppModel;
                final AppDetailActivity context = AppDetailActivity.this;
                final int actionId = mModel.getInt("text", -1);
                if(actionId == R.string.action_item_manifest) {
                    Intent intent = new Intent(context, HTMLViewerActivity.class);
                    intent.putExtra("package", appModel.getPackageName());
                    ActivityCompat.startActivity(context, intent, null);
                } else if(actionId > 0) {
                    Intent intent = new Intent(context, ActionListActivity.class);
                    intent.putExtra("package", appModel.getPackageName());
                    intent.putExtra("action", actionId);
                    ActivityCompat.startActivity(context, intent, null);
                }
            } else if(viewType == R.layout.detail_app_header) {
                // nothing
            } else if(viewType == R.layout.detail_app_size) {
                // nothing
            } else if(viewType == R.layout.detail_button_list) {
                final PackageModel appModel = mAppModel;
                Object tag = v.getTag();
                if(tag == null) {
                    // nothing
                } else if((int)tag == R.id.app_uninstall_btn) {
                    _P.uninstallPackage(appModel.getPackageName());
                } else if((int)tag == R.id.app_block_btn) {
                    _P.blockPackage(appModel.getPackageName(), !appModel.isBlocked());
                    mRecyclerView.getAdapter().notifyItemChanged(0);
                    if(appModel.isBlocked()) {
                        ((AppCompatButton)v).setText(R.string.app_block_btn2);
                    } else {
                        ((AppCompatButton)v).setText(R.string.app_block_btn1);
                    }
                }
            }
        }

        public void bindModel(Bundle model) {
            int viewType = model.getInt("type", 0);
            if(viewType == R.layout.detail_action_item) {
                bindItemModel(model);
            } else if(viewType == R.layout.detail_app_header) {
                bindHeaderModel(model);
            } else if(viewType == R.layout.detail_app_size) {
                bindSizeModel(model);
            } else if(viewType == R.layout.detail_button_list) {
                bindButtonModel(model);
            }
            mModel = model;
        }

        private void bindButtonModel(Bundle model) {
            final PackageModel appModel = mAppModel;
            AppCompatButton uninstallBtn = (AppCompatButton)itemView.findViewById(R.id.app_uninstall_btn);
            AppCompatButton blockBtn = (AppCompatButton)itemView.findViewById(R.id.app_block_btn);

            uninstallBtn.setEnabled(appModel.isUnstallEnabled());
            uninstallBtn.setTag(R.id.app_uninstall_btn);
            uninstallBtn.setOnClickListener(this);

            if(appModel.isBlocked()) {
                blockBtn.setText(R.string.app_block_btn2);
            } else {
                blockBtn.setText(R.string.app_block_btn1);
            }
            blockBtn.setTag(R.id.app_block_btn);
            blockBtn.setOnClickListener(this);
        }

        private void bindHeaderModel(Bundle model) {
            final PackageModel appModel = mAppModel;
            ImageView iconView = (ImageView)itemView.findViewById(R.id.app_icon);
            TextView nameView = (TextView)itemView.findViewById(R.id.app_name);
            TextView packageView = (TextView)itemView.findViewById(R.id.app_package);
            TextView descriptionView = (TextView)itemView.findViewById(R.id.app_desc);

            ImageView blockedView = (ImageView)itemView.findViewById(R.id.blocked_icon);
            ImageView xposedView = (ImageView)itemView.findViewById(R.id.xposed_icon);
            ImageView systemView = (ImageView)itemView.findViewById(R.id.system_icon);
            ImageView adblockView = (ImageView)itemView.findViewById(R.id.adblock_icon);
            ImageView edgeView = (ImageView)itemView.findViewById(R.id.edge_icon);
            ImageView stepView = (ImageView)itemView.findViewById(R.id.step_icon);

            iconView.setImageDrawable(appModel.getIcon());
            nameView.setText(appModel.getName());
            packageView.setText(appModel.getPackageName());
            descriptionView.setText(appModel.getPackageDescription());

            boolean isBlocked = appModel.isBlocked() || !appModel.isApplicationEnabled();
            blockedView.setVisibility(isBlocked ? View.VISIBLE : View.INVISIBLE);
            xposedView.setVisibility(appModel.isXposedModule() ? View.VISIBLE : View.INVISIBLE);
            systemView.setVisibility(appModel.isSystemApplication() ? View.VISIBLE : View.INVISIBLE);
            adblockView.setVisibility(appModel.isHaveAdMob() ? View.VISIBLE : View.INVISIBLE);
            edgeView.setVisibility(appModel.isEdgeApp() ? View.VISIBLE : View.INVISIBLE);
            stepView.setVisibility(appModel.isStepCounter() ? View.VISIBLE : View.INVISIBLE);
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

            int textId = model.getInt("text", -1);
            int iconId = model.getInt("icon", -1);

            if(textId != -1) {
                String textValue = getString(textId);
                if(textValue.contains("%")) {
                    textValue = String.format(textValue, model.getInt("value"));
                }
                nameView.setVisibility(View.VISIBLE);
                nameView.setText(textValue);
            } else {
                nameView.setVisibility(View.GONE);
            }

            if(iconId != -1) {
                iconView.setVisibility(View.VISIBLE);
                iconView.setImageResource(model.getInt("icon"));
            } else {
                iconView.setVisibility(View.GONE);
                itemView.setClickable(false);
            }
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
        setContentView(R.layout.activity_detail);
        _G.attachContext(this);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        handleIntent(getIntent());

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listInfos);
        if(mAppModel != null && recyclerView != null) {
            final GridDividerDecoration decoration = new GridDividerDecoration(
                    this.getDrawable(R.drawable.line_divider), true, true
            );
            final ActionListAdapter adapter = new ActionListAdapter();
            adapter.setSource(getActionList());

            GridLayoutManager manager = new GridLayoutManager(this, 4);
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int viewType = adapter.getItemViewType(position);
                    if (viewType == R.layout.detail_app_header ||
                            viewType == R.layout.detail_app_size ||
                            viewType == R.layout.detail_button_list) {
                        return 4;
                    } else {
                        return 1;
                    }
                }
            });

            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(decoration);
            recyclerView.setHasFixedSize(true);

            mRecyclerView = recyclerView;
            setTitle(mAppModel.getName());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if(itemId == R.id.unstallApp) {
            _P.uninstallPackage(mAppModel.getPackageName());
        } else if(itemId == R.id.notifyCtrl) {
            PackageInfo pkg = mAppModel.getPackageInfo();
            if(pkg != null && pkg.applicationInfo != null) {
                Toast.makeText(this, pkg.applicationInfo.sourceDir + ", " + pkg.applicationInfo.publicSourceDir, Toast.LENGTH_SHORT).show();
            }
            //boolean enabled = mAppModel.isNotificationEnabled();
            //mAppModel.setNotificationEnabled(!enabled);
        } else if(itemId == R.id.extractApk) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                extractApk();
            } else {
                requestWritePermission();
            }
        } else if(itemId == R.id.aboutMe) {
            Toast.makeText(this, "About Me", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void handleIntent(Intent intent) {
        String packageName = intent.getStringExtra("package");
        if(packageName != null && !packageName.isEmpty()) {
            mAppModel = _P.getPackageDetail(packageName);
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
                extractApk();
            }
        }
    }

    private void extractApk() {
        PackageInfo pkg = mAppModel.getPackageInfo();
        if(pkg.applicationInfo == null || pkg.applicationInfo.sourceDir == null) {
            return;
        }

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
            String pkgName = mAppModel.getPackageName();
            File sdcardPath = Environment.getExternalStorageDirectory();
            String outputFile = sdcardPath.getAbsolutePath() +
                    File.separator + "Download" + File.separator + pkgName + ".apk";
            File source = new File(pkg.applicationInfo.sourceDir);
            File destination = new File(outputFile);
            if(_G.copyFile(source, destination)) {
                Toast.makeText(this, "Save to: " + outputFile, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<Bundle> getActionList() {
        int actionCount = 0;
        Bundle model = null;
        ArrayList<Bundle> dataset = new ArrayList<>();
        PackageInfo pkg = mAppModel.getPackageInfo();
        //ManifestModel manifestModel = mAppModel.getManifest();
        android.content.pm.PackageStats pkgStats = mAppModel.getPackageStats();

        model = new Bundle();
        model.putInt("type", R.layout.detail_app_header);
        dataset.add(model);

        if(pkgStats != null && (pkgStats.codeSize > 0 || pkgStats.dataSize > 0)) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_app_size);
            dataset.add(model);
        }

        model = new Bundle();
        model.putInt("type", R.layout.detail_action_item);
        model.putInt("text", R.string.action_item_manifest);
        model.putInt("icon", android.R.drawable.ic_menu_gallery);
        dataset.add(model);
        ++actionCount;

        if(pkg.activities != null && pkg.activities.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_activities);
            model.putInt("icon", android.R.drawable.ic_menu_today);
            model.putInt("value", pkg.activities.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.services != null && pkg.services.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_services);
            model.putInt("icon", android.R.drawable.ic_menu_compass);
            model.putInt("value", pkg.services.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.receivers != null && pkg.receivers.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_receivers);
            model.putInt("icon", android.R.drawable.ic_menu_camera);
            model.putInt("value", pkg.receivers.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.providers != null && pkg.providers.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_providers);
            model.putInt("icon", android.R.drawable.ic_menu_crop);
            model.putInt("value", pkg.providers.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.permissions != null && pkg.permissions.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_permissions);
            model.putInt("icon", android.R.drawable.ic_menu_agenda);
            model.putInt("value", pkg.permissions.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.requestedPermissions != null && pkg.requestedPermissions.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_reqPermissions);
            model.putInt("icon", android.R.drawable.ic_menu_mapmode);
            model.putInt("value", pkg.requestedPermissions.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.reqFeatures != null && pkg.reqFeatures.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_reqFeatures);
            model.putInt("icon", android.R.drawable.ic_menu_directions);
            model.putInt("value", pkg.reqFeatures.length);
            dataset.add(model);
            ++actionCount;
        }

        if(pkg.signatures != null && pkg.signatures.length > 0) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_signatures);
            model.putInt("icon", android.R.drawable.ic_menu_info_details);
            model.putInt("value", pkg.signatures.length);
            dataset.add(model);
            ++actionCount;
        }

        /*if(manifestModel != null && manifestModel.intentFilters != null) {
            model = new Bundle();
            model.putInt("type", R.layout.detail_action_item);
            model.putInt("text", R.string.action_item_intentFilters);
            model.putInt("icon", android.R.drawable.ic_menu_slideshow);
            model.putInt("value", manifestModel.intentFilters.size());
            dataset.add(model);
        }*/

        actionCount = actionCount % 4;
        if(actionCount > 0) {
            actionCount = 4 - actionCount;
            for(int i = 0; i < actionCount; ++i) {
                model = new Bundle();
                model.putInt("type", R.layout.detail_action_item);
                dataset.add(model);
            }
        }

        //model = new Bundle();
        //model.putInt("type", R.layout.detail_button_list);
        //dataset.add(model);

        return dataset;
    }

    public void handlePackageRemoved(String packageName) {
        if(mAppModel != null && mAppModel.getPackageName().equals(packageName)) {
            finish();
        }
    }
}
