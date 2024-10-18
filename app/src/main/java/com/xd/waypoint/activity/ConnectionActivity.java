package com.xd.waypoint.activity;

import android.Manifest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.xd.waypoint.R;
import com.xd.waypoint.fragment.DJIDemoApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class ConnectionActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = ConnectionActivity.class.getName();

    private TextView TextConnectionStatus;
    private TextView TextProduct;
    private Button BtnOpen;
 //   private Button BtnRegister;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,

            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

     setContentView(R.layout.activity_connection);
        initUI();
        checkAndRequestPermissions();

        IntentFilter filter=new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        //更新产品连接状态

    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, missingPermission.toArray(new String[missingPermission.size()]), REQUEST_PERMISSION_CODE);
        } else {
            startSDKRegistration();
        }
    }

    //@RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("缺少权限!!!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                   showToast("注册中，请稍后...");
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
//                            boolean hasRegistered=DJISDKManager.getInstance().hasSDKRegistered();
//                            if (hasRegistered){
//                                if (djiError!=null){
//                                    Log.d(TAG, djiError.getDescription());
//                                }
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                               showToast("注册成功");
                            } else {
                                showToast("注册 sdk 失败, 请检查网络是否可用");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }
                         //与遥控器连接
                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("与遥控器连接已断开");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshSDKRelativeUI();
                                }
                            });
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                           showToast("连接成功");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshSDKRelativeUI();
                                }
                            });
                        }

                        @Override
                        public void onProductChanged(BaseProduct baseProduct) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshSDKRelativeUI();
                                }
                            });
                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }
                    });
                }
            });
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void initUI() {

        context=this;
        TextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        TextProduct = (TextView) findViewById(R.id.text_product_info);
        BtnOpen = (Button) findViewById(R.id.btn_open);
     //   BtnRegister =(Button)findViewById(R.id.btn_register);
        BtnOpen.setOnClickListener(this);
        BtnOpen.setEnabled(true);
//        BtnRegister.setOnClickListener(this);
//        BtnRegister.setEnabled(true);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = DJIDemoApplication.getProductInstance();
        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK:True");
           BtnOpen.setEnabled(true);
            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            TextConnectionStatus.setText("状态:" + str + "已连接");
            if (null != mProduct.getModel()) {
                TextProduct.setText("" + mProduct.getModel().getDisplayName());
            } else {
                TextProduct.setText(R.string.product_information);
            }
        } else {
            Log.v(TAG, "refreshSDK:False");
       //     BtnOpen.setEnabled(false);
            TextProduct.setText(R.string.product_information);
            TextConnectionStatus.setText(R.string.connection_loose);
        }
    }

    private void startMainActivity1(){
        Intent intent = new Intent(this, MainActivity1.class);
        startActivity(intent);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open: {
                startMainActivity1();
                break;
            }
//            case R.id.btn_register:
//                checkAndRequestPermissions();

            default:
                break;
        }
    }

//  private void showToast(final String toastMsg) {
//      runOnUiThread(new Runnable() {
//       @Override
//         public void run() {
//              Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
//           }
//     });
//   }
}
