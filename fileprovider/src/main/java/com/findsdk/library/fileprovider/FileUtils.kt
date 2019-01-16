package com.findsdk.library.fileprovider

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import java.io.*
import java.util.*

/**
 * Created by bvb on 2019/1/16.
 */
object FileUtils {

    private const val TAG = "FileProvider.FileUtils"

    /**
     * 创建一个临时缓存路径
     * @param context Context
     * @return String
     */
    fun getDiskCacheDir(context: Context): String {
        val cachePath: String
        cachePath = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            val f = context.externalCacheDir
            if (f != null) {
                f.path
            } else {
                context.cacheDir.path
            }
        } else {
            context.cacheDir.path
        }
        return cachePath + File.separator
    }

    /**
     * 获取临时图片文件
     * @param context Context
     * @param photoUri Uri
     * @return File
     */
    fun getTempImageFile(context: Context, photoUri: Uri): File {
        val minType = MimeTypeUtil.getMimeType(context, photoUri)
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (filesDir != null && !filesDir.exists()) filesDir.mkdirs()
        return File(filesDir, UUID.randomUUID().toString() + "." + minType)
    }

    /**
     * 获取临时文件
     * @param context Context
     * @param uri Uri
     * @return File
     */
    fun getTempFile(context: Context, uri: Uri): File {
        val minType = MimeTypeUtil.getMimeType(context, uri)
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (filesDir != null && !filesDir.exists()) filesDir.mkdirs()
        return File(filesDir, UUID.randomUUID().toString() + "." + minType)
    }

    /**
     * 通过URI获取文件
     * @param context Context
     * @param uri Uri
     * @return File?
     */
    fun getFileWithUri(context: Context, uri: Uri): File? {
        var picturePath: String? = null
        val scheme = uri.scheme
        if (ContentResolver.SCHEME_CONTENT == scheme) {
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri,
                    filePathColumn, null, null, null)//从系统表中查询指定Uri对应的照片
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            if (columnIndex >= 0) {
                picturePath = cursor.getString(columnIndex)  //获取照片路径
            } else if (TextUtils.equals(uri.authority, FileProviderUtil.getFileProviderName(context))) {
                picturePath = UriUtil.parseUri(context, uri)
            }
            cursor.close()
        } else if (ContentResolver.SCHEME_FILE == scheme) {
            picturePath = uri.path
        }
        return if (TextUtils.isEmpty(picturePath)) null else File(picturePath)
    }

    /**
     * 通过URI获取文件的路径
     * @param context Context
     * @param uri Uri
     * @return String?
     */
    fun getFilePathWithUri(context: Context, uri: Uri): String? {
        return getFileWithUri(context, uri)?.path
    }


    /**
     * 通过从文件中得到的URI获取文件的路径
     * @param context Context
     * @param uri Uri
     * @return String?
     */
    fun getFilePathWithDocumentsUri(context: Context, uri: Uri): String? {
        return if (ContentResolver.SCHEME_CONTENT == uri.scheme && uri.path!!.contains("document")) {
            val tempFile = getTempFile(context, uri)
            try {
                inputStreamToFile(context.contentResolver.openInputStream(uri), tempFile)
                tempFile.path
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                null
            }
        } else {
            getFilePathWithUri(context, uri)
        }
    }

    /**
     * InputStream 转File
     */
    fun inputStreamToFile(inputStream: InputStream?, file: File?) {
        if (file == null || inputStream == null) {
            Log.i(TAG, "inputStreamToFile:file not be null")
        }
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            val buffer = ByteArray(1024 * 10)
            var i: Int
            while (true) {
                i = inputStream!!.read(buffer)
                if (i == -1) break
                fos.write(buffer, 0, i)
            }

        } catch (e: IOException) {
            Log.e(TAG, "InputStream write error:" + e.toString())
        } finally {
            try {
                if (fos != null) {
                    fos.flush()
                    fos.close()
                }
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
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
    fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                      selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(index)
            }
        } catch (e: IllegalArgumentException) {

        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * 获取app存储根目录
     *
     * @param context
     * @return
     */
    fun getBasePath(context: Context): File {
        val dir = (Environment
                .getExternalStorageDirectory().toString()
                + "/" + getAppName(context) + "/")

        val f = File(dir)
        if (!f.exists()) {
            f.mkdirs()
        }
        return f
    }

    /**
     * 获取系统相册下的app目录
     *
     * @param context
     * @return
     */
    fun getImagePath(context: Context): File {
        val dir = (Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                + "/" + getAppName(context) + "/")

        val f = File(dir)
        if (!f.exists()) {
            f.mkdirs()
        }
        return f
    }

    private fun getAppName(context: Context): String {
        return context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    /**
     * 获取图片文件夹路径
     *
     * @param context
     * @return
     */
    fun getExternalFilesDirForPic(context: Context): String {
        var externalPrivatePath = ""
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val f = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (f != null && f.exists()) {
                externalPrivatePath = f.path
            }
        }
        return externalPrivatePath
    }

    /**
     * 检测文件是否存在
     *
     * @param filePath
     * @return
     */
    fun checkFileExist(filePath: String): Boolean {
        return File(filePath).exists()
    }

}