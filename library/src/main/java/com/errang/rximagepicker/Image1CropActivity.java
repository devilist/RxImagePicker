package com.errang.rximagepicker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.ui.ImageBaseActivity;
import com.errang.rximagepicker.widget.CropImageView;

import java.io.File;
import java.io.IOException;

/**
 * Created by zengp on 2017/7/9.
 */

public class Image1CropActivity extends ImageBaseActivity implements View.OnClickListener,
        CropImageView.ImageCropListener {

    private CropImageView civ_cropimage;
    private Image image;

    private boolean isCamera = false;
    private boolean isPreview = true;
    private int mode = Config.MULTIPLE;

    private int mOutputX = 1000;
    private int mOutputY = 1000;

    public static void start(Context context, Image image) {
        Intent starter = new Intent(context, Image1CropActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        starter.putExtra("image", image);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rx_activity_image_crop);

        image = (Image) getIntent().getSerializableExtra("image");
        mode = PickManager.getInstance().getConfig().getMode();
        isCamera = PickManager.getInstance().getConfig().isShowCamera();
        isPreview = PickManager.getInstance().getConfig().isPreview();

        initView();
    }

    private void initView() {
        initToolBar("裁剪");
        civ_cropimage = findView(R.id.civ_cropimage);
        findView(R.id.tv_ok).setOnClickListener(this);
        PickManager.getInstance().showImage(civ_cropimage, image.path, image.width, image.height, true);
        civ_cropimage.cropOutSize(mOutputX, mOutputY)
                .cropSaveDir(PickManager.getInstance().getCacheFolder())
                .addCropListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_ok) {
            civ_cropimage.cropToFile();
        }
    }

    @Override
    public void onBackPressed() {
        if (isCamera && !isPreview) {
            image.isSelected = false;
            ObservableManager.getInstance().getChangedSubject().onNext(image);
            ObservableManager.getInstance().getChangedSubject().onComplete();
        }
        super.onBackPressed();
    }


    @Override
    public void onCropResult(String filePath, String message, boolean isCropSuccess) {
        if (isCropSuccess) {
            image.path = filePath;
            image.width = mOutputX;
            image.height = mOutputY;
            ObservableManager.getInstance().getCropSubject().onNext(image);
            if (isCamera || mode == Config.SINGLE) {
                ObservableManager.getInstance().getCropSubject().onComplete();
            }
        } else {
            Toast.makeText(this, "裁剪失败，图片无效", Toast.LENGTH_SHORT).show();
            ObservableManager.getInstance().getCropSubject().onComplete();
        }
        finish();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = width / reqWidth;
            } else {
                inSampleSize = height / reqHeight;
            }
        }
        return inSampleSize;
    }

    private int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
}
