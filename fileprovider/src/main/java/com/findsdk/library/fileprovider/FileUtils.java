package com.findsdk.library.fileprovider;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 通过FilrProvider方式 操作文件存储
 * Created by bvb on 2017/7/11.
 */

public class FileUtils {
    public static final String TAG = "FileUtils";

    public final static String getFileProviderName(Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    /**
     * 创建一个临时缓存路径
     *
     * @param context
     * @return
     */
    public static String getDiskCacheDir(Context context) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            File f = context.getExternalCacheDir();
            if (f != null) {
                cachePath = f.getPath();
            } else {
                cachePath = context.getCacheDir().getPath();
            }
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath + File.separator;
    }

    /**
     * 创建一个用于拍照图片输出路径的Uri (FileProvider)
     *
     * @param context
     * @return
     */
    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, getFileProviderName(context), file);
    }

    /**
     * 获取URI path
     *
     * @param context
     * @param uri
     * @return
     */
    public static String parseUri(Context context, Uri uri) {
        if (uri == null) return null;
        String path;
        if (TextUtils.equals(uri.getAuthority(), getFileProviderName(context))) {
            path = new File(uri.getPath().replace("file_path/", "")).getAbsolutePath();
        } else {
            path = uri.getPath();
        }
        return path;
    }

    /**
     * 通过URI获取文件
     *
     * @param context
     * @param uri
     * @return
     */
    public static File getFileWithUri(Context context, Uri uri) {
        String picturePath = null;
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri,
                    filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            if (columnIndex >= 0) {
                picturePath = cursor.getString(columnIndex);  //获取照片路径
            } else if (TextUtils.equals(uri.getAuthority(), getFileProviderName(context))) {
                picturePath = parseUri(context, uri);
            }
            cursor.close();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            picturePath = uri.getPath();
        }
        return TextUtils.isEmpty(picturePath) ? null : new File(picturePath);
    }

    /**
     * 通过URI获取文件的路径
     *
     * @param uri
     * @param context
     */
    public static String getFilePathWithUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        File picture = getFileWithUri(context, uri);
        String picturePath = picture == null ? null : picture.getPath();
        return picturePath;
    }

    /**
     * 通过从文件中得到的URI获取文件的路径
     *
     * @param context
     * @param uri
     * @return
     */
    public static String getFilePathWithDocumentsUri(Context context, Uri uri) {
        if (uri == null) {
            Log.e(TAG, "uri is null,activity may have been recovered?");
            return null;
        }
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && uri.getPath().contains("document")) {
            File tempFile = getTempFile(context, uri);
            try {
                inputStreamToFile(context.getContentResolver().openInputStream(uri), tempFile);
                return tempFile.getPath();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return getFilePathWithUri(context, uri);
        }
    }

    /**
     * InputStream 转File
     */
    public static void inputStreamToFile(InputStream is, File file) {
        if (file == null) {
            Log.i(TAG, "inputStreamToFile:file not be null");
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024 * 10];
            int i;
            while ((i = is.read(buffer)) != -1) {
                fos.write(buffer, 0, i);
            }
        } catch (IOException e) {
            Log.e(TAG, "InputStream 写入文件出错:" + e.toString());
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取临时文件
     *
     * @param context
     * @param photoUri
     * @return
     */
    public static File getTempFile(Context context, Uri photoUri) {
        String minType = getMimeType(context, photoUri);
//        if (!checkMimeType(context,minType))throw new TException(TExceptionType.TYPE_NOT_IMAGE);
        File filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (filesDir != null && !filesDir.exists()) filesDir.mkdirs();
        File photoFile = new File(filesDir, UUID.randomUUID().toString() + "." + minType);
        return photoFile;
    }

    /**
     * To find out the extension of required object in given uri
     * Solution by http://stackoverflow.com/a/36514823/1171484
     */
    public static String getMimeType(Context context, Uri uri) {
        String extension;
        //Check uri format to avoid null
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            //If scheme is a content
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(uri));
            if (TextUtils.isEmpty(extension))
                extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
            if (TextUtils.isEmpty(extension))
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(uri));
        }
        if (TextUtils.isEmpty(extension)) {
            extension = getMimeTypeByFileName(getFileWithUri(context, uri).getName());
        }
        return extension;
    }

    /**
     * 通过文件名获取扩展名
     *
     * @param fileName
     * @return
     */
    public static String getMimeTypeByFileName(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."), fileName.length());
    }

    /**
     * 将scheme为file的uri转成FileProvider 提供的content uri
     *
     * @param context
     * @param uri
     * @return
     */
    public static Uri convertFileUriToFileProviderUri(Context context, Uri uri) {
        if (uri == null) return null;
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return getUriForFile(context, new File(uri.getPath()));
        }
        return uri;

    }

    /**
     * 获取一个临时的Uri, 文件名随机生成
     *
     * @param context
     * @return
     */
    public static Uri getTempUri(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = new File(Environment.getExternalStorageDirectory(), "/images/" + timeStamp + ".jpg");
        if (file != null && file.getParentFile() != null && !file.getParentFile().exists())
            file.getParentFile().mkdirs();
        return getUriForFile(context, file);
    }

    /**
     * 将Uri转换为绝对路径，适用于4.4以下版本
     *
     * @param context, uri
     * @return String
     */
    public static String convertUriToPath(Context context, Uri uri) {
        String currentImagePath = "";
        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                currentImagePath = cursor.getString(index);
            }
        } catch (IllegalArgumentException e) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return currentImagePath;
    }

    /**
     * 将Uri转换为绝对路径，适用于4.4及以上版本
     *
     * @param context, uri
     * @return String
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String convertUriToPathForKitKat(Context context, Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        String[] projection = {MediaStore.Images.Media.DATA};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(index);
            }
        } catch (IllegalArgumentException e) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * 获取app存储根目录
     *
     * @param context
     * @return
     */
    public static File getBasePath(Context context) {
        final String dir = Environment
                .getExternalStorageDirectory()
                + "/" + getAppName(context) + "/";

        File f = new File(dir);
        if (f != null && !f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    /**
     * 获取系统相册下的app目录
     *
     * @param context
     * @return
     */
    public static File getImagePath(Context context) {
        final String dir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "/" + getAppName(context) + "/";

        File f = new File(dir);
        if (f != null && !f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    private static String getAppName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    /**
     * 获取图片文件夹路径
     *
     * @param context
     * @return
     */
    public static String getExternalFilesDirForPic(Context context) {
        String externalPrivatePath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File f = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (f != null && f.exists()) {
                externalPrivatePath = f.getPath();
            }
        }
        return externalPrivatePath;
    }

    /**
     * 检测文件是否存在
     *
     * @param filePath
     * @return
     */
    public static boolean checkFileExist(String filePath) {
        return new File(filePath).exists();
    }
}
