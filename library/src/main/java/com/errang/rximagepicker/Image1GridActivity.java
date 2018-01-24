package com.errang.rximagepicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.errang.rximagepicker.adapter.image.ImageListAdapter;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.ui.ImageBaseActivity;
import com.errang.rximagepicker.utils.StatusbarUtil;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * Created by zengp on 2017/7/9.
 */

public class Image1GridActivity extends ImageBaseActivity implements View.OnClickListener,
        BaseQuickAdapter.OnRecyclerViewItemChildClickListener {

    private TextView tv_preview, tv_selected;
    private ImageView iv_ori;
    private RecyclerView imageRv;
    private ImageListAdapter imageListAdapter;

    private boolean isCrop = true;
    private boolean isPreview = true;
    private int mode = Config.MULTIPLE;
    private int max = 9;

    private Folder imageFolder;
    private int folderPosition;
    private ArrayList<Image> selectedImages = new ArrayList<>();

    public static void start(Context context, int folderPosition) {
        Intent starter = new Intent(context, Image1GridActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        starter.putExtra("folderPosition", folderPosition);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rx_activity_image1_grid);
        StatusbarUtil.StatusBarLightMode(this);
        folderPosition = getIntent().getIntExtra("folderPosition", 0);
        imageFolder = PickManager.getInstance().getImageFolders().get(folderPosition);

        mode = PickManager.getInstance().getConfig().getMode();
        isPreview = PickManager.getInstance().getConfig().isPreview();
        isCrop = PickManager.getInstance().getConfig().isCrop();
        max = PickManager.getInstance().getConfig().getMaxValue();

        initView();
    }

    private void initView() {
        initToolBar(imageFolder.name);

        iv_ori = findView(R.id.iv_ori);
        tv_preview = findView(R.id.tv_preview);
        tv_selected = findView(R.id.tv_selected);
        tv_preview.setVisibility(isPreview ? View.VISIBLE : View.GONE);
        tv_selected.setText(mode == Config.SINGLE ? "确定" : "确定(0/" + max + ")");
        findView(R.id.rl_bottom).setVisibility(mode == Config.SINGLE ? View.GONE : View.VISIBLE);
        findView(R.id.ll_ori).setOnClickListener(this);
        tv_preview.setOnClickListener(this);
        tv_selected.setOnClickListener(this);

        imageRv = findView(R.id.rv_list);
        imageListAdapter = new ImageListAdapter(this, R.layout.rx_item_image_list, 4);
        imageRv.setLayoutManager(new GridLayoutManager(this, 4));
        imageRv.setAdapter(imageListAdapter);
        imageListAdapter.setItemChildClickListener(this);
        imageListAdapter.setNewList(imageFolder.imageList);

        // refresh
        refreshSelectedImages();
    }

    private void refreshSelectedImages() {
        // preview observer
        ObservableManager.getInstance().createChangedObservable();
        // crop observer
        ObservableManager.getInstance().createCropObservable();

        ObservableManager.getInstance().getChangedSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Image>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Image image)
                            throws Exception {
                        // selected image changed
                        refreshChanged(image);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        // selected finish
                        // 预览页面点击确定
                        ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
                        ObservableManager.getInstance().getSelectedSubject().onComplete();
                        finish();
                    }
                });

        ObservableManager.getInstance().getCropSubject()
                .subscribe(new Consumer<Image>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Image image)
                            throws Exception {
                        // cropped image
                        refreshCropChanged(image);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        if (mode == Config.SINGLE && !isPreview) {
                            // 剪裁页面点击完成
                            ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
                            ObservableManager.getInstance().getSelectedSubject().onComplete();
                            finish();
                        }
                    }
                });
    }

    private void refreshChanged(Image image) {
        if (null == image)
            return;
        if (image.isSelected) {
            selectedImages.add(image);
        } else {
            selectedImages.remove(image);
        }
        toggleView();
        imageListAdapter.refreshData(image, false);
    }

    private void refreshCropChanged(Image image) {
        if (null == image)
            return;
        if (image.isSelected) {
            for (int i = 0; i < selectedImages.size(); i++) {
                if (selectedImages.get(i).name.equals(image.name)
                        && selectedImages.get(i).addTime == image.addTime) {
                    selectedImages.get(i).path = image.path;
                    selectedImages.get(i).width = image.width;
                    selectedImages.get(i).height = image.height;
                }
            }
        }
        imageListAdapter.refreshData(image, true);
    }

    private void toggleView() {
        tv_selected.setEnabled(selectedImages.size() > 0);
        tv_preview.setEnabled(selectedImages.size() > 0);
        if (mode == Config.SINGLE) {
            tv_selected.setText("确定");
        } else
            tv_selected.setText("确定(" + selectedImages.size() + "/" + max + ")");
        tv_selected.setTextColor(getResources().getColor(selectedImages.size() > 0 ? R.color.colorAccent :
                R.color.color_66dcb97a));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_ori) {
            iv_ori.setActivated(!iv_ori.isActivated());
        } else if (v.getId() == R.id.tv_selected) {
            ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
            ObservableManager.getInstance().getSelectedSubject().onComplete();
            finish();
        } else if (v.getId() == R.id.tv_preview) {
            // preview
            Image1PreviewActivity.start(this, selectedImages, 1, selectedImages.size());
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View v, int i) {
        Log.d("Image1GridActivity", "item click");
        Image item = (Image) adapter.getItem(i);
        if (v.getId() == R.id.iv_item_selected) {
            ImageView selected = (ImageView) v;
            if (selected.isActivated()) {
                selectedImages.remove(item);
                item.isSelected = false;
                selected.setActivated(false);
            } else if (selectedImages.size() >= max) {
                Toast.makeText(this, "最多可选" + max + "张", Toast.LENGTH_SHORT).show();
            } else {
                selectedImages.add(item);
                item.isSelected = true;
                selected.setActivated(true);
            }
            toggleView();
        } else if (v.getId() == R.id.iv_image_item) {
            // preview
            // bug 传递数据太大
            // !!! FAILED BINDER TRANSACTION !!!  (parcel size = 610188)
            // android.os.TransactionTooLargeException: data parcel size 610188 bytes
            if (PickManager.getInstance().getConfig().getMode() == Config.MULTIPLE
                    && PickManager.getInstance().getConfig().isPreview()) {
                // 多张，可预览
                Image1PreviewActivity.start(this, folderPosition, i + 1, selectedImages.size(), v);

            } else if (PickManager.getInstance().getConfig().getMode() == Config.SINGLE) {
                // 单张
                selectedImages.clear();
                item.isSelected = true;
                selectedImages.add(item);
                if (isPreview) {
                    // 去预览
                    Image1PreviewActivity.start(this, selectedImages, 1, selectedImages.size());
                } else if (isCrop) {
                    //  去剪裁
                    Image1CropActivity.start(this, item);
                } else {
                    // 直接返回结果
                    ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
                    ObservableManager.getInstance().getSelectedSubject().onComplete();
                    finish();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 返回到列表页，清除已经选的
        imageListAdapter.resetData();
        super.onBackPressed();
    }
}
