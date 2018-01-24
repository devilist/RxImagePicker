package com.errang.rximagepicker;

import android.content.Context;

import com.errang.rximagepicker.imageloader.ImageLoader;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.model.Video;

import java.util.List;

import io.reactivex.Observable;

/**
 * Created by zengp on 2017/7/8.
 */

public class RxImagePicker {

    public static void init(ImageLoader loader, String cacheFolder) {
        PickManager.getInstance().init(loader, cacheFolder);
    }

    private RxImagePicker(Config config) {
        PickManager.getInstance().setConfig(config);
    }

    public static RxImagePicker ready() {
        return new RxImagePicker(new Config());
    }

    public static RxImagePicker ready(Config config) {
        return new RxImagePicker(config);
    }

    public RxImagePicker single(boolean isSingle) {
        PickManager.getInstance().setMode(isSingle ? Config.SINGLE : Config.MULTIPLE);
        return this;
    }

    public RxImagePicker camera(boolean showCamera) {
        PickManager.getInstance().setShowCamera(showCamera);
        return this;
    }

    public RxImagePicker crop(boolean crop) {
        PickManager.getInstance().setCrop(crop);
        return this;
    }

    public RxImagePicker tempCacheFolder(String folder) {
        PickManager.getInstance().setTempCacheFolder(folder);
        return this;
    }

    public RxImagePicker preview(boolean preview) {
        PickManager.getInstance().setPreview(preview);
        return this;
    }

    public RxImagePicker limit(int limit) {
        PickManager.getInstance().setLimit(limit);
        return this;
    }

    public Observable<List<Image>> fire(Context context) {
        ObservableManager.getInstance().createSelectedObservable();
        ImageGridActivity.start(context);
        return ObservableManager.getInstance().getSelectedSubject();
    }

    public Observable<List<Image>> go(Context context) {
        ObservableManager.getInstance().createSelectedObservable();
        if (PickManager.getInstance().getConfig().isShowCamera()) {
            ImageCameraActivity.start(context);
        } else
            Image1FolderListActivity.start(context);
        return ObservableManager.getInstance().getSelectedSubject();
    }

    public Observable<List<Video>> goVideo(Context context) {
        ObservableManager.getInstance().createVideoSelectObservable();
        VideoFolderListActivity.start(context);
        return ObservableManager.getInstance().getVideoSelectSubject();
    }
}
