package com.example.videostreamingapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button startBtn, stopBtn;
    private EditText ipInput, portInput;
    private TextView statusText;

    private Socket socket;
    private OutputStream outputStream;
    private boolean isStreaming = false;

    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);
        ipInput = findViewById(R.id.ipInput);
        portInput = findViewById(R.id.portInput);
        statusText = findViewById(R.id.statusText);

        startBtn.setOnClickListener(v -> startStreaming());
        stopBtn.setOnClickListener(v -> stopStreaming());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        }
    }

    //  CAMERA
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {

                    try {
                        if (isStreaming && outputStream != null) {

                            Bitmap bitmap = previewView.getBitmap();

                            if (bitmap != null) {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, out);

                                byte[] data = out.toByteArray();

                                byte[] size = ByteBuffer.allocate(4).putInt(data.length).array();

                                outputStream.write(size);
                                outputStream.write(data);
                                outputStream.flush();
                            }
                        }

                        Thread.sleep(1000); // slow = stable

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    image.close();
                });

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ================= START =================
    private void startStreaming() {
        String ip = ipInput.getText().toString().trim();
        String portStr = portInput.getText().toString().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Enter IP & Port", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);

        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                outputStream = socket.getOutputStream();

                isStreaming = true;

                runOnUiThread(() -> {
                    statusText.setText("● Connected");
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    statusText.setText("● Failed");
                });
            }
        }).start();
    }

    // ================= STOP =================
    private void stopStreaming() {
        isStreaming = false;

        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        statusText.setText("● Stopped");
    }

    // ================= PERMISSION =================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}