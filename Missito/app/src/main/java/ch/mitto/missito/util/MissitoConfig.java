package ch.mitto.missito.util;

import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MissitoConfig {

    private static final String ROOT_DIR = "Missito";
    private static ArrayList<String> wrongExternalStorageStates = new ArrayList<>(Arrays.asList(Environment.MEDIA_REMOVED, Environment.MEDIA_UNMOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY));

    public static String getAttachmentsPath(String companion) {
        if (Build.VERSION.SDK_INT >= 19) {
            wrongExternalStorageStates.add(Environment.MEDIA_UNKNOWN);
        }

        String dirPath = "";
        if (!wrongExternalStorageStates.contains(Environment.getExternalStorageState())) {
            dirPath = Environment.getExternalStorageDirectory() + File.separator + ROOT_DIR + File.separator + companion + File.separator;
        } else {
            dirPath = Environment.getDataDirectory() + File.separator + ROOT_DIR + File.separator + companion + File.separator;
        }
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return dirPath;
    }

    public static void clearAttachments(String companion) {
        String deleteCmd = "rm -r " + getAttachmentsPath(Helper.addPlus(companion));
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
