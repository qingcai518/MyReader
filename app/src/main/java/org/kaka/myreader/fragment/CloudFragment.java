package org.kaka.myreader.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.activity.ReaderActivity;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.task.FileDownloadTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CloudFragment extends ListFragment {
    private List<Map<String, Object>> listData = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                AppUtility.getJSONFromServer(listData);
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), listData, R.layout.bookitem,
                new String[]{"image", "name", "detail"}, new int[]{R.id.image, R.id.bookName, R.id.bookDetail});
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if ((view instanceof ImageView) && (data instanceof Bitmap)) {
                    ImageView imageView = (ImageView) view;
                    Bitmap bmp = (Bitmap) data;
                    imageView.setImageBitmap(bmp);
                    return true;
                }
                return false;
            }
        });
        setListAdapter(adapter);
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
                task.execute(currentMap);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity(), ReaderActivity.class);
        startActivity(intent);
    }
}
