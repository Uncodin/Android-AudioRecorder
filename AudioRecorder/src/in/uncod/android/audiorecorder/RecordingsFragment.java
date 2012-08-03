package in.uncod.android.audiorecorder;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

class RecordingsFragment extends SherlockFragment {
    ListView mRecordingsListView;

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
        File recordingsDir = ((AudioRecorderActivity) getActivity()).getRecordingStorageDirectory();
        File[] recordingFiles = recordingsDir.listFiles();
        AudioRecordingsAdapter filesAdapter = new AudioRecordingsAdapter(getActivity(), recordingFiles);
        mRecordingsListView.setAdapter(filesAdapter);
        mRecordingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getAdapter().getItem(position);

                Uri fileUri = Uri.fromFile(file);
                Log.d("NowTu Audio Recorder", "Choosing file at: " + fileUri);
                getActivity().setResult(Activity.RESULT_OK, new Intent().setData(fileUri));
                getActivity().finish();
            }
        });
    }
}
