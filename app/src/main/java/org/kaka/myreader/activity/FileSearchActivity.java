package org.kaka.myreader.activity;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.MyBookEntity;
import org.kaka.myreader.fragment.FileAutoSearchFragment;
import org.kaka.myreader.fragment.FileManualFindFragment;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

public class FileSearchActivity extends FragmentActivity {
    private FragmentTabHost tabHost;
    private List<Fragment> fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filesearch);

        Button backBtn = (Button) findViewById(R.id.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSearchActivity.this.setResult(RESULT_OK);
                FileSearchActivity.this.finish();
            }
        });

        fragments = new Vector<>();
        fragments.add(new FileManualFindFragment());
        fragments.add(new FileAutoSearchFragment());

        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        tabHost.addTab(tabHost.newTabSpec(AppConstants.FILE_SEARCH_TAG1).setIndicator("手动导入"),
                FileManualFindFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec(AppConstants.FILE_SEARCH_TAG2).setIndicator("智能导入"),
                FileAutoSearchFragment.class, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tabHost.getCurrentTabTag());
        if (fragment instanceof FileManualFindFragment) {
            return ((FileManualFindFragment) fragment).onKeyDown(keyCode, event);
        }
        return false;
    }
}
