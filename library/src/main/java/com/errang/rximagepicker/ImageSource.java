package com.errang.rximagepicker;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.subjects.PublishSubject;

/**
 * Created by zengp on 2017/7/8.
 */

class ImageSource implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ALL = 0;         // load all images
    public static final int LOADER_CATEGORY = 1;    // load image by folder

    private final String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DISPLAY_NAME,   // aaa.jpg
            MediaStore.Images.Media.DATA,           // /storage/emulated/0/.../aaa.jpg
            MediaStore.Images.Media.SIZE,           // long
            MediaStore.Images.Media.WIDTH,          // int
            MediaStore.Images.Media.HEIGHT,         // int
            MediaStore.Images.Media.MIME_TYPE,      // image/jpeg
            MediaStore.Images.Media.DATE_ADDED};    // long

    private FragmentActivity activity;
    private ArrayList<Folder> folders = new ArrayList<>();

     ImageSource(FragmentActivity activity) {
        this.activity = activity;
        LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.initLoader(LOADER_ALL, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        if (id == LOADER_ALL) {
            // scan all
            String selection = MediaStore.Images.Media.MIME_TYPE +
                    " in ('image/jpeg', 'IMAGE/JPEG', 'image/jpg', 'IMAGE/JPG', 'image/png', 'IMAGE/PNG')";
            cursorLoader = new CursorLoader(activity,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    IMAGE_PROJECTION,
                    selection,
                    null,
                    IMAGE_PROJECTION[6] + " DESC");
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        folders.clear();
        if (data != null) {
            ArrayList<Image> allImageList = new ArrayList<>();
            while (data.moveToNext()) {
                //query
                String imageName = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                String imagePath = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                long imageSize = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                if (null == imagePath || imagePath.contains(","))
                    continue;
                if (imageSize == 0)
                    continue;
                int imageWidth = data.getInt(data.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
                int imageHeight = data.getInt(data.getColumnIndexOrThrow(IMAGE_PROJECTION[4]));
                if (imageWidth <= 0 || imageHeight <= 0)
                    continue;
                String imageMimeType = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[5]));
                long imageAddTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
                // image
                Image item = new Image();
                item.name = imageName;
                item.path = imagePath;
                item.size = imageSize;
                item.width = imageWidth;
                item.height = imageHeight;
                item.mimeType = imageMimeType;
                item.addTime = imageAddTime;
                allImageList.add(item);
                // folder
                File imageFile = new File(imagePath);
                File imageParentFile = imageFile.getParentFile();
                Folder imageFolder = new Folder();
                imageFolder.name = imageParentFile.getName();
                imageFolder.path = imageParentFile.getAbsolutePath();

                if (!folders.contains(imageFolder)) {
                    ArrayList<Image> imageList = new ArrayList<>();
                    imageList.add(item);
                    imageFolder.cover = item;
                    imageFolder.imageList = imageList;
                    folders.add(imageFolder);
                } else {
                    folders.get(folders.indexOf(imageFolder)).imageList.add(item);
                }
            }
            // all folder
            if (data.getCount() > 0) {
                Folder allImagesFolder = new Folder();
                allImagesFolder.name = "全部图片";
                allImagesFolder.path = "/";
                allImagesFolder.cover = allImageList.get(0);
                allImagesFolder.imageList = allImageList;
                folders.add(0, allImagesFolder);
            }
        }
        ObservableManager.getInstance().getLoadSubject().onNext(folders);
        ObservableManager.getInstance().getLoadSubject().onComplete();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
