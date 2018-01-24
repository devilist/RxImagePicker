package com.errang.rximagepicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.video.VideoListAdapter;
import com.errang.rximagepicker.model.Video;
import com.errang.rximagepicker.model.VideoFolder;
import com.errang.rximagepicker.ui.ImageBaseActivity;
import com.errang.rximagepicker.utils.StatusbarUtil;
import com.errang.rximagepicker.widget.MediaPlayerDialog;

import java.util.ArrayList;

/**
 * Created by zengp on 2017/7/11.
 */

public class VideoGridActivity extends ImageBaseActivity implements View.OnClickListener,
        BaseQuickAdapter.OnRecyclerViewItemChildClickListener {

    private TextView tv_selected;
    private RecyclerView videoRv;
    private VideoListAdapter videoListAdapter;

    private int mode = Config.MULTIPLE;
    private int max = 9;

    private VideoFolder videoFolder;
    private Video selectedVideo;
    private int folderPosition;
    private ArrayList<Video> selectedVideos = new ArrayList<>();

    public static void start(Context context, int folderPosition) {
        Intent starter = new Intent(context, VideoGridActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        starter.putExtra("folderPosition", folderPosition);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rx_activity_video_grid);

        StatusbarUtil.StatusBarLightMode(this);
        folderPosition = getIntent().getIntExtra("folderPosition", 0);
        videoFolder = PickManager.getInstance().getVideoFolders().get(folderPosition);

        mode = PickManager.getInstance().getConfig().getMode();
        max = PickManager.getInstance().getConfig().getMaxValue();

        initView();
    }

    private void initView() {
        initToolBar(videoFolder.name);

        tv_selected = findView(R.id.tv_selected);
        tv_selected.setText(mode == Config.SINGLE ? "确定" : "确定(0/" + max + ")");
        tv_selected.setOnClickListener(this);

        videoRv = findView(R.id.rv_list);
        videoListAdapter = new VideoListAdapter(this, R.layout.rx_item_video_list, 3);
        videoRv.setLayoutManager(new GridLayoutManager(this, 3));
        videoRv.setAdapter(videoListAdapter);
        videoListAdapter.setItemChildClickListener(this);
        videoListAdapter.setNewList(videoFolder.videoList);

    }

    private void toggleView() {
        tv_selected.setEnabled(selectedVideos.size() > 0);
        if (mode == Config.SINGLE) {
            tv_selected.setText("确定");
        } else
            tv_selected.setText("确定(" + selectedVideos.size() + "/" + max + ")");
        tv_selected.setTextColor(getResources().getColor(selectedVideos.size() > 0 ? R.color.color_f26d85 :
                R.color.color_66dcb97a));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_selected) {
            ObservableManager.getInstance().getVideoSelectSubject().onNext(selectedVideos);
            ObservableManager.getInstance().getVideoSelectSubject().onComplete();
            finish();
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View v, int i) {
        Video item = (Video) adapter.getItem(i);
        if (v.getId() == R.id.iv_item_selected) {
            ImageView selected = (ImageView) v;
            if (mode == Config.SINGLE) {
                // single
                if (selected.isActivated()) {
                    selectedVideos.remove(item);
                    item.isSelected = false;
                    selected.setActivated(false);
                    selectedVideo = null;
                } else {
                    if (null != selectedVideo && !selectedVideo.equals(item)) {
                        selectedVideo.isSelected = false;
                        videoListAdapter.refreshData(selectedVideo);
                    }
                    selectedVideos.add(item);
                    item.isSelected = true;
                    selected.setActivated(true);
                    selectedVideo = item;
                }

            } else if (selected.isActivated()) {

                selectedVideos.remove(item);
                item.isSelected = false;
                selected.setActivated(false);
            } else if (selectedVideos.size() >= max) {
                Toast.makeText(this, "最多可选" + max + "个", Toast.LENGTH_SHORT).show();
            } else {
                selectedVideos.add(item);
                item.isSelected = true;
                selected.setActivated(true);
            }
            toggleView();
        } else if (v.getId() == R.id.iv_image_item || v.getId() == R.id.iv_play) {
            // preview
            MediaPlayerDialog.newInstance(item.path, item.thumbnail)
                    .show(getSupportFragmentManager());
        }
    }
}
