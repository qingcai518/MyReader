package org.kaka.myreader.activity;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.fragment.CloudFragment;
import org.kaka.myreader.fragment.LocalBooksFragment;
import org.kaka.myreader.fragment.UserFragment;


public class MainActivity extends FragmentActivity {
    private FragmentTabHost tabHost;
    private String[] tabNameArray = {"书架", "书库", "我的"};
    private Class[] fragmentArray = {LocalBooksFragment.class, CloudFragment.class, UserFragment.class};
    private int[] resourceArray = {R.drawable.selector_tab_local,
            R.drawable.selector_tab_cloud,
            R.drawable.selector_tab_user};
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        inflater = LayoutInflater.from(this);
        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
        tabHost.getTabWidget().setDividerDrawable(null);

        for (int i = 0; i < AppConstants.TAG_ARRAY.length; i++) {
            TabHost.TabSpec tabSpec = tabHost.newTabSpec(AppConstants.TAG_ARRAY[i]).setIndicator(getTabItemView(i));
            tabHost.addTab(tabSpec, fragmentArray[i], null);
        }
    }

    private View getTabItemView(int index) {
        View view = inflater.inflate(R.layout.tab_content, null);
        ImageView imageView = (ImageView) view
                .findViewById(R.id.tab_imageview);
        imageView.setBackgroundResource(resourceArray[index]);

        TextView textView = (TextView) view.findViewById(R.id.tab_textview);
        textView.setText(tabNameArray[index]);
        return view;
    }

    public FragmentTabHost getTabHost() {
        return tabHost;
    }
}
