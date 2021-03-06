package org.kaka.myreader.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.View;
import android.widget.Button;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.fragment.BookmarkFragment;
import org.kaka.myreader.fragment.ChapterFragment;

public class ChapterActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Button backBtn = (Button) findViewById(R.id.back);
        backBtn.setText(getIntent().getStringExtra("name"));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChapterActivity.this.finish();
            }
        });

//        List<Fragment> fragments = new Vector<>();
//        fragments.add(new ChapterFragment());
//        fragments.add(new BookmarkFragment());
//        fragments.add(new ChapterFragment());

        FragmentTabHost tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        tabHost.addTab(tabHost.newTabSpec(AppConstants.CAPTURE_TAG1).setIndicator("目录",
                        getResources().getDrawable(R.drawable.ic_action_chapter)),
                ChapterFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec(AppConstants.CAPTURE_TAG2).setIndicator("书签",
                        getResources().getDrawable(R.drawable.ic_action_bookmark)),
                BookmarkFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec(AppConstants.CAPTURE_TAG3).setIndicator("笔记",
                        getResources().getDrawable(R.drawable.ic_action_edit)),
                ChapterFragment.class, null);

    }
}
