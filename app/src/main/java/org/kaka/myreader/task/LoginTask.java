package org.kaka.myreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.kaka.myreader.activity.LoginActivity;
import org.kaka.myreader.common.AppConstants;

public class LoginTask extends AsyncTask<String, Integer, Integer> {
    private final static String TAG = "ConnectTask";
    private Context context;
    private String[] infos;

    public LoginTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String userId = params[0];
        String inputPass = params[1];
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(AppConstants.USER_SERVER + userId);
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
        try {
            HttpResponse response = client.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                String msg = "Fail to connect:" + response.getStatusLine().getReasonPhrase();
                throw new Exception(msg);
            }

            JSONArray array = new JSONArray(EntityUtils.toString(response.getEntity(), "UTF-8"));

            if (array.length() == 0) {
                return 1;
            }

            JSONArray item = array.getJSONArray(0);
            String userName = item.getString(2);
            String password = item.getString(3);
            if (inputPass != null && inputPass.equals(password)) {
                String sex = item.getString(4);
                int point = item.getInt(5);
                String userImage = item.getString(6);
//                byte[] data = Base64.decode(item.getString(6), Base64.DEFAULT);
//                Bitmap bitmap = null;
//                if (data.length != 0) {
//                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                }
                infos = new String[4];
                infos[0] = userName;
                infos[1] = point + "";
                infos[2] = sex;
                infos[3] = userImage;
            } else {
                return 1;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return 2;
        }

        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (context instanceof LoginActivity) {
            ((LoginActivity) context).finishTask(result, infos);
        }
    }
}
