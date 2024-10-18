package com.xd.waypoint.activity;

import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LONGITUDE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xd.waypoint.R;
import com.xd.waypoint.fragment.DJIDemoApplication;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.mission.waypoint.WaypointTurnMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;

//load->upload->start
//测试航点
public class WaypointMissionActivity extends BaseActivity implements View.OnClickListener {
    private static final double BASE_LATITUDE = 34.27;
    private static final double BASE_LONGITUDE = 108.93;

    private Button mBtnLoad, mBtnUpload, mBtnStart,mBtnSimulator;
    private TextView mTvLat, mFlightState, mWaypointState;
    private static final int WAYPOINT_COUNT = 4;
    private double homeLatitude = 181;
    private double homeLongitude = 181;

    private static final int REFRESH_FREQ=10;
    private static final int SATELLITE_COUNT=10;
    private static final double HORIZONTAL_DISTANCE=30;
    private static final double VERTICAL_DISTANCE=30;
    private static final double ONE_METER_OFFSET=0.00000899322;//纬度每米偏移



    private WaypointMissionOperatorListener listener;
    private WaypointMissionOperator waypointMissionOperator = null;
    private FlightMode flightState = null;
    private FlightController flightController = null;
    private WaypointMission mission = null;


    public static void start(Context context) {
        Intent intent = new Intent(context, WaypointMissionActivity.class);
        context.startActivity(intent);
    }

    private void initUI() {
        context = this;
        mBtnLoad = (Button) findViewById(R.id.btn_load);
        mBtnUpload = (Button) findViewById(R.id.btn_upload);
        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnSimulator=(Button)findViewById(R.id.startSimulator);
//        mBtnSetHome=(Button)findViewById(R.id.setHome);
        mTvLat = (TextView) findViewById(R.id.tv_Lat);
        mFlightState = (TextView) findViewById(R.id.tv_flightState);
        mWaypointState = (TextView) findViewById(R.id.tv_waypointState);

        mBtnLoad.setOnClickListener(this);
        mBtnUpload.setOnClickListener(this);
        mBtnStart.setOnClickListener(this);
        mBtnSimulator.setOnClickListener(this);
//        mBtnSetHome.setOnClickListener(this);
    }

    //更新航点任务状态
    private void updateWaypointMissionState() {

        if (waypointMissionOperator != null && waypointMissionOperator.getCurrentState() != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mTvLat.setText("返航点经度 "+homeLongitude+" 纬度"+homeLatitude);
                    mFlightState.setText("当前飞行状态： " + flightState.name());
                    mWaypointState.setText("航点状态： " + waypointMissionOperator.getCurrentState().getName());
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFlightState.setText("当前飞行状态： " + flightState.name());
                    //  mWaypointState.setText("航点状态： "+waypointMissionOperator.getCurrentState().getName());
                }
            });
        }
    }

    private void setUpListener() {
        listener = new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
                if (waypointMissionDownloadEvent.getProgress() != null
                        && waypointMissionDownloadEvent.getProgress().isSummaryDownloaded
                        && waypointMissionDownloadEvent.getProgress().downloadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
                    showToast("任务下载成功");
                }
                updateWaypointMissionState();
            }

            @Override
            public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {

                if (waypointMissionUploadEvent.getProgress() != null
                        && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                        && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
                    showToast("任务上传成功");
                }
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

                //之前状态为空？ ：打印之前状态+当前状态+执行进度为空？ ：目标航点数
                Log.d("TAG",
                        (waypointMissionExecutionEvent.getPreviousState() == null
                                ? " "
                                : waypointMissionExecutionEvent.getPreviousState().getName())
                                + ", "
                                + waypointMissionExecutionEvent.getCurrentState().getName()
                                + (waypointMissionExecutionEvent.getProgress() == null
                                ? ""
                                : waypointMissionExecutionEvent.getProgress().targetWaypointIndex));
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionStart() {

                showToast("任务开始执行");
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {

                showToast("任务完成");
                updateWaypointMissionState();
            }
        };
        if (waypointMissionOperator != null && listener != null) {
            waypointMissionOperator.addListener(listener);
        }
    }

    private void tearDownListener() {
        if (waypointMissionOperator != null && listener != null) {
            waypointMissionOperator.removeListener(listener);
        }
    }


    private WaypointMissionOperator getWaypointMissionOperator() {
        if (null == waypointMissionOperator) {
            if (null != MissionControl.getInstance()) {
                return MissionControl.getInstance().getWaypointMissionOperator();
            }
        }
        return waypointMissionOperator;
    }

    private FlightController getFlightController() {
        if (null == flightController) {
            if (null != DJIDemoApplication.getAircraftInstance()) {
                return DJIDemoApplication.getAircraftInstance().getFlightController();
            }
            showToast("产品已连接");
        }
        return flightController;
    }

    private void initFlightController() {
        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
            if (flightController != null) {
                flightController.setStateCallback(new FlightControllerState.Callback() {
                    @Override
                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                        homeLatitude = flightControllerState.getHomeLocation().getLatitude();
                        homeLongitude = flightControllerState.getHomeLocation().getLongitude();

                        flightState = flightControllerState.getFlightMode();
                        if (flightControllerState.isLandingConfirmationNeeded()) {
                            flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    showToast(djiError == null ? "确认降落完成" : djiError.getDescription());
                                }
                            });
                        }
                        updateWaypointMissionState();
                    }
                });
            }
        } else {
            showToast("连接已断开");
            return;
        }
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        setUpListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypointmissiontest);
        initUI();
        setUpListener();
        initFlightController();
        updateWaypointMissionState();
    }

    private WaypointMission createRectangleWaypointMission() {
        WaypointMission.Builder builder = new WaypointMission.Builder();
        double baseLatitude = 34;
        double baseLongitude = 108;
        Object latitudeValue = KeyManager.getInstance().getValue(FlightControllerKey.create(HOME_LOCATION_LATITUDE));
        Object longitudeValue = KeyManager.getInstance().getValue(FlightControllerKey.create(HOME_LOCATION_LONGITUDE));

        if (latitudeValue != null && latitudeValue instanceof Double) {
            baseLatitude = (double) latitudeValue;
        }
        if (longitudeValue != null && longitudeValue instanceof Double) {
            baseLongitude = (double) longitudeValue;
        }
        final float baseAltitude = 30.0f;

        builder.autoFlightSpeed(5f)
                .maxFlightSpeed(10f)
                .setExitMissionOnRCSignalLostEnabled(false)
                .finishedAction(WaypointMissionFinishedAction.GO_HOME)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY)
                .headingMode(WaypointMissionHeadingMode.AUTO)
                .repeatTimes(1);

        WaypointAction stay=new WaypointAction(WaypointActionType.STAY,2000);
        //w0(0,0)
        Waypoint w0=new Waypoint(baseLatitude,baseLongitude,baseAltitude);
        w0.turnMode= WaypointTurnMode.CLOCKWISE;
        w0.addAction(stay);
        builder.addWaypoint(w0);

        Waypoint w1=new Waypoint(baseLatitude,baseLongitude+HORIZONTAL_DISTANCE*ONE_METER_OFFSET,baseAltitude);
        w1.addAction(stay);
        builder.addWaypoint(w1);

        Waypoint w2=new Waypoint(baseLatitude+VERTICAL_DISTANCE*ONE_METER_OFFSET,baseLongitude+HORIZONTAL_DISTANCE*ONE_METER_OFFSET,baseAltitude);
        w2.addAction(stay);
        builder.addWaypoint(w2);

        Waypoint w3=new Waypoint(baseLatitude+VERTICAL_DISTANCE*ONE_METER_OFFSET,baseLongitude,baseAltitude);
        w3.addAction(stay);
        builder.addWaypoint(w3);

        return builder.build();

    }

    /*private void UpdateHome(@NonNull FlightControllerState state){
        homeLatitude=state.getAircraftLocation().getLatitude();
        homeLongitude=state.getAircraftLocation().getLongitude();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvLat.setText("返航点经度： "+ homeLongitude+" 纬度： "+homeLatitude);
            }
        });
    }*/
    private void startSimulator(){
        if (null!=getFlightController()){
            flightController.getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE), REFRESH_FREQ, SATELLITE_COUNT), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    showToast(error==null?"开始仿真":error.getDescription());
                }
            });
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_load:
                mission = createRectangleWaypointMission();
                DJIError error = waypointMissionOperator.loadMission(mission);
                if (error == null) {
                    showToast("任务加载成功");
                } else {
                    showToast(error.getDescription());
                }
                break;

            case R.id.btn_upload:
                if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())||WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())){
                    waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            showToast(error==null?"上传成功":error.getDescription());
                        }
                    });
                }else{
                    showToast("等待任务上传");
                }
                break;
            case R.id.btn_start:
                if (null!=mission){
                    waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            showToast(error==null?"任务开始成功":error.getDescription());
                        }
                    });
                }else{
                    showToast("任务为空，等待上传任务");
                }
                break;
            case R.id.btn_stop:
                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        showToast(error==null?" ":error.getDescription());
                    }
                });
                break;
            case R.id.btn_pause:
                waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        showToast(error==null?"任务已暂停":error.getDescription());
                    }
                });
            case R.id.btn_resume:
                waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        showToast(error==null?"任务已继续":error.getDescription());
                    }
                });
                break;
            case R.id.btn_download:
                if (WaypointMissionState.EXECUTING.equals(waypointMissionOperator.getCurrentState())||WaypointMissionState.EXECUTION_PAUSED.equals(waypointMissionOperator.getCurrentState())){
                    waypointMissionOperator.downloadMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {

                            showToast(error==null?" ":error.getDescription());
                        }
                    });
                }else{
                    showToast("当任务正在执行或暂停时才能下载");
                }
                break;
            case R.id.startSimulator:
                startSimulator();
                updateWaypointMissionState();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        tearDownListener();
        if (flightController != null) {
            flightController.setStateCallback(null);
        }
        super.onDestroy();

    }
}
