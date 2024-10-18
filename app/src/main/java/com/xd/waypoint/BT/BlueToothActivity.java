package com.xd.waypoint.BT;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.xd.waypoint.activity.BaseActivity;
import com.xd.waypoint.DeviceInformation;
import com.xd.waypoint.R;
import com.xd.waypoint.activity.WayPointActivity;
import com.xd.waypoint.adapter.DeviceAdapter;
import com.xd.waypoint.adapter.MyArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressLint("MissingPermission")
public class BlueToothActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG=BlueToothActivity.class.getName();
    private Button btnOpenBt, btnScanDevice;
    private ListView deviceList;
    private ListView bondedList;
    private List<DeviceInformation> devices = new ArrayList<>();
    private List<DeviceInformation> bondedDevices=new ArrayList<>();//已配对设备
    private static int REQUEST_ENABLE = 1;
    private BroadcastReceiver bluetoothStateReceiver;//蓝牙状态广播
    private BroadcastReceiver bluetoothReceiver;//蓝牙扫描广播
    private BluetoothAdapter bluetoothAdapter;
    private MyArrayAdapter adapter;
    private DeviceAdapter adapter1;
    private Set<BluetoothDevice> pairedDevices;
    private static final ArrayList<String> REQUIRED_PERMISSION_LIST = new ArrayList<>();
    private List<String> missingPermission = new ArrayList<>(
    );
    private boolean isAdded;

    public static void start(Context context) {
        Intent intent = new Intent(context, BlueToothActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams params=getWindow().getAttributes();
        params.width=(int)(getResources().getDisplayMetrics().widthPixels*0.8);
        params.height=(int)(getResources().getDisplayMetrics().heightPixels*0.6);
        getWindow().setAttributes(params);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bluetooth);
        initView();
        checkAndRequestPermissions();
        initReceiver();
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(receiver,filter);
        initListener();
    }
    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            pairedDevices=bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size()>0){
                for (BluetoothDevice device : pairedDevices) {
                    if(!bondedDevices.contains(device)) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        Log.d("bonded device", deviceName + deviceHardwareAddress);
                        bondedDevices.add(new DeviceInformation(deviceName, deviceHardwareAddress));
                    }
                }
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                isAdded=false;
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //扫描到的设备
                String deviceName=device.getName();
                String deviceAddress=device.getAddress();
                //新建一个deviceinformation对象
                DeviceInformation deviceInformation = new DeviceInformation(deviceName, deviceAddress);
                if (deviceName!=null&&deviceAddress!=null){
                for (BluetoothDevice bluetoothDevice : pairedDevices) {
                    //判断已保存的设备信息是否一样
                    if (bluetoothDevice.getAddress().equals(deviceAddress)) {
                        isAdded = true;
                        break;
                    }

                }
                    if(!isAdded){
                        devices.add(deviceInformation);
                        adapter.notifyDataSetChanged();
                    }

                }
               // Log.d(TAG, "onReceive: "+devices.size()+devices);
            }
        }
    };

    private void initView() {

        context = this;
        btnOpenBt = findViewById(R.id.btn_openbt);
        btnScanDevice = findViewById(R.id.btn_scanDevice);
        deviceList = findViewById(R.id.list_device);
        bondedList= findViewById(R.id.list_bonded_device);

        btnOpenBt.setOnClickListener(this);
        btnScanDevice.setOnClickListener(this);
        adapter=new MyArrayAdapter(devices,this);
        //传入上下文、子项布局的id、要适配的数据
        //adapter1 = new DeviceAdapter(this, R.layout.item_device, devices);
        deviceList.setAdapter(adapter);
      //  bondedList.setAdapter(adapter);

    }

    private void checkAndRequestPermissions() {

        /*for (String permission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(permission);
            }
        }*/
        // ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_BT);
        if (Build.VERSION.SDK_INT >= 6.0) {
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.ACCESS_FINE_LOCATION);
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);

            REQUIRED_PERMISSION_LIST.add(Manifest.permission.BLUETOOTH);
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.BLUETOOTH_ADMIN);
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.BLUETOOTH_SCAN);
            REQUIRED_PERMISSION_LIST.add(Manifest.permission.BLUETOOTH_CONNECT);
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION_LIST.toArray(new String[missingPermission.size()]), REQUEST_ENABLE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        if (!missingPermission.isEmpty()) {
            showToast("缺少权限！！！");
        }
    }


    private void initReceiver() {
        //接收蓝牙状态改变

        bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        showToast("蓝牙已打开");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        showToast("蓝牙已关闭");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        showToast("蓝牙正在打开");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        showToast("蓝牙正在关闭");
                        break;
                }
            }
        };
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter1);//注册！！！

        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("action", action);//调试
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    boolean isAdded = false;
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d("bluetooth", String.valueOf(device.getName()));
                    DeviceInformation deviceInformation = new DeviceInformation(device.getName(), device.getAddress());
                  for (DeviceInformation deviceInformation1 : devices) {
                        //判断已保存的设备信息是否一样
                        if (deviceInformation1.getDeviceAddress().equals(deviceInformation.getDeviceAddress())) {
                            isAdded = true;
                            break;
                        }
                    }
                    if (!isAdded) {
                        devices.add(deviceInformation);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        };
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter2);
    }

    private void openBlueTooth() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            showToast("您的设备不支持蓝牙");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                startActivityForResult(enableBtIntent, REQUEST_ENABLE);
            }
        }

    }
    //扫描逻辑
    private void discoverDevice() {
        if(bluetoothAdapter.isEnabled()){
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            return;
        }
        bluetoothAdapter.startDiscovery();
            Log.d(TAG, "discoverDevice: "+"开始扫描"+bluetoothAdapter.getState());
            showToast("正在搜索设备");
        }
        else{
            Log.d("tag","蓝牙未打开");
            showToast("蓝牙未打开");
        }
    }

    //连接设备
    private void initListener(){
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //停止扫描
                if (bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                }
                DeviceInformation deviceInformation = devices.get(position);
                Intent intent2=new Intent();
                //传递名称和地址
                SharedPreferences.Editor editor =getSharedPreferences("devices",MODE_PRIVATE).edit();
                editor.putString("NAME",deviceInformation.getDeviceName());
                editor.putString("ADDRESS",deviceInformation.getDeviceAddress());
                editor.apply();
//                intent2.putExtra("NAME",deviceInformation.getDeviceName());
//                intent2.putExtra("ADDRESS",deviceInformation.getDeviceAddress());
//                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                setResult(RESULT_OK,intent2);
             //   startActivityForResult(intent2,1);
                finish();
            }
        });
        bondedList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                }
                DeviceInformation deviceInformation = devices.get(position);
                Intent intent2=new Intent(context, CommunicationActivity.class);
                //传递名称和地址
                intent2.putExtra("NAME",deviceInformation.getDeviceName());
                intent2.putExtra("ADDRESS",deviceInformation.getDeviceAddress());
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent2);
                finish();
            }
        });
    }
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_openbt) {
            openBlueTooth();
        } else if (id == R.id.btn_scanDevice) {
            discoverDevice();
        }

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(bluetoothStateReceiver);
    }
}