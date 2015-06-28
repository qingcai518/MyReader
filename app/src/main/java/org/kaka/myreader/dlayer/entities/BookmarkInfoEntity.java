package org.kaka.myreader.dlayer.entities;

import java.sql.Timestamp;

public class BookmarkInfoEntity {
    private int id;
    private int offset;
    private String captureName;
    private String progress;
    private Timestamp addDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getCaptureName() {
        return captureName;
    }

    public void setCaptureName(String captureName) {
        this.captureName = captureName;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public Timestamp getAddDate() {
        return addDate;
    }

    public void setAddDate(Timestamp addDate) {
        this.addDate = addDate;
    }
}
