package org.kaka.myreader.dlayer.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
        ContentValues values = new ContentValues();
        values.put("id", entity.getId());
        values.put("chapterIndex", entity.getChapterIndex());
        values.put("offset", entity.getOffset());
        values.put("captureName", entity.getChapterName());
        values.put("progress", entity.getProgress());

        return db.insert(TABLE_NAME, null, values);
    }

    public int delete(String id, int chapterIndex, int offset) {
        String whereClause = "id=? and chapterIndex=? and offset=?";
        String[] args = new String[]{id, chapterIndex + "", offset + ""};
        return db.delete(TABLE_NAME, whereClause, args);
    }

    public int deleteAll(String id) {
        String whereClause = "id=?";
        String[] args = new String[]{id};
        return db.delete(TABLE_NAME, whereClause, args);
    }

    public boolean exist(BookmarkInfoEntity entity) {
        String sql = "select * from " + TABLE_NAME + " where id=? and chapterIndex=? and offset=?";
        Cursor queryCursor = null;
        boolean result = false;
        try {
            queryCursor = db.rawQuery(sql, new String[]{entity.getId(), entity.getChapterIndex() + "", entity.getOffset() + ""});
            result = queryCursor.getCount() > 0;
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }

    public List<BookmarkInfoEntity> selectById(String id) {
        List<BookmarkInfoEntity> result = new ArrayList<>();
        String sql = "select * from " + TABLE_NAME + " where id=? order by addDate";
        Cursor queryCursor = null;
        try {
            queryCursor = db.rawQuery(sql, new String[]{id});
            while (queryCursor.moveToNext()) {
                BookmarkInfoEntity entity = new BookmarkInfoEntity();
                entity.setId(queryCursor.getString(queryCursor.getColumnIndex("id")));
                entity.setChapterIndex(queryCursor.getInt(queryCursor.getColumnIndex("chapterIndex")));
                entity.setOffset(queryCursor.getInt(queryCursor.getColumnIndex("offset")));
                entity.setChapterName(queryCursor.getString(queryCursor.getColumnIndex("captureName")));
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
