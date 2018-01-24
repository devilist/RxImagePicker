package com.errang.rximagepicker.adapter.base;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;


public abstract class BaseQuickAdapter<T> extends RecyclerView.Adapter<BaseViewHolder> {

    protected Context mContext;
    protected LayoutInflater mInflater = null;
    protected List<T> mData = null;
    protected int layoutResId = 0;


    public BaseQuickAdapter(int layoutResId) {
        this(layoutResId, null);
    }

    public BaseQuickAdapter(int layoutResId, List<T> l) {
        this.layoutResId = layoutResId;
        this.mData = l;
    }

    /**
     * 新数据
     *
     * @param l
     */
    public void setNewList(List<T> l) {
        this.mData = l;
        this.notifyDataSetChanged();
    }

    /**
     * 追加数据
     *
     * @param l
     */
    public void addList(List<T> l) {
        if (null == l || l.size() == 0) {
            return;
        }
        if (null == this.mData) {
            this.mData = l;
            this.notifyItemRangeInserted(0, l.size());
            return;
        }
        int star = null == this.mData ? 0 : this.mData.size();
        this.mData.addAll(l);
        this.notifyItemRangeInserted(star, l.size());
    }

    /**
     * 删除
     *
     * @param i
     */
    public void delete(int i) {
        if (null == mData || mData.size() == 0 || mData.size() <= i) {
            return;
        }
        this.mData.remove(i);
        this.notifyItemRemoved(i);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (null == mContext) {
            this.mContext = parent.getContext();
            this.mInflater = LayoutInflater.from(mContext);
        }
        return onCreateDefViewHolder(parent, viewType);
    }

    public BaseViewHolder onCreateDefViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder holder = null;
        if (layoutResId > 0) {
            View v = mInflater.inflate(layoutResId, parent, false);
            holder = new BaseViewHolder(v);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        T t = mData.get(position);
        convert(holder, position, t);
    }

    protected abstract void convert(BaseViewHolder holder, int position, T t);

    @Override
    public int getItemCount() {
        return null == mData ? 0 : mData.size();
    }

    /**
     * 获取数据
     *
     * @param position
     * @return
     */
    public T getItem(int position) {
        if (null == mData || position < 0 || position >= mData.size()) {
            return null;
        }
        return this.mData.get(position);
    }

    // 子视图item点击事件
    private OnRecyclerViewItemChildClickListener mChildClickListener = null;

    public void setItemChildClickListener(OnRecyclerViewItemChildClickListener listener) {
        this.mChildClickListener = listener;
    }

    /**
     * 点击事件
     */
    public interface OnRecyclerViewItemChildClickListener {
        void onItemChildClick(BaseQuickAdapter adapter, View v, int i);
    }

    /**
     * 点击事件
     */
    public class OnItemChildClickListener implements View.OnClickListener {
        private int position;

        public OnItemChildClickListener(int i) {
            this.position = i;
        }

        public void onClick(View v) {
            if (null != BaseQuickAdapter.this.mChildClickListener) {
                BaseQuickAdapter.this.mChildClickListener.onItemChildClick(BaseQuickAdapter.this, v, this.position);
            }
        }
    }
}
