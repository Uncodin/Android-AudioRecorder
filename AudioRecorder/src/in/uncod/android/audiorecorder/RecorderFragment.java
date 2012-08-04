package in.uncod.android.audiorecorder;

import in.uncod.android.media.widget.AudioPlayerView;

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

class RecorderFragment extends SherlockFragment implements View.OnClickListener {
    private final int RECORD_VIEW = 0;
    private final int PREVIEW_VIEW = 1;
    private final int LOADING_VIEW = 2;

    MediaRecorder mRecorder;
    boolean mRecording;
    File mRecordingLocation;

    ViewSwitcher mRecordPreviousFlipper;
    ViewFlipper mStateViewSwitcher;

    Button mRecordButton;
    Chronometer mRecordChronometer;

    Button mConfirmButton;
    Button mDiscardButton;

    boolean saveFile = false;

    AudioPlayerView mAudioPlayerView;
    private String mGeneratedName;

    private boolean mInitialized;

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

        mInitialized = true;

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        mStateViewSwitcher.setDisplayedChild(LOADING_VIEW);
        prepareRecorder();
        mStateViewSwitcher.setDisplayedChild(RECORD_VIEW);
    }

    @Override
    public void onPause() {
        super.onPause();

        resetRecorder(false);
    }

    @Override
    public void onClick(View view) {
        if (view == mRecordButton) {
            if (mRecording) {
                mRecorder.stop();
                mRecordChronometer.stop();
                mRecording = false;
                mRecordButton.setText(R.string.record);
                mAudioPlayerView.setMediaLocation(mRecordingLocation.getAbsolutePath());
                mStateViewSwitcher.setDisplayedChild(PREVIEW_VIEW);
            }
            else {
                mRecorder.start();
                mRecordChronometer.setBase(SystemClock.elapsedRealtime());
                mRecordChronometer.start();
                mRecording = true;
                mRecordButton.setText(R.string.stop);
            }
        }
        else if (view == mConfirmButton) {
            confirmAndSave();
            mStateViewSwitcher.setDisplayedChild(RECORD_VIEW);
        }
        else if (view == mDiscardButton) {
            resetRecorder(true);
            mStateViewSwitcher.setDisplayedChild(RECORD_VIEW);
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
                    File oldLocation = mRecordingLocation;
                    setRecordingLocation(name);

                    if (!oldLocation.renameTo(mRecordingLocation)) {
                        Toast.makeText(getActivity(), R.string.unable_to_rename_file, Toast.LENGTH_LONG)
                                .show();

                        // Couldn't rename; work with the previous name
                        mRecordingLocation = oldLocation;
                    }

                    dialog.dismiss();

                    saveFile = true;

                    Uri fileUri = Uri.fromFile(mRecordingLocation);
                    Log.d("NowTu Audio Recorder", "Saving file at: " + fileUri);

                    if (AudioRecorderActivity.REQUEST_INTENTS.contains(getActivity().getIntent().getAction())) {
                        // Recorder was started via a request for audio; set result and finish
                        getActivity().setResult(Activity.RESULT_OK, new Intent().setData(fileUri));
                        getActivity().finish();
                    }
                    else {
                        // Return to record view
                        resetRecorder(true);
                    }
                }
            }
        }).create().show();
    }

    private void prepareRecorder() {
        saveFile = false;

        mRecordChronometer.setBase(SystemClock.elapsedRealtime());

        // Make sure we're not recording music playing in the background; ask the MediaPlaybackService to pause playback
        stopAudioPlayback();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mGeneratedName = UUID.randomUUID().toString();
        setRecordingLocation(mGeneratedName);
        mRecorder.setOutputFile(mRecordingLocation.getAbsolutePath());

        try {
            mRecorder.prepare();
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        resetRecorder(mInitialized && isVisibleToUser);
    }

    private void setRecordingLocation(String recordingName) {
        mRecordingLocation = new File(((AudioRecorderActivity) getActivity()).getRecordingStorageDirectory(),
                recordingName + ".mp4");
    }

    private void stopAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");

        getActivity().sendBroadcast(i);
    }

    private void resetRecorder(boolean prepare) {
        if (mRecorder != null) {
            mRecorder.release();
        }

        mRecording = false;

        if (!saveFile) {
            deleteRecording();
        }

        if (prepare) {
            prepareRecorder();
        }
    }

    private void deleteRecording() {
        if (mRecordingLocation != null && mRecordingLocation.exists()) {
            mRecordingLocation.delete();
        }
    }
}
