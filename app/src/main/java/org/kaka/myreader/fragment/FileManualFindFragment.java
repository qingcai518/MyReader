package org.kaka.myreader.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileManualFindFragment extends ListFragment {
    private List<Map<String, Object>> listData = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File dir = Environment.getExternalStorageDirectory();
        if (dir.isDirectory()) {
            getData(dir);
        }

        setAdapter();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Map<String, Object> map = listData.get(position);
        File file = (File) map.get("file");
        if (file.isFile()) {
        } else {
            listData.clear();
            getData(file);
            setAdapter();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setAdapter() {
        SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), listData, R.layout.importitem,
                new String[]{"fileLogo", "fileName", "fileDetail", "fileSelection"},
                new int[]{R.id.fileLogo, R.id.fileName, R.id.fileDetail, R.id.fileSelection});
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if ((view instanceof ImageView) && (data instanceof Drawable)) {
                    ImageView imageView = (ImageView) view;
                    imageView.setImageDrawable((Drawable) data);
                    return true;
                }
                return false;
            }
        });
        setListAdapter(adapter);
    }

    private void getData(File dir) {
        List<Map<String, Object>> dirList = new ArrayList<>();
        for (File file : dir.listFiles()) {
            Map<String, Object> map = new HashMap<>();

            if (file.isFile()) {
                String fileName = file.getName();
                if (!fileName.endsWith(".pdf") && !fileName.endsWith(".txt") && !fileName.endsWith(".epub")) {
                    continue;
                }
                String ext = fileName.substring(file.getName().lastIndexOf(".") + 1);
                map.put("fileName", fileName);
                double size = (double) file.getTotalSpace() / 1024;
                Log.d("File size", file.getTotalSpace() + "");
                map.put("fileDetail", "类型:" + ext + "  大小:" + AppConstants.DECIMAL_FORMAT.format(size) + "KB");
                map.put("fileLogo", getResources().getDrawable(R.drawable.ic_action_document));
                map.put("fileSelection", getResources().getDrawable(R.drawable.checkbox_click));
                map.put("file", file);
                listData.add(map);
            } else {
                map.put("fileName", file.getName());
                map.put("fileDetail", getFileCount(file) + "个文件");
                map.put("fileLogo", getResources().getDrawable(R.drawable.ic_action_folder_closed));
                map.put("fileSelection", getResources().getDrawable(R.drawable.right));
                map.put("file", file);
                dirList.add(map);
            }
        }
        listData.addAll(dirList);
    }

    private int getFileCount(File dir) {
        int count = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                count++;
            }
        }

        return count;
    }
}