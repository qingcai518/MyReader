package org.kaka.myreader.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.activity.CloudDetailActivity;
import org.kaka.myreader.task.ConnectTask;
import org.kaka.myreader.task.FileDownloadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CloudFragment extends ListFragment {
    private List<Map<String, Object>> listData = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private BaseAdapter adapter;
    private boolean isRefreshed = false;

    public final static String KEY_ID = "id";
    public final static String KEY_IMAGE = "image";
    public final static String KEY_NAME = "name";
    public final static String KEY_AUTHOR = "author";
    public final static String KEY_DETAIL = "detail";
    public final static String KEY_PATH = "path";
    public final static String KEY_SIZE = "size";
    public final static String KEY_SCORE = "score";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                ViewHolder holder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(
                            R.layout.item_cloud, null);
                    holder = new ViewHolder();
                    holder.image = (ImageView) convertView.findViewById(R.id.image);
                    holder.bookName = (TextView) convertView.findViewById(R.id.bookName);
                    holder.author = (TextView) convertView.findViewById(R.id.author);
                    holder.detail = (TextView) convertView.findViewById(R.id.bookDetail);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }


                Map<String, Object> item = listData.get(position);

                String imageStr = (String)item.get(KEY_IMAGE);
                byte[] data = Base64.decode(imageStr, Base64.DEFAULT);
                Bitmap bitmap = null;
                if (data.length != 0) {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
                holder.image.setImageBitmap(bitmap);
                holder.bookName.setText((String) item.get(KEY_NAME));
                holder.author.setText("作者 : " + item.get(KEY_AUTHOR));
                holder.detail.setText((String) item.get(KEY_DETAIL));

                return convertView;
            }
        };
        setListAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cloud, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ConnectTask(CloudFragment.this).execute(listData);
            }
        });
        refresh();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        registerForContextMenu(getListView());
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_cloudbooks, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {

            case R.id.download:
                Toast.makeText(getActivity(), "开始下载..", Toast.LENGTH_SHORT).show();
                Map<String, Object> currentMap = listData.get(info.position);
                FileDownloadTask task = new FileDownloadTask(getActivity());
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentMap);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, Object> map = listData.get(position);
        Intent intent = new Intent(getActivity(), CloudDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ID, (String) map.get(KEY_ID));
        bundle.putString(KEY_NAME, (String) map.get(KEY_NAME));
        bundle.putString(KEY_AUTHOR, (String) map.get(KEY_AUTHOR));
        bundle.putString(KEY_PATH, (String) map.get(KEY_PATH));
        bundle.putString(KEY_DETAIL, (String) map.get(KEY_DETAIL));
        bundle.putString(KEY_IMAGE, (String) map.get(KEY_IMAGE));
//        bundle.putParcelable(KEY_IMAGE, (Bitmap) map.get(KEY_IMAGE));
        bundle.putString(KEY_SIZE, (String) map.get(KEY_SIZE));
        double score = (Double) map.get(KEY_SCORE);
        bundle.putFloat(KEY_SCORE, (float) score);
        intent.putExtra("bundle", bundle);
        getActivity().startActivity(intent);
    }


    private void refresh() {
        if (isRefreshed) {
            return;
        }
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                new ConnectTask(CloudFragment.this).execute(listData);
            }
        });
        isRefreshed = true;
    }

    public void finishConnection() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        adapter.notifyDataSetChanged();
    }

    class ViewHolder {
        ImageView image;
        TextView bookName;
        TextView author;
        TextView detail;
    }
}
