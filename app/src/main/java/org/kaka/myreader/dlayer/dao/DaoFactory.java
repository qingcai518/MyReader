package org.kaka.myreader.dlayer.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.kaka.myreader.common.AppDBHelper;

public class DaoFactory {
    private SQLiteDatabase db;

    public DaoFactory(Context context) {
        AppDBHelper dbHelper = new AppDBHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void closeDB() {
        if (db != null) {
            db.close();
        }
    }

    public MyBookDao getMyBookDao() {
        return new MyBookDao(db);
    }

    public CaptureInfoDao getCaptureInfoDao() {
        return new CaptureInfoDao(db);
    }

    public BookmarkInfoDao getBookmarkInfoDao() {
        return new BookmarkInfoDao(db);
    }
}
