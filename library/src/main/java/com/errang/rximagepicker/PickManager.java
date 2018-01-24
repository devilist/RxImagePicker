package com.errang.rximagepicker;

import android.text.TextUtils;
import android.widget.ImageView;

import com.errang.rximagepicker.imageloader.ImageLoader;
import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.VideoFolder;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by zengp on 2017/7/8.
 */
public class PickManager {

    private Config config;
    private ImageLoader imageLoader;
    private ArrayList<Folder> imageFolders;
    private String defCacheFolder;


    private ArrayList<VideoFolder> videoFolders;

    private static PickManager instance;

    public static PickManager getInstance() {
        if (instance == null) {
            synchronized (PickManager.class) {
                if (instance == null) {
                    instance = new PickManager();
                }
            }
        }
        return instance;
    }

    private PickManager() {
    }

    void init(ImageLoader imageLoader, String cacheFolder) {
        this.imageLoader = imageLoader;
        this.defCacheFolder = cacheFolder;
        File file = new File(cacheFolder);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdir();
        if (!file.exists())
            file.mkdir();
    }

    public void showImage(ImageView imageView, String path, int width, int height, boolean isPreview) {
        if (imageLoader == null) {
            throw new NullPointerException("imageLoader is not initialized. you must call 'init()' to initialize !");
        }
        imageLoader.showImage(imageView, path, width, height, isPreview);
    }

    public Config getConfig() {
        return config;
    }

    void setConfig(Config config) {
        this.config = config;
    }

    void setLimit(int limit) {
        this.config.setMaxValue(limit);
    }

    void setShowCamera(boolean showCamera) {
        this.config.setShowCamera(showCamera);
    }

    void setPreview(boolean preview) {
        this.config.setPreview(preview);
    }

    void setCrop(boolean crop) {
        this.config.setCrop(crop);
    }

    void setMode(int mode) {
        this.config.setMode(mode);
    }

    ArrayList<Folder> getImageFolders() {
        return imageFolders;
    }

    void setImageFolders(ArrayList<Folder> imageFolders) {
        this.imageFolders = imageFolders;
    }

    ArrayList<VideoFolder> getVideoFolders() {
        return videoFolders;
    }

    void setVideoFolders(ArrayList<VideoFolder> videoFolders) {
        this.videoFolders = videoFolders;
    }

    public String getDefCacheFolder() {
        File file = new File(defCacheFolder);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdir();
        if (!file.exists())
            file.mkdir();
        return defCacheFolder;
    }

    public String getCacheFolder() {

        String tempCacheFolder = this.config.getTempCacheFolder();

        if (!TextUtils.isEmpty(tempCacheFolder)) {
            File tempFile = new File(tempCacheFolder);
            if (!tempFile.getParentFile().exists())
                tempFile.getParentFile().mkdir();
            if (!tempFile.exists())
                tempFile.mkdir();
            return tempCacheFolder;
        }

        File file = new File(defCacheFolder);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdir();
        if (!file.exists())
            file.mkdir();
        return defCacheFolder;
    }

    void setTempCacheFolder(String folder) {
        this.config.setTempCacheFolder(folder);
    }
}
