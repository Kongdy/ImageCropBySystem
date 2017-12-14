# ImageCropBySystem
调用系统自带裁剪工具对图片进行裁剪、7.0适配，以及6.0动态权限封装

##一、前文
<p>&nbsp;&nbsp;之前使用的图片裁剪功能一直是使用第三方的，也没时间去思考自己写一个的想法。后来无意间发现android自己本来就有裁剪功能，所以自己动手去集成了一把，并且把自己的权限封装以及7.0的适配都加进去</p>

##二、注意的几个点
&nbsp;&nbsp;其实也没有什么好说的，基本没有难度，只是有几个需要注意的点
1.一个是7.0的文件安全机制，7.0之后android对于文件的安全增加了保护，在部分地方使用Uri会产生FileUriExposedException文件暴露异常。
2.其次，就是对于权限的封装，只有拿到了权限才能进行操作，做好权限适配，这些下面会一并讲到。
##三、权限封装
&nbsp;&nbsp;权限封装最好封装到一个方法里面，独立处理权限，这样也可以比较轻松的集成在BaseActivity里面，在用的地方直接调，可以达到权限随用随取得效果。先看看关键代码
```java
	  public void needPermission(AppPermissionListener mAppPermissionListener, List<String> permissions) {
        if (null != permissions && permissions.size() > 0) {
            this.appPermissionListener = mAppPermissionListener;
            this.allPermission = permissions;
            // 允许的权限
            allowPermission.clear();
            // 被拒绝的权限
            deniedPermission.clear();
            // 不在询问的权限
            neverAskPermission.clear();
            // 开始遍历拿到的权限
            Observable.fromIterable(allPermission)
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            // 判断是否有WRITE_SETTINGS特殊权限
                            if (s.equals(Manifest.permission.WRITE_SETTINGS)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
	                                // WRITE_SETTINGS需要用该方式判断
                                    if (!Settings.System.canWrite(mActivity))
                                        allowPermission.add(s);
                                    else
                                        haveWriteSetting = true;
                                }
                            } else if (ActivityCompat.checkSelfPermission(mActivity, s)         
                            == PackageManager.PERMISSION_GRANTED) {
                                allowPermission.add(s);
                            } else {
                                deniedPermission.add(s);
                            }
                        }
                    });
            if (haveWriteSetting) {
	            // 请求特殊权限
                requestWriteSettings();
                return;
            }
            if (!deniedPermission.isEmpty()) {
                String[] tempArray = new String[deniedPermission.size()];
                deniedPermission.toArray(tempArray);
                requestPermissions(tempArray);
                if (null != mAppPermissionListener) {
                    mAppPermissionListener.onHaveDenied(deniedPermission);
                }
                return;
            }
            if (!neverAskPermission.isEmpty()) {
                showNeverAskDialog();
                if (null != mAppPermissionListener) {
                    mAppPermissionListener.onNeverAsk(neverAskPermission);
                }
                return;
            }
            // 权限通过，回调onAllGranted方法
            if (null != mAppPermissionListener) {
                mAppPermissionListener.onAllGranted();
            }
        }
    }
```
<font color="purple" size ="4" face="黑体" ><b>android有两个特殊权限，一个是WRITE_SETTINGS，另外一个是SYSTEM_ALERT_WINDOW，这里只对WRITE_SETTINGS做了处理，我在想有没有更优雅的方法把SYSTEM_ALERT_WINDOW加进去，所以后面会完善。
&nbsp;&nbsp;在这里处理了会触发拒绝权限的弹框。但是当用户点击了不再询问权限，就不会再弹框。android原生给我们提供了shouldShowRequestPermissionRationale()方法来判断是否不再弹框。但是 这个方法会在部分手机上失效，比如我手中的魅族pro6手机，可能由于ROM问题，在调用该方法的时候就是失效的。那么，我们可以用另外一种方法来达到相同的目的。
&nbsp;&nbsp;我们可以直接调用requestPermissions方法，在onRequestPermissionsResult的权限回调方法中进行检查，如果拒绝权限列表中还有这个权限的话，就可以进行弹框引导用户去设置中手动打开。不但替代了shouldShowRequestPermissionRationale方法，而且在用户一通拒绝之后还能给予提醒。下面是onRequestPermissionsResult方法的代码
</b>
</font >
```java
 // 权限返回结果
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String per = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (deniedPermission.contains(per)) {
                        deniedPermission.remove(per);
                    } else if (neverAskPermission.contains(per)) {
                        neverAskPermission.remove(per);
                    }
                    if (!allowPermission.contains(per))
                        allowPermission.add(per);
                } else {
                    if (deniedPermission.contains(per)) {
                        deniedPermission.remove(per);
                        neverAskPermission.add(per);
                    }
                }
            }
            if (!deniedPermission.isEmpty()) {
                neverAskPermission.addAll(new ArrayList<>(deniedPermission));
                deniedPermission.clear();
            }
            if (!neverAskPermission.isEmpty()) {
                showNeverAskDialog();
                if (null != appPermissionListener) {
                    appPermissionListener.onNeverAsk(neverAskPermission);
                }
                return;
            }
            if (null != appPermissionListener) {
                appPermissionListener.onAllGranted();
            }
        }
    }
```
##四、7.0文件安全机制
&nbsp;&nbsp;不少安卓应用开发的程序员，一直都很少有机会接触到安卓四大组件之一的Content Provider，但是由于android7.0文件安全机制的限制，使我们不得不去接触这个组件。我个人觉得这也是一件好事情。不用再只局限于部分组件的开发。
&nbsp;&nbsp;首先我们需要在res文件下简历一个xml文件夹，然后再建立一个xml文件，这个xml文件名字自己可以随便命名，这里我命名为file_path。如下图所示

![xml文件位置](http://img.blog.csdn.net/20171214203529099?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcXFfMjQ4NTkzMDk=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

&nbsp;&nbsp;file_path里面的内容有事有讲究的。里面有一个root-path 节点，虽说android会提示element roo-path is not allowed here。但是我目前还没有找到在不用这个节点的时候，能够正常运行的手段。如果有哪位大神知道，可以指出。
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <files-path
        name="my_images"
        path="path/" />
    <cache-path
        name="my_cache"
        path="path/" />
    <external-files-path
        name="my_file"
        path="path/" />
    <external-path
        name="myApp_file"
        path="path/" />
    <external-cache-path
        name="myApp_cache"
        path="path/" />
    <root-path name="myApp"
        path="" />
</paths>
```

然后，我们需要建立一个自己的provider。这样在问题出现的时候有利于查找问题。最后，我们需要在Manifest里面声名这个Provdier。如下所示
```xml
    <provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="${applicationId}.common.MyImageFileProvider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_path" />
        </provider>
```
这里使用了${applicationId}来代替包名很方便，但是在java代码中还是需要写完全。
接下来说使用。使用起来就会变得很简单，一句代码就可以了
```java
FileProvider.getUriForFile(context, DEFAULT_AUTHORITIES, imageFile)
```
这个方法会返回Uri对象，在需要的地方进行使用。

##五、如何调用相机、图库以及裁剪工具
相机和图库的调用，网上其实已经在很多地方写烂了，我这里直接贴出我的方法
```java
    /**
     * 打开图库
     */
    private void openGallery() {
        initImageFile();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        ((Activity) context).startActivityForResult(intent, REQUEST_PICK);
    }

    /**
     * 开启摄像头
     */
    private void doPhoto() {
        initImageFile();
        Uri uri = FileProvider.getUriForFile(context, DEFAULT_AUTHORITIES, imageFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, 
	        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ((Activity) context).startActivityForResult(intent, REQUEST_PICK);
    }
```
接下来，在打开系统自带裁剪工具的时候会有个坑，也是android 7.0的文件安全机制的问题。不过这个和之前的文件安全机制不一样。使用FileProvider会提示"无法加载该图片"。通过找到该图片路径可以了解到，android7.0的文件安全机制其实是希望能做成类似IOS沙盒机制的效果，每个app只能访问自己的沙盒，但是从系统相册拿到的图片却不属于当前app本身，所以我们这时候就需要一个临时授权。intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
调用系统裁剪功能的代码如下
```java
	private void beginCropDirect(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        //添加这一句表示对目标应用临时授权该Uri所代表的文件
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //可以选择图片类型，如果是*表明所有类型的图片
        intent.setDataAndType(uri, "image/*");
        // 下面这个crop = true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例，这里设置的是正方形（长宽比为1:1）
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 500);
        intent.putExtra("outputY", 500);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        //是否将数据保留在Bitmap中返回,true返回bitmap，false返回uri
        intent.putExtra("return-data", false);
        ((Activity) context).startActivityForResult(intent, REQUEST_CROP);
    }
```


使用方法<br/>
使用库之前在项目的主build.gradle里面加入下面代码

```xml
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }

```

图片裁剪库
```java
compile 'com.github.Kongdy.ImageCropBySystem:croplib:V1.0'
```
权限封装库
```java
compile 'com.github.Kongdy.ImageCropBySystem:permissionlib:V1.0'
```

<h1>使用图片裁剪库注意事项</h1>
将以下代码加入您的AndroidManifest中

```xml
  <provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="${applicationId}.common.MyImageFileProvider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_path" />
        </provider>
```

<h2>
    另外，使用图片裁剪库之后，可以不再引用权限库，
    图片裁剪库已经依赖了权限封装库
</h2>

本文代码:https://github.com/Kongdy/ImageCropBySystem<br/>
个人github地址:https://github.com/Kongdy<br/>
个人掘金主页:https://juejin.im/user/595a64def265da6c2153545b<br/>
csdn主页:http://blog.csdn.net/qq_24859309<br/>
