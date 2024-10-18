package com.xd.waypoint.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xd.waypoint.DeviceInformation;
import com.xd.waypoint.R;

import java.util.List;
//自定义适配器
public class MyArrayAdapter extends BaseAdapter {
    private final List<DeviceInformation> devices;
    private final Context context;


    public MyArrayAdapter(List<DeviceInformation> devices, Context context) {
        this.devices = devices;
        this.context = context;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int i) {
        return devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
       if (view==null){
           view= LayoutInflater.from(context).inflate(R.layout.item_device,null);
       }
        TextView name=view.findViewById(R.id.device_name);
        TextView address=view.findViewById(R.id.device_address);
        DeviceInformation devInformation=devices.get(i);
        name.setText(devInformation.getDeviceName());
        address.setText(devInformation.getDeviceAddress());
        return view;
    }
}
