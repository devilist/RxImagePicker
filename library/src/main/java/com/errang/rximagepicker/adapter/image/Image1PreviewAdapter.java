package com.errang.rximagepicker.adapter.image;

import android.widget.ImageView;

import com.errang.rximagepicker.PickManager;
import com.errang.rximagepicker.R;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.base.BaseViewHolder;
import com.errang.rximagepicker.model.Image;

/**
 * Created by zengp on 2017/7/9.
 */

public class Image1PreviewAdapter extends BaseQuickAdapter<Image> {

    public Image1PreviewAdapter(int layoutResId) {
        super(layoutResId);
    }

    public void refreshData(Image image, int position) {
        if (null == mData)
            return;
        if (mData.size() == 0 || mData.size() - 1 < position)
            return;
        mData.get(position).path = image.path;
        mData.get(position).width = image.width;
        mData.get(position).height = image.height;
        notifyItemChanged(position);
    }

    @Override
    protected void convert(BaseViewHolder holder, int position, Image image) {
        holder.setIsRecyclable(false);
        ImageView iv_image_item = holder.getView(R.id.iv_image_item);
        PickManager.getInstance().showImage(iv_image_item, image.path, image.width, image.height, true);
    }
}
