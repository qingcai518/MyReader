package org.kaka.myreader.common;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDBHelper extends SQLiteOpenHelper {
    private final static String DB_NAME = "MyReader.db";
    private final static int DB_VERSION = 1;
    private final static String CREATE_TABLE_MYBOOK =
            "CREATE TABLE IF NOT EXISTS MyBook (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "author VARCHAR(64) NOT NULL, " +
                    "detail TEXT," +
                    "path VARCHAR(256) NOT NULL," +
                    "image BLOB, " +
                    "currentOffset INTEGER DEFAULT 0, " +
                    "currentChapterIndexForEpub INTEGER DEFAULT 1, " +
                    "downloadDate Timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "readDate Timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP) ";
    private final static String CREATE_TABLE_CAPTUREINFO =
            "CREATE TABLE IF NOT EXISTS CaptureInfo (" +
                    "id VARCHAR(64), " +
                    "captureId INTEGER DEFAULT 0, " +
                    "captureName VARCHAR(128) NOT NULL, " +
                    "offset INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (id) REFERENCES MyBooks(id), " +
                    "PRIMARY KEY (id, captureId))";
    private final static String CREATE_TABLE_BOOKMARKINFO =
            "CREATE TABLE IF NOT EXISTS BookmarkInfo (" +
                    "id VARCHAR(64), " +
                    "offset INTEGER DEFAULT 0, " +
                    "captureName VARCHAR(128) NOT NULL, " +
                    "progress VARCHAR(16) NOT NULL, " +
                    "addDate Timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (id) REFERENCES MyBooks(id), " +
                    "PRIMARY KEY (id, offset)) ";

    private final static String CREATE_INDEX_CAPTUREINFO_INDEX_ID =
            "CREATE INDEX CaptureInfo_Index_Id ON CaptureInfo (id) ";
    private final static String CREATE_INDEX_BOOKMARKINFO_INDEX_ID =
            "CREATE INDEX BookmarkInfo_Index_Id ON BookmarkInfo (id) ";

    public AppDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MYBOOK);
        db.execSQL(CREATE_TABLE_CAPTUREINFO);
        db.execSQL(CREATE_TABLE_BOOKMARKINFO);
        db.execSQL(CREATE_INDEX_CAPTUREINFO_INDEX_ID);
        db.execSQL(CREATE_INDEX_BOOKMARKINFO_INDEX_ID);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
