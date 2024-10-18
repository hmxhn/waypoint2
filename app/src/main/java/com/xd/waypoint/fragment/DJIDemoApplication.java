package com.xd.waypoint.fragment;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJIDemoApplication extends Application {
    private static final String TAG = DJIDemoApplication.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private static BaseProduct mProduct;
    public static Handler mHandler;
    private Application instance;
    private static Application app=null;
  //  private static final Handler sHandler = new Handler();
    private static Toast sToast; // 单例Toast,避免重复创建，显示时间过长

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public DJIDemoApplication() {

    }

    public static Application getInstance(){return DJIDemoApplication.app;}
    //获取无人机实例
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }
    //获取相机实例
    public static synchronized Camera getCameraInstance(){
        if(getProductInstance()==null) {
            return null;
        }
        Camera camera=null;
        if(getProductInstance() instanceof Aircraft){
            Log.d(TAG, "getAircraftInstance");
            camera=((Aircraft)getProductInstance()).getCamera();
        }else if(getProductInstance() instanceof HandHeld){
            Log.d(TAG, "getHandHeldInstance");
            camera=((HandHeld)getProductInstance()).getCamera();
        }
        return camera;
    }

    public static boolean isAircraftConnected(){
        //当连接的产品不为空且属于无人机
        return getProductInstance()!=null&&getProductInstance() instanceof Aircraft;
    }
    public static synchronized Aircraft getAircraftInstance(){
        if (!isAircraftConnected()){
            return null;
        }
        return (Aircraft) getProductInstance();
    }

    @Override
    public void onCreate() {

        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        mDJISDKManagerCallback=new DJISDKManager.SDKManagerCallback() {
            @Override
            public void onRegister(DJIError djiError) {
                if (djiError== DJISDKError.REGISTRATION_SUCCESS){
                    Handler handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"注册成功",Toast.LENGTH_LONG).show();
                        }
                    });
                    DJISDKManager.getInstance().startConnectionToProduct();
                }else{
                    Handler handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"注册失败，请检查网络是否可用",Toast.LENGTH_LONG).show();
                        }
                    });
                }
                Log.e("TAG", djiError.toString());
            }

            @Override
            public void onProductDisconnect() {
                Log.d(TAG, "onProductDisconnect: ");
                notifyStatusChange();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                 notifyStatusChange();
            }

            @Override
            public void onProductChanged(BaseProduct baseProduct) {
                  notifyStatusChange();
            }

            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                  if (newComponent!=null){
                      newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                          @Override
                          public void onConnectivityChange(boolean isConnected) {
                              Log.d("TAG", "onConnectivityChange: "+isConnected);
                              notifyStatusChange();
                          }
                      });
                  }
                Log.d("TAG", String.format("onComponentChange key:%s,oldComponent:%s,newComponent:%s",
                        componentKey,oldComponent,newComponent));
            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

            }

            @Override
            public void onDatabaseDownloadProgress(long l, long l1) {

            }
        };


        int permissionCheck= ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2=ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.M||(permissionCheck==0&&permissionCheck2==0)){
            Toast.makeText(getApplicationContext(),"注册中，请稍后...",Toast.LENGTH_LONG).show();
        }else
        {
            Toast.makeText(getApplicationContext(),"请检查是否授予应用权限",Toast.LENGTH_LONG).show();
        }
    }

    private void notifyStatusChange(){
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable,500);
    }

    private Runnable updateRunnable=new Runnable() {
        @Override
        public void run() {
            Intent intent=new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };
    public static void showToast(String txt) {
        sToast.setText(txt);
        sToast.show();
    }

    public static void runUi(Runnable runnable) {
        mHandler.post(runnable);
    }
}
