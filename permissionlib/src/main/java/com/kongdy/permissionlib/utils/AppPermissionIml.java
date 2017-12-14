package com.kongdy.permissionlib.utils;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * @author kongdy
 * @date 2017-09-04 10:06
 * @TIME 10:06
 * <p>
 * activity权限封装
 **/
public class AppPermissionIml {
    /**
     * 请求权限
     */
    protected static final int PERMISSION_REQUEST_CODE = 0x000512; // 手动弹框授权
    protected static final int PERMISSION_REQUEST_SETTING = 0x000513; // 请求write_setting权限
    protected List<String> allowPermission = new ArrayList<>(); // 允许的权限
    protected List<String> deniedPermission = new ArrayList<>(); // 拒绝的权限
    protected List<String> neverAskPermission = new ArrayList<>(); // 不在询问的权限
    protected List<String> allPermission = new ArrayList<>();
    protected Activity mActivity;
    protected static final String PACKAGE_URL_SCHEME = "package:";//权限方案
    private AppPermissionListener appPermissionListener;
    private AlertDialog permissionNerverAskAgainDialog;
    private boolean haveWriteSetting = false;

    public AppPermissionIml(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void needPermission(AppPermissionListener mAppPermissionListener, String... permissionStr) {
        if (null != permissionStr && permissionStr.length > 0)
            needPermission(mAppPermissionListener, Arrays.asList(permissionStr));
    }

    public void needPermission(AppPermissionListener mAppPermissionListener, List<String> permissions) {
        if (null != permissions && permissions.size() > 0) {
            this.appPermissionListener = mAppPermissionListener;
            this.allPermission = permissions;
            allowPermission.clear();
            deniedPermission.clear();
            neverAskPermission.clear();
            Observable.fromIterable(allPermission)
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            if (s.equals(Manifest.permission.WRITE_SETTINGS)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.System.canWrite(mActivity))
                                        allowPermission.add(s);
                                    else
                                        haveWriteSetting = true;
                                }
                            } else if (ActivityCompat.checkSelfPermission(mActivity, s) == PackageManager.PERMISSION_GRANTED) {
                                allowPermission.add(s);
                            } else {
                                deniedPermission.add(s);
                            }
                        }
                    });
            if (haveWriteSetting) {
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
            if (null != mAppPermissionListener) {
                mAppPermissionListener.onAllGranted();
            }
        }
    }

    /**
     * 设置需要的权限str
     */
    public void needPermission(String... permissionStr) {
        needPermission(null, permissionStr);
    }

    public void showNeverAskDialog() {
        if (null == permissionNerverAskAgainDialog) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle("提示");//提示帮助
            builder.setMessage("使用应用全部功能需要您授予必要的权限，点击确定前往授予权限");

            //如果是拒绝授权，则退出
            builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != appPermissionListener) {
                        appPermissionListener.onJiuShiBuGeiQuanXian();
                    }
                }
            });
            //打开设置，让用户选择打开权限
            builder.setPositiveButton("去设置打开", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startAppSettings();//打开设置
                }
            });
            builder.setCancelable(false);
            permissionNerverAskAgainDialog = builder.create();
        }
        permissionNerverAskAgainDialog.show();
    }

    //打开系统应用设置(ACTION_APPLICATION_DETAILS_SETTINGS:系统设置权限)
    public void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + mActivity.getPackageName()));
        mActivity.startActivityForResult(intent, PERMISSION_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_REQUEST_SETTING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(mActivity))
                    allowPermission.add(Manifest.permission.WRITE_SETTINGS);
                else
                    haveWriteSetting = true;
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            needPermission(appPermissionListener, allPermission);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestWriteSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + mActivity.getPackageName()));
        mActivity.startActivityForResult(intent, PERMISSION_REQUEST_SETTING);
    }

    //去请求权限去兼容版本
    public void requestPermissions(String... permission) {
        ActivityCompat.requestPermissions(mActivity, permission, PERMISSION_REQUEST_CODE);
    }

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

    public interface AppPermissionListener {
        /**
         * 所有权限通过
         */
        void onAllGranted();

        /**
         * 当最终由拒绝的权限的时候
         */
        void onHaveDenied(List<String> deniedPermissions);

        /**
         * 当存在不再询问的权限
         */
        void onNeverAsk(List<String> neverAskPermissions);

        /**
         * 用户就是不给权限
         */
        void onJiuShiBuGeiQuanXian();
    }

}
