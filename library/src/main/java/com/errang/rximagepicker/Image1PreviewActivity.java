package com.errang.rximagepicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.errang.rximagepicker.adapter.image.Image1PreviewAdapter;
import com.errang.rximagepicker.model.Image;
import com.errang.rximagepicker.ui.ImageBaseActivity;
import com.errang.rximagepicker.widget.RecyclerViewPager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

/**
 * Created by zengp on 2017/7/9.
 */

public class Image1PreviewActivity extends ImageBaseActivity implements View.OnClickListener,
        RecyclerViewPager.OnPageSelectListener {

    private TextView tv_title, tv_crop, tv_selected;
    private ImageView iv_item_selected, iv_ori;

    private RecyclerViewPager viewPager;
    private Image1PreviewAdapter adapter;

    private boolean isCrop = true;
    private boolean isCamera = false;
    private int mode = Config.MULTIPLE;
    private int max = 9;

    private List<Image> imageList;
    private int folderPosition = -1;
    private int currentPosition = 1;
    private int selectedCount = 0;
    private Image currentImage;

    public static void start(Context activity, int folderPosition, int position, int selectedCount, View view) {
        Intent intent = new Intent(activity, Image1PreviewActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("folderPosition", folderPosition);
        intent.putExtra("list_position", position);
        intent.putExtra("selectedCount", selectedCount);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(view,
                view.getWidth() / 2, view.getHeight() / 2, view.getWidth(), view.getHeight());
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    public static void start(Context activity, ArrayList<Image> imageList, int position, int selectedCount) {
        Intent intent = new Intent(activity, Image1PreviewActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("imageList", imageList);
        intent.putExtra("list_position", position);
        intent.putExtra("selectedCount", selectedCount);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rx_activity_image1_preview);
        folderPosition = getIntent().getIntExtra("folderPosition", -1);
        if (folderPosition == -1)
            imageList = (List<Image>) getIntent().getSerializableExtra("imageList");
        else
            imageList = PickManager.getInstance().getImageFolders().get(folderPosition).imageList;
        currentPosition = getIntent().getIntExtra("list_position", 1);
        selectedCount = getIntent().getIntExtra("selectedCount", 0);
        currentImage = imageList.get(currentPosition - 1);

        mode = PickManager.getInstance().getConfig().getMode();
        isCrop = PickManager.getInstance().getConfig().isCrop();
        isCamera = PickManager.getInstance().getConfig().isShowCamera();
        max = PickManager.getInstance().getConfig().getMaxValue();

        initView();
    }

    private void initView() {

        String title = isCamera || mode == Config.SINGLE ? "预览" : currentPosition + " / " + imageList.size();
        initToolBar(title);

        tv_title = findView(R.id.tv_title);
        tv_crop = findView(R.id.tv_crop);
        tv_selected = findView(R.id.tv_selected);
        iv_ori = findView(R.id.iv_ori);

        iv_item_selected = findView(R.id.iv_item_selected);
        iv_item_selected.setActivated(currentImage.isSelected);
        iv_item_selected.setVisibility(isCamera || mode == Config.SINGLE ? View.GONE : View.VISIBLE);
        tv_crop.setEnabled(iv_item_selected.isActivated());
        tv_crop.setVisibility(isCrop ? View.VISIBLE : View.GONE);
        toggleView();

        viewPager = findView(R.id.rv_pager);
        adapter = new Image1PreviewAdapter(R.layout.rx_item_image1_preview);
        adapter.setNewList(imageList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentPage(currentPosition - 1);
        viewPager.setOnPageSelectListener(this);

        iv_item_selected.setOnClickListener(this);
        tv_selected.setOnClickListener(this);
        tv_crop.setOnClickListener(this);
        findView(R.id.ll_ori).setOnClickListener(this);

        refreshCropImage();
    }

    private void refreshCropImage() {
        ObservableManager.getInstance().getCropSubject()
                .subscribe(new Consumer<Image>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Image image)
                            throws Exception {
                        // image cropped
                        refreshCrop(image);
                    }
                });
    }

    private void refreshCrop(Image image) {
        currentImage.path = image.path;
        currentImage.width = image.width;
        currentImage.height = image.height;
        adapter.refreshData(image, currentPosition - 1);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_ori) {
            iv_ori.setActivated(!iv_ori.isActivated());
        } else if (v.getId() == R.id.tv_crop) {
            Image1CropActivity.start(this, currentImage);
        } else if (v.getId() == R.id.tv_selected) {
            ObservableManager.getInstance().getChangedSubject().onComplete();
            if (mode == Config.MULTIPLE)
                ObservableManager.getInstance().getCropSubject().onComplete();
            finish();
        } else if (v.getId() == R.id.iv_item_selected) {
            if (currentImage.isSelected) {
                currentImage.isSelected = false;
                iv_item_selected.setActivated(false);
                tv_crop.setEnabled(false);
                selectedCount -= 1;
                toggleView();
                ObservableManager.getInstance().getChangedSubject().onNext(currentImage);
            } else if (selectedCount < max) {
                currentImage.isSelected = true;
                iv_item_selected.setActivated(true);
                tv_crop.setEnabled(true);
                selectedCount += 1;
                toggleView();
                ObservableManager.getInstance().getChangedSubject().onNext(currentImage);
            } else {
                Toast.makeText(this, "最多可选" + max + "张", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleView() {
        if (isCamera || mode == Config.SINGLE) {
            tv_selected.setText("确定");
        } else
            tv_selected.setText("确定(" + selectedCount + "/" + max + ")");
        tv_selected.setEnabled(selectedCount > 0);
        tv_selected.setTextColor(getResources().getColor(selectedCount > 0 ? R.color.color_f26d85 :
                R.color.color_66dcb97a));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset) {

    }

    @Override
    public void onPageSelected(int position) {
        currentPosition = position + 1;
        int mode = PickManager.getInstance().getConfig().getMode();
        String title = isCamera || mode == Config.SINGLE ? "预览" : currentPosition + " / " + imageList.size();
        tv_title.setText(title);
        currentImage = imageList.get(currentPosition - 1);
        iv_item_selected.setActivated(currentImage.isSelected);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onBackPressed() {
        if (isCamera) {
            currentImage.isSelected = false;
            ObservableManager.getInstance().getChangedSubject().onNext(currentImage);
            ObservableManager.getInstance().getChangedSubject().onComplete();
        }
        super.onBackPressed();
    }
}
