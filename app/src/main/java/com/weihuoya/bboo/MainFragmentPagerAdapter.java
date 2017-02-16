package com.weihuoya.bboo;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import com.weihuoya.bboo.fragment.PackageListFragment;

/**
 * Created by zhangwei on 2016/5/29.
 */
public class MainFragmentPagerAdapter extends FragmentPagerAdapter {

    private String[] mTabTitles;

    public MainFragmentPagerAdapter(FragmentManager fm) {
        super(fm);

        mTabTitles = new String[]{
                _G.getString(R.string.main_tab_usr),
                _G.getString(R.string.main_tab_sys),
                _G.getString(R.string.main_tab_prc)
        };
    }

    @Override
    public Fragment getItem(int position) {
        int typeId = -1;
        if(position == 0) {
            typeId = R.string.main_tab_usr;
        } else if(position == 1) {
            typeId = R.string.main_tab_sys;
        } else if(position == 2) {
            typeId = R.string.main_tab_prc;
        }
        return PackageListFragment.newInstance(typeId);
    }

    @Override
    public int getCount() {
        return mTabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
    }
}
