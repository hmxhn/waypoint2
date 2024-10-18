package com.xd.waypoint.activity;

import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LONGITUDE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;


import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.vividsolutions.jts.operation.overlay.OverlayOp;
import com.xd.waypoint.BT.BlueToothActivity;
import com.xd.waypoint.BT.CommunicationActivity;
import com.xd.waypoint.R;
import com.xd.waypoint.fragment.DJIDemoApplication;
import com.xd.waypoint.service.FileTask;
import com.xd.waypoint.utils.DataTransfer;
import com.xd.waypoint.utils.FileHelper;
import com.xd.waypoint.utils.Transformer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import butterknife.BindColor;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.mission.waypoint.WaypointTurnMode;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class WayPointActivity extends BaseActivity implements View.OnClickListener, OnMapClickListener {

    @BindColor(R.color.blue)
    int pathColor;

    private static final String TAG = WayPointActivity.class.getName();
    private Button locate, add, clear, start, config, upload, stop, manual;
    private boolean isAdd = false;
    private double droneLocationLat = 181, droneLocationLng = 181;
    private MapView mapView;
    private AMap aMap;
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    private List<Waypoint> waypointList = new ArrayList<>();
    private List<LatLng> latLngs = new ArrayList<>();
    private List<LatLng> idealLatLng = new ArrayList<>();
    private BlockingQueue<WaypointMission> missionQueue = new LinkedBlockingQueue<>();
    private boolean isManualFlag = true;
    private String mAddress;
    private BluetoothDevice mDevice;

    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;
    private Polyline polyline;
    private Polyline idealLine;
    private FlightController mFlightController;

    private static boolean isTakeOff = false;
    private FlightControllerState flightState;
    private WaypointMissionOperator instance;
    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private WaypointMission mission = null;
    private WaypointMissionOperator waypointMissionOperator = null;
    private float calculateTotalTime = 0.0f;
    private boolean finish = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;

    private final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//蓝牙串口服务
    private static int REQUEST_ENABLE = 1;
    private static final ArrayList<String> REQUIRED_PERMISSION_LIST = new ArrayList<>();
    private List<String> missingPermission = new ArrayList<>(
    );

    public static void start(Context context) {
        Intent intent = new Intent(context, WayPointActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onResume() {

        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        removeListener();

        super.onDestroy();
        try {
            if (mBluetoothSocket.isConnected()) {
                //关闭socket
                mBluetoothSocket.close();
                bluetoothAdapter= null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onReturn(View view) {
        Log.d(TAG, "onReturn");
        this.finish();
    }


    private void initUI() {
        context = this;
        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        manual = (Button) findViewById(R.id.btn_manual);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        manual.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        Toolbar head = (Toolbar) findViewById(R.id.toolbar);
        head.setTitle("航点飞行");
        setSupportActionBar(head);
    }


    private void initMapView() {
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);
        }
        // LatLng lat = new LatLng(34.233869, 108.913601);
        LatLng Xian = Transformer.wgs84ToGcj02(34.1276557162179, 108.822823555737);
        aMap.addMarker(new MarkerOptions().position(Xian).title("Marker in xian"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(Xian));
        //  drawLine();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        initMapView();
        initUI();
        addListener();

    }

    private void markWayPoint(LatLng point) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange() {
        initFlightController();
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void initFlightController() {
        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    //  String str=droneLocationLat+" ,"+droneLocationLng+"\n";
                    //  filetask=new FileTask(context,str,5000);
                    latLngs.add(Transformer.wgs84ToGcj02(droneLocationLat, droneLocationLng));
                    polyline = aMap.addPolyline(new PolylineOptions()
                            .addAll(latLngs)
                            .width(20)
                            .color(Color.argb(235, 1, 180, 247)));
                    updateDroneLocation();
                }
            });
        }
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (waypointMissionOperator != null) {
            waypointMissionOperator.addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (waypointMissionOperator != null) {
            waypointMissionOperator.removeListener(eventNotificationListener);
        }
    }


    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
            if (waypointMissionDownloadEvent.getProgress() != null
                    && waypointMissionDownloadEvent.getProgress().isSummaryDownloaded
                    && waypointMissionDownloadEvent.getProgress().downloadedWaypointIndex == (waypointList.size() - 1)) {
                showToast("任务下载成功");
            }
        }

        @Override
        public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
            if (waypointMissionUploadEvent.getProgress() != null
                    && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                    && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (waypointList.size() - 1)) {
                showToast("任务上传成功");
            }
        }

        @Override
        public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

        }

        @Override
        public void onExecutionStart() {
            showToast("任务开始执行");

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            Toast.makeText(context, "当前任务执行完毕：" + (error == null ? "成功！" : error.getDescription()), Toast.LENGTH_SHORT).show();
            if(!missionQueue.isEmpty()) {
                WaypointMission mission = missionQueue.poll();
                waypointMissionOperator.loadMission(mission);
                uploadWaypointMission();

                startWaypointMission();
            }
        }
    };

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (null == waypointMissionOperator) {
            if (null != MissionControl.getInstance()) {
                return MissionControl.getInstance().getWaypointMissionOperator();
            }
        }
        return waypointMissionOperator;
    }

    private void updateDroneLocation() {

        LatLng position = Transformer.wgs84ToGcj02(droneLocationLat, droneLocationLng);
        ;
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(position);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }

            }
        });
    }

    private void cameraUpdate() {
        LatLng position = Transformer.wgs84ToGcj02(droneLocationLat, droneLocationLng);

        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(position, zoomlevel);
        aMap.moveCamera(cu);
    }

    private void sendLocation(boolean flag) {
        if (flag) {
            droneLocationLat = flightState.getAircraftLocation().getLatitude();
            droneLocationLng = flightState.getAircraftLocation().getLongitude();
            FileHelper helper = new FileHelper(this);
            String lat = String.valueOf(droneLocationLat);
            String lng = String.valueOf(droneLocationLng);
            try {

                helper.save("dronelat", lat + " , " + lng + "\n");

                showToast("数据写入成功");
            } catch (Exception e) {
                e.printStackTrace();
                showToast("数据写入失败");
            }
        }
    }

    private void enableDisableAdd() {
        if (isAdd == false) {
            isAdd = true;
            add.setText("退出");
        } else {
            isAdd = false;
            add.setText("添加");
        }
    }

    private void setManual() {
        if (isManualFlag == true) {
            isManualFlag = false;
            manual.setText("手动添加航点");
            checkAndRequestPermissions();
            openBlueTooth();
            startBtActivity();
        } else {
            isManualFlag = true;

            manual.setText("自动添加航点");
        }
    }

    private void openBlueTooth() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            showToast("您的设备不支持蓝牙");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    checkAndRequestPermissions();
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE);
            }
        }
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

    private void showSettingDialog() {

        LinearLayout waypointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);
        final TextView tvWpAltitude = (TextView) waypointSettings.findViewById(R.id.rg_Altitude);
        RadioGroup rgSpeed = (RadioGroup) waypointSettings.findViewById(R.id.rg_Speed);
        RadioGroup rgActionAfterFinished = (RadioGroup) waypointSettings.findViewById(R.id.rg_ActionAfterFinished);
        RadioGroup rgHeading = (RadioGroup) waypointSettings.findViewById(R.id.rg_Heading);

        rgSpeed.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.lowSpeed) {
                mSpeed = 3.0f;
            } else if (checkedId == R.id.midSpeed) {
                mSpeed = 5.0f;
            } else if (checkedId == R.id.highSpeed) {
                mSpeed = 10.0f;
            }

        });

        rgActionAfterFinished.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "select finish action");
            if (checkedId == R.id.finishNone) {
                mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
            } else if (checkedId == R.id.finishGoHome) {
                mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
            } else if (checkedId == R.id.finishAutoLanding) {
                mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
            } else if (checkedId == R.id.finishToFirst) {
                mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
            }
        });

        rgHeading.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "select heading");
            if (checkedId == R.id.headingNext) {
                mHeadingMode = WaypointMissionHeadingMode.AUTO;

            } else if (checkedId == R.id.headingInitial) {
                mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
            } else if (checkedId == R.id.headingRC) {
                mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
            } else if (checkedId == R.id.headingWP) {
                mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
            }
        });

//        设置一个对话框
        new AlertDialog.Builder(this)
                .setTitle("航点配置信息")
                .setView(waypointSettings)
                .setPositiveButton("完成", (dialog, which) -> {
                    String altitudeString = tvWpAltitude.getText().toString();
                    altitude = Integer.parseInt(nulltoIntegerDefault(altitudeString));
                    configWayPointMission();

                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.cancel();
                })
                .create()
                .show();
    }

    String nulltoIntegerDefault(String value) {

        if (!isIntValue(value)) {
            value = "0";
        }
        return value;
    }

    boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
//            parseInt() 方法用于将字符串参数作为有符号的十进制整数进行解析。
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private void configWayPointMission() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }

        if (waypointMissionBuilder.getWaypointList().size() > 0) {
            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;

            }
            Toast.makeText(context, "航点高度设置成功", Toast.LENGTH_SHORT).show();
        }

        DJIError error = waypointMissionOperator.loadMission(waypointMissionBuilder.build());
        if (error == null) {
            Toast.makeText(context, "航点加载成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "航点加载失败" + error.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar2, menu);
        return true;
    }

//    //加载文件
//    public List<Waypoint> load() {
//        List<Waypoint> waypoints = new ArrayList<>();
//        FileInputStream in = null;
//        BufferedReader reader = null;
//        StringBuilder content = new StringBuilder();
//        try {
//            in = openFileInput("loglat.txt");
//            reader = new BufferedReader(new InputStreamReader(in));
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                String[] s = line.split(",");
//                double lon = Double.parseDouble(s[0]);
//                double lat = Double.parseDouble(s[1]);
//                float altitude = Float.parseFloat(s[2]);
//                waypoints.add(new Waypoint(lon, lat, altitude));
//            }
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.d(TAG, "文件不存在");
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return waypoints;
//    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:
                updateDroneLocation();
                cameraUpdate();
                break;
            case R.id.add:
                enableDisableAdd();
                break;
            case R.id.btn_manual:
                setManual();
                break;
            case R.id.clear:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }
                });
                DataTransfer.getInstance().setWaypointList(null);
                DataTransfer.getInstance().setPoints(null);
                waypointList.clear();
                //后续需要把这里修改为读取到的文件列表
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            case R.id.config:
                showSettingDialog();
                break;
            case R.id.upload:
                drawLine();
                uploadWaypointMission();
                break;
            case R.id.start:
                startWaypointMission();
                break;
            case R.id.stop:
                stopWaypointMission();
                break;

            default:
                break;
        }
    }

    private void drawLine() {
        idealLine = aMap.addPolyline(new PolylineOptions()
                .addAll(idealLatLng)
                .width(20)
                .color(Color.argb(235, 50, 180, 247))
        );
    }

    private void startBtActivity() {
        BlueToothActivity.start(context);
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true) {
            markWayPoint(point);
            // List<LatLng> list=new ArrayList<>();
            LatLng latLng = Transformer.Gcj02Towgs84(point.latitude, point.longitude);
            //  list.add(point);
            showToast(String.format("%.6f,%.6f", point.latitude, point.longitude));
            Waypoint mWaypoint = new Waypoint(latLng.latitude, latLng.longitude, altitude);
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);

                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());

            } else {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        } else {

            Toast.makeText(context, "添加航点失败", Toast.LENGTH_SHORT).show();
        }
    }

    private WaypointMission createWaypointMission() {
        WaypointMission.Builder builder = new WaypointMission.Builder();

        builder.autoFlightSpeed(5f)
                .maxFlightSpeed(10f)
                .setExitMissionOnRCSignalLostEnabled(false)
                .finishedAction(WaypointMissionFinishedAction.CONTINUE_UNTIL_END)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY)
                .headingMode(WaypointMissionHeadingMode.AUTO)
                .repeatTimes(1);
        WaypointAction stay = new WaypointAction(WaypointActionType.STAY, 2000);
        for (Waypoint waypoint : waypointList) {
            waypoint.turnMode = WaypointTurnMode.CLOCKWISE;
            builder.addWaypoint(waypoint);
        }
        calculateTotalTime = builder.calculateTotalTime();
        return builder.build();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth://正常
                Intent intent=new Intent(WayPointActivity.this,BlueToothActivity.class);
                startActivityForResult(intent,1);
               // startBtActivity();

                break;
            case R.id.import_waypoint:
                waypointList = DataTransfer.getInstance().getWaypointList();
                idealLatLng = DataTransfer.getInstance().getPoints();
                mission = createWaypointMission();
//              waypointMissionOperator=getWaypointMissionOperator();
                DJIError djiError = waypointMissionOperator.loadMission(mission);
                if (djiError == null) {
                    showToast("任务加载成功，预计飞行时间为 " + calculateTotalTime + " 秒");
                } else {
                    showToast(djiError.getDescription());
                }
//                Log.i(TAG,"航点队列"+waypointList.size());
//                Log.i(TAG,"经纬度队列"+points.size());
                break;
            default:

        }
        return true;
    }

    public class loadMissionThread extends Thread {
        public loadMissionThread() {
            super();
        }

        @Override
        public void run() {
           while(true) {
               try{
                   waypointMissionOperator.loadMission(missionQueue.poll(10, TimeUnit.SECONDS));
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               if(waypointMissionOperator.getCurrentState()==WaypointMissionState.READY_TO_UPLOAD){
                    waypointMissionOperator.clearMission();

                    waypointMissionOperator.loadMission(missionQueue.poll(10, TimeUnit.SECONDS));
                    uploadWaypointMission();
                    startWaypointMission();
                }

            }
        }
    }

    private void uploadWaypointMission() {
        showToast("目前状态：" + waypointMissionOperator.getCurrentState());
        if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState()) || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
            waypointMissionOperator.uploadMission(error -> {
                if (error == null) {
                    showToast("任务上传成功");
                } else {
                    showToast("任务上传失败" + error.getDescription() + "尝试重新上传中...");
                    waypointMissionOperator.retryUploadMission(null);
                }
            });
        } else {
            showToast("等待任务上传");
        }
    }

    private void startWaypointMission() {
        if (mission != null) {
            waypointMissionOperator.startMission(error -> {
                showToast("任务开始：" + (error == null ? "成功！" : error.getDescription()));
            });
        } else {
            showToast("任务为空，等待上传任务");
        }
    }

    private void stopWaypointMission() {
        waypointMissionOperator.stopMission(error -> {
            Toast.makeText(this, "任务结束：" + (error == null ? "成功！" : error.getDescription()), Toast.LENGTH_SHORT).show();
        });
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    connectDevice();
                }
                break;
            default:
        }
    }
    @SuppressLint("MissingPermission")
    private void connectDevice() {
        SharedPreferences pref = getSharedPreferences("devices", MODE_PRIVATE);
        //获取默认蓝牙设配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAddress = pref.getString("ADDRESS", "");
        //通过地址拿到该蓝牙设备device
        mDevice = bluetoothAdapter.getRemoteDevice(mAddress);
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

    //    private void showToast(final String toastMsg) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
//            }
//        });
//    }
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
           // 结束时插入结束的字符串
            super.run();
            String line = "";
            int count = 0;//行数
            if (inputStream != null) {
                inputReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputReader,4096);
                try {
                    while ((line = reader.readLine()) != null) {

                        String regex = ",|，";
                        String[] s = line.split(regex);
                        String a = line;
                  //      pointList.add(new Waypoint(Double.parseDouble(s[0].trim()), Double.parseDouble(s[1].trim()), Float.parseFloat(s[2].trim())));
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

//                                tvReceive.append(a + "\r\n");
//                                int count = tvReceive.getLineCount();
//                                if (count > 28) {
//                                    int offset = tvReceive.getLineCount() * tvReceive.getLineHeight();
//                                    tvReceive.scrollTo(0, offset - tvReceive.getHeight() + tvReceive.getLineHeight());
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
}
