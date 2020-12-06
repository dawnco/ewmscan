package com.wumashi.ewmscan;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class BarcodeActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    /**
     * Called when the activity is first created.
     */
    private ImageView imgView;
    private View centerView;
    private TextView txtScanResult;
    private Timer mTimer;
    private MyTimerTask mTimerTask;
    // 按照标准HVGA
    final static int width = 480;
    final static int height = 320;
    final static String TAG = "BarcodeActivity";

    int dstLeft, dstTop, dstWidth, dstHeight;

    private SurfaceView mSurfaceView = null;  // SurfaceView对象：(视图组件)视频显示
    private SurfaceHolder mSurfaceHolder = null;  // SurfaceHolder对象：(抽象接口)SurfaceView支持类
    private Camera mCamera = null;     // Camera对象，相机预览

    private int previewWidth;
    private int previewHeight;

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
    }

    // InitSurfaceView
    private void initSurfaceView() {
        mSurfaceView = (SurfaceView) this.findViewById(R.id.sfvCamera);
        mSurfaceHolder = mSurfaceView.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
    }

    protected void autoFocus() {
        Log.d("camera", "自动执行了");
        if (mCamera != null) {
            try {
                mCamera.cancelAutoFocus();
                mCamera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
//                            Log.d("camera", "对焦成功");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("camera", "对焦失败 " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

        Log.d("camera", "surfaceCreated");
        mCamera = Camera.open();// 开启摄像头（2.3版本后支持多摄像头,需传入参数）


        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();

            Camera.Size size = mCamera.getParameters().getPreviewSize();
            previewWidth = size.width;
            previewHeight = size.height;
            Log.d(TAG, "预览尺寸 " + previewWidth + " x " + previewHeight);

            mCamera.addCallbackBuffer(new byte[((previewWidth * previewHeight) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);

            mCamera.setPreviewCallbackWithBuffer(previewCallback);

            // 初始化定时器
            mTimerTask = new MyTimerTask();
            mTimer.schedule(mTimerTask, 0, 1000);

        } catch (Exception ex) {
            if (null != mCamera) {
                mCamera.release();
                mCamera = null;
            }
            Log.d("camera", "surfaceCreated Exception " + ex.getMessage());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(this.getLocalClassName(), "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(this.getLocalClassName(), "surfaceDestroyed");
        mTimer.cancel();
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            if (dstLeft == 0) {

                WindowManager manager = BarcodeActivity.this.getWindowManager();
                DisplayMetrics outMetrics = new DisplayMetrics();
                manager.getDefaultDisplay().getMetrics(outMetrics);
                int oWidth = outMetrics.widthPixels;
                int oHeight = outMetrics.heightPixels;


                //只赋值一次
                dstLeft = centerView.getLeft() * width
                        / oWidth;
                dstTop = centerView.getTop() * height
                        / oHeight;
                dstWidth = (centerView.getRight() - centerView.getLeft()) * width
                        / oWidth;
                dstHeight = (centerView.getBottom() - centerView.getTop()) * height
                        / oHeight;
                Log.d("camera bonus", " dstTop " + dstTop +
                        " dstLeft " + dstLeft +
                        " dstWidth " + dstWidth +
                        " dstHeight " + dstHeight);
            }

            BarcodeActivity.this.autoFocus();
        }
    }

    /**
     * 自动对焦后输出图片
     */
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera c) {


            Log.d("camera", "previewCallback");
            // 默认是  NV21.NV21

            BarcodeActivity.this.initFromCameraParameters(c, BarcodeActivity.this.getApplicationContext());

            Log.d(TAG, "byte[] data w:h " + previewWidth + " " + previewHeight);

            Bitmap bitmap = toBitmap(data, previewWidth, previewHeight);

            try {
                Result result = decodeQR(bitmap);
                String strResult = "BarcodeFormat:"
                        + result.getBarcodeFormat().toString() + "  text:"
                        + result.getText();
                txtScanResult.setText(strResult);
            } catch (Exception e) {
//                Log.d(TAG, "识别错误 " + e.getMessage());
                txtScanResult.setText("识别失败");
            }

            c.addCallbackBuffer(data);

        }
    };

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(Camera camera, Context context) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int displayRotation = display.getRotation();
        int cwRotationFromNaturalToDisplay;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                cwRotationFromNaturalToDisplay = 0;
                break;
            case Surface.ROTATION_90:
                cwRotationFromNaturalToDisplay = 90;
                break;
            case Surface.ROTATION_180:
                cwRotationFromNaturalToDisplay = 180;
                break;
            case Surface.ROTATION_270:
                cwRotationFromNaturalToDisplay = 270;
                break;
            default:
                // Have seen this return incorrect values like -90
                if (displayRotation % 90 == 0) {
                    cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
                } else {
                    throw new IllegalArgumentException("Bad rotation: " + displayRotation);
                }
        }
        Log.i(TAG, "Display at: " + cwRotationFromNaturalToDisplay);

    }


    protected Bitmap toBitmap(byte[] data, int width, int height) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(
                data,
                ImageFormat.NV21,
                width,
                height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);// 80--JPG图片的质量[0-100],100最高
        byte[] rawImage = baos.toByteArray();
        //将rawImage转换成bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        return bitmap;
    }

    protected com.google.zxing.Result decodeQR(Bitmap bitmap) {
        com.google.zxing.Result result = null;
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];

            Log.d(TAG, "bit map w:h " + width + " " + height);

            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            // 新建一个RGBLuminanceSource对象
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            // 将图片转换成二进制图片
            BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            QRCodeReader reader = new QRCodeReader();// 初始化解析对象
            try {
                Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
                result = reader.decode(binaryBitmap, hints);// 开始解析
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (ChecksumException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            throw new NullPointerException("识别失败");
        }

        return result;
    }
}
