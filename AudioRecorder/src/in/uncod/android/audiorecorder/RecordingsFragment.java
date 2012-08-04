package in.uncod.android.audiorecorder;

import in.uncod.android.media.widget.AudioPlayerView;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.SherlockFragment;

class RecordingsFragment extends SherlockFragment {
    ListView mRecordingsListView;
    private AudioRecordingsAdapter mAdapter;
    private ViewFlipper mStateSwitcher;
    private AudioPlayerView mAudioPlayer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recordings, null);

        mStateSwitcher = (ViewFlipper) view.findViewById(R.id.state_viewswitcher);

        mAudioPlayer = (AudioPlayerView) mStateSwitcher.findViewById(R.id.audio_player);

        mStateSwitcher.findViewById(R.id.btn_okay).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mStateSwitcher.setDisplayedChild(0);
            }
        });

        mRecordingsListView = (ListView) mStateSwitcher.findViewById(R.id.recordingsListView);

        mRecordingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getAdapter().getItem(position);

                if (AudioRecorderActivity.REQUEST_INTENTS.contains(getActivity().getIntent().getAction())) {
                    // Recorder was started via audio request
                    Uri fileUri = Uri.fromFile(file);
                    Log.d("NowTu Audio Recorder", "Choosing file at: " + fileUri);
                    getActivity().setResult(Activity.RESULT_OK, new Intent().setData(fileUri));
                    getActivity().finish();
                }
                else {
                    // Preview the selected recording
                    mAudioPlayer.setMediaLocation(file.getAbsolutePath());
                    mStateSwitcher.setDisplayedChild(1);
                }
            }
        });

        mRecordingsListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                File file = (File) arg0.getAdapter().getItem(arg2);

                // TODO Show menu/confirm before delete
                file.delete();

                loadExistingRecordings();

                return true;
            }
        });

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            loadExistingRecordings();
        }
    }

    private void loadExistingRecordings() {
        File recordingsDir = ((AudioRecorderActivity) getActivity()).getRecordingStorageDirectory();
        File[] recordingFiles = recordingsDir.listFiles();

        mAdapter = new AudioRecordingsAdapter(getActivity(), recordingFiles);

        mRecordingsListView.setAdapter(mAdapter);
    }
}
