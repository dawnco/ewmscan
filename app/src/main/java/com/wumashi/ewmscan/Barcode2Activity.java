package com.wumashi.ewmscan;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;

public class Barcode2Activity extends AppCompatActivity implements SurfaceHolder.Callback {

    private ImageView imgView;
    private View centerView;
    private TextView txtScanResult;
    private Timer mTimer;

    final static String TAG = "Barcode2Activity";

    private SurfaceView mSurfaceView = null;  // SurfaceView对象：(视图组件)视频显示
    private SurfaceHolder mSurfaceHolder = null;  // SurfaceHolder对象：(抽象接口)SurfaceView支持类

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcode_main);
        this.setTitle("条码/二维码识别");
        imgView = (ImageView) this.findViewById(R.id.ImageView01);
        centerView = (View) this.findViewById(R.id.centerView);
        txtScanResult = (TextView) this.findViewById(R.id.txtScanResult);
        txtScanResult.setBackgroundColor(android.graphics.Color.rgb(255, 255, 255));

        mTimer = new Timer();
        initSurfaceView();
        initCamera();
    }

    // InitSurfaceView
    private void initSurfaceView() {
        mSurfaceView = (SurfaceView) this.findViewById(R.id.sfvCamera);
        mSurfaceHolder = mSurfaceView.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
    }

    private void initCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                Log.d(TAG, "cameraId " + cameraId);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                isHardwareSupported(characteristics);
            }
//
//            manager.openCamera();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // CameraCharacteristics  可通过 CameraManager.getCameraCharacteristics() 获取
    private int isHardwareSupported(CameraCharacteristics characteristics) {
        Integer deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == null) {
            Log.e(TAG, "can not get INFO_SUPPORTED_HARDWARE_LEVEL");
            return -1;
        }
        switch (deviceLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                Log.w(TAG, "hardware supported level:LEVEL_FULL");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                Log.w(TAG, "hardware supported level:LEVEL_LEGACY");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                Log.w(TAG, "hardware supported level:LEVEL_3");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                Log.w(TAG, "hardware supported level:LEVEL_LIMITED");
                break;
        }
        return deviceLevel;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }
}
