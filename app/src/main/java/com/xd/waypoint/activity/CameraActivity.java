package com.xd.waypoint.activity;

import androidx.annotation.NonNull;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.xd.waypoint.R;
import com.xd.waypoint.fragment.DJIDemoApplication;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class CameraActivity extends BaseActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG=CameraActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener=null;

    protected DJICodecManager mCodecManager=null;
    protected TextureView mVideoSurface=null;
    private Button mPhotoBtn,mShootPhotoModeBtn,mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    private Handler handler;

    public static void start(Context context){
        Intent intent=new Intent(context,CameraActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        handler=new Handler();
        initUI();

        mReceivedVideoDataListener=new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager!=null){
                    mCodecManager.sendDataToDecoder(videoBuffer,size);
                }
            }
        };

        Camera camera= DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            camera.setSystemStateCallback(new SystemState.Callback(){

                @Override
                public void onUpdate(@NonNull SystemState cameraSystemState) {
                    if(null!=cameraSystemState){
                        int recordTime=cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes=(recordTime%3600)/60;
                        int seconds=recordTime%60;

                        final String timeString=String.format("%02d:%02d",minutes,seconds);
                        final boolean isVideoRecording=cameraSystemState.isRecording();

                        CameraActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recordingTime.setText(timeString);

                                if(isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else{
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });
        }
    }


    private void initUI(){
        mVideoSurface=findViewById(R.id.video_previewer_surface);
        recordingTime=findViewById(R.id.timer);
        mPhotoBtn=findViewById(R.id.btn_photo);
        mRecordBtn=findViewById(R.id.btn_record);
        mShootPhotoModeBtn=findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn=findViewById(R.id.btn_record_video_mode);
        mPhotoBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked){
                if(isChecked){
                     recordingTime.setVisibility(View.VISIBLE);
                     startRecord();
                }else{
                    recordingTime.setVisibility(View.INVISIBLE);
                     stopRecord();
                }
            }
        });
    }

    protected void onProductChange(){
        initPreviewer();
    }

    @Override
    public void onResume(){
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();
        if (mVideoSurface==null){
            Log.e(TAG, "mVideoSurface is null" );
        }
    }
    private void initPreviewer(){
        BaseProduct product=DJIDemoApplication.getProductInstance();
        if(product==null||!product.isConnected()){
            showToast("未连接");
        }else{
            if(null!=mVideoSurface){
                //绑定SurfaceTextureListener
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if(!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)){

                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer(){
        Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if(mCodecManager==null){
            //创建解码器 绑定surface,宽高等
            mCodecManager=new DJICodecManager(this,surface,width,height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        Log.e(TAG, "onSurfaceTextureSizeChanged" );
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        Log.e(TAG, "onSurfaceTextureDestroyed" );
        if (mCodecManager!=null){
            mCodecManager.cleanSurface();
            mCodecManager=null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.btn_photo:
                captureAction();
                break;
            case R.id.btn_shoot_photo_mode:
                if(isMavicAir2()||isM300()){
                    switchCameraFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE);
                }else{
                    switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                }
                break;
            case R.id.btn_record_video_mode:
                if(isMavicAir2()||isM300()){
                    switchCameraFlatMode(SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL);
                }else{
                    switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                }
                break;
            default:
                break;
        }
    }

    private void switchCameraFlatMode(SettingsDefinitions.FlatCameraMode flatCameraMode){
        Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            camera.setFlatMode(flatCameraMode,error->{
                if(error==null){
                    showToast("相机模式切换成功");
                }else{
                    showToast(error.getDescription());
                }
            });
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){
        Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            camera.setMode(cameraMode,error->{
                if(error==null){
                    showToast("相机模式切换成功");
                }else{
                    showToast(error.getDescription());
                }
            });
        }
    }
    private void captureAction(){
        final Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            if(isMavicAir2()||isM300()){
                camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE,djiError -> {
                    if(null==djiError){
                        takePhoto();
                    }
                });
            }else{
                camera.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE,djiError -> {
                    if(null==djiError){
                        takePhoto();
                    }
                });
            }
        }
    }

    private void takePhoto(){
        final Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera==null){
            showToast("未获取到相机");
            return;
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                camera.startShootPhoto(djiError -> {
                    if(djiError==null){
                        showToast("拍摄成功");
                    }else{
                        showToast(djiError.getDescription());
                    }
                });
            }
        },2000);
    }

    private void startRecord(){
        final Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            camera.startRecordVideo(djiError -> {
                if(djiError==null){
                    showToast("录像成功");
                }else{
                    showToast(djiError.getDescription());
                }
            });
        }
    }

    private void stopRecord(){
        Camera camera=DJIDemoApplication.getCameraInstance();
        if(camera!=null){
            camera.stopRecordVideo(djiError -> {
                if(djiError==null){
                    showToast("停止录制成功");
                }else{
                    showToast(djiError.getDescription());
                }
            });
        }
    }

    private boolean isMavicAir2(){
        BaseProduct baseProduct=DJIDemoApplication.getProductInstance();
        if(baseProduct!=null){
            return baseProduct.getModel()==Model.MAVIC_AIR_2;
        }
        return false;
    }

    private boolean isM300(){
        BaseProduct baseProduct=DJIDemoApplication.getProductInstance();
        if (baseProduct!=null){
            return baseProduct.getModel()== Model.MATRICE_300_RTK;
        }
        return false;
    }
}