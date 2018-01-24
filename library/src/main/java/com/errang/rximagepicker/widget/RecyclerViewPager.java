package com.errang.rximagepicker.widget;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 具有viewpager效果的recyclerview
 * Created by zengpu on 2016/10/28.
 */
public class RecyclerViewPager extends RecyclerView implements
        View.OnTouchListener,
        GestureDetector.OnGestureListener {

    /**
     * The RecyclerViewPager is not currently scrolling.
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The RecyclerViewPager is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The RecyclerViewPager is currently animating to a final position while not under
     * outside control.
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    /**
     * 触发翻页动作的最小滑动距离
     */
    private float mFlingSlop = 0;

    /**
     * 触发翻页动作的最小滑动距离的比例因子
     */
    private float mFlingFactor = 0.5f;

    /**
     * 触发翻页动作的最小滑动速度
     */
    private float mVelocitySlop = 0;

    /**
     * 当前recyclerView第一个可见的item的位置
     */
    private int currentPosition = 0;

    /**
     * 滑动事件结束后，选中的item的位置
     */
    protected int mSelectedPosition = 0;

    /**
     * recyclerView的item个数
     */
    private int mItemCount = 0;

    /**
     * touch操作按下的位置 x
     */
    private float mTouchDownX = 0;

    /**
     * touch操作抬起的位置 x
     */
    private float mTouchUpX = 0;

    /**
     * 滑动过程中是否触发了onFling事件
     */
    private boolean is_trigger_onFling = false;


    protected OnPageSelectListener mOnPageSelectListener;

    private Adapter adapter;

    private GestureDetector gestureDetector;

    private SpeedControlLayoutManager layoutManager;

    public RecyclerViewPager(Context context) {
        super(context);
        init(context);
    }

    public RecyclerViewPager(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RecyclerViewPager(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        int mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mFlingFactor = 0.55f;
        mFlingSlop = mScreenWidth * mFlingFactor;
        mVelocitySlop = 2000;

        layoutManager = new SpeedControlLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        setLayoutManager(layoutManager);

        gestureDetector = new GestureDetector(context, this);

        this.setOnTouchListener(this);

        this.addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    /*
                     * 页面停止滚动后，偶尔会出现目标页面没有完全滚入屏幕的情况，
                     * 通过获得当前item的偏移量来判断是否完全滚入
                     * 如果偏移量不为0，则需要用 smoothScrollBy()方法完成页面滚动
                     */
                    int mSelectedPageOffsetX = getScollOffsetX(mSelectedPosition);
                    if (mSelectedPageOffsetX != 0) {
                        smoothScrollBy(mSelectedPageOffsetX, 0, new DecelerateInterpolator());
                    }
                }

                if (null != mOnPageSelectListener) {
                    mOnPageSelectListener.onPageScrollStateChanged(newState);

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        mOnPageSelectListener.onPageSelected(mSelectedPosition);
                    }

                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (null != mOnPageSelectListener) {
                    mOnPageSelectListener.onPageScrolled(mSelectedPosition, getScollOffsetX(mSelectedPosition));
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {

        // 如果recyclerview的item里添加了onClick事件，则触摸事件会被onClick事件消费掉，
        // OnTouchLisenter监听就获取不到ACTION_DOWN时的触摸位置，因此在这里记录mTouchDownX
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = e.getX();
                is_trigger_onFling = false;
                break;
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (null == layoutManager) {
            layoutManager = (SpeedControlLayoutManager) getLayoutManager();
        }
        if (null == adapter) {
            adapter = getAdapter();
        }
        currentPosition = layoutManager.findFirstVisibleItemPosition();
        mSelectedPosition = currentPosition;
        if (null != adapter) {
            mItemCount = adapter.getItemCount();
        }

        // 将触摸事件传递给GestureDetector
        gestureDetector.onTouchEvent(event);

        /*
         * 手指滑动时，页面有两种翻页效果：
         * 1.如果不需要页面跟着一起滑动，只在手指抬起后进行翻页，则只需要处理onFling事件即可；在onTouch方法里直接返回true
         * 2.如果滑动时需要页面跟着一起滑动(像ViewPager一样)，则需要同时处理onFling和onScroll事件；
         *   onFling事件在OnGestureListener里处理；
         *   onScroll事件最终需要在onTouch里 ACTION_UP 触发后进行后续判断页面的滚动
         *
         * OnGestureListener监听里事件执行顺序有两种：
         *  onFling事件流： onDown —— onScroll —— onScroll... —— onFling
         *  onScroll事件流：onDown —— onScroll —— onScroll —— onScroll...
         * 当滑动速度比较快时，会进入第一种情况，最后执行onFling；
         * 当滑动速度比较慢时，会进入第二种情况，这种情况不会进入到onFling里，最终会进入onTouch的ACTION_UP里
         */
        switch (event.getAction()) {

            // ACTION_DOWN 事件在onInterceptTouchEvent方法里记录
            case MotionEvent.ACTION_UP:

                if (!is_trigger_onFling) {

                    mTouchUpX = event.getX() - mTouchDownX;

                    if (mTouchUpX >= mFlingSlop) {
                        // 往右滑，position减小
                        mSelectedPosition = currentPosition == 0 ? 0 : currentPosition;

                    } else if (mTouchUpX <= -mFlingSlop) {
                        // 往左滑动，position增大
                        mSelectedPosition = currentPosition == mItemCount - 1 ? mItemCount - 1 : currentPosition + 1;

                    } else if (mTouchUpX < mFlingSlop && mTouchUpX > 0) {
                        // 往右滑动，但未达到阈值
                        if (currentPosition == 0 && getScollOffsetX(0) >= 0)
                            // 边界控制，如果当前已经停留在第一页
                            mSelectedPosition = 0;
                        else
                            mSelectedPosition = currentPosition + 1;

                    } else {
                        mSelectedPosition = currentPosition;
                    }

                    smoothScrollToPosition(mSelectedPosition);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        is_trigger_onFling = false;

        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        is_trigger_onFling = true;

        if (null == e1 | null == e2) {

            if (velocityX >= mVelocitySlop) {
                // 往右滑动，position减少
                mSelectedPosition = currentPosition == 0 ? 0 : currentPosition;

            } else if (velocityX <= -mVelocitySlop) {
                // 往左滑动，position增大
                mSelectedPosition = currentPosition == mItemCount - 1 ? mItemCount - 1 : currentPosition + 1;

            } else if (velocityX < mVelocitySlop && velocityX >= 0) {
                // 往右滑动，未达到速度阈值
                if (currentPosition == 0 && getScollOffsetX(0) >= 0)
                    // 边界控制，如果当前已经停留在第一页
                    mSelectedPosition = 0;
                else
                    mSelectedPosition = currentPosition + 1;

            } else
                mSelectedPosition = currentPosition;

        } else {

            float x_fling = e2.getX() - e1.getX();

            if (x_fling >= mFlingSlop | velocityX >= mVelocitySlop) {
                // 往右滑动，position减少
                mSelectedPosition = currentPosition == 0 ? 0 : currentPosition;

            } else if (x_fling <= -mFlingSlop | velocityX <= -mVelocitySlop) {
                // 往左滑动，position增大
                mSelectedPosition = currentPosition == mItemCount - 1 ? mItemCount - 1 : currentPosition + 1;

            } else {
                if (x_fling < mFlingSlop && x_fling > 0) {
                    // 往右滑动，未达到阈值
                    if (currentPosition == 0 && getScollOffsetX(0) >= 0)
                        // 边界控制，如果当前已经停留在第一页
                        mSelectedPosition = 0;
                    else
                        mSelectedPosition = currentPosition + 1;
                } else
                    mSelectedPosition = currentPosition;
            }
        }
        smoothScrollToPosition(mSelectedPosition);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        is_trigger_onFling = false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        is_trigger_onFling = false;
        return true;
    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        is_trigger_onFling = false;
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    public void setCurrentPage(int position) {
        mSelectedPosition = position;
        scrollToPosition(position);
    }

    /**
     * 获得当前页面相对于屏幕左侧边缘的偏移量
     *
     * @param position 当前页面位置
     * @return 偏移量
     */
    private int getScollOffsetX(int position) {

        if (null == layoutManager) {
            layoutManager = (SpeedControlLayoutManager) getLayoutManager();
        }

        View childView = layoutManager.findViewByPosition(position);
        if (null == childView) {
            return 0;
        }
        return childView.getLeft();
    }

    public void setOnPageSelectListener(OnPageSelectListener mOnPageSelectListener) {
        this.mOnPageSelectListener = mOnPageSelectListener;
    }

    /**
     * recyclerPager页面滚动监听
     */
    public interface OnPageSelectListener {

        /**
         * 滚动的过程中被调用
         *
         * @param position
         * @param positionOffset 当前第一个可见的item的左侧距离屏幕左边缘的距离
         */
        void onPageScrolled(int position, float positionOffset);

        /**
         * 滚动事件结束后被调用
         *
         * @param position
         */
        void onPageSelected(int position);

        /**
         * 滚动状态变化时被调用
         *
         * @param state
         */
        void onPageScrollStateChanged(int state);

    }

    private class SpeedControlLayoutManager extends LinearLayoutManager {

        private final float MILLISECONDS_PER_INCH = 50f;

        public SpeedControlLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
            LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
                }

                @Nullable
                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    return SpeedControlLayoutManager.this.computeScrollVectorForPosition(targetPosition);
                }
            };

            smoothScroller.setTargetPosition(position);
            startSmoothScroll(smoothScroller);
        }
    }
}
