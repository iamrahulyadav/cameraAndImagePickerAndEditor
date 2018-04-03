package com.pickerandeditor.fragments;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.pickerandeditor.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;


public class CameraAndPicker extends Fragment implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = "CameraAndPicker";
    private static final int MEDIA_TYPE_VIDEO = 1;
    private static final int MEDIA_TYPE_IMAGE = 2;
    private SurfaceView cameraPreview;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ImageView takeBTN;
    private ImageView changeCameraBTN;

    private AudioManager audioManager;
    private boolean isCameraConfigured = false, isPreview = false, isRecording = false;
    private MediaRecorder mediaRecorder;
    private File outputFile;
    private int numOfCameras = 0;
    private int currentCameraID;


    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View view = inflater.inflate(R.layout.camera_picker, null);

        cameraPreview = view.findViewById(R.id.cameraPreview);
        takeBTN = view.findViewById(R.id.takeBTN);
        changeCameraBTN = view.findViewById(R.id.changeCameraBTN);
        surfaceHolder = cameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        setCameraIDs();
        camera = getCameraInstance();
        changeCameraBTN.setOnClickListener(this);
        return view;
    }

    private void setCameraIDs(){
        numOfCameras = Camera.getNumberOfCameras();
        if (numOfCameras>1){
            currentCameraID = 1;
            changeCameraBTN.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void startPreview() {
        if (isCameraConfigured && camera != null) {
            camera.startPreview();
            isPreview = true;
            setTakeBTNTouchListener();
        }
    }

    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "MyVideoApps");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = null;
        if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath(), "VID_" + timeStamp + ".mp4");
        } else if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath(), "IMG_" + timeStamp + ".png");
        }
        return mediaFile;
    }

    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(currentCameraID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private void initPreview() {
        if (camera != null && surfaceHolder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!isCameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewFpsRange(30000, 30000);
                camera.setParameters(parameters);
                setCameraDisplayOrientation(0,camera);
                isCameraConfigured = true;
            }
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    protected void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.stopPreview();
            releaseCamera();
            isRecording = false;
        }
    }

    private boolean prepareVideoRecorder() {

        if (camera != null) {
            camera.release();
        }
        camera = getCameraInstance();
        if (camera == null) {
            return false;
        }
        mediaRecorder = new MediaRecorder();

        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

        outputFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        mediaRecorder.setOutputFile(outputFile.toString());
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 180;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void setTakeBTNTouchListener(){
        takeBTN.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isPreview) {
                    return false;
                }
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        takeBTN.setImageResource(R.drawable.camera_btn_pressed_bg);
                        return true;
                    case MotionEvent.ACTION_UP:
                        takeBTN.setImageResource(R.drawable.cemera_btn_bg);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseCamera();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.changeCameraBTN){
            switchCameraAction();
        }
    }

    private void switchCameraAction(){
        if (isPreview){
            camera.stopPreview();
            camera.release();
            isPreview = false;
            camera = null;
        }
        camera = getCameraInstance();
        if (camera == null){
            return;
        }
        if(currentCameraID == Camera.CameraInfo.CAMERA_FACING_BACK){
            currentCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else {
            currentCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        startPreview();
    }

}
