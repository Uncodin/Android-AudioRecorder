package in.uncod.android.audiorecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AudioRecordingsAdapter extends BaseAdapter {

    LayoutInflater mLayoutInflater;
    File[] files;
    SimpleDateFormat dateFormatter;

    public AudioRecordingsAdapter(Context context, File[] files) {
        this.files = files;
        mLayoutInflater = LayoutInflater.from(context);
        dateFormatter = new SimpleDateFormat("EEE, MMM d, ''yy h:mm a");
    }

    public int getCount() {
        return files.length;
    }

    public File getItem(int position) {
        if (position >= 0 && position < files.length) {
            return files[position];
        }
        else {
            return null;
        }
    }

    public long getItemId(int position) {
        return position;
    }

    private class ViewHolder {
        TextView dateRecorded;
        TextView size;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(android.R.layout.simple_list_item_2, null);
            holder.dateRecorded = (TextView) convertView.findViewById(android.R.id.text1);
            holder.dateRecorded.setTextColor(Color.WHITE);
            holder.size = (TextView) convertView.findViewById(android.R.id.text2);
            holder.size.setTextColor(Color.WHITE);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        File file = getItem(position);
        holder.dateRecorded.setText(dateFormatter.format(new Date(file.lastModified())));
        holder.size.setText(file.length() + " bytes");

        return convertView;
    }

}
