package org.kaka.myreader.task;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.kaka.myreader.activity.CloudDetailActivity;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.CaptureInfoDao;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.MyBookEntity;
import org.kaka.myreader.fragment.CloudFragment;
import org.kaka.myreader.fragment.LocalBooksFragment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class FileDownloadTask extends AsyncTask<Map<String, Object>, Long, String> {
    private Context context;
    private String id;
    private String fileName;

    public FileDownloadTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Map<String, Object>... params) {
        long startTime = System.currentTimeMillis();
        FileOutputStream outputStream = null;
        URLConnection conn;
        BufferedInputStream bis = null;
        byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
        try {
            id = (String) params[0].get(CloudFragment.KEY_ID);
            String filePath = AppConstants.BASE_URL + params[0].get(CloudFragment.KEY_PATH);
            String author = (String) params[0].get(CloudFragment.KEY_AUTHOR);
            String detail = (String) params[0].get(CloudFragment.KEY_DETAIL);
            fileName = (String) params[0].get(CloudFragment.KEY_NAME);
//            Bitmap bitmap = (Bitmap) params[0].get(CloudFragment.KEY_IMAGE);
            byte[] decodedString = Base64.decode((String)params[0].get(CloudFragment.KEY_IMAGE), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            String path = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String name = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
            String extension = filePath.substring(filePath.lastIndexOf("."));
            Log.i("FileDownloadTask", path + URLEncoder.encode(name, "UTF-8") + extension);
            URL url = new URL(path + URLEncoder.encode(name, "UTF-8") + extension);
            conn = url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setRequestProperty("Accept-Encoding", "identity");
            bis = new BufferedInputStream(conn.getInputStream());
            long fileLength = conn.getContentLength();
            Log.i("FileDownloadTask", "fileLength=" + fileLength);

            File dir = new File(AppConstants.APP_DOWNLOAD_DIR);
            if (!dir.exists()) {
                boolean result = dir.mkdirs();
                if (!result) {
                    return "Error: fail to create directory.";
                }
            }

            String downloadFile = AppConstants.APP_DOWNLOAD_DIR + filePath.substring(filePath.lastIndexOf("/"), filePath.length());
            File file = new File(downloadFile);
            if (file.exists()) {
                boolean result = file.delete();
                if (!result) {
                    throw new Exception("fail to delete current file.");
                }
            }

            outputStream = new FileOutputStream(downloadFile);
            long total = 0;
            int len;
            while ((len = bis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                total += len;
                publishProgress(total, fileLength, startTime);
            }

            //DB insert
            DaoFactory factory = new DaoFactory(context);
            MyBookDao dao = factory.getMyBookDao();
            MyBookEntity entity = new MyBookEntity();
            entity.setId(String.valueOf(id));
            entity.setName(fileName);
            entity.setAuthor(author);
            entity.setDetail(detail);
            entity.setPath(downloadFile);
            entity.setImage(bitmap);
            dao.addBook(entity);

            Map<Integer, String> chapterMap = AppUtility.getChapterInfoJSON(id);
            CaptureInfoDao captureInfoDao = factory.getCaptureInfoDao();
            captureInfoDao.insert(id, chapterMap);
            publishProgress(-1l);

            return fileName + "下载完成!";
        } catch (Exception e) {
            return e.toString() + e.getMessage();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        if (context instanceof CloudDetailActivity) {
            CloudDetailActivity activity = (CloudDetailActivity) context;
            activity.finishDownload();
        } else if (context instanceof FragmentActivity) {
            FragmentActivity fragmentActivity = (FragmentActivity) context;
            LocalBooksFragment fragment = (LocalBooksFragment) fragmentActivity.getSupportFragmentManager().
                    findFragmentByTag(AppConstants.TAG_ARRAY[0]);
            if (fragment != null) {
                fragment.update();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        if (values[0] == -1) {
            Intent intent = new Intent(AppConstants.BROADCAST_DOWNLOAD);
            intent.putExtra("id", id);
            context.sendBroadcast(intent);
            return;
        }
        long endTime = System.currentTimeMillis();
        double downloadSize = ((double) values[0]) / 1024;
        double totalSize = ((double) values[1]) / 1024;

        String speed = AppConstants.DECIMAL_FORMAT.format(downloadSize * 1000 / (endTime - values[2]));

        Intent intent = new Intent(AppConstants.BROADCAST_DOWNLOAD);
        intent.putExtra("downloadSize", AppConstants.DECIMAL_FORMAT.format(downloadSize));
        intent.putExtra("totalSize", AppConstants.DECIMAL_FORMAT.format(totalSize));
        intent.putExtra("speed", speed);
        intent.putExtra("name", fileName);
        intent.putExtra("id", id);
        context.sendBroadcast(intent);
    }
}
