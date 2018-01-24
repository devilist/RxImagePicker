package com.errang.rximagepicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.video.VideoFolderListAdapter;
import com.errang.rximagepicker.model.Video;
import com.errang.rximagepicker.model.VideoFolder;
import com.errang.rximagepicker.ui.ImageBaseActivity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by zengp on 2017/7/11.
 */

public class VideoFolderListActivity extends ImageBaseActivity implements
        BaseQuickAdapter.OnRecyclerViewItemChildClickListener {

    private static final int REQUEST_PERMISSION_STORAGE = 100;

    private TextView tv_permission_tip;
    private RecyclerView rv_list;
    private VideoFolderListAdapter adapter;

    private ArrayList<VideoFolder> videoFolders = new ArrayList<>();

    public static void start(Context context) {
        Intent starter = new Intent(context, VideoFolderListActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rx_activity_video_folder);
        initView();
    }

    private void initView() {
        initToolBar("视频");

        tv_permission_tip = findView(R.id.tv_permission_tip);
        rv_list = findView(R.id.rv_list);
        adapter = new VideoFolderListAdapter(R.layout.rx_item_video_folder);
        rv_list.setLayoutManager(new LinearLayoutManager(this));
        rv_list.setAdapter(adapter);
        adapter.setItemChildClickListener(this);

        // create observable
        ObservableManager.getInstance().createVideoLoadObservable();
        // permission
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                new VideoSource(this);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_STORAGE);
            }
        } else {
            new VideoSource(this);
        }

        loadVideoFolders();
        observableSelectedFinish();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tv_permission_tip.setVisibility(View.GONE);
                new VideoSource(this);
            } else {
                tv_permission_tip.setVisibility(View.VISIBLE);
                tv_permission_tip.setText("权限被禁止，无法选择本地视频");
            }
        }
    }

    private void loadVideoFolders() {
        ObservableManager.getInstance().getVideoLoadSubject()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<VideoFolder>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull List<VideoFolder> folders)
                            throws Exception {
                        videoFolders = (ArrayList<VideoFolder>) folders;
                        PickManager.getInstance().setVideoFolders(videoFolders);
                        adapter.setNewList(folders);
                    }
                });
    }

    private void observableSelectedFinish() {
        ObservableManager.getInstance().getVideoSelectSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FinishObserver<List<Video>>() {
                    @Override
                    public void onFinish() {
                        finish();
                    }
                });
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View v, int i) {
        if (v.getId() == R.id.rl_root)
            VideoGridActivity.start(this, i);
    }
}
