package org.kaka.myreader.dlayer.entities;

import android.graphics.Bitmap;

import java.sql.Timestamp;
import java.util.Map;

public class MyBookEntity {
    private String id;
    private String name;
    private String author;
    private String detail;
    private String path;
    private Bitmap image;
    private int currentOffset;
    private int currentChapterIndexForEpub;
    private Timestamp downloadDate;
    private Timestamp readDate;

    private Map<Integer, String> chapterInfo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public int getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = currentOffset;
    }

    public int getCurrentChapterIndexForEpub() {
        return currentChapterIndexForEpub;
    }

    public void setCurrentChapterIndexForEpub(int currentChapterIndexForEpub) {
        this.currentChapterIndexForEpub = currentChapterIndexForEpub;
    }

    public Timestamp getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(Timestamp downloadDate) {
        this.downloadDate = downloadDate;
    }

    public Timestamp getReadDate() {
        return readDate;
    }

    public void setReadDate(Timestamp readDate) {
        this.readDate = readDate;
    }

    public Map<Integer, String> getChapterInfo() {
        return chapterInfo;
    }

    public void setChapterInfo(Map<Integer, String> chapterInfo) {
        this.chapterInfo = chapterInfo;
    }
}
