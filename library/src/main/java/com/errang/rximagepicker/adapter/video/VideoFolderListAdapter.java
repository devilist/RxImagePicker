package com.errang.rximagepicker.adapter.video;

import android.widget.ImageView;

import com.errang.rximagepicker.PickManager;
import com.errang.rximagepicker.R;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.base.BaseViewHolder;
import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.VideoFolder;

/**
 * Created by zengp on 2017/7/11.
 */

public class VideoFolderListAdapter extends BaseQuickAdapter<VideoFolder> {

    public VideoFolderListAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder holder, int position, VideoFolder folder) {
        ImageView iv_folder_cover = holder.getView(R.id.iv_folder_cover);

        holder.setText(R.id.tv_folder_name, folder.name);
        holder.setText(R.id.tv_folder_image_count, folder.videoList.size() + "");
        int size = mContext.getResources().getDimensionPixelSize(R.dimen.folder_item_height);
        PickManager.getInstance().showImage(iv_folder_cover, folder.cover, size, size, false);

        holder.setOnClickListener(R.id.rl_root, new OnItemChildClickListener(position));
    }
}

