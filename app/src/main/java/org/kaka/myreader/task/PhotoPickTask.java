package org.kaka.myreader.task;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import org.kaka.myreader.activity.PhotoPickerActivity;

import java.util.ArrayList;
import java.util.List;

public class PhotoPickTask extends AsyncTask<String, Integer, Integer> {
    private Context context;
    private final static String TAG = "PhotoPickTask";
    private List<String> pathList = new ArrayList<>();

    public PhotoPickTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(String... params) {
        try {
            Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(imageUri, null,
                    MediaStore.Images.Media.MIME_TYPE + "=? or "
                            + MediaStore.Images.Media.MIME_TYPE + "=?",
                    new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

            if (cursor == null) {
                return 1;
            }

            while (cursor.moveToNext()) {
                //获取图片的路径
                String path = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));

                //获取该图片的父路径名
//                String parentName = new File(path).getParentFile().getName();
                //根据父路径名将图片放入到mGruopMap中
//                if (!groupMap.containsKey(parentName)) {
//                    List<String> chileList = new ArrayList<>();
//                    chileList.add(path);
//                    groupMap.put(parentName, chileList);
//                } else {
//                    groupMap.get(parentName).add(path);
//                }

                pathList.add(path);
            }
            cursor.close();

            return 0;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return 2;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (context instanceof PhotoPickerActivity) {
            ((PhotoPickerActivity) context).finishTask(pathList);
        }
    }
}
