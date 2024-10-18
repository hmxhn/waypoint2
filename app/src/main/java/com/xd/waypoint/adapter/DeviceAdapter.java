package com.xd.waypoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xd.waypoint.DeviceInformation;
import com.xd.waypoint.R;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<DeviceInformation> {
    private int id;
    public DeviceAdapter(@NonNull Context context, int resourceId, @NonNull List<DeviceInformation> objects) {
        super(context, resourceId, objects);
        id=resourceId;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        DeviceInformation deviceInformation=getItem(position);
        View view= LayoutInflater.from(getContext()).inflate(id,parent,false);
        TextView deviceName=(TextView) view.findViewById(R.id.device_name);
        TextView deviceAddress=(TextView) view.findViewById(R.id.device_address);
        deviceName.setText(deviceInformation.getDeviceName());
        deviceAddress.setText(deviceInformation.getDeviceAddress());
        return view;
    }
}
