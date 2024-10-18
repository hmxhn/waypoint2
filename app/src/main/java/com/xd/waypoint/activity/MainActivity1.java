package com.xd.waypoint.activity;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.xd.waypoint.BT.BlueToothActivity;
import com.xd.waypoint.R;

import dji.common.error.DJIError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity1 extends BaseActivity {

    private CardView cardFlightMode;
    private CardView cardSimulator;
    private CardView cardWaypoint;
    private CardView cardCamera;
    protected static final String TAG = "MainActivity1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        initUI();
        initListener();

    }

    private void initUI() {
        context = this;
        cardFlightMode = (CardView) findViewById(R.id.card_flightmode);
        cardSimulator = (CardView) findViewById(R.id.card_simulator);
        cardWaypoint = (CardView) findViewById(R.id.card_waypoint);
        cardCamera = (CardView) findViewById(R.id.card_camera);
        Toolbar head = (Toolbar) findViewById(R.id.head);
        //!!!重要 没有这一行不显示
        setSupportActionBar(head);

    }



    private void startFlyTestActivity() {
        FlyTestActivity.start(context);
    }

    private void startCameraActivity() {
        CameraActivity.start(context);
    }

    private void startWayPointActivity() {
        WayPointActivity.start(context);
    }

    private void startWayPointMission() {
        WaypointMissionActivity.start(context);
    }

    private void startFileOperation() {
        FileActivity.start(context);
    }

    private void startSimulator() {
        SimulatorActivity.start(context);
    }

    @Override

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
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

    private void initListener() {
        cardFlightMode.setOnClickListener(v -> {
                    startFlyTestActivity();
                }
        );
        cardWaypoint.setOnClickListener(v -> startWayPointActivity());
        cardCamera.setOnClickListener(v -> startCameraActivity());
        cardSimulator.setOnClickListener(v -> startSimulator());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.login:
                loginAccount();
                break;
            case R.id.logout:
                logOutAccount();
                break;
            case R.id.start_test:
                startWayPointMission();
                break;
            case R.id.start_file_operator:
                startFileOperation();
                break;
            default:
        }
        return true;
    }
}