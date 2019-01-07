package com.customcamera.gallery.model;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class MediaData extends RealmObject {

    @PrimaryKey
    private String id = UUID.randomUUID().toString();
    private String filePath;
    private String outPutFilePath;
    private int fileType;
    private String fileCaption;
    private int startTime;
    private int endTime;
    private int mDuration;
    private float mThumb1Pos;
    private float mThumb2Pos;
    private boolean isTrimmed;

    public float getmThumb1Pos() {
        return mThumb1Pos;
    }

    public void setmThumb1Pos(float mThumb1Pos) {
        this.mThumb1Pos = mThumb1Pos;
    }

    public float getmThumb2Pos() {
        return mThumb2Pos;
    }

    public void setmThumb2Pos(float mThumb2Pos) {
        this.mThumb2Pos = mThumb2Pos;
    }

    public String getOutPutFilePath() {
        return outPutFilePath;
    }

    public void setOutPutFilePath(String outPutFilePath) {
        this.outPutFilePath = outPutFilePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isTrimmed() {
        return isTrimmed;
    }

    public void setTrimmed(boolean trimmed) {
        isTrimmed = trimmed;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getmDuration() {
        return mDuration;
    }

    public void setmDuration(int mDuration) {
        this.mDuration = mDuration;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getFileCaption() {
        return fileCaption;
    }

    public void setFileCaption(String fileCaption) {
        this.fileCaption = fileCaption;
    }
}
