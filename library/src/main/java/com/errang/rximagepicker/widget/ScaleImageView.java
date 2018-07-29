/*
 * Copyright  2017  zengp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.errang.rximagepicker.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;


/**
 * Created by zengp on 2017/11/13.
 */

public class ScaleImageView extends ImageView implements
        ViewTreeObserver.OnGlobalLayoutListener,
        ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener {

    private final static String TAG = "ScaleImageView";
    private float mMinScale = 1;
    private float mMidScale = 2;
    private float mMaxScale = 4;
    private float mFlingTriggerVelocity = 1000;

    private Matrix mMatrix;
    private boolean mIsScaleOrTransAnimating = false;
    private boolean mIsTouchScaling = false;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;
    private OnClickListener mOnClickListener;
    private OnLongClickListener mOnLongClickListener;


    public ScaleImageView(Context context) {
        this(context, null);
    }

    public ScaleImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        mMatrix = new Matrix();
        resetState();
        setOnTouchListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        mGestureDetector = new GestureDetectorCompat(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (mIsScaleOrTransAnimating)
                            return true;
                        float targetScale = mMinScale;
                        if (getCurrentScale() < mMidScale)
                            targetScale = mMidScale;
                        else if (getCurrentScale() >= mMidScale && getCurrentScale() < mMaxScale)
                            targetScale = mMaxScale;
                        else if (getCurrentScale() >= mMaxScale)
                            targetScale = mMinScale;
                        doScaleAnimator(targetScale, e.getX(), e.getY(), 800);
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        if (mIsScaleOrTransAnimating || getCurrentScale() <= mMinScale)
                            return true;
                        Log.d("ScaleImageView", "onScroll ");
                        if (e2.getAction() == MotionEvent.ACTION_MOVE) {
                            if (!mIsTouchScaling) {
                                mMatrix.postTranslate(-distanceX, -distanceY);
                                checkBound();
                            }
                        }
                        mIsTouchScaling = false;
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        Log.d("ScaleImageView1", "onFling ");
                        if (mIsScaleOrTransAnimating || getCurrentScale() <= mMinScale)
                            return true;
                        RectF bound = getCurrentBoundRectF();
                        float factor = getCurrentScale() / mMaxScale;
                        float transX = (e2.getX() - e1.getX()) * factor * factor;
                        float transY = (e2.getY() - e1.getY()) * factor * factor;
                        float targetX = 0, targetY = 0;
                        if (Math.abs(velocityX) > mFlingTriggerVelocity) {
                            if (transX < 0) {
                                float maxTransX = Math.min(getWidth() - bound.right, 0);
                                targetX = Math.max(transX, maxTransX);
                            }
                            if (transX > 0) {
                                float maxTransX = -Math.min(bound.left, 0);
                                targetX = Math.min(transX, maxTransX);
                            }
                        }
                        if (Math.abs(velocityY) > mFlingTriggerVelocity) {
                            if (transY < 0) {
                                float maxTransY = Math.min(getHeight() - bound.bottom, 0);
                                targetY = Math.max(transY, maxTransY);
                            }
                            if (transY > 0) {
                                float maxTransY = -Math.min(bound.top, 0);
                                targetY = Math.min(transY, maxTransY);
                            }
                        }
                        if (targetX != 0 || targetY != 0)
                            doTransAnimator(targetX, targetY, 350);
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (null != mOnClickListener) {
                            mOnClickListener.onClick(ScaleImageView.this);
                            return true;
                        }
                        return super.onSingleTapConfirmed(e);
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (null != mOnLongClickListener && e.getPointerCount() == 1)
                            mOnLongClickListener.onLongClick(ScaleImageView.this);
                        super.onLongPress(e);
                    }
                });
    }

    private void resetState() {
        mMatrix.reset();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
        initScale();
    }

    private void initScale() {
        int width = getWidth();
        int height = getHeight();
        int intrinsicWidth = getDrawable().getIntrinsicWidth();
        int intrinsicHeight = getDrawable().getIntrinsicHeight();
        // scale by width
        float scale = width * 1f / intrinsicWidth;
        mMinScale = scale;
        mMidScale = scale * 2;
        mMaxScale = scale * 4;
        // trans to center
        int transX = getMeasuredWidth() / 2 - intrinsicWidth / 2;
        int transY = getMeasuredHeight() / 2 - intrinsicHeight / 2;
        mMatrix.reset();
        mMatrix.postTranslate(transX, transY);
        mMatrix.postScale(scale, scale, width / 2, height / 2);
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(mMatrix);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() > 1) {
            // scale event
            mScaleGestureDetector.onTouchEvent(event);
            return true;
        } else {
            mGestureDetector.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE
                || event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            disallowInterceptTouchEvent();
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (getDrawable() == null || mIsScaleOrTransAnimating)
            return true;
        float scaleFactor = detector.getScaleFactor();
        float currentScale = getCurrentScale();
        // scale. only handle zoom in
        float afterScale = scaleFactor * currentScale;
        if (afterScale > mMaxScale)
            scaleFactor = mMaxScale / currentScale;
        checkBound();
        mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
        setImageMatrix(mMatrix);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mIsTouchScaling = true;
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;
    }

    @Override
    public void onScaleEnd(final ScaleGestureDetector detector) {
        Log.d("ScaleImageView", "onScaleEnd");
        disallowInterceptTouchEvent();
        // end scale to ori size
        if (!mIsScaleOrTransAnimating && getCurrentScale() < mMinScale) {
            doScaleAnimator(mMinScale, getWidth() / 2, getHeight() / 2,
                    700);
        }
    }

    private void doTransAnimator(final float targetTransX, final float targetTransY, int duration) {

        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (float) animation.getAnimatedValue();
                mMatrix.postTranslate(targetTransX * current, targetTransY * current);
                checkBound();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsScaleOrTransAnimating = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mIsScaleOrTransAnimating = true;
            }
        });
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(duration);
        animator.start();

    }

    private void doScaleAnimator(float targetScale, final float scaleCenterX,
                                 final float scaleCenterY, int duration) {
        ValueAnimator animator = ValueAnimator.ofFloat(getCurrentScale(), targetScale);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentScale = (float) animation.getAnimatedValue();
                float currentFactor = currentScale / getCurrentScale();
                checkBound();
                mMatrix.postScale(currentFactor, currentFactor, scaleCenterX, scaleCenterY);
                setImageMatrix(mMatrix);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsScaleOrTransAnimating = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mIsScaleOrTransAnimating = true;
            }
        });
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(duration);
        animator.start();
    }

    private void disallowInterceptTouchEvent() {
        if (getParent() != null) {
            if (getCurrentScale() <= mMinScale) {
                getParent().requestDisallowInterceptTouchEvent(false);
            } else {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
    }

    private void checkBound() {
        RectF currentBound = getCurrentBoundRectF();
        float transX = 0, transY = 0;
        if (currentBound.width() >= getWidth()) {
            if (currentBound.left > 0)
                transX = -currentBound.left;
            if (currentBound.right < getWidth())
                transX = getWidth() - currentBound.right;
        }
        if (currentBound.width() < getWidth()) {
            transX = currentBound.width() * 0.5f + getWidth() * 0.5f - currentBound.right;
        }
        if (currentBound.height() >= getHeight()) {
            if (currentBound.top > 0)
                transY = -currentBound.top;
            if (currentBound.bottom < getHeight())
                transY = getHeight() - currentBound.bottom;
        }
        if (currentBound.height() < getHeight()) {
            transY = currentBound.height() * 0.5f + getHeight() * 0.5f - currentBound.bottom;
        }
        mMatrix.postTranslate(transX, transY);
        setImageMatrix(mMatrix);
    }

    private float getCurrentScale() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private RectF getCurrentBoundRectF() {
        RectF rectF = new RectF();
        if (getDrawable() != null) {
            rectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            mMatrix.mapRect(rectF);
        }
        return rectF;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        resetState();
        super.setImageBitmap(bm);
    }

    @Override
    public void setImageResource(int resId) {
        resetState();
        super.setImageResource(resId);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        resetState();
        super.setImageDrawable(drawable);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.mOnClickListener = l;
        super.setOnClickListener(l);
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        this.mOnLongClickListener = l;
        super.setOnLongClickListener(l);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return; // couldn't resolve the URI
        }

        if (drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;     // nothing to draw (empty bounds)
        }

        if (getImageMatrix() == null && getPaddingTop() == 0 && getPaddingLeft() == 0) {
            drawResizeDrawable(drawable, canvas);
        } else {
            final int saveCount = canvas.getSaveCount();
            canvas.save();

            if (getCropToPadding()) {
                final int scrollX = getScrollX();
                final int scrollY = getScrollY();
                canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                        scrollX + getRight() - getLeft() - getPaddingRight(),
                        scrollY + getBottom() - getTop() - getPaddingBottom());
            }

            canvas.translate(getPaddingLeft(), getPaddingTop());

            if (getImageMatrix() != null) {
                canvas.concat(getImageMatrix());
            }
            drawResizeDrawable(drawable, canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * when the image (from local or net) size is too large,ImageView may show the image failure
     * with the warning as follow:
     * <p>
     * OpenGLRenderer: Bitmap too large to be uploaded into a texture (4912x3264, max=4096x4096)
     * <p>
     * this method gives a solution for the problem through resizing the BitmapDrawable if the image
     * size is over the max size.
     *
     * @param drawable
     * @param canvas
     */
    private void drawResizeDrawable(Drawable drawable, Canvas canvas) {

        if (getLayerType() == LAYER_TYPE_SOFTWARE) {
            drawable.draw(canvas);
        } else if (!(drawable instanceof BitmapDrawable)) {
            drawable.draw(canvas);
        } else {
            int maxW = canvas.getMaximumBitmapWidth();
            int maxH = canvas.getMaximumBitmapHeight();
            int bitmapW = drawable.getIntrinsicWidth();
            int bitmapH = drawable.getIntrinsicHeight();
            if (bitmapW > maxW || bitmapH > maxH) {
                // need resize
                float ratio = 0.3f * Math.min(maxW * 1f / bitmapW, maxH * 1f / bitmapH);
                Bitmap resizeBmp = Bitmap.createScaledBitmap(((BitmapDrawable) drawable).getBitmap(),
                        (int) (ratio * bitmapW), (int) (ratio * bitmapH), false);
                drawable = new BitmapDrawable(resizeBmp);
                setImageDrawable(drawable);
            } else {
                drawable.draw(canvas);
            }
        }
    }

    @Override
    public boolean getCropToPadding() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return false;
        return super.getCropToPadding();
    }
}
