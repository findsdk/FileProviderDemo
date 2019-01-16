package com.findsdk.library.fileprovider

import android.content.Context

/**
 * Created by bvb on 2019/1/16.
 */
internal object FileProviderUtil {

    fun getFileProviderName(context: Context): String {
        return context.packageName + ".fileprovider"
    }
}