package com.xd.waypoint;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xd.waypoint.activity.BaseActivity;
import com.xd.waypoint.activity.FlyTestActivity;
import com.xd.waypoint.activity.WayPointActivity;
import com.xd.waypoint.activity.WaypointMissionActivity;

import dji.common.error.DJIError;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends BaseActivity implements View.OnClickListener {


    protected static final String TAG = "MainActivity";
    private Button BtnLogin;
    private Button BtnLogout;
  //  private Button BtnBindAircraft;
    private Button BtnBuildMission;
    private Button BtnAppActivation;
    private Button BtnFlyTest;
    private Button BtnWaypointTest;

    private TextView tvAppActivation;
  //  private TextView tvBindAircraft;

    private AppActivationState.AppActivationStateListener activationStateListener;
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initListener();
        initUI();

    }

    private void initUI() {
        context = this;
        BtnLogin = (Button) findViewById(R.id.btn_login);
        BtnLogout = (Button) findViewById(R.id.btn_logout);
  //      BtnBindAircraft = (Button) findViewById(R.id.btn_bindAircraft);
        BtnBuildMission = (Button) findViewById(R.id.btn_buildMission);
        BtnAppActivation = (Button) findViewById(R.id.btn_appActivation);
        BtnFlyTest = (Button) findViewById(R.id.btn_flyTest);
        BtnWaypointTest=(Button)findViewById(R.id.btn_waypointTest);


   //     tvBindAircraft = (TextView) findViewById(R.id.tv_status_binding);
        tvAppActivation = (TextView) findViewById(R.id.tv_status_activation);

        BtnLogin.setOnClickListener(this);
        BtnLogout.setOnClickListener(this);
    //    BtnBindAircraft.setOnClickListener(this);
        BtnBuildMission.setOnClickListener(this);
        BtnAppActivation.setOnClickListener(this);
        BtnFlyTest.setOnClickListener(this);
        BtnWaypointTest.setOnClickListener(this);

    }

    private void loginAccount() {
        Log.d(TAG, "登录：");
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(UserAccountState userAccountState) {

                        Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "登录成功");
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        Toast.makeText(context, "登录失败！" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logOutAccount() {
        UserAccountManager.getInstance().logoutOfDJIUserAccount(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    Toast.makeText(context, "退出成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "退出失败！" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

//    @Override
//    public void onAttachedToWindow() {
//        super.onAttachedToWindow();
//        UserAccountState currentState = UserAccountManager.getInstance().getUserAccountState();
//
//    }

    private void startDJIGO4APP() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("dji.go.v4");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(context, "未安装DJI GO 4？", Toast.LENGTH_SHORT).show();
        }
    }


    private void startFlyTestActivity() {
        FlyTestActivity.start(context);
    }

    private void startWayPointActivity() {
        WayPointActivity.start(context);
    }

    private void startWaypointTestActivity(){
        WaypointMissionActivity.start(context);
    }
    private void getAppActivationStatus() {
        AppActivationManager mgrActivation = DJISDKManager.getInstance().getAppActivationManager();
        Toast.makeText(context, "激活状态" + mgrActivation.getAppActivationState(), Toast.LENGTH_SHORT).show();
    }

    private void initListener() {
        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState state) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvAppActivation.setText("当前应用激活状态：" + state.name());
                    }
                });
            }
        };
//        bindingStateListener = new AircraftBindingState.AircraftBindingStateListener() {
//            @Override
//            public void onUpdate(AircraftBindingState state) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tvBindAircraft.setText("当前无人机绑定状态：" + state.name());
//                    }
//                });
//            }
//        };
        AppActivationManager.getInstance().addAppActivationStateListener(activationStateListener);
     //   AppActivationManager.getInstance().addAircraftBindingStateListener(bindingStateListener);
    }


    private boolean checkBefore() {
        //应用程序激活管理器
        AppActivationManager mgrActivation = DJISDKManager.
                getInstance().getAppActivationManager();
        //判断是否注册
        if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            showToast("应用程序未注册！");
            return false;
        }
        //判断应用程序是否激活
        if (mgrActivation.getAppActivationState() != AppActivationState.ACTIVATED) {
            showToast("应用程序未激活！");
            return false;
        }
        //判断无人机绑定
//        if (mgrActivation.getAircraftBindingState() != AircraftBindingState.BOUND) {
//            showToast("无人机未绑定！");
//            return false;
//        }
        //判断连接状态
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product == null || !product.isConnected()) {
            showToast("无人机连接失败！");
            return false;
        }
        return true;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                loginAccount();
                break;
            case R.id.btn_logout:
                logOutAccount();
                break;
//       case R.id.btn_bindAircraft:
//                startDJIGO4APP();
//
//                break;

            case R.id.btn_waypointTest:
                startWaypointTestActivity();
//                if (checkBefore()){
//
//                }
                break;
            case R.id.btn_flyTest:
                if (checkBefore()) {
                    startFlyTestActivity();
                }

                break;
            case R.id.btn_buildMission:
                if (checkBefore()){
                startWayPointActivity();}
                break;
            case R.id.btn_appActivation:
                getAppActivationStatus();
                break;

            default:
                break;
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
    private void removeListener() {
        AppActivationManager.getInstance().removeAppActivationStateListener(activationStateListener);
  //      AppActivationManager.getInstance().removeAircraftBindingStateListener(bindingStateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }
}