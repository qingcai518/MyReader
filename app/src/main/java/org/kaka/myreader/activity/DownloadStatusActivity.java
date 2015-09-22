package org.kaka.myreader.activity;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadStatusActivity extends ListActivity {
    private BaseAdapter adapter;
    private List<Map<String, Object>> listData = new ArrayList<>();
    private String[] keys = new String[]{"bookName", "status", "statusText", "speed", "cancel"};
    BroadcastReceiver receiver;
    private int status = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloadstatus);
        Button btnBack = (Button) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadStatusActivity.this.finish();
            }
        });
        addAdapter();
        addBroadcast();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    private void addBroadcast() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String id = intent.getStringExtra("id");
                String downloadSize = intent.getStringExtra("downloadSize");
                if (downloadSize == null) {
                    for (Map<String, Object> map : listData) {
                        String bookId = (String) map.get("id");
                        if (id.equals(bookId)) {
                            listData.remove(map);
                            break;
                        }
                    }
                } else {
                    String totalSize = intent.getStringExtra("totalSize");
                    String speed = intent.getStringExtra("speed");
                    String name = intent.getStringExtra("name");
                    status = (int) (Double.parseDouble(downloadSize) * 100 / Double.parseDouble(totalSize));
                    boolean isExist = false;
                    for (Map<String, Object> map : listData) {
                        String bookId = (String) map.get("id");
                        if (id.equals(bookId)) {
                            map.put(keys[0], name);
                            map.put(keys[1], status);
                            map.put(keys[2], downloadSize + "KB/" + totalSize + "KB");
                            map.put(keys[3], speed);
                            isExist = true;
                            break;
                        }
                    }

                    if (!isExist) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", id);
                        map.put(keys[0], name);
                        map.put(keys[1], status);
                        map.put(keys[2], downloadSize + "KB/" + totalSize + "KB");
                        map.put(keys[3], speed);
                        listData.add(map);
                    }
                }

                update();
            }
        };
        registerReceiver(receiver, new IntentFilter(AppConstants.BROADCAST_DOWNLOAD));
    }

    private void update() {
        adapter.notifyDataSetChanged();
    }

    private void addAdapter() {
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return listData.size();
            }

            @Override
            public Object getItem(int position) {
                return listData.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                ViewHolder holder;
                if (view == null) {
                    LayoutInflater inflater =
                            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.item_downloadstatus, null);
                    holder = new ViewHolder();
                    holder.bookName = (TextView) view.findViewById(R.id.bookName);
                    holder.statusText = (TextView) view.findViewById(R.id.statusText);
                    holder.speed = (TextView) view.findViewById(R.id.speed);
                    holder.status = (ProgressBar) view.findViewById(R.id.status);
                    holder.cancel = (Button) view.findViewById(R.id.cancel);
                    view.setTag(holder);
                } else {
                    holder = (ViewHolder) view.getTag();
                }
                Map<String, Object> map = listData.get(position);
                holder.bookName.setText((String) map.get(keys[0]));
                holder.statusText.setText((String) map.get(keys[2]));
                String downloadSpeed = (String) map.get(keys[3]);
                holder.speed.setText(downloadSpeed + "KB");
                holder.status.setMax(100);
                holder.status.setProgress((int) map.get(keys[1]));
                return view;
            }
        };
        setListAdapter(adapter);
    }

    class ViewHolder {
        public TextView bookName;
        public TextView statusText;
        public TextView speed;
        public Button cancel;
        public ProgressBar status;
    }
}
