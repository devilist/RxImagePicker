package com.errang.rximagepicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.errang.rximagepicker.adapter.image.ImageListAdapter;
import com.errang.rximagepicker.adapter.base.BaseQuickAdapter;
import com.errang.rximagepicker.model.Folder;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.ui.ImageBaseActivity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by zengp on 2017/7/8.
 */

public class ImageGridActivity extends ImageBaseActivity implements View.OnClickListener,
        BaseQuickAdapter.OnRecyclerViewItemChildClickListener {

    public static final int REQUEST_PERMISSION_STORAGE = 100;
    public static final int REQUEST_PERMISSION_CAMERA = 110;

    private TextView tv_preview, tv_selected;
    private RecyclerView imageRv;
    private ImageListAdapter imageListAdapter;

    private ArrayList<Folder> imageFolders = new ArrayList<>();
    private PublishSubject<List<Folder>> imageLoadSubject = PublishSubject.create();

    private List<Image> selectedImages = new ArrayList<>();

    public static void start(Context context) {
        Intent starter = new Intent(context, ImageGridActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rx_activity_image_grid);
        initView();
    }

    private void initView() {
        initToolBar("全部图片");

        int mode = PickManager.getInstance().getConfig().getMode();
        boolean isPreview = PickManager.getInstance().getConfig().isPreview();
        int max = PickManager.getInstance().getConfig().getMaxValue();
        tv_preview = findView(R.id.tv_preview);
        tv_selected = findView(R.id.tv_selected);
        tv_preview.setVisibility(isPreview ? View.VISIBLE : View.GONE);
        tv_selected.setText(mode == Config.SINGLE ? "确定" : "确定(0/" + max + ")");
        tv_preview.setOnClickListener(this);
        tv_selected.setOnClickListener(this);

        imageRv = findView(R.id.rv_list);
        imageListAdapter = new ImageListAdapter(this, R.layout.rx_item_image_list, 3);
        imageRv.setLayoutManager(new GridLayoutManager(this, 3));
        imageRv.setAdapter(imageListAdapter);
        imageListAdapter.setItemChildClickListener(this);

        // create observable
        ObservableManager.getInstance().createLoadObservable();
        // permission
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                new ImageSource(this);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_STORAGE);
            }
        } else {
            new ImageSource(this);
        }
        // load image
        loadImages();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE && grantResults.length > 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new ImageSource(this);
            } else {
                Toast.makeText(this, "权限被禁止，无法选择本地图片", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "权限被禁止，无法打开相机", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadImages() {
        ObservableManager.getInstance().getLoadSubject()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Folder>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull List<Folder> folders)
                            throws Exception {
                        imageFolders = (ArrayList<Folder>) folders;
                        imageListAdapter.setNewList(folders.get(0).imageList);
                    }
                });
    }

    private void refreshSelectedImages() {
        ObservableManager.getInstance().getChangedSubject()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Image>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Image image)
                            throws Exception {

                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_selected) {
            ObservableManager.getInstance().getSelectedSubject().onNext(selectedImages);
            ObservableManager.getInstance().getSelectedSubject().onComplete();
            finish();
        } else if (v.getId() == R.id.tv_preview) {
            // preview
            ObservableManager.getInstance().createChangedObservable();

            refreshSelectedImages();
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View v, int i) {
        Image item = (Image) adapter.getItem(i);
        int max = PickManager.getInstance().getConfig().getMaxValue();
        if (v.getId() == R.id.iv_item_selected) {
            ImageView selected = (ImageView) v;
            if (selected.isActivated()) {
                selectedImages.remove(item);
                selected.setActivated(false);
            } else if (selectedImages.size() >= max) {
                Toast.makeText(this, "最多可选" + max + "张", Toast.LENGTH_SHORT).show();
            } else {
                selectedImages.add(item);
                selected.setActivated(true);
            }
            tv_selected.setText("确定(" + selectedImages.size() + "/" + max + ")");
            tv_selected.setEnabled(selectedImages.size() > 0);
            tv_selected.setTextColor(getResources().getColor(selectedImages.size() > 0 ? android.R.color.white :
                    R.color.color_757575));
        } else if (v.getId() == R.id.iv_image_item) {

        }
    }
}
