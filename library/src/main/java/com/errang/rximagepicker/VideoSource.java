package com.errang.rximagepicker;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.model.Video;
import com.errang.rximagepicker.model.VideoFolder;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by zengp on 2017/7/11.
 */

class VideoSource implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ALL = 0;         // load all images
    public static final int LOADER_CATEGORY = 1;    // load image by folder

    private final String[] VIDEO_PROJECTION = {
            MediaStore.Video.Media.DISPLAY_NAME,    // aaa.mp4
            MediaStore.Video.Media.DATA,            // /storage/emulated/0/.../aaa.mp4
            MediaStore.Video.Thumbnails.DATA,       // /storage/emulated/0/.../aaa.jpg
            MediaStore.Video.Media.SIZE,            // long
            MediaStore.Video.Media.DURATION,        // long
            MediaStore.Video.Media.WIDTH,           // int
            MediaStore.Video.Media.HEIGHT,          // int
            MediaStore.Video.Thumbnails.WIDTH,      // int
            MediaStore.Video.Thumbnails.HEIGHT,     // int
            MediaStore.Video.Media.MIME_TYPE,       // image/jpeg
            MediaStore.Video.Media.DATE_ADDED};     // long

    private FragmentActivity activity;
    private ArrayList<VideoFolder> folders = new ArrayList<>();

    VideoSource(FragmentActivity activity) {
        this.activity = activity;
        LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.initLoader(LOADER_ALL, null, this);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        if (id == LOADER_ALL) {
            // scan all
            String selection = MediaStore.Video.Media.MIME_TYPE +
                    " in ('video/mp4')";
            cursorLoader = new CursorLoader(activity,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    VIDEO_PROJECTION,
                    selection,
                    null,
                    VIDEO_PROJECTION[10] + " DESC");
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        folders.clear();
        if (data != null) {
            ArrayList<Video> allVideoList = new ArrayList<>();
            while (data.moveToNext()) {
                //query
                long videoSize = data.getLong(data.getColumnIndexOrThrow(VIDEO_PROJECTION[3]));
                if (videoSize == 0)
                    continue;
                long videoDuration = data.getLong(data.getColumnIndexOrThrow(VIDEO_PROJECTION[4]));
                if (videoDuration == 0)
                    continue;
                int videoWidth = 0;
                int videoHeight = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    videoWidth = data.getInt(data.getColumnIndexOrThrow(VIDEO_PROJECTION[5]));
                    videoHeight = data.getInt(data.getColumnIndexOrThrow(VIDEO_PROJECTION[6]));
                } else {
                    videoWidth = data.getInt(data.getColumnIndexOrThrow(VIDEO_PROJECTION[7]));
                    videoHeight = data.getInt(data.getColumnIndexOrThrow(VIDEO_PROJECTION[8]));
                }
//                if (videoWidth <= 0 || videoHeight <= 0)
//                    continue;
                String videoName = data.getString(data.getColumnIndexOrThrow(VIDEO_PROJECTION[0]));
                String videoPath = data.getString(data.getColumnIndexOrThrow(VIDEO_PROJECTION[1]));
                String thumbnailPath = data.getString(data.getColumnIndexOrThrow(VIDEO_PROJECTION[2]));

                String videoMimeType = data.getString(data.getColumnIndexOrThrow(VIDEO_PROJECTION[9]));
                long videoAddTime = data.getLong(data.getColumnIndexOrThrow(VIDEO_PROJECTION[10]));
                // video
                Video item = new Video();
                item.name = videoName;
                item.path = videoPath;
                item.thumbnail = thumbnailPath;
                item.size = videoSize;
                item.duration = videoDuration;
                item.width = videoWidth;
                item.height = videoHeight;
                item.mimeType = videoMimeType;
                item.addTime = videoAddTime;
                allVideoList.add(item);
                // folder
                File videoFile = new File(videoPath);
                File videoParentFile = videoFile.getParentFile();
                VideoFolder videoFolder = new VideoFolder();
                videoFolder.name = videoParentFile.getName();
                videoFolder.path = videoParentFile.getAbsolutePath();
                if (!folders.contains(videoFolder)) {
                    ArrayList<Video> videoList = new ArrayList<>();
                    videoList.add(item);
                    videoFolder.cover = item.thumbnail;
                    videoFolder.videoList = videoList;
                    folders.add(videoFolder);
                } else {
                    folders.get(folders.indexOf(videoFolder)).videoList.add(item);
                }
            }
            // all folder
            if (data.getCount() > 0) {
                VideoFolder allVideoFolder = new VideoFolder();
                allVideoFolder.name = "全部视频";
                allVideoFolder.path = "/";
                allVideoFolder.cover = allVideoList.get(0).thumbnail;
                allVideoFolder.videoList = allVideoList;
                folders.add(0, allVideoFolder);
            }
        }
        ObservableManager.getInstance().getVideoLoadSubject().onNext(folders);
        ObservableManager.getInstance().getVideoLoadSubject().onComplete();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
