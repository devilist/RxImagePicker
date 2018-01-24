package com.devilist.rximagepicker;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.errang.rximagepicker.imageloader.ImageLoader;

/**
 * Created by zengp on 2017/7/8.
 */

public class GlideImageLoader implements ImageLoader {

    @Override
    public void showImage(ImageView imageView, String path, int width, int height, boolean isPreview) {
        if (!isPreview)
            Glide.with(imageView.getContext()).load(path).centerCrop().override(width, height)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
        else
            Glide.with(imageView.getContext()).load(path).asBitmap()
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
    }
}
