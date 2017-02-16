package com.weihuoya.bboo.activity;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.widget.Toast;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.weihuoya.bboo.MainFragmentPagerAdapter;
import com.weihuoya.bboo.ShellThread;
import com.weihuoya.bboo._P;
import com.weihuoya.bboo.knox.KnoxMainActivity;
import com.weihuoya.bboo.knox.KnoxManager;
import com.weihuoya.bboo.model.PackageModel;
import com.weihuoya.bboo.fragment.PackageListFragment;
import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;
import com.weihuoya.bboo.root.RootMainActivity;
import com.weihuoya.bboo.root.RootManager;
import com.weihuoya.bboo.xposed.XposedMainActivity;
import com.weihuoya.bboo.xposed.XposedManager;


public class MainActivity extends AppCompatActivity implements PackageListFragment.OnFragmentInteractionListener {

    public class PackageListLoadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            String selfPkg = getPackageName();
            PackageManager pm = _G.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
            ArrayList<PackageModel> pkgList = new ArrayList<>();
            ArrayList<PackageModel> prcList = new ArrayList<>();

            for(PackageInfo p : packages) {
                if(selfPkg.equals(p.packageName)) {
                    continue;
                }
                pkgList.add(new PackageModel(p));
            }
            Collections.sort(pkgList);
            _P.PackageList = pkgList;

            for(AndroidAppProcess p : processes) {
                if(selfPkg.equals(p.getPackageName())) {
                    continue;
                }
                prcList.add(new PackageModel(p));
            }
            Collections.sort(prcList);
            _P.ProcessList = prcList;

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            onPackageListLoad();
        }
    }

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ProgressDialog mDialog;
    private ShellThread mShell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        _G.attachContext(this);

        ViewPager pager = (ViewPager)findViewById(R.id.main_pager);
        TabLayout tabLayout = (TabLayout)findViewById(R.id.main_tab);

        if(pager != null) {
            final MainFragmentPagerAdapter pagerAdapter = new MainFragmentPagerAdapter(getSupportFragmentManager());
            pager.setOffscreenPageLimit(3);
            pager.setAdapter(pagerAdapter);
            mViewPager = pager;
        }

        if(tabLayout != null) {
            tabLayout.setupWithViewPager(pager);
            tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
            mTabLayout = tabLayout;
        }

        handleIntent(getIntent());
        new PackageListLoadTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);

        /*if(!KnoxManager.isKnoxEnabled()) {
            menu.findItem(R.id.menu_main_knox).setVisible(false);
        }

        if(!XposedManager.isXposedModuleEnabled()) {
            menu.findItem(R.id.menu_main_xposed).setVisible(false);
        }

        if(!RootManager.isRootedDevice()) {
            menu.findItem(R.id.menu_main_root).setVisible(false);
        }*/

        MenuItem menuItem = menu.findItem(R.id.menu_main_search);
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mTabLayout.setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setQuery("", false);
                searchView.clearFocus();
                mTabLayout.setVisibility(View.VISIBLE);
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
                getPackageListFragment().setQuery(newText);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        SearchView searchView;

        int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.menu_main_search:
                searchView = (SearchView) menuItem.getActionView();
                searchView.setIconified(false);
                searchView.requestFocusFromTouch();
                break;
            case R.id.menu_main_knox:
                startActivity(new Intent(this, KnoxMainActivity.class));
                break;
            case R.id.menu_main_xposed:
                startActivity(new Intent(this, XposedMainActivity.class));
                /*if(XposedManager.isXposedModuleEnabled()) {
                    Toast.makeText(this, "xposed module is enabled!", Toast.LENGTH_SHORT).show();

                    List<String> pkgList = XposedManager.getLoadedPackages();
                    List<String> intentList = XposedManager.getQueriedIntents();

                    if(pkgList != null && pkgList.size() > 0) {
                        TextUtils.join(", ", pkgList);
                    }

                    if(intentList != null && intentList.size() > 0) {
                        TextUtils.join(", ", intentList);
                    }
                } else {
                    Toast.makeText(this, "xposed module is disabled!", Toast.LENGTH_SHORT).show();
                }*/
                break;
            case R.id.menu_main_root:
                startActivity(new Intent(this, RootMainActivity.class));
                /*if(mShell == null) {
                    mShell = new ShellThread(true);
                    mShell.start();
                }*/
                break;
            //case R.id.menu_main_logcat:
            //    startActivity(new Intent(this, HTMLViewerActivity.class).putExtra("command", "logcat -d"));
            //    break;
            case R.id.menu_main_about:
                startActivity(new Intent(this, AboutActivity.class));
                //Toast.makeText(this, "about me", Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

	@Override
    protected void onResume() {
        super.onResume();

        if(_P.DetailPackage != null) {
            getPackageListFragment().handlePackageChanged(_P.DetailPackage);
            _P.DetailPackage = null;
        }

        if(_P.UninstallPackage != null) {
            getPackageListFragment().handlePackageRemoved(_P.UninstallPackage);
            _P.UninstallPackage = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _G.log("$$$ onActivityResult: " + requestCode + ", " + resultCode);
        KnoxManager.onActivityResult(requestCode, resultCode, data);
    }

    public void handlePackageRemoved(String packageName) {
        getPackageListFragment().handlePackageRemoved(packageName);
    }

    public void onPackageListLoad() {
        PackageListFragment fragment = getPackageListFragment();
        fragment.setQuery(null);

        for(int i = 0; i < mTabLayout.getTabCount(); ++i) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if(tab != null) {
                try {
                    Field e = tab.getClass().getDeclaredField("mView");
                    e.setAccessible(true);
                    View tabview = (View)e.get(tab);
                    tabview.setOnClickListener(new View.OnClickListener() {
                        private long mLastClickTime = 0;
                        @Override
                        public void onClick(View view) {
                            long clickTime = System.currentTimeMillis();
                            if(clickTime - mLastClickTime < 500) {
                                getPackageListFragment().scrollToPosition(0, false);
                            }
                            mLastClickTime = clickTime;
                        }
                    });
                } catch (Exception e) {
                    _G.log(e.toString());
                }
            }
        }
    }

    protected PackageListFragment getPackageListFragment() {
        FragmentPagerAdapter adapter = (FragmentPagerAdapter)mViewPager.getAdapter();
        return (PackageListFragment)adapter.instantiateItem(mViewPager, mViewPager.getCurrentItem());
    }

    protected void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            getPackageListFragment().setQuery(query);
            getPackageListFragment().scrollToPosition(0, false);
            _G.getSearchSuggestions().saveRecentQuery(query, null);
        }
    }
}
