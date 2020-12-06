package com.wumashi.ewmscan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.activity.CaptureActivity;
import com.utils.CommonUtil;

public class FormActivity extends AppCompatActivity {

    //打开扫描界面请求码
    private int REQUEST_CODE = 0x01;
    //扫描成功返回码
    private int RESULT_OK = 0xA1;
    private String TAG = "FormActivity";

    private TextView barcodeText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_main);
        Button btn = findViewById(R.id.buttonScan);
        barcodeText = findViewById(R.id.barcodeText);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtil.isCameraCanUse()) {
                    Intent intent = new Intent(FormActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(FormActivity.this, "请打开此应用的摄像头权限！", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "FormActivity onActivityResult");
        Log.d(TAG, "FormActivity onActivityResult requestCode " + requestCode);
        Log.d(TAG, "FormActivity onActivityResult resultCode " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (resultCode == RESULT_OK) { //RESULT_OK = -1
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("qr_scan_result");
            //将扫描出的信息显示出来
            barcodeText.setText("扫码结果: " + scanResult);
        } else {
            barcodeText.setText("扫码回调");
        }
    }
}
