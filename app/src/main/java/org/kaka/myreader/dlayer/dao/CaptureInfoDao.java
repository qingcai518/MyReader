package org.kaka.myreader.dlayer.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class CaptureInfoDao {
    private SQLiteDatabase db;
    private final static String TABLE_NAME = "CaptureInfo";
    private final static String TAG = "CaptureInfoDao";

    public CaptureInfoDao(SQLiteDatabase db) {
        this.db = db;
    }

    public Map<Integer, String> getCaptureInfo(String id) {
        Map<Integer, String> result = new LinkedHashMap<>();
        String sql = "select * from " + TABLE_NAME + " where id=? order by captureId";
        Cursor queryCursor = null;
        try {
            queryCursor = db.rawQuery(sql, new String[]{id});
            while (queryCursor.moveToNext()) {
                String captureName = queryCursor.getString(queryCursor.getColumnIndex("captureName"));
                int offset = queryCursor.getInt(queryCursor.getColumnIndex("offset"));
                result.put(offset, captureName);
            }
        } finally {
            if (queryCursor != null) {
                queryCursor.close();
            }
        }
        return result;
    }

    public void insert(String id, Map<Integer, String> data) {
        int captureId = 0;
        for (Integer offset : data.keySet()) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("captureId", ++captureId);
            values.put("captureName", data.get(offset));
            values.put("offset", offset);
            db.insert(TABLE_NAME, null, values);
        }
    }

    public void delete(String id) {
        int result = db.delete(TABLE_NAME, "id=?", new String[]{id});
        Log.d(TAG, "delete result = " + result);
    }
}
