package com.errang.rximagepicker.imageloader;

import android.widget.ImageView;

/**
 * Created by zengp on 2017/7/8.
 */

public interface ImageLoader {

    void showImage(ImageView imageView, String path, int width, int height, boolean isPreview);
}
