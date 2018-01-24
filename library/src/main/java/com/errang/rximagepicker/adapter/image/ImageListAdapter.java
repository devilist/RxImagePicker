package com.errang.rximagepicker.adapter.image;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.errang.rximagepicker.Config;
import com.errang.rximagepicker.PickManager;
import com.errang.rximagepicker.R;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.base.BaseViewHolder;
import com.errang.rximagepicker.model.Image;

import java.util.List;

/**
 * Created by zengp on 2017/7/8.
 */

public class ImageListAdapter extends BaseQuickAdapter<Image> {

    private int mScreenWidth;
    private int mSpanCount = 3;

    public ImageListAdapter(Context context, int layoutResId) {
        super(layoutResId);
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
    }

    public ImageListAdapter(Context context, int layoutResId, int spanCount) {
        super(layoutResId);
        if (spanCount > 1) {
            mSpanCount = spanCount;
        }
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
    }

    private void setContentViewSize(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.width != mScreenWidth / mSpanCount) {
            params.width = params.height = mScreenWidth / mSpanCount;
        }
    }

    public void resetData() {
        if (null == mData)
            return;
        if (mData.size() == 0)
            return;
        int count = 0;
        int maxValue = PickManager.getInstance().getConfig().getMode() == Config.SINGLE ?
                1 : PickManager.getInstance().getConfig().getMaxValue();
        for (Image image : mData) {
            if (count >= maxValue)
                break;
            if (image.isSelected) {
                image.isSelected = false;
                count++;
            }
        }
    }

    /**
     * @param image
     * @param isCropImage 是否是剪裁过的image
     */
    public void refreshData(Image image, boolean isCropImage) {
        if (null == mData)
            return;
        if (mData.size() == 0)
            return;

        if (isCropImage) {
            for (int i = 0; i < mData.size(); i++) {
                if (mData.get(i).name.equals(image.name)
                        && mData.get(i).addTime == image.addTime) {
                    mData.get(i).path = image.path;
                    mData.get(i).width = image.width;
                    mData.get(i).height = image.height;
                    mData.get(i).isSelected = image.isSelected;
                    Log.d("crop===", "isSelected " + image.isSelected);
                    notifyItemChanged(i);
                    break;
                }
            }
        } else {
            for (int i = 0; i < mData.size(); i++) {
                if (mData.get(i).equals(image)) {
                    mData.get(i).isSelected = image.isSelected;
                    notifyItemChanged(i);
                    break;
                }
            }
        }
//
//        for (int i = 0; i < mData.size(); i++) {
//            if (!isCropImage) {
//                if (mData.get(i).equals(image)) {
//                    mData.get(i).isSelected = image.isSelected;
//                    notifyItemChanged(i);
//                    break;
//                }
//            } else if (mData.get(i).name.equals(image.name)
//                    && mData.get(i).addTime == image.addTime) {
//                mData.get(i).path = image.path;
//                mData.get(i).width = image.width;
//                mData.get(i).height = image.height;
//                mData.get(i).isSelected = image.isSelected;
//                notifyItemChanged(i);
//                break;
//            }
//        }
    }

    @Override
    public BaseViewHolder onCreateDefViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder holder = null;
        if (layoutResId > 0) {
            View v = mInflater.inflate(layoutResId, parent, false);
            setContentViewSize(v);
            holder = new BaseViewHolder(v);
        }
        return holder;
    }

    @Override
    protected void convert(BaseViewHolder holder, int position, Image image) {
        setContentViewSize(holder.convertView);
        ImageView iv_selected = holder.getView(R.id.iv_item_selected);
        ImageView iv_item = holder.getView(R.id.iv_image_item);

        if (PickManager.getInstance().getConfig().getMode() == Config.MULTIPLE) {
            iv_selected.setActivated(image.isSelected);
            iv_selected.setVisibility(View.VISIBLE);
        } else {
            iv_selected.setVisibility(View.GONE);
        }
        PickManager.getInstance().showImage(iv_item, image.path,
                mScreenWidth / mSpanCount, mScreenWidth / mSpanCount, false);

        holder.setOnClickListener(R.id.iv_item_selected, new OnItemChildClickListener(position))
                .setOnClickListener(R.id.iv_image_item, new OnItemChildClickListener(position));
    }
}
