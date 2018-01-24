package com.errang.rximagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.ui.ImageBaseActivity;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * Created by zengp on 2017/7/10.
 */

public class ImageCameraActivity extends ImageBaseActivity {

    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_CODE_CAMERA = 110;

    private File cameraFile;
    private boolean isCrop = true;
    private boolean isPreview = true;
    private ArrayList<Image> selectedImages = new ArrayList<>();

    public static void start(Context context) {
        Intent starter = new Intent(context, ImageCameraActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCrop = PickManager.getInstance().getConfig().isCrop();
        isPreview = PickManager.getInstance().getConfig().isPreview();

        CreateResultObserver();
        // permission
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            boolean permission_camera = ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean permission_storage = ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (permission_camera && permission_storage) {
                takePhoto();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION);
            }
        } else {
            takePhoto();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, "权限被禁止，无法拍照", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void CreateResultObserver() {
        // preview observer
        ObservableManager.getInstance().createChangedObservable();
        // crop observer
        ObservableManager.getInstance().createCropObservable();

        ObservableManager.getInstance().getChangedSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Image>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Image image)
                            throws Exception {
                        // selected image changed
                        refreshImage(image);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        // selected finish
                        // 预览页面返回 预览页面点击确定
                        if (selectedImages.size() > 0 && selectedImages.get(0).isSelected)
                            ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
                        ObservableManager.getInstance().getSelectedSubject().onComplete();
                        finish();
                    }
                });

        ObservableManager.getInstance().getCropSubject()
                .subscribe(new Consumer<Image>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Image image)
                            throws Exception {
                        // cropped image
                        refreshImage(image);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        if (!isPreview) {
                            // 剪裁页面返回 点击完成
                            if (selectedImages.size() > 0 && selectedImages.get(0).isSelected)
                                ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
                            ObservableManager.getInstance().getSelectedSubject().onComplete();
                            finish();
                        }
                    }
                });
    }

    private void refreshImage(Image image) {
        selectedImages.clear();
        selectedImages.add(image);
    }

    private void takePhoto() {
        if (!isExitsSdcard()) {
            Toast.makeText(this, "SD卡不存在，不能拍照", Toast.LENGTH_SHORT).show();
            ObservableManager.getInstance().getSelectedSubject().onComplete();
            finish();
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING",
                    Camera.CameraInfo.CAMERA_FACING_FRONT);
            cameraFile = new File(PickManager.getInstance().getCacheFolder() + "IMG_CAMERA_" + System.currentTimeMillis() + ".jpg");
            Uri uri;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                String authorities = getPackageName() + ".fileProvider";
                uri = FileProvider.getUriForFile(this, authorities, cameraFile);
            } else {
                uri = Uri.fromFile(cameraFile);
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CAMERA) {
            // 发送照片
            if (cameraFile != null && cameraFile.exists()) {
                Image image = new Image();
                image.path = cameraFile.getAbsolutePath();
                image.isSelected = true;
                image.size = 1;
                image.width = 1;
                image.height = 1;
                image.name = "IMG_";
                selectedImages.add(image);

                if (isPreview) {
                    // 去预览
                    Image1PreviewActivity.start(this, selectedImages, 1, selectedImages.size());
                } else if (isCrop) {
                    //  去剪裁
                    Log.d("ImageCameraActivity", " crop ");
                    Image1CropActivity.start(this, image);
                } else {
                    // 直接返回结果
                    ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
                    ObservableManager.getInstance().getSelectedSubject().onComplete();
                    finish();
                }
            } else {
                ObservableManager.getInstance().getSelectedSubject().onComplete();
                finish();
            }
        } else {
            ObservableManager.getInstance().getSelectedSubject().onComplete();
            finish();
        }
    }

    private boolean isExitsSdcard() {
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }
}
