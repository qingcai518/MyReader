package org.kaka.myreader.task;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.kaka.myreader.activity.AddCommitActivity;
import org.kaka.myreader.common.AppConstants;

import java.util.ArrayList;
import java.util.List;

public class AddCommitTask extends AsyncTask<String, Integer, Integer> {
    private final static String TAG = "AddCommitTask";
    private AddCommitActivity activity;

    public AddCommitTask(AddCommitActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String commitText = params[0];
        String ratingValue = params[1];
        String bookId = params[2];
        String userId = params[3];

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(AppConstants.ADD_COMMIT_SERVER);

            List<NameValuePair> nameValueList = new ArrayList<>();
            nameValueList.add(new BasicNameValuePair("bookId", bookId));
            nameValueList.add(new BasicNameValuePair("userId", userId));
            nameValueList.add(new BasicNameValuePair("star", ratingValue));
            nameValueList.add(new BasicNameValuePair("commit", commitText));

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
        if (result != 0) {
            Toast.makeText(activity, "无法连接网络", Toast.LENGTH_SHORT).show();
        }

        activity.finishConnection();
    }
}
