package com.weihuoya.bboo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import java.util.List;

public class ActionListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private AppItemModel mAppModel;
    private int mActionId;

    public class ActionModel {
        public Drawable getIcon() {
            return null;
        }

        public String getName() {
            return null;
        }

        public String getDescription() {
            return null;
        }

        public boolean isRunning() {
            return false;
        }

        public boolean isAuthorized() {
            return false;
        }

        public void onClick() {

        }
    }

    public class ActionStringModel extends ActionModel {

        private String mInfo;
        private int mFlag;

        public ActionStringModel(String info) {
            mInfo = info;
            mFlag = -1;
        }

        public ActionStringModel(String info, int flag) {
            mInfo = info;
            mFlag = flag;
        }

        @Override
        public String getName() {
            int index = mInfo.lastIndexOf(".");
            if(index != -1) {
                return mInfo.substring(index);
            } else {
                return mInfo;
            }
        }

        @Override
        public String getDescription() {
            return mInfo;
        }

        @Override
        public boolean isAuthorized() {
            return (mFlag & PackageInfo.REQUESTED_PERMISSION_GRANTED) > 0;
        }
    }

    public class ActionPermissionModel extends ActionModel {
        private PermissionInfo mInfo;
        private int mFlag;

        public ActionPermissionModel(PermissionInfo info) {
            mInfo = info;
            mFlag = 0;
        }

        public ActionPermissionModel(PermissionInfo info, int flag) {
            mInfo = info;
            mFlag = flag;
        }

        @Override
        public String getName() {
            PackageManager pm = _G.getPackageManager();
            String name = mInfo.loadLabel(pm).toString();
            int index = name.lastIndexOf(".");
            if(index != -1) {
                return name.substring(index);
            } else {
                return name;
            }
        }

        @Override
        public String getDescription() {
            return mInfo.name;
        }

        @Override
        public boolean isAuthorized() {
            return (mFlag & PackageInfo.REQUESTED_PERMISSION_GRANTED) > 0;
        }
    }

    public class ActionActivityModel extends ActionModel {
        private ActivityInfo mInfo;

        public ActionActivityModel(ActivityInfo info) {
            mInfo = info;
        }

        @Override
        public Drawable getIcon() {
            PackageManager pm = _G.getPackageManager();
            return mInfo.loadIcon(pm);
        }

        @Override
        public String getName() {
            PackageManager pm = _G.getPackageManager();
            String name = mInfo.loadLabel(pm).toString();
            if(name.isEmpty()) {
                name = mInfo.name.substring(mInfo.name.lastIndexOf("."));
            }
            return name;
        }

        @Override
        public String getDescription() {
            return mInfo.name;
        }

        @Override
        public void onClick() {
            Intent intent = new Intent();
            intent.setClassName(mInfo.packageName, mInfo.name);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            try {
                _G.getContext().startActivity(intent);
            } catch (SecurityException e) {
                Toast.makeText(_G.getContext(), _G.getString(R.string.access_permission_denial), Toast.LENGTH_SHORT).show();
                _G.log(e.toString());
            }
        }
    }

    public class ActionServiceModel extends ActionModel {
        private ServiceInfo mInfo;

        public ActionServiceModel(ServiceInfo info) {
            mInfo = info;
        }

        @Override
        public Drawable getIcon() {
            PackageManager pm = _G.getPackageManager();
            return mInfo.loadIcon(pm);
        }

        @Override
        public String getName() {
            PackageManager pm = _G.getPackageManager();
            return mInfo.loadLabel(pm).toString();
        }

        @Override
        public String getDescription() {
            return mInfo.name;
        }

        @Override
        public boolean isRunning() {
            return _G.isServiceRunning(mInfo.name);
        }
    }

    public class ActionProviderModel extends ActionModel {
        private ProviderInfo mInfo;

        public ActionProviderModel(ProviderInfo info) {
            mInfo = info;
        }

        @Override
        public Drawable getIcon() {
            PackageManager pm = _G.getPackageManager();
            return mInfo.loadIcon(pm);
        }

        @Override
        public String getName() {
            PackageManager pm = _G.getPackageManager();
            return mInfo.loadLabel(pm).toString();
        }

        @Override
        public String getDescription() {
            return mInfo.name;
        }
    }

    public class ActionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ActionModel mModel;

        public ActionViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mModel.onClick();
        }

        public void bindModel(ActionModel model) {
            ImageView iconView = (ImageView)itemView.findViewById(R.id.action_icon);
            TextView nameView = (TextView)itemView.findViewById(R.id.action_name);
            TextView descView = (TextView)itemView.findViewById(R.id.action_desc);
            ImageView markView = (ImageView)itemView.findViewById(R.id.marked_icon);

            if(iconView.getVisibility() == View.VISIBLE) {
                Drawable icon = model.getIcon();
                if(icon != null) {
                    iconView.setImageDrawable(icon);
                } else {
                    //LinearLayout linearLayout = (LinearLayout)itemView.findViewById(R.id.action_info_layout);
                    //ViewGroup.LayoutParams params = linearLayout.getLayoutParams();
                    //params.width += iconView.getLayoutParams().width;
                    //linearLayout.setLayoutParams(params);
                    iconView.setVisibility(View.GONE);
                }
            }

            String name = model.getName();
            if(name != null) {
                nameView.setText(name);
            }

            String desc = model.getDescription();
            if(desc != null) {
                descView.setText(desc);
            }

            boolean running = model.isRunning();
            boolean authorized = model.isAuthorized();
            if(running || authorized) {
                markView.setVisibility(View.VISIBLE);
            } else {
                markView.setVisibility(View.INVISIBLE);
            }

            mModel = model;
        }
    }

    public class ActionListAdapter extends RecyclerView.Adapter<ActionViewHolder> {

        List<ActionModel> mSource;

        @Override
        public ActionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.action_info_item, parent, false);
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

        public void setSource(List<ActionModel> source) {
            mSource = source;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_list);
        _G.attachContext(this);

        handleIntent(getIntent());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listActions);
        if(mAppModel != null && recyclerView != null) {
            final ActionListAdapter adapter = new ActionListAdapter();
            final GridDividerDecoration decoration = new GridDividerDecoration(this.getDrawable(R.drawable.line_divider));
            final PackageInfo pkg = mAppModel.getPackageInfo();
            final PackageManager pm = _G.getPackageManager();

            ArrayList<ActionModel> dataset = new ArrayList<>();
            switch (mActionId) {
                case R.string.action_item_activities:
                    for(ActivityInfo info : pkg.activities) {
                        dataset.add(new ActionActivityModel(info));
                    }
                    break;
                case R.string.action_item_permissions:
                    for(PermissionInfo info : pkg.permissions) {
                        dataset.add(new ActionPermissionModel(info));
                    }
                    break;
                case R.string.action_item_providers:
                    for(ProviderInfo info : pkg.providers) {
                        dataset.add(new ActionProviderModel(info));
                    }
                    break;
                case R.string.action_item_receivers:
                    for(ActivityInfo info : pkg.receivers) {
                        dataset.add(new ActionActivityModel(info));
                    }
                    break;
                case R.string.action_item_services:
                    for(ServiceInfo info : pkg.services) {
                        dataset.add(new ActionServiceModel(info));
                    }
                    break;
                case R.string.action_item_reqPermissions:
                    for(int i = 0; i < pkg.requestedPermissions.length; ++i) {
                        String info = pkg.requestedPermissions[i];
                        PermissionInfo permissionInfo = null;
                        try {
                            permissionInfo = pm.getPermissionInfo(info, 0);
                        } catch (Exception e) {
                            // todo
                        }
                        int flag = pkg.requestedPermissionsFlags[i];
                        if(permissionInfo != null) {
                            dataset.add(new ActionPermissionModel(permissionInfo, flag));
                        } else {
                            dataset.add(new ActionStringModel(info, flag));
                        }
                    }
                    break;
                case R.string.action_item_reqFeatures:
                    for(FeatureInfo info : pkg.reqFeatures) {
                        dataset.add(new ActionStringModel(info.name));
                    }
                    break;
            }

            setTitle(String.format(getString(mActionId), dataset.size()));
            adapter.setSource(dataset);

            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(decoration);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        mActionId = intent.getIntExtra("action", 0);

        for(AppItemModel model : _G.ApplicationList) {
            if(model.getPackageName().equals(packageName)) {
                mAppModel = model;
                break;
            }
        }
    }
}
