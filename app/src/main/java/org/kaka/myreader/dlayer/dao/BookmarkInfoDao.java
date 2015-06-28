package org.kaka.myreader.dlayer.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.kaka.myreader.dlayer.entities.BookmarkInfoEntity;

import java.util.ArrayList;
import java.util.List;

public class BookmarkInfoDao {
    private SQLiteDatabase db;
    private final static String TABLE_NAME = "BookmarkInfo";

    public BookmarkInfoDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(BookmarkInfoEntity entity) {
        if (exist(entity)) {
            return -1;
        }
        Log.i("inert id", entity.getId() + "");
        ContentValues values = new ContentValues();
        values.put("id", entity.getId());
        values.put("offset", entity.getOffset());
        values.put("captureName", entity.getCaptureName());
        values.put("progress", entity.getProgress());

        return db.insert(TABLE_NAME, null, values);
    }

    public int delete(int id, int offset) {
        String whereClause = "id=? and offset=?";
        String[] args = new String[]{id + "", offset + ""};
        return db.delete(TABLE_NAME, whereClause, args);
    }

    public int deleteAll(int id) {
        String whereClause = "id=?";
        String[] args = new String[]{id + ""};
        return db.delete(TABLE_NAME, whereClause, args);
    }

    public boolean exist(BookmarkInfoEntity entity) {
        String sql = "select * from " + TABLE_NAME + " where id=? and offset=?";
        Cursor queryCursor = null;
        boolean result = false;
        try {
            queryCursor = db.rawQuery(sql, new String[]{entity.getId() + "", entity.getOffset() + ""});
            result = queryCursor.getCount() > 0;
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }

    public List<BookmarkInfoEntity> selectById(int id) {
        Log.i("select id", id + "");
        List<BookmarkInfoEntity> result = new ArrayList<>();
        String sql = "select * from " + TABLE_NAME + " where id=? order by addDate";
        Cursor queryCursor = null;
        try {
            queryCursor = db.rawQuery(sql, new String[]{id + ""});
            while (queryCursor.moveToNext()) {
                BookmarkInfoEntity entity = new BookmarkInfoEntity();
                entity.setId(queryCursor.getInt(queryCursor.getColumnIndex("id")));
                entity.setOffset(queryCursor.getInt(queryCursor.getColumnIndex("offset")));
                entity.setCaptureName(queryCursor.getString(queryCursor.getColumnIndex("captureName")));
                entity.setProgress(queryCursor.getString(queryCursor.getColumnIndex("progress")));

                result.add(entity);
            }
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }
}
