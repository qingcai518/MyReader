package org.kaka.myreader.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.ViewConfiguration;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.kaka.myreader.R;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.MyBookEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.DomUtil;
import nl.siegmann.epublib.util.ResourceUtil;

public class AppUtility {

    public static Map<Integer, String> getChapterInfoJSON(String id) {
        Map<Integer, String> result = new LinkedHashMap<>();
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(AppConstants.CHAPTER_SERVER + id);
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
        try {
            HttpResponse response = client.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                Log.e("Fail to connect:", responseCode + "");
                return result;
            }

            JSONArray array = new JSONArray(EntityUtils.toString(response.getEntity(), "UTF-8"));

            for (int i = 0; i < array.length(); i++) {
                JSONArray item = array.getJSONArray(i);
                int offset = item.getInt(2);
                String chapterName = item.getString(3);
                result.put(offset, chapterName);
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String readFile(String filePath) {
        long startTime = System.currentTimeMillis();
        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), getCharset(file)));
            String str;
            while ((str = reader.readLine()) != null) {
                builder.append(str);
                builder.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();
        Log.e("File read time : ", (endTime - startTime) / 1000 + "s");
        return builder.toString();
    }

    public static List<Resource> readEpubFile(String filePath) {
        List<Resource> list = null;
        try {
            EpubReader epubReader = new EpubReader();
            Book book = epubReader.readEpub(new FileInputStream(filePath));
            list = book.getContents();

        } catch (Exception e) {
            Log.e("AppUtility", e.getMessage());
        }
        return list;
    }

    public static String getEpubContent(List<Resource> resourceList, int index) {
        if (resourceList == null || resourceList.size() <= index) {
            return null;
        }
        Resource resource = resourceList.get(index);
        StringBuilder builder = new StringBuilder();
        try {
            Document document = ResourceUtil.getAsDocument(resource);
            Element element = document.getDocumentElement();
            NodeList pList = element.getElementsByTagName("p");

            for (int i = 0; i < pList.getLength(); i++) {
                Node node = pList.item(i);
                String content = node.getTextContent();
                builder.append(content);
            }
        } catch (Exception e) {
            Log.e("AppUtility", e.getMessage());
        }

        return builder.toString();
    }

    public static String getCharset(File file) {
        String charset = "GBK";
        try {
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(file));
            int head = bis.read() << 8 + bis.read();
            switch (head) {
                case 0xefbb:
                    charset = "UTF-8";
                    break;
                case 0xfffe:
                    charset = "Unicode";
                    break;
                case 0xfeff:
                    charset = "UTF-16BE";
                    break;
                default:
                    charset = "GBK";
                    break;
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }

    public static List<Map<String, Object>> getData(Context context, int currentOrder) {
        List<Map<String, Object>> listData = new ArrayList<>();
        MyBookDao dao = new DaoFactory(context).getMyBookDao();

        List<MyBookEntity> data = dao.getBooks(currentOrder);

        for (MyBookEntity entity : data) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", entity.getId());
            map.put("name", entity.getName());
            map.put("author", entity.getAuthor());
            map.put("detail", entity.getDetail());
            map.put("path", entity.getPath());

            Bitmap image = entity.getImage();
            if (image == null) {
                image = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_txt);
            }
            map.put("image", image);
            map.put("currentOffset", entity.getCurrentOffset());
            listData.add(map);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", "-1");
        map.put("name", "添加更多书籍");
        map.put("author", "");
        map.put("detail", "");
        map.put("path", "");
        map.put("image", BitmapFactory.decodeResource(context.getResources(), R.drawable.addbook));
        map.put("pageIndex", -1);
        listData.add(map);

        return listData;
    }

    public static Map<Integer, String> getCaptureInfo(String contents) {
        int CAPTURE_SIZE = 10000;
        String CAPTURE = "章节-";
        Map<Integer, String> result = new LinkedHashMap<>();

        int count = 0;
        int offset = 0;
        while (contents.length() > 0) {
            result.put(offset, CAPTURE + (++count));

            char lastChar;
            if (contents.length() > CAPTURE_SIZE) {
                lastChar = contents.charAt(CAPTURE_SIZE - 1);
                contents = contents.substring(CAPTURE_SIZE);
            } else {
                break;
            }

            offset += CAPTURE_SIZE;
            if (lastChar != '\n') {
                int index = contents.indexOf("\n") + 1;
                if (index > 0 && index < contents.length()) {
                    contents = contents.substring(index);
                    offset += index;
                    while (contents.startsWith("\n")) {
                        contents = contents.substring(1);
                        offset++;
                    }
                } else {
                    contents = "";
                    offset = contents.length();
                }
            }
        }
        return result;
    }

    public static String convertS2J(String str) {
        StringBuilder builder = new StringBuilder();
        for (char c : str.toCharArray()) {
            int index = AppConstants.SIMPLES.indexOf(c);
            if (index < 0) {
                builder.append(c);
            } else {
                builder.append(AppConstants.TRADITIONS.charAt(index));
            }
        }
        return builder.toString();
    }

    public static String convertJ2S(String str) {
        StringBuilder builder = new StringBuilder();
        for (char c : str.toCharArray()) {
            int index = AppConstants.TRADITIONS.indexOf(c);
            if (index < 0) {
                builder.append(c);
            } else {
                builder.append(AppConstants.SIMPLES.charAt(index));
            }
        }
        return builder.toString();
    }

    public static int getVirtualButtonHeight(Context context, Display display, int leftHeight) {
        int height = 0;
        if (!ViewConfiguration.get(context).hasPermanentMenuKey()) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (AppConstants.API_LEVEL >= 17) {
                display.getRealMetrics(displayMetrics);
            } else {
                display.getMetrics(displayMetrics);
            }
            int realHeight = displayMetrics.heightPixels;
            height = realHeight - leftHeight;
        }

        return height;
    }

    public static double getFileSize(File file) {
        double result = 0;
        try (FileInputStream fis = new FileInputStream(file)) {
            result = ((double) fis.available()) / 1024;
        } catch (Exception e) {
            Log.e("getFileSize::", e.getMessage());
        }
        return result;
    }

    public static String getDateTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }

    public static String getAuthCode(int digits) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits; i++) {

            builder.append(String.valueOf(((Double) (Math.random() * 10))).charAt(0));
        }
        return builder.toString();
    }

    public static boolean isMailAddress(String mailAddress) {
        if (mailAddress == null || mailAddress.length() == 0) {
            return false;
        }

        if (!mailAddress.contains("@") || !mailAddress.contains(".")) {
            return false;
        }

        return true;
    }
}
