package com.uncodin.android.audiorecorder;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.ironclad.android.nowtu.R;
import com.ironclad.android.nowtu.widget.AudioPlayerView;

public class AudioRecorderActivity extends Activity implements OnClickListener {
    private int RECORD_VIEW = 0;
    private int PREVIEW_VIEW = 1;
    private int LOADING_VIEW = 2;

    boolean saveFile = false;

    MediaRecorder mRecorder;
    boolean mRecording;
    String mRecordingLocation;

    ViewFlipper mRecordPreviousFlipper;
    ViewFlipper mStateViewSwitcher;

    Button mRecordButton;
    Chronometer mRecordChronometer;

    Button mConfirmButton;
    Button mDiscardButton;

    Button mShowRecordingsButton;

    ListView mRecordingsListView;

    AudioPlayerView mAudioPlayerView;
    private String mGeneratedName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.audio_recorder);

        mRecordPreviousFlipper = (ViewFlipper) findViewById(R.id.record_previous_flipper);
        mStateViewSwitcher = (ViewFlipper) findViewById(R.id.state_viewswitcher);

        mRecordButton = (Button) findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(this);

        mRecordChronometer = (Chronometer) findViewById(R.id.record_chronometer);

        mDiscardButton = (Button) findViewById(R.id.discard_button);
        mDiscardButton.setOnClickListener(this);

        mConfirmButton = (Button) findViewById(R.id.confirm_button);
        mConfirmButton.setOnClickListener(this);

        mShowRecordingsButton = (Button) findViewById(R.id.show_recordings_button);
        mShowRecordingsButton.setOnClickListener(this);

        mRecordingsListView = (ListView) findViewById(R.id.recordingsListView);

        mAudioPlayerView = (AudioPlayerView) findViewById(R.id.audio_player);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecorder.release();
        mRecording = false;
        if (!saveFile) {
            deleteRecording();
        }
    }

    public void onClick(View v) {
        if (v == mRecordButton) {
            if (mRecording) {
                mRecorder.stop();
                mRecordChronometer.stop();
                mRecording = false;
                mRecordButton.setText("Record");
                mStateViewSwitcher.setDisplayedChild(PREVIEW_VIEW);
                mAudioPlayerView.setMediaLocation(mRecordingLocation);
            }
            else {
                mRecorder.start();
                mRecordChronometer.setBase(SystemClock.elapsedRealtime());
                mRecordChronometer.start();
                mRecording = true;
                mRecordButton.setText("Stop");
            }
        }
        else if (v == mConfirmButton) {
            confirmAndSave();
        }
        else if (v == mDiscardButton) {
            resetRecorder();
        }
        else if (v == mShowRecordingsButton) {
            mRecordPreviousFlipper.showNext();
        }
    }

    private void confirmAndSave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set up recording name input
        final EditText recordingNameEditor = new EditText(this);
        recordingNameEditor.setText(mGeneratedName);
        recordingNameEditor.selectAll();
        builder.setView(recordingNameEditor);

        builder.setTitle(R.string.save_as).setNeutralButton(R.string.okay, new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = recordingNameEditor.getText().toString();
                if (name.length() > 0) {
                    // Set the recording's new location, close the dialog, and save
                    String oldLocation = mRecordingLocation;
                    setRecordingLocation(name);

                    if (!new File(oldLocation).renameTo(new File(mRecordingLocation))) {
                        Toast.makeText(AudioRecorderActivity.this, R.string.unable_to_rename_file,
                                Toast.LENGTH_LONG).show();
                    }

                    dialog.dismiss();

                    saveSample();
                    finish();
                }
            }
        }).create().show();
    }

    private void prepareRecorder() {
        mStateViewSwitcher.setDisplayedChild(LOADING_VIEW);
        mRecordChronometer.setBase(SystemClock.elapsedRealtime());
        stopAudioPlayback();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mGeneratedName = UUID.randomUUID().toString();
        setRecordingLocation(mGeneratedName);
        mRecorder.setOutputFile(mRecordingLocation);
        try {
            mRecorder.prepare();
            mStateViewSwitcher.setDisplayedChild(RECORD_VIEW);
        }
        catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setRecordingLocation(String recordingName) {
        mRecordingLocation = getRecordingStorageDirectory() + recordingName + ".mp4";
    }

    private String getRecordingStorageDirectory() {
        String dir = Environment.getExternalStorageDirectory() + "/data/com.ironclad.nowtu/recordings/";
        File folder = new File(dir);
        folder.mkdirs();
        return dir;
    }

    /*
     * Make sure we're not recording music playing in the background, ask the MediaPlaybackService to pause
     * playback.
     */
    private void stopAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which
        // should be public, but isn't.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");

        sendBroadcast(i);
    }

    /*
     * If we have just recorded a smaple, this adds it to the media data base and sets the result to the
     * sample's URI.
     */
    private void saveSample() {
        saveFile = true;
        Log.d("NowTu Audio Recorder", "Saving file at:  file:///" + mRecordingLocation);
        setResult(RESULT_OK, new Intent().setData(Uri.parse("file:///" + mRecordingLocation)));
    }

    private void resetRecorder() {
        mRecorder.release();
        mRecording = false;
        deleteRecording();
        prepareRecorder();
    }

    private void deleteRecording() {
        File recording = new File(mRecordingLocation);
        if (recording.exists()) {
            recording.delete();
        }
    }

    private void loadExistingRecordings() {
        File recordingsDir = new File(getRecordingStorageDirectory());
        File[] recordingFiles = recordingsDir.listFiles();
        AudioRecordingsAdapter filesAdapter = new AudioRecordingsAdapter(this, recordingFiles);
        mRecordingsListView.setAdapter(filesAdapter);
        mRecordingsListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getAdapter().getItem(position);
                deleteRecording();
                mRecordingLocation = file.getAbsolutePath();
                saveSample();
                finish();
            }
        });
    }
}
