package org.kaka.myreader.dlayer.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

    public List<MyBookEntity> getBooks() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        List<MyBookEntity> result = new ArrayList<>();
        Cursor queryCursor = null;
        try {
            queryCursor = db.rawQuery(sql, null);
            while (queryCursor.moveToNext()) {
                MyBookEntity entity = new MyBookEntity();
                entity.setId(queryCursor.getInt(queryCursor.getColumnIndex("id")));
                entity.setName(queryCursor.getString(queryCursor.getColumnIndex("name")));
                entity.setAuthor(queryCursor.getString(queryCursor.getColumnIndex("author")));
                entity.setDetail(queryCursor.getString(queryCursor.getColumnIndex("detail")));
                entity.setPath(queryCursor.getString(queryCursor.getColumnIndex("path")));

                byte[] imageData = queryCursor.getBlob(queryCursor.getColumnIndex("image"));
                Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                entity.setImage(image);

                entity.setCurrentOffset(queryCursor.getInt(queryCursor.getColumnIndex("currentOffset")));

                result.add(entity);
            }
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }

        return result;
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

    public int updateCurrentOffset(int id, int currentOffset) {
        ContentValues values = new ContentValues();
        values.put("currentOffset", currentOffset);
        return db.update(TABLE_NAME, values, "id=?", new String[]{id + ""});
    }

    public int getCurrentOffset(int id) {
        String sql = "select currentOffset from " + TABLE_NAME + " where id=?";
        Cursor queryCursor = null;
        int result = 0;
        try {
            queryCursor = db.rawQuery(sql, new String[]{id + ""});
            queryCursor.moveToNext();
            result = queryCursor.getInt(queryCursor.getColumnIndex("currentOffset"));
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }
}
