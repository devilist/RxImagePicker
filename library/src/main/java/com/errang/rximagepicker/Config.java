package com.errang.rximagepicker;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by zengp on 2017/7/8.
 */

public class Config {

    private int maxValue = 9;
    private boolean showCamera = false;
    private boolean isPreview = true;
    private boolean isCrop = true;

    public static final int SINGLE = 0;
    public static final int MULTIPLE = 1;
    private int mode = SINGLE;
    // a temp cache folder to store created image if it is not allowed to put into the default
    // cache folder for some reasons(for example , cache images from image chat messages in the
    // IM chatting system at most time are not hoped to delete ,otherwise history image messages
    // may be missing).
    // this folder string  is temporary, means that it is always null after the picking image work
    // finishes at each time;
    private String tempCacheFolder;

    @IntDef({SINGLE, MULTIPLE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Mode {
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isShowCamera() {
        return showCamera;
    }

    public void setShowCamera(boolean showCamera) {
        this.showCamera = showCamera;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public void setPreview(boolean preview) {
        isPreview = preview;
    }

    public boolean isCrop() {
        return isCrop;
    }

    public void setCrop(boolean crop) {
        isCrop = crop;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getTempCacheFolder() {
        return tempCacheFolder;
    }

    public void setTempCacheFolder(String tempCacheFolder) {
        this.tempCacheFolder = tempCacheFolder;
    }
}
