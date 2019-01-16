package com.findsdk.demo.fileprovider;

import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

/**
 * Created by bvb on 16/8/29.
 */
public class PermissionUtil {

    public static boolean requestPermissions(Activity activity, String[] permissions,int requestCode){
        byte[] per = checkPermission(activity,permissions);
        int i = 0;
        for(byte b : per){
            if(b>0)i++;
        }
        if(i == 0)return true;
        String[] s = new String[i];
        i = 0;
        int index = 0;
        for(String permission : permissions){
            if(per[i] > 0){
                s[index] = permission;
                index++;
            }
            i++;
        }
        ActivityCompat.requestPermissions(activity, s, requestCode);
        return false;
    }


    private static byte[] checkPermission(Activity activity, String[] permissions) {
        byte[] per = new byte[permissions.length];
        int i = 0;
        for(String permission : permissions){
            byte b = 0;
            if(ActivityCompat.checkSelfPermission(activity,permission) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        permission)) {
                    b = 2;
                }else{
                    b = 1;
                }
            }
            per[i] = b;
            i++;
        }
        return per;
    }

}
