package org.kaka.myreader.fragment;

import android.graphics.drawable.Drawable;

import java.io.File;

public class ItemHolder {
    private File file;
    private String fileName;
    private String fileDetail;
    private Drawable fileLogo;
    private boolean imported;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileDetail() {
        return fileDetail;
    }

    public void setFileDetail(String fileDetail) {
        this.fileDetail = fileDetail;
    }

    public Drawable getFileLogo() {
        return fileLogo;
    }

    public void setFileLogo(Drawable fileLogo) {
        this.fileLogo = fileLogo;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }
}