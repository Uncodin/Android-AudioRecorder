package com.uncodin.android.audiorecorder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragment;
import com.ironclad.android.nowtu.R;

import java.io.File;

public class RecordingsFragment extends SherlockFragment {

    ListView mRecordingsListView;
    String mRecordingLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recordings, null);

        mRecordingsListView = (ListView) view.findViewById(R.id.recordingsListView);

        return view;

    }

    @Override
    public void onStart() {
        super.onStart();

        loadExistingRecordings();
    }

    private void loadExistingRecordings() {
        File recordingsDir = new File(RecorderUtil.getRecordingStorageDirectory());
        File[] recordingFiles = recordingsDir.listFiles();
        AudioRecordingsAdapter filesAdapter = new AudioRecordingsAdapter(getActivity(), recordingFiles);
        mRecordingsListView.setAdapter(filesAdapter);
        mRecordingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getAdapter().getItem(position);
                mRecordingLocation = file.getAbsolutePath();
                RecorderUtil.saveSample(getActivity(), mRecordingLocation);
                getActivity().finish();
            }
        });
    }

}
