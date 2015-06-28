package org.kaka.myreader.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.View;
import android.widget.Button;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.fragment.BookmarkFragment;
import org.kaka.myreader.fragment.CaptureFragment;
import org.kaka.myreader.fragment.ManualFindFragment;

import java.util.List;
import java.util.Vector;

public class FileSearchActivity extends FragmentActivity {
    private FragmentTabHost tabHost;
    private List<Fragment> fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filesearch);

        Button backBtn = (Button) findViewById(R.id.back);
        backBtn.setText(getIntent().getStringExtra("name"));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSearchActivity.this.finish();
            }
        });

        fragments = new Vector<>();
        fragments.add(new ManualFindFragment());
        fragments.add(new ManualFindFragment());

        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        tabHost.addTab(tabHost.newTabSpec(AppConstants.FILE_SEARCH_TAG1).setIndicator("手动导入"),
                ManualFindFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec(AppConstants.FILE_SEARCH_TAG2).setIndicator("智能导入"),
                ManualFindFragment.class, null);
    }
}
