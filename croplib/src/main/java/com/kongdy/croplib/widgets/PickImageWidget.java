package com.kongdy.croplib.widgets;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.AppCompatTextView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.kongdy.croplib.R;
import com.kongdy.croplib.Utils.Utils;
import com.kongdy.permissionlib.utils.AppPermissionIml;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

/**
 * @author kongdy
 * @date 2017/12/14 13:59
 * @describe 拾取照片组件
 **/
public class PickImageWidget {

    private static final int REQUEST_PICK = 0x01;
    private static final int REQUEST_CROP = 0x02;
    private static final int RESULT_ERROR = 0x03;
    private static final String DEFAULT_AUTHORITIES = "com.kongdy.imagecropbysystem.common.MyImageFileProvider";

    private Context context;
    /**
     * 照相机图片
     */
    private File imageFile;
    private Dialog iconChooseSheetDialog;
    private IconPickListener iconPickListener;
    private OnCancelListener onCancelListener;
    private boolean isCancel;

    private AppPermissionIml appPermissionIml;

    public PickImageWidget(Context context, IconPickListener iconPickListener) {
        this.context = context;
        this.iconPickListener = iconPickListener;
        appPermissionIml = new AppPermissionIml((Activity) context);
    }

    public void show(boolean removeBitmapFlag) {
        show("", removeBitmapFlag);
    }

    public void show(final String cancelFlagStr, final boolean removeBitmapFlag) {
        appPermissionIml.needPermission(new AppPermissionIml.AppPermissionListener() {
            @Override
            public void onAllGranted() {
                showPickDialog(cancelFlagStr,removeBitmapFlag);
            }

            @Override
            public void onHaveDenied(List<String> deniedPermissions) {
            }

            @Override
            public void onNeverAsk(List<String> neverAskPermissions) {
            }

            @Override
            public void onJiuShiBuGeiQuanXian() {
                if (null != iconChooseSheetDialog && iconChooseSheetDialog.isShowing())
                    iconChooseSheetDialog.dismiss();
            }
        }, Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

    }

    private void showPickDialog(String cancelFlagStr, boolean removeBitmapFlag)
    {
        if (null == cancelFlagStr) {
            cancelFlagStr = "";
        }
        isCancel = true;
        iconChooseSheetDialog = new Dialog(context, R.style.AppBase_dialog_bottomSheet);
        View v = LayoutInflater.from(context).inflate(R.layout.view_bottom_sheet, null);
        v.setBackgroundResource(android.R.color.transparent);

        AppCompatTextView tv_bottom_cancel = (AppCompatTextView) v.findViewById(R.id.tv_bottom_cancel);
        AppCompatTextView tv_camera = (AppCompatTextView) v.findViewById(R.id.tv_camera);
        AppCompatTextView tv_from_gallery = (AppCompatTextView) v.findViewById(R.id.tv_from_gallery);
        AppCompatTextView tv_remove = (AppCompatTextView) v.findViewById(R.id.tv_remove_photo);
        tv_remove.setVisibility(removeBitmapFlag ? View.VISIBLE : View.GONE);

        tv_remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCancel = true;
                iconChooseSheetDialog.dismiss();
                iconPickListener.onResult(null, null);
            }
        });
        tv_bottom_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCancel = true;
                iconChooseSheetDialog.dismiss();
            }
        });
        tv_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCancel = false;
                iconChooseSheetDialog.dismiss();
                doPhoto();
            }
        });
        tv_from_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCancel = false;
                iconChooseSheetDialog.dismiss();
                openGallery();
            }
        });
        iconChooseSheetDialog.setContentView(v);

        Window window = iconChooseSheetDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);

        tv_bottom_cancel.setText("取消" + cancelFlagStr);

        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.y = 20;

        window.setAttributes(layoutParams);

        iconChooseSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (null != onCancelListener && isCancel) {
                    onCancelListener.onCancel();
                }
            }
        });

        iconChooseSheetDialog.show();
    }

    public void show(String cancelFlagStr) {
        show(cancelFlagStr, false);
    }

    public void show() {
        show("");
    }

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
        intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ((Activity) context).startActivityForResult(intent, REQUEST_PICK);
    }

    private void initImageFile()
    {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "未找到sd卡", Toast.LENGTH_SHORT).show();
            return;
        }
        String imageName = createImageName("");
        imageFile = new File(Environment.getExternalStorageDirectory(), context.getPackageName() + File.separator + "icon" + imageName + ".jpg");
        if (!imageFile.exists())
            imageFile.getParentFile().mkdirs();
    }

    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }

    public void activityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK) {
            if (data == null)
                beginCrop(imageFile);
            else
                beginCrop(data.getData());
        } else if (requestCode == REQUEST_CROP && resultCode == RESULT_OK) {
            handleCrop(resultCode, data);
        } else if (resultCode != RESULT_OK) {
            if (null != onCancelListener) {
                onCancelListener.onCancel();
            }
        }
    }

    private void beginCrop(Uri source) {
        beginCropDirect(source);
    }

    private String createImageName(String text) {
        String name = text;
        if (text.contains("/")) {
            name = text.subSequence(text.lastIndexOf("/") + 1, text.length() - 1).toString();
        }
        UUID uuid = UUID.randomUUID();
        String result = uuid + name;
        return result.replaceAll("\\W\\s_-", "");
    }

    private void beginCrop(File f) {
        Uri destination = FileProvider.getUriForFile(context, DEFAULT_AUTHORITIES, f);
        beginCropDirect(destination);
    }

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

    private void handleCrop(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Uri imageUri = data.getData();
            String imagePath = Utils.getPhotoPathFromContentUri(context, imageUri);
            File file = new File(imagePath);

            File compressFile = Utils.compressPic(file);
            if (compressFile.exists()) {
                if (iconPickListener != null)
                    iconPickListener.onResult(compressFile, imageUri);
            } else {
                if (iconPickListener != null)
                    iconPickListener.onFail(RESULT_ERROR);
                Toast.makeText(context, "图片获取失败", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_ERROR) {
            if (iconPickListener != null)
                iconPickListener.onFail(RESULT_ERROR);
            Toast.makeText(context, "图片获取失败", Toast.LENGTH_SHORT).show();
        }
    }

    public interface IconPickListener {
        void onResult(File compressImage, Uri uri);

        void onFail(int code);
    }

    public interface OnCancelListener {
        void onCancel();
    }
}
