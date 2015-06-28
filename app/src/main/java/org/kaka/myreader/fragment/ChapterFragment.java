package org.kaka.myreader.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.kaka.myreader.R;

import java.util.List;

public class ChapterFragment extends ListFragment {
    private String[] captureNames;
    private List<Integer> offsets;
    private int currentCapture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captureNames = getActivity().getIntent().getStringArrayExtra("captureNames");
        offsets = getActivity().getIntent().getIntegerArrayListExtra("offsets");
        currentCapture = getActivity().getIntent().getIntExtra("currentCapture", 0);

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return captureNames.length;
            }

            @Override
            public Object getItem(int position) {
                return captureNames[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) LayoutInflater.from(ChapterFragment.this.getActivity()).inflate(
                        R.layout.captureitem, null);
                if (currentCapture == offsets.get(position)) {
                    textView.setTextColor(Color.BLUE);
                }

                textView.setText(captureNames[position]);

                return textView;
            }
        };
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent();
        currentCapture = offsets.get(position);
        intent.putExtra("currentOffset", currentCapture);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}