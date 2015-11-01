package org.kaka.myreader.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
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
import org.kaka.myreader.activity.AbstractReaderActivity.BookType;

import org.kaka.myreader.R;

import java.util.List;

public class ChapterFragment extends ListFragment {
    private String[] captureNames;
    private List<Integer> offsets;
    private int currentCapture;
    private int currentIndex;
    private ProgressDialog dialog;
    BookType type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captureNames = getActivity().getIntent().getStringArrayExtra("captureNames");
        type = (BookType)getActivity().getIntent().getSerializableExtra("bookType");

        // txt文件时用到的信息.
        offsets = getActivity().getIntent().getIntegerArrayListExtra("offsets");
        currentCapture = getActivity().getIntent().getIntExtra("currentCapture", 0);

        // epub文件时用到的信息.
        currentIndex = getActivity().getIntent().getIntExtra("currentIndex", 1);

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
                        R.layout.item_chapter, null);
                if (type == BookType.TXT) {
                    if (currentCapture == offsets.get(position)) {
                        textView.setTextColor(Color.BLUE);
                    }
                } else if (type == BookType.EPUB) {
                    if (currentIndex == position + 1) {
                        textView.setTextColor(Color.BLUE);
                    }
                }

                textView.setText(captureNames[position]);

                return textView;
            }
        };
        setListAdapter(adapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (type == BookType.TXT) {
            int position = offsets.indexOf(currentCapture);
            getListView().setSelection(position);
        } else if (type == BookType.EPUB) {
            getListView().setSelection(currentIndex - 1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        dialog = ProgressDialog.show(getActivity(), "请稍后", "正在为您加载选中章节..");
        Intent intent = new Intent();
        if (type == BookType.TXT) {
            currentCapture = offsets.get(position);
            intent.putExtra("currentOffset", currentCapture);
        } else if (type == BookType.EPUB) {
            intent.putExtra("currentIndex", position + 1);
            intent.putExtra("currentOffset", 0);
        }
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}