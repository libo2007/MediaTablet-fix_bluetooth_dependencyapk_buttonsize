package com.jiaying.mediatablet.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * 作者：lenovo on 2016/6/15 09:36
 * 邮箱：353510746@qq.com
 * 功能：OpenCV安装器
 */
public class OpenCVInstaller {

    private static String OPENCV_PACKAGENAME = "org.opencv.engine";
    private static String OPENCV_PATH = Environment
            .getExternalStorageDirectory() + File.separator + "jiaying"+File.separator;
    private static  String OPENCV_APK_NAME = "opencv.apk";

    private Context context;
    private OpenCVInstaller(){

    }
    public static  void checkOpenCVInstalledState(Context context) {
        if(AppInfoUtils.isAppInstalled(context,OPENCV_PACKAGENAME)){
            return;
        }
        copyOpenCVFromAssert(context);
        installOpenCV(context);
    }

    private static void copyOpenCVFromAssert(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File file = new File(OPENCV_PATH);
            if (!file.exists()) {
                file.mkdir();
            }
            if (!AppInfoUtils.fileIsExists(OPENCV_PATH + OPENCV_APK_NAME)) {
                AppInfoUtils.copyApkFromAssets(context, OPENCV_APK_NAME,
                        OPENCV_PATH + OPENCV_APK_NAME);
            }

        }

    }
    private static void installOpenCV(Context context){
        if(!AppInfoUtils.isAppInstalled(context,OPENCV_PACKAGENAME)){
            AppInfoUtils.installApp(context, OPENCV_PATH + OPENCV_APK_NAME);
        }
    }
}
