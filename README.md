# RxImagePicker

这是一个基于RxJava的图片选择器。可选择图片和视频。

1. 选择图片

![image](https://github.com/devilist/RxImagePicker/raw/master/images/image.gif)


配置方法：

a.首先通过实现接口ImageLoader来自定义图片加载方式，例如，如果用glide框架，可如下定义：
```
public class GlideImageLoader implements ImageLoader {

    @Override
    public void showImage(ImageView imageView, String path, int width, int height, boolean isPreview) {
        if (!isPreview)
            Glide.with(imageView.getContext()).load(path).centerCrop().override(width, height)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
        else
            Glide.with(imageView.getContext()).load(path).asBitmap()
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
    }
}
```
参数说明：

path：图片路径

width：图片宽度

height：图片高度

isPreview:是否是预览图片

b. 在Application的onCreate方法中初始化：
```
 @Override
    public void onCreate() {
        super.onCreate();
        RxImagePicker.init(new GlideImageLoader(), storageDir);
    }
```
参数说明：
storageDir：缓存图片的存储位置

c.调用方法
调用过程摆脱了通过activity/fragment的 onActivityResult()方法来接收返回结果的繁琐方式；
而通过RxJava创建观察者，并采用链式调用，即选即得，简单易用，整个调用过程一目了然。

```
RxImagePicker
            .ready()
            .limit(Integer.valueOf(et_num.getText().toString()))   // 设置最大数目
            .single(sc_single.isChecked())                         // 是否单选
            .crop(sc_crop.isChecked())                             // 是否裁剪 
            .preview(sc_preview.isChecked())                       // 是否预览
            .camera(sc_camera.isChecked())                         // 开启相机
            .go(this)                                              // 创建观察者
            .subscribeOn(AndroidSchedulers.mainThread())         
            .observeOn(AndroidSchedulers.mainThread())             
            .subscribe(                                            // 监听订阅事件
              new Consumer<List<Image>>() {
                  @Override
                  public void accept(@NonNull List<Image> images) throws Exception {
                     // 在这里返回了选择的图片
            });              
```
d. 特别说明
针对6.0以上系统，内部已经处理了权限申请相关工作，因此调用之前无需再判断权限。

2.选择视频

![image](https://github.com/devilist/RxImagePicker/raw/master/images/video.gif)

选择视频和选择图片类似，调用方法如下：

```
RxImagePicker
        .ready()
        .limit(Integer.valueOf(et_num.getText().toString()))          // 设置最大数目
        .single(sc_single.isChecked())                                // 是否单选
        .goVideo(this)                                                // 创建观察者
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<List<Video>>() {
          @Override
          public void accept(@NonNull List<Video> videos) throws Exception {
            // 在这里返回了选择的视频        
          });
```

3.其他相关

model类
```
public class Image implements Serializable {
    public long id;
    public String name;                 // 图片名字
    public String path;                 // 图片路径
    public long size;                   // 图片大小
    public int width;                   // 图片宽度
    public int height;                  // 图片高度
    public String mimeType;
    public long addTime;
}


public class Video {

    public String name;                   // 视频名字
    public String path;                   // 视频路径
    public String thumbnail;              // 视频缩略图
    public long size;                     // 视频大小
    public long duration;                 // 视频时长
    public int width;                     // 视频宽度
    public int height;                    // 视频高度
    public String mimeType;
    public long addTime;
}

```




