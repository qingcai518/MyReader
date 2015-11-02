package org.kaka.myreader.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.fragment.CloudFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectTask extends AsyncTask<List<Map<String, Object>>, Integer, Integer> {
    private final static String TAG = "ConnectTask";
    private CloudFragment fragment;

    public ConnectTask(CloudFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    protected Integer doInBackground(List<Map<String, Object>>... params) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(AppConstants.SERVER);
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
        try {
            List<Map<String, Object>> listData = params[0];
            HttpResponse response = client.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                throw new Exception(responseCode + " : " + response.getStatusLine().getReasonPhrase());
            }

            JSONArray array = new JSONArray(EntityUtils.toString(response.getEntity(), "UTF-8"));
            List<Map<String, Object>> tempList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONArray item = array.getJSONArray(i);
                Map<String, Object> map = new HashMap<>();

                map.put(CloudFragment.KEY_ID, String.valueOf(item.getInt(0)));
                map.put(CloudFragment.KEY_NAME, item.getString(1));
                map.put(CloudFragment.KEY_AUTHOR, item.getString(2));
                map.put(CloudFragment.KEY_DETAIL, item.getString(3));
                map.put(CloudFragment.KEY_PATH, item.getString(4));
                map.put(CloudFragment.KEY_IMAGE, item.getString(5));

//                byte[] data = Base64.decode(item.getString(5), Base64.DEFAULT);
//                Bitmap bitmap = null;
//                if (data.length != 0) {
//                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                }
//                map.put(CloudFragment.KEY_IMAGE, bitmap);
                map.put(CloudFragment.KEY_SIZE, item.getString(6));
                map.put(CloudFragment.KEY_SCORE, item.getDouble(7));
                tempList.add(map);
            }

            if (tempList.size() > 0) {
                listData.clear();
                listData.addAll(tempList);
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
            Toast.makeText(fragment.getActivity(), "无法连接网络", Toast.LENGTH_SHORT).show();
        }

        fragment.finishConnection();
    }
}
