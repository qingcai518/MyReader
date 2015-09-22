package org.kaka.myreader.task;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.kaka.myreader.activity.PhotoClipActivity;
import org.kaka.myreader.common.AppConstants;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class UserUpdateTask extends AsyncTask<Bitmap, Integer, Integer> {
    private Context context;
    private final static String TAG = "UserUpdateTask";

    public UserUpdateTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(Bitmap... params) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(AppConstants.USER_UPDATE_SERVER);

            List<NameValuePair> nameValueList = new ArrayList<>();

            SharedPreferences preferences = context.getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
            String userId = preferences.getString(AppConstants.PREF_KEY_USERID, "");
            nameValueList.add(new BasicNameValuePair("userId", userId));

            Bitmap bitmap = params[0];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);//参数100表示不压缩
            byte[] bytes = bos.toByteArray();
            String imageStr = Base64.encodeToString(bytes, Base64.DEFAULT);
            nameValueList.add(new BasicNameValuePair("image", imageStr));

            httpPost.setEntity(new UrlEncodedFormEntity(nameValueList, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(httpPost);

            int status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(AppConstants.PREF_KEY_USER_IMAGE, imageStr);
                editor.apply();
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
        if (context instanceof PhotoClipActivity) {
            ((PhotoClipActivity) context).finishTask();
        }
    }
}
