package com.kongdy.croplib.Utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kongdy
 * @date 2017/12/14 14:17
 * @describe TODO
 **/
public class Utils {

    private static String ChineseRegEx = "[\u4e00-\u9fa5]";

    public static String getPhotoPathFromContentUri(Context context, Uri uri) {
        String photoPath = "";
        if (context == null || uri == null) {
            return photoPath;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if (isExternalStorageDocument(uri)) {
                String[] split = docId.split(":");
                if (split.length >= 2) {
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        photoPath = Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
            } else if (isDownloadsDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                photoPath = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String[] split = docId.split(":");
                if (split.length >= 2) {
                    String type = split[0];
                    Uri contentUris = null;
                    if ("image".equals(type)) {
                        contentUris = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUris = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUris = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    String selection = MediaStore.Images.Media._ID + "=?";
                    String[] selectionArgs = new String[]{split[1]};
                    photoPath = getDataColumn(context, contentUris, selection, selectionArgs);
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            photoPath = uri.getPath();
        } else {
            photoPath = getDataColumn(context, uri, null, null);
        }

        return photoPath;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return null;
    }

    private static String getPackName(Context context) {
        return context.getPackageName();
    }

    /**
     * 将图片压缩到服务器可接收范围
     *
     * @param file
     * @return
     */
    public static File compressPic(File file) {
        return compressPicWithNewPath(file, file.getPath());
    }

    public static File compressPicWithNewPath(File file, String newPath) {
        File newFile = file;
        if (file.getPath().toLowerCase().endsWith(".jpg")
                || file.getPath().toLowerCase().endsWith(".gif")
                || file.getPath().toLowerCase().endsWith(".png")
                || file.getPath().toLowerCase().endsWith(".bmp")
                || file.getPath().toLowerCase().endsWith(".jpeg")) {
            long length = file.length();
            /**
             * 服务器显示图片大小为512k
             */
            if (length > 512 * 1024) {
                BitmapFactory.Options newOpts = new BitmapFactory.Options();
                newOpts.inJustDecodeBounds = true;
                Bitmap bitmap;
                BitmapFactory.decodeFile(file.getPath(), newOpts);
                newOpts.inJustDecodeBounds = false;
                float w = newOpts.outWidth;
                float h = newOpts.outHeight;
                //计算出取样率
                newOpts.inSampleSize = (int) Math.ceil(2.5F * Math.max(w / Math.sqrt(840 * 1024), h / Math.sqrt(840 * 1024)));
                bitmap = BitmapFactory.decodeFile(file.getPath(), newOpts);
                newFile = saveImage(bitmap, newPath);
            }
        }
        if (isContainChinese(newFile.getPath())) {
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), newOpts);
            newOpts.inJustDecodeBounds = false;
            //计算出取样率
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), newOpts);
            String newNoChinesePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + "myFolder" + File.separator + "icon"
                    + createImageName(file.getPath()) + ".jpg";
            newFile = saveImage(bitmap, newNoChinesePath);
        }
        return newFile;
    }

    public static File saveImage(Bitmap bitmap, String newPath) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        FileOutputStream fos;

        File newFile = new File(newPath);
        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }
        try {
            fos = new FileOutputStream(newFile);
            fos.write(baos.toByteArray());
            fos.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newFile;
    }

    public static String createImageName(String text) {
        String name = text;
        if (text.contains("/")) {
            name = text.subSequence(text.lastIndexOf("/") + 1, text.length() - 1).toString();
        }
        UUID uuid = UUID.randomUUID();
        String result = uuid + name;
        return result.replaceAll("\\W\\s_-", "");
    }

    /**
     * 判断是否包含中文
     */
    public static boolean isContainChinese(String s) {
        Pattern p = Pattern.compile(ChineseRegEx);
        Matcher matcher = p.matcher(s);
        return matcher.find();
    }
}
