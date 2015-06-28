package org.kaka.myreader.task;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.CaptureInfoDao;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.MyBookEntity;
import org.kaka.myreader.fragment.LocalBooksFragment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class ConnectTask extends AsyncTask<Map<String, Object>, Integer, String> {
    private FragmentActivity context;

    public ConnectTask(FragmentActivity context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Map<String, Object>... params) {
        FileOutputStream outputStream = null;
        URLConnection conn;
        BufferedInputStream bis = null;
        byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
        try {
            String id = (String) params[0].get("id");
            String filePath = AppConstants.BASE_URL + params[0].get("path");
            String author = (String) params[0].get("author");
            String detail = (String) params[0].get("detail");
            Bitmap bitmap = (Bitmap) params[0].get("image");

            String path = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String name = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
            String extension = filePath.substring(filePath.lastIndexOf("."));
            URL url = new URL(path + URLEncoder.encode(name, "UTF-8") + extension);
            conn = url.openConnection();
            conn.setConnectTimeout(3000);
            bis = new BufferedInputStream(conn.getInputStream());
            int fileLength = conn.getContentLength();

            // String downloadDir = Environment.getExternalStorageDirectory() + "/MyReader/";
            File dir = new File(AppConstants.APP_DOWNLOAD_DIR);
            if (!dir.exists()) {
                boolean result = dir.mkdirs();
                if (!result) {
                    return "Error: fail to create directory.";
                }
            }
            String fileName = filePath.substring(filePath.lastIndexOf("/"), filePath.length());
            String downloadFile = AppConstants.APP_DOWNLOAD_DIR + fileName;
            File file = new File(downloadFile);
            if (file.exists()) {
                boolean result = file.delete();
                if(!result) {
                    throw new Exception("fail to delete current file.");
                }
            }

            outputStream = new FileOutputStream(downloadFile);
            int total = 0;
            int len;
            while ((len = bis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                total += len;
                publishProgress(total * 100 / fileLength);
            }

            //DB insert
            DaoFactory factory = new DaoFactory(context);
            MyBookDao dao = factory.getMyBookDao();
            MyBookEntity entity = new MyBookEntity();
            entity.setId(String.valueOf(id));
            entity.setName(name);
            entity.setAuthor(author);
            entity.setDetail(detail);
            entity.setPath(downloadFile);
            entity.setImage(bitmap);
            dao.addBook(entity);

            Map<Integer, String> chapterMap = AppUtility.getChapterInfoJSON(id);
            CaptureInfoDao captureInfoDao = factory.getCaptureInfoDao();
            captureInfoDao.insert(id, chapterMap);

            return name + "下载完成!";
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
        LocalBooksFragment fragment = (LocalBooksFragment) context.getSupportFragmentManager().findFragmentByTag(AppConstants.TAG_ARRAY[0]);
        if (fragment != null) {
            fragment.update();
        }
    }
}
