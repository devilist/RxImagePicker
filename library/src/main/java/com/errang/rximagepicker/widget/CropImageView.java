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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * crop ImageView
 * Created by zengp on 2017/11/15.
 */

public class CropImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener,
        ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private final static String TAG = "CropImageView";

    // scale
    private float mMinScale = 1;
    private float mMidScale = 2;
    private float mMaxScale = 4;

    // rotate
    private Distance mDistanceDown, mDistanceMove;
    private float mLastRotation = 0, mTotalRotation = 0;

    // crop
    private float mCropSize = 0;
    private String mCropFileDir = "";
    private int mCropOutWidth = 0, mCropOutHeight = 0;
    private boolean mIsCropFinish = true;

    private Matrix mMatrix;
    private boolean mIsScaleOrRotateAnimating = false;
    private boolean mIsTouchScaling = false; // whether touch scaling currently or not

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;

    private ImageCropListener mImageCropListener;

    public interface ImageCropListener {
        void onCropResult(String filePath, String message, boolean isCropSuccess);
    }

    // record the two touch position in the screen when scaling
    private class Distance {
        static final float INVALID = -1;
        float fromX = INVALID, fromY = INVALID, toX = INVALID, toY = INVALID;

        void update(float fromX, float fromY, float toX, float toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        boolean isInvalid() {
            return fromX == INVALID || fromY == INVALID || toX == INVALID || toY == INVALID;
        }

        void reset() {
            fromX = fromY = toX = toY = INVALID;
        }

        // the included angle between this distance and the axis-X
        float getDistanceAngleFromAxisX() {
            float distanceX = toX - fromX;
            float distanceY = toY - fromY;
            float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
            if (distance == 0)
                return 0;
            else {
                float deg = (float) (180 / Math.PI * Math.asin(Math.abs(distanceY) / distance));
                if (distanceX <= 0 && distanceY > 0)
                    deg = 180 - deg;
                if (distanceX <= 0 && distanceY < 0)
                    deg += 180;
                if (distanceX > 0 && distanceY < 0)
                    deg = 360 - deg;
                return deg;
            }
        }

        // the rotation value from the target distance to this distance
        float getRotateFrom(Distance target) {
            float angleTo = getDistanceAngleFromAxisX();
            float angleFrom = target.getDistanceAngleFromAxisX();
            float rotation = angleTo - angleFrom;
            if (angleTo < angleFrom)
                rotation += 360;
            return rotation;
        }

        @Override
        public String toString() {
            return "from(" + fromX + ", " + fromY + "), to(" + toX + ", " + toY + ")";
        }
    }

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
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

    private void init() {
        setBackgroundColor(0xff000000);
        setScaleType(ScaleType.MATRIX);
        mMatrix = new Matrix();
        mDistanceDown = new Distance();
        mDistanceMove = new Distance();
        resetState();
        setOnTouchListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        mGestureDetector = new GestureDetectorCompat(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (mIsScaleOrRotateAnimating)
                            return true;
                        doScaleAndRotateAnimator(mMidScale, e.getX(), e.getY(), 400);
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        if (mIsScaleOrRotateAnimating)
                            return true;
                        if (e2.getAction() == MotionEvent.ACTION_MOVE) {
                            if (!mIsTouchScaling) {
                                mMatrix.postTranslate(-distanceX, -distanceY);
                                checkBound(false);
                            }
                        }
                        mIsTouchScaling = false;
                        return true;
                    }
                });
    }

    private void resetState() {
        mMatrix.reset();
        // rotate
        mDistanceDown.reset();
        mDistanceMove.reset();
        mTotalRotation = 0;
        // crop
        mIsCropFinish = true;
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
        // crop area size
        mCropSize = Math.min(width, height) * 0.7f;
        mMinScale = mCropSize * 1f / intrinsicWidth;
        mMidScale = scale;
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
        } else {
            // drag
            mGestureDetector.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE
                || event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            disallowInterceptTouchEvent();
        }

        // record touch position for rotation
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDistanceDown.reset();
            mDistanceMove.reset();
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() > 1) {
                float pointX1 = event.getAxisValue(MotionEvent.AXIS_X, 0);
                float pointY1 = event.getAxisValue(MotionEvent.AXIS_Y, 0);
                float pointX2 = event.getAxisValue(MotionEvent.AXIS_X, 1);
                float pointY2 = event.getAxisValue(MotionEvent.AXIS_Y, 1);
                if (mDistanceDown.isInvalid()) {
                    mDistanceDown.update(pointX1, pointY1, pointX2, pointY2);
                }
                mDistanceMove.update(pointX1, pointY1, pointX2, pointY2);
            } else {
                mDistanceDown.reset();
                mDistanceMove.reset();
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mDistanceDown.reset();
            mDistanceMove.reset();
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (getDrawable() == null || mIsScaleOrRotateAnimating)
            return true;

        // post trans
        checkBound(false);

        // post scale
        float scaleFactor = detector.getScaleFactor();
        float currentScale = getCurrentScale();
        // scale. only handle zoom in
        float afterScale = scaleFactor * currentScale;
        if (afterScale > mMaxScale) scaleFactor = mMaxScale / currentScale;
        if (afterScale < mMinScale) scaleFactor = mMinScale / currentScale;
        mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

        // post rotate
        float deg = mDistanceMove.getRotateFrom(mDistanceDown);
        float currentDeg = deg - mLastRotation;
        currentDeg = Math.min(360 - currentDeg, currentDeg);
        mTotalRotation += currentDeg;
        mMatrix.postRotate(currentDeg, getWidth() / 2, getHeight() / 2);
        mLastRotation = deg;
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
        disallowInterceptTouchEvent();
        mLastRotation = 0;
        // total rotation
        mTotalRotation %= 360;
        if (mTotalRotation < 0) mTotalRotation += 360;
    }

    private void doScaleAndRotateAnimator(float targetScale, final float scaleCenterX,
                                          final float scaleCenterY, int duration) {
        // for rotation
        int timeCount = 100;
        final float everyDeg = mTotalRotation > 180 ?
                (360 - mTotalRotation) / timeCount : -mTotalRotation / timeCount;

        // for trans XY (only when current scale equals midScale)
        RectF currentBound = getCurrentBoundRectF();
        final float everyTransX = (getWidth() / 2 - currentBound.centerX()) / timeCount;
        final float everyTransY = (getHeight() / 2 - currentBound.centerY()) / timeCount;

        final boolean needRotation = mTotalRotation > 0;
        final boolean needTrans = getCurrentScale() == targetScale && mTotalRotation == 0;

        for (int i = 0; i < timeCount; i++) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (needRotation)
                        mMatrix.postRotate(everyDeg, getWidth() / 2, getHeight() / 2);
                    if (needTrans) {
                        mMatrix.postTranslate(everyTransX, everyTransY);
                    }
                    setImageMatrix(mMatrix);
                }
            }, duration / timeCount * i);
        }

        // for scale
        final boolean needScale = getCurrentScale() != targetScale;
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(getCurrentScale(), targetScale);
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (needScale) {
                    float currentScale = (float) animation.getAnimatedValue();
                    float currentFactor = currentScale / getCurrentScale();
                    checkBound(true);
                    mMatrix.postScale(currentFactor, currentFactor, scaleCenterX, scaleCenterY);
                    setImageMatrix(mMatrix);
                }
            }
        });
        scaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsScaleOrRotateAnimating = false;
                mTotalRotation = 0;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mIsScaleOrRotateAnimating = true;
            }
        });
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.setDuration(duration);
        scaleAnimator.start();
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

    private void checkBound(boolean strictMode) {
        RectF currentBound = getCurrentBoundRectF();
        float transX = 0, transY = 0;
        if (strictMode) {
            if (currentBound.width() >= getWidth()) {
                if (currentBound.left > 0)
                    transX = -currentBound.left;
                if (currentBound.right < getWidth())
                    transX = getWidth() - currentBound.right;
            }

            if (currentBound.height() >= getHeight()) {
                if (currentBound.top > 0)
                    transY = -currentBound.top;
                if (currentBound.bottom < getHeight())
                    transY = getHeight() - currentBound.bottom;
            }
            if (currentBound.width() < getWidth()) {
                transX = currentBound.width() * 0.5f + getWidth() * 0.5f - currentBound.right;
            }
            if (currentBound.height() < getHeight()) {
                transY = currentBound.height() * 0.5f + getHeight() * 0.5f - currentBound.bottom;
            }
        }
        mMatrix.postTranslate(transX, transY);
        setImageMatrix(mMatrix);
    }

    private float getCurrentScale() {
        if (null == getDrawable()) {
            float[] values = new float[9];
            mMatrix.getValues(values);
            return values[Matrix.MSCALE_X];
        } else {
            // rotation would certainly change the value of scale. for example ,when the rotation value is 90 deg,
            // the scale value would be changed to 0; so if the image is rotated, the actually scale value can be
            // got as follow.
            RectF bound = getCurrentBoundRectF();
            return (bound.width()) / getDrawable().getIntrinsicWidth();
        }
    }

    private RectF getCurrentBoundRectF() {
        RectF rectF = new RectF();
        if (getDrawable() != null) {
            rectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            mMatrix.mapRect(rectF);
        }
        return rectF;
    }

    public CropImageView addCropListener(ImageCropListener cropListener) {
        this.mImageCropListener = cropListener;
        return this;
    }

    public CropImageView cropSaveDir(String dir) {
        mCropFileDir = dir;
        return this;
    }

    public CropImageView cropOutSize(int width, int height) {
        mCropOutWidth = width;
        mCropOutHeight = height;
        return this;
    }

    public void cropToFile(ImageCropListener cropListener) {
        this.mImageCropListener = cropListener;
        cropToFile();

    }

    public void cropToFile() {
        if (mIsScaleOrRotateAnimating) return;
        if (!mIsCropFinish) return;
        mIsCropFinish = false;
        if (null != getDrawable()) {
            if (null == mCropFileDir || TextUtils.isEmpty(mCropFileDir)) {
                dispatchCropListener("", "crop failure: invalid file dir", false);
            } else {
                File outFile = new File(mCropFileDir);
                if (!outFile.exists())
                    outFile.mkdir();
                if (mCropOutWidth <= 0 || mCropOutHeight <= 0) {
                    mCropOutWidth = mCropOutHeight = (int) mCropSize;
                }

                Bitmap outBitmap = ((BitmapDrawable) getDrawable()).getBitmap();
                if (null == outBitmap) {
                    dispatchCropListener("", "crop failure: invalid src bitmap", false);
                    return;
                }
                // trans,scale,rotate
                Matrix matrix = new Matrix(mMatrix);
                try {
                    outBitmap = Bitmap.createBitmap(outBitmap, 0, 0, outBitmap.getWidth(),
                            outBitmap.getHeight(), matrix, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    dispatchCropListener("", "crop failure: " + e.getMessage(), false);
                    return;
                }
                // crop area
                RectF currentBound = new RectF();
                currentBound.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
                matrix.mapRect(currentBound);
                float focusLeft = (getWidth() - mCropSize) / 2;
                float focusTop = (getHeight() - mCropSize) / 2;
                float focusRight = focusLeft + mCropSize;
                float focusBottom = focusTop + mCropSize;
                // check bound
                if (focusLeft >= currentBound.left && focusTop >= currentBound.top
                        && focusRight <= currentBound.right && focusBottom <= currentBound.bottom) {
                    float cropLeft = focusLeft - currentBound.left;
                    float cropTop = focusTop - currentBound.top;
                    // create crop area image
                    outBitmap = Bitmap.createBitmap(outBitmap, (int) cropLeft, (int) cropTop, (int) mCropSize, (int) mCropSize,
                            null, true);
                } else {
                    // out of bitmap bound
                    Bitmap bitmap = Bitmap.createBitmap((int) mCropSize, (int) mCropSize, Bitmap.Config.ARGB_8888);
                    bitmap.setHasAlpha(true);
                    Canvas canvas = new Canvas(bitmap);
                    float cropLeft = currentBound.left - focusLeft;
                    float cropTop = currentBound.top - focusTop;
                    canvas.drawBitmap(outBitmap, cropLeft, cropTop, new Paint(Paint.ANTI_ALIAS_FLAG));
                    outBitmap = bitmap;
                }

                if (mCropOutWidth != mCropSize || mCropOutHeight != mCropSize) {
                    outBitmap = Bitmap.createScaledBitmap(outBitmap, mCropOutWidth, mCropOutHeight, true);
                }
                // save crop bitmap
                saveBitmapToFile(outBitmap);
            }
        }
    }

    private void saveBitmapToFile(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mCropFileDir.endsWith("/")) mCropFileDir += "/";
                String fileName = mCropFileDir + "IMAGE_CROP_" + System.currentTimeMillis() + ".png";
                try {
                    FileOutputStream fos = new FileOutputStream(fileName);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    dispatchCropListener(fileName, "crop image success", true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    dispatchCropListener("", "crop failure: " + e.getMessage(), false);
                } catch (IOException e) {
                    e.printStackTrace();
                    dispatchCropListener("", "crop failure: " + e.getMessage(), false);
                }
            }
        }).start();
    }


    private void dispatchCropListener(final String filePath, final String message, final boolean isCropSuccess) {
        post(new Runnable() {
            @Override
            public void run() {
                if (null != mImageCropListener)
                    mImageCropListener.onCropResult(filePath, message, isCropSuccess);
                mIsCropFinish = true;
            }
        });
    }

    private void drawCropBorder(Canvas canvas) {
        int cropBorderColor = 0xffffffff;
        int shieldColor = 0xbb000000;
        Path focusPath = new Path();
        float focusLeft = (getWidth() - mCropSize) / 2;
        float focusTop = (getHeight() - mCropSize) / 2;
        RectF focusRectF = new RectF(focusLeft, focusTop, focusLeft + mCropSize, focusTop + mCropSize);
        focusPath.addRect(focusRectF, Path.Direction.CCW);
        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.clipPath(focusPath, Region.Op.DIFFERENCE);
        canvas.drawColor(shieldColor);
        canvas.restore();
        Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setColor(cropBorderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(0.5f);
        mBorderPaint.setAntiAlias(true);
        canvas.drawPath(focusPath, mBorderPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /*========================================================================================*/
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

        /*========================================================================================*/
        // draw crop area
        drawCropBorder(canvas);
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
        return super.getCropToPadding();
    }
}