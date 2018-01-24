package com.errang.rximagepicker;

import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.model.Video;
import com.errang.rximagepicker.model.VideoFolder;

import java.util.List;

import io.reactivex.subjects.PublishSubject;

/**
 * Created by zengp on 2017/7/9.
 */

class ObservableManager {

    // observable  image
    private PublishSubject<List<Folder>> imageLoadSubject;
    private PublishSubject<List<Image>> imageSelectSubject;
    private PublishSubject<Image> imageSelectChangedSubject;
    private PublishSubject<Image> imageCropSubject;

    // observable  video
    private PublishSubject<List<VideoFolder>> videoLoadSubject;
    private PublishSubject<List<Video>> videoSelectSubject;

    private static ObservableManager instance;

    public static ObservableManager getInstance() {
        if (instance == null) {
            synchronized (ObservableManager.class) {
                if (instance == null) {
                    instance = new ObservableManager();
                }
            }
        }
        return instance;
    }

    private ObservableManager() {
    }

    void createSelectedObservable() {
        if (null != imageSelectSubject) {
            imageSelectSubject = null;
        }
        imageSelectSubject = PublishSubject.create();
    }

    PublishSubject<List<Image>> getSelectedSubject() {
        return imageSelectSubject;
    }

    void createLoadObservable() {
        if (null != imageLoadSubject) {
            imageLoadSubject = null;
        }
        imageLoadSubject = PublishSubject.create();
    }

    PublishSubject<List<Folder>> getLoadSubject() {
        return imageLoadSubject;
    }

    void createChangedObservable() {
        if (null != imageSelectChangedSubject) {
            imageSelectChangedSubject = null;
        }
        imageSelectChangedSubject = PublishSubject.create();
    }

    PublishSubject<Image> getChangedSubject() {
        return imageSelectChangedSubject;
    }

    void createCropObservable() {
        if (null != imageCropSubject) {
            imageCropSubject = null;
        }
        imageCropSubject = PublishSubject.create();
    }

    PublishSubject<Image> getCropSubject() {
        return imageCropSubject;
    }

    void createVideoLoadObservable() {
        if (null != videoLoadSubject) {
            videoLoadSubject = null;
        }
        videoLoadSubject = PublishSubject.create();
    }

    PublishSubject<List<VideoFolder>> getVideoLoadSubject() {
        return videoLoadSubject;
    }

    void createVideoSelectObservable() {
        if (null != videoSelectSubject) {
            videoSelectSubject = null;
        }
        videoSelectSubject = PublishSubject.create();
    }

    PublishSubject<List<Video>> getVideoSelectSubject() {
        return videoSelectSubject;
    }
}
