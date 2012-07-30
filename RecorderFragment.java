package com.uncodin.android.audiorecorder;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;
import com.actionbarsherlock.app.SherlockFragment;
import com.ironclad.android.nowtu.R;
import com.ironclad.android.nowtu.widget.AudioPlayerView;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RecorderFragment extends SherlockFragment implements View.OnClickListener {

    private final int RECORD_VIEW = 0;
    private final int PREVIEW_VIEW = 1;
    private final int LOADING_VIEW = 2;

    MediaRecorder mRecorder;
    boolean mRecording;
    String mRecordingLocation;

    ViewSwitcher mRecordPreviousFlipper;
    ViewFlipper mStateViewSwitcher;

    Button mRecordButton;
    Chronometer mRecordChronometer;

    Button mConfirmButton;
    Button mDiscardButton;

    boolean saveFile = false;

    AudioPlayerView mAudioPlayerView;
    private String mGeneratedName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recorder, null);

        mStateViewSwitcher = (ViewFlipper) view.findViewById(R.id.state_viewswitcher);

        mRecordButton = (Button) view.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(this);

        mRecordChronometer = (Chronometer) view.findViewById(R.id.record_chronometer);

        mDiscardButton = (Button) view.findViewById(R.id.discard_button);
        mDiscardButton.setOnClickListener(this);

        mConfirmButton = (Button) view.findViewById(R.id.confirm_button);
        mConfirmButton.setOnClickListener(this);

        mAudioPlayerView = (AudioPlayerView) view.findViewById(R.id.audio_player);

        return view;

    }

    @Override
    public void onStart() {
        super.onStart();

        prepareRecorder();
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

    @Override
    public void onClick(View view) {

        if (view == mRecordButton) {
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
        else if (view == mConfirmButton) {
            confirmAndSave();
        }
        else if (view == mDiscardButton) {
            resetRecorder();
        }
    }

    private void confirmAndSave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set up recording name input
        final EditText recordingNameEditor = new EditText(getActivity());
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
                        Toast.makeText(getActivity(), R.string.unable_to_rename_file,
                                Toast.LENGTH_LONG).show();
                    }

                    dialog.dismiss();

                    RecorderUtil.saveSample(getActivity(), mRecordingLocation);
                    getActivity().finish();
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
        mRecordingLocation = RecorderUtil.getRecordingStorageDirectory() + recordingName + ".mp4";
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

        getActivity().sendBroadcast(i);
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
}
