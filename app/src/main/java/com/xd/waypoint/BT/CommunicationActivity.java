package com.xd.waypoint.BT;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amap.api.maps2d.model.LatLng;
import com.xd.waypoint.activity.BaseActivity;
import com.xd.waypoint.R;
import com.xd.waypoint.activity.WayPointActivity;
import com.xd.waypoint.utils.DataTransfer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dji.common.mission.waypoint.Waypoint;

public class CommunicationActivity extends BaseActivity {
    private String mAddress;
    private String mName;
    private Button btnSend;
    private Button btnReceive;
    private EditText etSend;
    private TextView tvReceive;
    private TextView tvDevice;
    private String TAG = "CommunicationActivity";
    public static final int RECEIVE_SUCCESS = 2;

    private BluetoothDevice mDevice;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothAdapter mBlueToothAdapter;
    private final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//蓝牙串口服务
    private String mSendContentStr;
    private static OutputStream mOS;
    private ArrayList<Waypoint> pointList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btconnect);
        context = this;
        Intent intent = getIntent();
        //得到传输过来的设备地址
        mAddress = intent.getStringExtra("ADDRESS");
        mName = intent.getStringExtra("NAME");
        initView();
        initListener();
        connectDevice();
        DataTransfer.getInstance().setWaypointList(null);
    }

    private void initView() {
       // btnSend = findViewById(R.id.btn_send);
        btnReceive = findViewById(R.id.btn_receive);
        tvReceive = findViewById(R.id.tv_receive);
        tvReceive.setMovementMethod(new ScrollingMovementMethod());
     //   etSend = findViewById(R.id.edit_text_send);
        tvDevice = findViewById(R.id.tv_device);
        tvDevice.setText(mName);
        DataTransfer.getInstance().setPoints(null);
        DataTransfer.getInstance().setWaypointList(null);
    }

    private void initListener() {
//        btnSend.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mSendContentStr = etSend.getText().toString();
//                //发送信息
//                sendMessage(mSendContentStr);
//            }
//        });
        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = RECEIVE_SUCCESS;
                        handler.sendMessage(msg);
                    }
                }).start();
            }
        });

    }

    private void sendMessage(String contentStr) {
        if (mBluetoothSocket.isConnected()) {
            try {
                //获取输出流
                mOS = mBluetoothSocket.getOutputStream();
                if (mOS != null) {
                    //写数据（参数为byte数组）
                    mOS.write(contentStr.getBytes("GBK"));

                    //   etSend.append(contentStr);
                    showToast("发送成功");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showToast("没有设备已连接");
        }
    }

    @SuppressLint("MissingPermission")
    private void connectDevice() {

        //获取默认蓝牙设配器
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        //通过地址拿到该蓝牙设备device
        mDevice = mBlueToothAdapter.getRemoteDevice(mAddress);
        try {
            //建立socket通信
            mBluetoothSocket = mDevice.createRfcommSocketToServiceRecord(mUUID);
            mBluetoothSocket.connect();
            if (mBluetoothSocket.isConnected()) {
                showToast("连接成功");
                //开启接收数据的线程
                ReceiveDataThread thread = new ReceiveDataThread();
                thread.start();

            }
        } catch (IOException e) {
            e.printStackTrace();
            showToast("连接出错！ ");
            finish();
            try {
                mBluetoothSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mBluetoothSocket.isConnected()) {
                //关闭socket
                mBluetoothSocket.close();
                mBlueToothAdapter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 负责接收数据的线程
     */
    public class ReceiveDataThread extends Thread {

        private InputStream inputStream;
        FileOutputStream out = null;
        InputStreamReader inputReader = null;
        BufferedReader reader = null;

        BufferedWriter writer = null;

        public ReceiveDataThread() {
            super();
            try {
                //获取连接socket的输入流
                inputStream = mBluetoothSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            String line = "";
            int count = 0;//行数
            if (inputStream != null) {
                inputReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputReader);
                try {
                    while ((line = reader.readLine()) != null) {

                        String regex = ",|，";
                        String[] s = line.split(regex);
                        String a = line;
                        pointList.add(new Waypoint(Double.parseDouble(s[0].trim()), Double.parseDouble(s[1].trim()), Float.parseFloat(s[2].trim())));
//                    byte[] gbks = "你好".getBytes("GBK");
//                    for (byte gbk : gbks) {
//                        Log.d(TAG,"gbk:" + gbk);
//                    }
//                    String[] chars = a.split(" ");
//                    String str = "";
//                    for(int i = 0; i<chars.length;i++){
//                        str += (char)Integer.parseInt(chars[i]);
//                    }
                        //Log.d(TAG,"str:" + str);
                        count++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //将收到的数据显示在TextView上

                                tvReceive.append(a + "\r\n");
                                int count = tvReceive.getLineCount();
                                if (count > 28) {
                                    int offset = tvReceive.getLineCount() * tvReceive.getLineHeight();
                                    tvReceive.scrollTo(0, offset - tvReceive.getHeight() + tvReceive.getLineHeight());
                                }
                            }
                        });
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "NumberFormatException occurred", e);
                    e.printStackTrace();
                }

            } else {
                showToast("未读取到文件或数据");
            }

        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEIVE_SUCCESS:
                    DataTransfer.getInstance().setWaypointList(pointList);
                    Log.i(TAG, "读取完毕");
                    showToast("读取完毕");
                    Intent intentAty = new Intent();
                    intentAty.setClass(context, WayPointActivity.class);
                    startActivity(intentAty);
                    finish();
                    break;
                default:
                    break;
            }
        }
    };
}
