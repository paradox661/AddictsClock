package com.paradox.addictsclock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

public class StatusListAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;

    List<RoomItem> roomItems;
    MainActivity   mainActivity;

    public StatusListAdapter(Context context, List<RoomItem> roomItems, MainActivity mainActivity) {
        mLayoutInflater = LayoutInflater.from(context);

        this.roomItems    = roomItems;
        this.mainActivity = mainActivity;
    }

    @Override
    public int getCount() {
        return roomItems.size();
    }

    @Override
    public Object getItem(int position) {
        return roomItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        public TextView tv_uname, tv_title, tv_roomId;
        public ToggleButton sub_toggle;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.layout_list_item, null);
            holder = new ViewHolder();

            holder.tv_uname = convertView.findViewById(R.id.tv_uname);
            holder.tv_title = convertView.findViewById(R.id.tv_title);
            holder.tv_roomId = convertView.findViewById(R.id.tv_roomId);
            holder.sub_toggle = convertView.findViewById(R.id.sub_toggle);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv_uname.setText(roomItems.get(position).uname);
        holder.tv_title.setText(roomItems.get(position).title);
        holder.tv_roomId.setText(String.valueOf(roomItems.get(position).roomId));

        holder.sub_toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                roomItems.get(position).isCheck = isChecked;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(mainActivity.looping == mainActivity.looping_now) {
                            mainActivity.detectionLoop();
                        }
                    }
                }).start();

                mainActivity.editText.clearFocus();
            }
        });

        holder.sub_toggle.setChecked(roomItems.get(position).isCheck);

        return convertView;
    }
}
