package com.errang.rximagepicker.adapter.image;

import android.widget.ImageView;

import com.errang.rximagepicker.PickManager;
import com.errang.rximagepicker.R;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.base.BaseViewHolder;
import com.errang.rximagepicker.model.Folder;

/**
 * Created by zengp on 2017/7/9.
 */

public class Image1FolderListAdapter extends BaseQuickAdapter<Folder> {

    public Image1FolderListAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder holder, int position, Folder folder) {
        ImageView iv_folder_cover = holder.getView(R.id.iv_folder_cover);

        holder.setText(R.id.tv_folder_name, folder.name);
        holder.setText(R.id.tv_folder_image_count, folder.imageList.size() + "");
        int size = mContext.getResources().getDimensionPixelSize(R.dimen.folder_item_height);
        PickManager.getInstance().showImage(iv_folder_cover, folder.cover.path, size, size, false);

        holder.setOnClickListener(R.id.rl_root, new OnItemChildClickListener(position));
    }
}
