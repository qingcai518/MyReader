package org.kaka.myreader.activity;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.fragment.CloudFragment;
import org.kaka.myreader.fragment.LocalBooksFragment;


public class MainActivity extends FragmentActivity {
    private FragmentTabHost tabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        tabHost.addTab(tabHost.newTabSpec(AppConstants.TAG1).setIndicator("书架"), LocalBooksFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec(AppConstants.TAG2).setIndicator("书库"), CloudFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec(AppConstants.TAG3).setIndicator("我的"), LocalBooksFragment.class, null);
    }

    public FragmentTabHost getTabHost() {
        return tabHost;
    }
}
