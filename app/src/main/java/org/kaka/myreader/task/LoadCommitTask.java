package org.kaka.myreader.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.kaka.myreader.R;
import org.kaka.myreader.activity.CloudDetailActivity;
import org.kaka.myreader.common.AppConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadCommitTask extends AsyncTask<String, Integer, Integer> {
    private final static String TAG = "LoadCommitTask";
    private CloudDetailActivity activity;
    private List<Map<String, Object>> listData = new ArrayList<>();

    public LoadCommitTask(CloudDetailActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String id = params[0];
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(AppConstants.LOAD_COMMIT_SERVER + id);
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
        try {
            HttpResponse response = client.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                throw new Exception(responseCode + " : " + response.getStatusLine().getReasonPhrase());
            }

            JSONArray array = new JSONArray(EntityUtils.toString(response.getEntity(), "UTF-8"));
            for (int i = 0; i < array.length(); i++) {
                JSONArray item = array.getJSONArray(i);
                Map<String, Object> map = new HashMap<>();

                map.put(CloudDetailActivity.KEY_NAME, item.getString(0));
                byte[] data = Base64.decode(item.getString(1), Base64.DEFAULT);
                Bitmap bitmap;
                if (data.length != 0) {
                    bitmap = resizeImage(BitmapFactory.decodeByteArray(data, 0, data.length));

                } else {
                    bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.user);
                }
                map.put(CloudDetailActivity.KEY_IMAGE, bitmap);
                map.put(CloudDetailActivity.KEY_STAR, item.getDouble(2));
                map.put(CloudDetailActivity.KEY_COMMIT, item.getString(3));
                map.put(CloudDetailActivity.KEY_TIME, item.getString(4));

                listData.add(map);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return 1;
        }

        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != 0) {
            Toast.makeText(activity, "无法连接网络", Toast.LENGTH_SHORT).show();
        }

        activity.finishConnection(listData);
    }

    private Bitmap resizeImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = 100;

        float scaleWidth = ((float) newWidth) / width;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
