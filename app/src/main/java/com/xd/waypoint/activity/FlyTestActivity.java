package com.xd.waypoint.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xd.waypoint.R;
import com.xd.waypoint.fragment.DJIDemoApplication;
import com.xd.waypoint.service.FileTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

public class FlyTestActivity extends BaseActivity implements View.OnClickListener {

    private ImageView ivBack;
    private Button btnTakeoff, btnCancelFly,
            btnLand, btnCancelLand,btnGoHome, btnCancelGohome;
    private TextView tvLocation;

    private FlightController flightController = null;
    private FlightControllerState flightControllerState=null;

    private FileTask filetask;
    public static void start(Context context) {
        Intent intent = new Intent(context, FlyTestActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flytest);
        initUI();
        initListener();
    }

    private void initUI() {
        context = this;

        ivBack = (ImageView) findViewById(R.id.iv_back);

        btnTakeoff = (Button) findViewById(R.id.btn_takeoff);
        btnCancelFly = (Button) findViewById(R.id.btn_cancel_takeoff);
        btnGoHome = (Button) findViewById(R.id.btn_goHome);
        btnCancelGohome = (Button) findViewById(R.id.btn_cancel_goHome);
        btnLand = (Button) findViewById(R.id.btn_land);
        btnCancelLand=(Button)findViewById(R.id.btn_cancel_land);
        tvLocation = (TextView) findViewById(R.id.tv_location);

        ivBack.setOnClickListener(this);
        btnTakeoff.setOnClickListener(this);
        btnCancelFly.setOnClickListener(this);
        btnGoHome.setOnClickListener(this);
        btnCancelGohome.setOnClickListener(this);
        btnLand.setOnClickListener(this);
        btnCancelLand.setOnClickListener(this);

    }

    private void initListener() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState state) {
                    //经度
                    final double longitude = state.getAircraftLocation().getLongitude();
                    //纬度
                    final double latitude = state.getAircraftLocation().getLatitude();
                    String str=longitude+", "+latitude;
                    filetask=new FileTask(context,str,5000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvLocation.setText(String.format("经度：%.6f,纬度：%.6f", longitude, latitude));
                        }
                    });
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常！");
        }
    }

    private FlightController getFlightController() {
//        BaseProduct product= DJISDKManager.getInstance().getProduct();
//        if (product!=null&& product.isConnected()){
//            if (product instanceof Aircraft){
//                return ((Aircraft)product).getFlightController();
//            }
//        }
//        return null;

        if (null == flightController) {
            if (null != DJIDemoApplication.getAircraftInstance()) {
                return DJIDemoApplication.getAircraftInstance().getFlightController();
            }
            showToast("产品已连接");
        }
        return flightController;
    }

    //起飞后悬停
    private void takeoff() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error != null) {
                        showToast(error.getDescription());
                    } else {
                        showToast("开始起飞");
                    }

                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接状态！");
        }
    }

    private void cancelTakeoff(){
        FlightController flightController=getFlightController();
        if (flightController!=null){
            flightController.cancelTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error!=null){
                        showToast(error.getDescription());
                    }else{
                        showToast("取消起飞成功！");
                    }
                }
            });
        }
    }
    private void land() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error != null) {
                        showToast(error.toString());
                    } else {
                        showToast("开始降落");
                        //当距地面距离小于0.3m时isLandingConfirmationNeeded为true
                        if (flightControllerState.isLandingConfirmationNeeded()){
                            flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    showToast(djiError==null?"确认降落": djiError.getDescription());
                                }
                            });
                        }

                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接状态！");
        }
    }

    private void cancelLand(){
        FlightController flightController=getFlightController();
        if (flightController!=null){
            flightController.cancelLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error!=null){
                        showToast(error.toString());
                    }else{
                        showToast("取消降落！");
                    }
                }
            });
        }
    }

    private void goHome(){
        final FlightController flightController=getFlightController();
        if (flightController!=null){
            flightController.setGoHomeHeightInMeters(20, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error!=null){
                        showToast("返航高度设置失败"+error.getDescription());
                    }
                    else
                    {
                        showToast("返航高度设置成功！");
                    }
                }
            });
            flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error!=null){
                        showToast(error.getDescription());
                    }else{
                        showToast("开始返航！");
                    }
                }
            });
        }else{
            showToast("飞行控制器获取失败，请检查飞行器连接！");
        }
    }
    private void cancelGoHome(){
        final FlightController flightController=getFlightController();
        if (flightController!=null){
            flightController.cancelGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error!=null){
                        showToast(error.getDescription());
                    }
                    else
                    {showToast("取消返航");}
                }
            });
        }else{
            showToast("飞行控制器获取失败，请检查飞行器连接！");
        }
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()) {


            case R.id.btn_takeoff:
                takeoff();

                break;

            case R.id.btn_cancel_takeoff:
                cancelTakeoff();
                break;
            case R.id.btn_land:
                land();
                break;
            case R.id.btn_cancel_land:
                cancelLand();
            case R.id.btn_goHome:
                goHome();
                break;
            case R.id.btn_cancel_goHome:

                cancelGoHome();
                break;
            case R.id.iv_back:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if(filetask!=null){
            filetask.stop();
        }
        super.onDestroy();
        removeListener();
    }
    private void removeListener(){
        FlightController flightController=getFlightController();
        if (flightController!=null){
            flightController.setStateCallback(null);
        }
    }
}
