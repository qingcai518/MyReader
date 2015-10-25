package org.kaka.myreader.dlayer.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.entities.MyBookEntity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MyBookDao {
    private SQLiteDatabase db;
    private final static String TABLE_NAME = "MyBook";

    public MyBookDao(SQLiteDatabase db) {
        this.db = db;
    }

    public List<MyBookEntity> getBooks(int currentOrder) {
        String orderColumn = "readDate";
        String order = " DESC";
        switch (currentOrder) {
            case AppConstants.ORDER_AUTHOR:
                orderColumn = "author";
                order = " ASC";
                break;
            case AppConstants.ORDER_BOOKNAME:
                orderColumn = "name";
                order = " ASC";
                break;
            case AppConstants.ORDER_DOWNLOAD:
                orderColumn = "downloadDate";
                order = " ASC";
                break;
        }
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + orderColumn + order;
        List<MyBookEntity> result = new ArrayList<>();
        Cursor queryCursor = null;
        try {
            queryCursor = db.rawQuery(sql, null);
            while (queryCursor.moveToNext()) {
                MyBookEntity entity = new MyBookEntity();
                entity.setId(queryCursor.getString(queryCursor.getColumnIndex("id")));
                entity.setName(queryCursor.getString(queryCursor.getColumnIndex("name")));
                entity.setAuthor(queryCursor.getString(queryCursor.getColumnIndex("author")));
                entity.setDetail(queryCursor.getString(queryCursor.getColumnIndex("detail")));
                entity.setPath(queryCursor.getString(queryCursor.getColumnIndex("path")));

                byte[] imageData = queryCursor.getBlob(queryCursor.getColumnIndex("image"));
                Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                entity.setImage(image);

                entity.setCurrentOffset(queryCursor.getInt(queryCursor.getColumnIndex("currentOffset")));
                entity.setCurrentChapterIndexForEpub(queryCursor.getInt(queryCursor.getColumnIndex("currentChapterIndexForEpub")));

                result.add(entity);
            }
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }

        return result;
    }

    public boolean exist(String bookName) {
        String sql = "SELECT count(*) FROM " + TABLE_NAME + " WHERE name=?";
        Cursor queryCursor = null;
        int result = 0;
        try {
            queryCursor = db.rawQuery(sql, new String[]{bookName});
            queryCursor.moveToNext();
            result = queryCursor.getInt(0);
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }

        return result != 0;
    }

    public long addBook(MyBookEntity entity) {
        ContentValues values = new ContentValues();
        values.put("id", entity.getId());
        values.put("name", entity.getName());
        values.put("author", entity.getAuthor());
        values.put("detail", entity.getDetail());
        values.put("path", entity.getPath());
        Bitmap image = entity.getImage();
        if (image == null) {
            values.put("image", new byte[]{});
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, bos);
            values.put("image", bos.toByteArray());
        }

        return db.insert(TABLE_NAME, null, values);
    }

    public void deleteBook(String id) {
        int result = db.delete(TABLE_NAME, "id=?", new String[]{id});
        Log.i("delete result", result + "");
    }

    public List<String> getFilePaths(String prefix) {
        String sql = "SELECT path FROM " + TABLE_NAME + " WHERE id LIKE '" + prefix + "%'";
        List<String> result = new ArrayList<>();
        Cursor queryCursor = null;
        try {
            queryCursor = db.rawQuery(sql, null);
            while (queryCursor.moveToNext()) {
                String filePath = queryCursor.getString(queryCursor.getColumnIndex("path"));
                result.add(filePath);
            }
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }

    public int updateCurrentOffset(String id, int currentOffset) {
        ContentValues values = new ContentValues();
        values.put("currentOffset", currentOffset);
        return db.update(TABLE_NAME, values, "id=?", new String[]{id});
    }


    public int updateCurrentOffset(String id, int currentChapterIndexForEpub, int currentOffset) {
        ContentValues values = new ContentValues();
        values.put("currentOffset", currentOffset);
        values.put("currentChapterIndexForEpub", currentChapterIndexForEpub);
        return db.update(TABLE_NAME, values, "id=?", new String[]{id});
    }

    public int updateReadTime(String id) {
        ContentValues values = new ContentValues();
        values.put("readDate", AppUtility.getDateTime());
        return db.update(TABLE_NAME, values, "id=?", new String[]{id});
    }

    public int[] getCurrentOffset(String id) {
        String sql = "select currentOffset, currentChapterIndexForEpub from " + TABLE_NAME + " where id=?";
        Cursor queryCursor = null;
        int[] result = new int[2];
        try {
            queryCursor = db.rawQuery(sql, new String[]{id + ""});
            queryCursor.moveToNext();
            result[0] = queryCursor.getInt(queryCursor.getColumnIndex("currentOffset"));
            result[1] = queryCursor.getInt(queryCursor.getColumnIndex("currentChapterIndexForEpub"));
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }
}
