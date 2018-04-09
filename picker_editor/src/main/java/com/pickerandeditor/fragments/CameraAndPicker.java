package com.pickerandeditor.fragments;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ThemedSpinnerAdapter;
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
import android.widget.TextView;
import android.widget.Toast;

import com.pickerandeditor.BaseActivity;
import com.pickerandeditor.R;
import com.pickerandeditor.adapters.ImageAdapter;
import com.pickerandeditor.modelclasses.ImageModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class CameraAndPicker extends Fragment implements SurfaceHolder.Callback, View.OnClickListener, ImageAdapter.OnItemClick {

    private static final String TAG = "CameraAndPicker";
    private static final int MEDIA_TYPE_VIDEO = 1;
    private static final int MEDIA_TYPE_IMAGE = 2;
    private SurfaceView cameraPreview;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ImageView takeBTN,nextIV;
    private TextView selectionCount;
    private ImageView changeCameraBTN;
    private RecyclerView imagesRV;

    private AudioManager audioManager;
    private boolean isCameraConfigured = false, isPreview = false, isRecording = false;
    private MediaRecorder mediaRecorder;
    private File outputFile;
    private int numOfCameras = 0;
    private int currentCameraID;
    private Handler videoHandler;
    private Boolean isRecordVideo = false;
    private ImageAdapter adapter;
    private CountDownTimer countDownTimer;
    private int count = 0;


    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View view = inflater.inflate(R.layout.camera_picker, null);

        changeCameraBTN = view.findViewById(R.id.changeCameraBTN);
        imagesRV = view.findViewById(R.id.imagesRV);

        videoHandler = new Handler();

        cameraPreview = view.findViewById(R.id.cameraPreview);
        takeBTN = view.findViewById(R.id.takeBTN);
        nextIV = view.findViewById(R.id.nextIV);
        selectionCount = view.findViewById(R.id.selectionCount);
        surfaceHolder = cameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        camera = getCameraInstance();
        changeCameraBTN.setOnClickListener(this);
        nextIV.setOnClickListener(this);


        setCameraIDs();
        setTakeBTNTouchListener();
        setRecyclerView();
        getAllShownImagesPath();
        setNextVisibilityAndText();
        return view;
    }

    private void setRecyclerView() {
        imagesRV.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new ImageAdapter(getActivity(), BaseActivity.imageModels,this);
        imagesRV.setAdapter(adapter);
    }

    private void setCameraIDs() {
        numOfCameras = Camera.getNumberOfCameras();
        if (numOfCameras > 1) {
            currentCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            changeCameraBTN.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initPreview();
        setCameraDisplayOrientation(currentCameraID, camera);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size optiSize = getBestPreviewSize(720, 720);
        if (optiSize != null) {
            parameters.setPreviewSize(optiSize.width, optiSize.height);
            parameters.setPictureSize(optiSize.width, optiSize.height);
        }
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(parameters);
        setCameraDisplayOrientation(currentCameraID,camera);
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void startPreview() {
        if (isCameraConfigured && camera != null) {
            camera.startPreview();
            isPreview = true;
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
            mediaFile = new File(mediaStorageDir.getPath(), "IMG_" + timeStamp + ".jpeg");
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
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
                isCameraConfigured = true;
            }
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height) {
        List<Camera.Size> sizes = camera.getParameters()
                .getSupportedPreviewSizes();
        if (sizes == null)
            return null;
        Camera.Size optimalSize = null;
        int tmpSize;

        int minWidthDiff = 1000;
        for (Camera.Size size : sizes) {
            if (size.width > size.height)
                tmpSize = size.height;
            else
                tmpSize = size.width;
            if (Math.abs(tmpSize - width) < minWidthDiff) {
                minWidthDiff = Math.abs(tmpSize - width);
                optimalSize = size;
            }
        }
        return optimalSize;
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
            isRecording = false;
            if (outputFile.length()>0){
                addClickedVideoTOList(outputFile.getAbsolutePath());
            }
        }
        Log.d(TAG, "File Name: " + outputFile.getAbsolutePath() + " \n Size: " + outputFile.length());
    }

    private boolean prepareVideoRecorder() {
        if (camera == null) {
            return false;
        }
        mediaRecorder = new MediaRecorder();
        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setOrientationHint(getOrientations(currentCameraID));
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
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(result);
        camera.setParameters(parameters);
    }

    public int getOrientations(int cameraId) {
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
        return result;
    }

    private void setTakeBTNTouchListener() {
        takeBTN.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isPreview) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        takeBTN.setImageResource(R.drawable.camera_btn_pressed_bg);
                        videoHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isRecordVideo = true;
                                Toast.makeText(getActivity(), "START RECORDING", Toast.LENGTH_SHORT).show();
                                startRecording();
                            }
                        }, 1000);
                        return true;
                    case MotionEvent.ACTION_UP:
                        takeBTN.setImageResource(R.drawable.cemera_btn_bg);
                        if (isRecordVideo) {
                            stopRecording();
                            Toast.makeText(getActivity(), "STOP RECORDING", Toast.LENGTH_SHORT).show();
                        } else {
                            takePicture();
                            Toast.makeText(getActivity(), "Image Clicked", Toast.LENGTH_SHORT).show();
                        }
                        isRecordVideo = false;
                        videoHandler.removeCallbacksAndMessages(null);
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

    private void startRecording() {
        if (prepareVideoRecorder()) {
            mediaRecorder.start();
            isRecording = true;
            countDownTimer = new CountDownTimer(30000,1000) {
                @Override
                public void onTick(long l) {
                    count ++;

                }

                @Override
                public void onFinish() {

                }
            }.start();

        } else {
            Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.changeCameraBTN) {
            switchCameraAction();
        }else if (v.getId() == R.id.nextIV){
            addEditorFragment();
            Toast.makeText(getActivity(), "Next Clicked", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchCameraAction() {
        if (isPreview) {
            camera.stopPreview();
            camera.release();
            isPreview = false;
        }
        if (currentCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            currentCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            currentCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        camera = getCameraInstance();
        if (camera == null) {
            return;
        }
        initPreview();
        startPreview();
        setCameraDisplayOrientation(currentCameraID, camera);

    }

    public void getAllShownImagesPath() {
        if (BaseActivity.imageModels.size() == 0) {
            Uri uri;
            Cursor cursor;
            int column_index_data, column_index_folder_name;
            String absolutePathOfImage = null;
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {MediaStore.MediaColumns.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            cursor = getActivity().getContentResolver().query(uri, projection, null,
                    null, null);

            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_folder_name = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);
                ImageModel model = new ImageModel();
                model.setPath(absolutePathOfImage);
                BaseActivity.imageModels.add(model);
            }
            Log.d(TAG, BaseActivity.imageModels.size() + "");
            Collections.reverse(BaseActivity.imageModels);
        }
        adapter.notifyDataSetChanged();
    }

    private void takePicture() {
        if (BaseActivity.selectedImagesList.size() == 3){
            Toast.makeText(getActivity(), "Limit exceed", Toast.LENGTH_SHORT).show();
            return;
        }
        setPictureSizeParams();
        camera.takePicture(null, null,null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                try {
                    File file = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                    FileOutputStream out = new FileOutputStream(file);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                    float rotation = rotationForImage(getActivity(), Uri.fromFile(file));
                    if (rotation != 0) {
                        //New rotation matrix
                        Matrix matrix2 = new Matrix();
                        matrix2.preRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                                matrix2, true);

                        out = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();
                    }
                    camera.startPreview();
                    addClickImageToList(file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addClickImageToList(String path){
        ImageModel model = new ImageModel();
        model.setPath(path);
        model.setSelected(true);
        model.setVideo(false);
        BaseActivity.imageModels.add(0,model);
        BaseActivity.selectedImagesList.add(model);
        adapter.notifyItemInserted(0);
        imagesRV.scrollToPosition(0);
        setNextVisibilityAndText();
    }

    private void addClickedVideoTOList(String file){
        ImageModel model = new ImageModel();
        model.setPath(file);
        model.setSelected(true);
        model.setVideo(true);
        BaseActivity.imageModels.add(0,model);
        BaseActivity.selectedImagesList.add(model);
        adapter.notifyItemInserted(0);
        imagesRV.scrollToPosition(0);
        setNextVisibilityAndText();
    }

    private void setNextVisibilityAndText(){
        if (BaseActivity.selectedImagesList.size()>0){
            nextIV.setVisibility(View.VISIBLE);
            selectionCount.setVisibility(View.VISIBLE);
            selectionCount.setText(""+BaseActivity.selectedImagesList.size());
        }else {
            nextIV.setVisibility(View.GONE);
            selectionCount.setVisibility(View.GONE);
        }
    }
    private void setPictureSizeParams(){
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizeList = params.getSupportedPictureSizes();
        Camera.Size size = sizeList.get(0);
        for (int i = 0; i < sizeList.size(); i++) {
            if (sizeList.get(i).width > size.width)
                size = sizeList.get(i);
        }
        params.setPictureSize(size.width, size.height);
    }

    @Override
    public void onItemClickEvent(int position) {
        if (!BaseActivity.imageModels.get(position).getSelected() && BaseActivity.selectedImagesList.size() == 3){
            Toast.makeText(getActivity(), "Limit exceed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (BaseActivity.imageModels.get(position).getSelected()){
            BaseActivity.imageModels.get(position).setSelected(false);
            if(BaseActivity.selectedImagesList.contains(BaseActivity.imageModels.get(position))){
                BaseActivity.selectedImagesList.remove(BaseActivity.imageModels.get(position));
            }
        }else {
            BaseActivity.imageModels.get(position).setSelected(true);
            BaseActivity.selectedImagesList.add(BaseActivity.imageModels.get(position));
        }
        Log.d(TAG,"Selection Size: "+BaseActivity.selectedImagesList.size());
        adapter.notifyItemChanged(position);
        setNextVisibilityAndText();
    }

    private void addEditorFragment(){
        Editor fragment = new Editor();
        ((BaseActivity)getActivity()).addFragment(fragment,"Editor");
    }

    public static float rotationForImage(Context context, Uri uri) {
        try {
            if (uri.getScheme().equals("content")) {
                //From the media gallery
                String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
                Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
                if (c.moveToFirst()) {
                    return c.getInt(0);
                }
            } else if (uri.getScheme().equals("file")) {
                //From a file saved by the camera
                ExifInterface exif = new ExifInterface(uri.getPath());
                int rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(ExifInterface
                        .TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface
                        .ORIENTATION_NORMAL);
                exif.saveAttributes();
                return rotation;
            }
            return 0;

        } catch (IOException e) {
            Log.e(TAG, "Error checking exit", e);
            return 0;
        }
    }

    private static float exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
}
