package com.errang.rximagepicker.adapter.video;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.errang.rximagepicker.Config;
import com.errang.rximagepicker.PickManager;
import com.errang.rximagepicker.R;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.adapter.base.BaseViewHolder;
import com.errang.rximagepicker.model.Video;

import java.math.BigDecimal;

/**
 * Created by zengp on 2017/7/11.
 */

public class VideoListAdapter extends BaseQuickAdapter<Video> {

    private int mScreenWidth;
    private int mSpanCount = 3;

    public VideoListAdapter(Context context, int layoutResId) {
        super(layoutResId);
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
    }

    public VideoListAdapter(Context context, int layoutResId, int spanCount) {
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

    public void refreshData(Video video) {
        if (null == mData)
            return;
        if (mData.size() == 0)
            return;
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).equals(video)) {
                mData.get(i).isSelected = video.isSelected;
                notifyItemChanged(i);
                break;
            }
        }
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
    protected void convert(BaseViewHolder holder, int position, Video video) {
        setContentViewSize(holder.convertView);
        ImageView iv_selected = holder.getView(R.id.iv_item_selected);
        ImageView iv_item = holder.getView(R.id.iv_image_item);

        iv_selected.setActivated(video.isSelected);

        PickManager.getInstance().showImage(iv_item, video.path,
                mScreenWidth / mSpanCount, mScreenWidth / mSpanCount, false);

        holder.setText(R.id.tv_size, getFormatSize(video.size));
        holder.setText(R.id.tv_duration, toFormatTime(video.duration));

        holder.setOnClickListener(R.id.iv_item_selected, new OnItemChildClickListener(position))
                .setOnClickListener(R.id.iv_image_item, new OnItemChildClickListener(position))
                .setOnClickListener(R.id.iv_play, new OnItemChildClickListener(position));
    }

    public String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "Byte(s)";
        }
        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

    public String toFormatTime(long duration) {
        duration /= 1000;
        int m = (int) (duration / 60);
        if (m >= 60) {
            m %= 60;
        }
        int s = (int) (duration % 60);
        return String.format("%02d:%02d", new Object[]{Integer.valueOf(m), Integer.valueOf(s)});
    }
}

