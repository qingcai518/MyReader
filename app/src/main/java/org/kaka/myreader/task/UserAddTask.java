package org.kaka.myreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.kaka.myreader.activity.RegisterInfoActivity;
import org.kaka.myreader.common.AppConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAddTask extends AsyncTask<Map<String, String>, Integer, Integer> {
    private Context context;
    private final static String TAG = "UserAddTask";

    public UserAddTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(Map<String, String>... params) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(AppConstants.USER_ADD_SERVER);

            List<NameValuePair> nameValueList = new ArrayList<>();

            for (String key : params[0].keySet()) {
                String value = params[0].get(key);
                nameValueList.add(new BasicNameValuePair(key, value));
                Log.i(TAG, key + " = " + value);
            }

            httpPost.setEntity(new UrlEncodedFormEntity(nameValueList, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(httpPost);

            int status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                return 0;
            } else {
                String message = response.getStatusLine().getReasonPhrase();
                throw new Exception(message);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return 1;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (context instanceof RegisterInfoActivity) {
            ((RegisterInfoActivity) context).finishTask(result);
        }
    }
}
