package in.uncod.android.audiorecorder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class RecorderUtil {

    public static String getRecordingStorageDirectory() {
        String dir = Environment.getExternalStorageDirectory() + "/data/com.ironclad.nowtu/recordings/";
        File folder = new File(dir);
        folder.mkdirs();
        return dir;
    }

    /*
    * If we have just recorded a smaple, this adds it to the media data base and sets the result to the
    * sample's URI.
    */
    public static void saveSample(Activity activity, String mRecordingLocation) {
        Log.d("NowTu Audio Recorder", "Saving file at:  file:///" + mRecordingLocation);
        activity.setResult(Activity.RESULT_OK, new Intent().setData(Uri.parse("file:///" + mRecordingLocation)));
    }
}
